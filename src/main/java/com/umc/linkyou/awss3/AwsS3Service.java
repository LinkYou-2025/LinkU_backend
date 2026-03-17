package com.umc.linkyou.awss3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final AwsProperties awsProperties;

    private final AmazonS3 amazonS3;

    /**
     * S3 파일 업로드 및 URL 반환
     */
    public String uploadFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new GeneralException(ErrorStatus._S3_FILE_EMPTY);
        }

        String fileName = createFileName(multipartFile.getOriginalFilename());
        try (InputStream originalStream = multipartFile.getInputStream()) {
            byte[] originalBytes = readToByteArray(originalStream);
            validateImageBytes(originalBytes);

            try (InputStream resizedStream = new ByteArrayInputStream(originalBytes)) {
                byte[] resizedBytes = processImage(resizedStream);

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(resizedBytes.length);
                metadata.setContentType("image/jpeg");

                // 🔥 컴파일 에러 해결: 단일 AmazonClientException catch
                try {
                    amazonS3.putObject(new PutObjectRequest(awsProperties.getS3().getBucket(),  fileName,
                            new ByteArrayInputStream(resizedBytes), metadata));
                } catch (AmazonClientException e) {
                    log.error("S3 업로드 실패 (클라이언트 오류): {}", fileName, e);
                    throw new GeneralException(ErrorStatus._S3_UPLOAD_FAILED);
                }
            }
        } catch (IOException e) {
            log.error("업로드 IO 실패: {}", fileName, e);
            throw new GeneralException(ErrorStatus._S3_UPLOAD_FAILED);
        }

        return getFileUrl(fileName);
    }

    /**
     * 폴더 업로드 메서드 (두 번째 오버로드 - 완전 수정)
     */
    public String uploadFile(MultipartFile multipartFile, String folder) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new GeneralException(ErrorStatus._S3_FILE_EMPTY);
        }

        String fileName = folder + "/" + createFileName(multipartFile.getOriginalFilename());
        try (InputStream originalStream = multipartFile.getInputStream()) {
            // 바이트 검증
            byte[] originalBytes = readToByteArray(originalStream);
            validateImageBytes(originalBytes);

            // 리사이징 (자동 close)
            try (InputStream resizedStream = new ByteArrayInputStream(originalBytes)) {
                byte[] resizedBytes = processImage(resizedStream);

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(resizedBytes.length);
                metadata.setContentType("image/jpeg");

                //  AmazonClientException 단일 catch
                try {
                    amazonS3.putObject(new PutObjectRequest(awsProperties.getS3().getBucket(),  fileName,
                            new ByteArrayInputStream(resizedBytes), metadata));
                } catch (AmazonClientException e) {
                    log.error("S3 폴더 업로드 실패 (클라이언트 오류): {}", fileName, e);
                    throw new GeneralException(ErrorStatus._S3_UPLOAD_FAILED);
                }
            }
        } catch (IOException e) {
            log.error("폴더 업로드 IO 실패: {}", fileName, e);
            throw new GeneralException(ErrorStatus._S3_UPLOAD_FAILED);
        }

        return getFileUrl(fileName);
    }



    /**
     * 파일명 난수화 + 확장자 유지
     */
    private String createFileName(String originalName) {
        return UUID.randomUUID().toString() + getFileExtension(originalName);
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        try {
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            throw new GeneralException(ErrorStatus._S3_INVALID_FILE);
        }
    }

    /**
     * S3에 업로드된 파일의 URL 반환
     */
    public String getFileUrl(String fileName) {
        String domain = awsProperties.getCloudfront().getDomain().trim();
        domain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        String key = fileName.startsWith("/") ? fileName.substring(1) : fileName;
        return domain + "/" + key;
    }
    /**
     * 파일명 기준으로 파일 삭제
     */
    public void deleteFile(String fileName) {
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(awsProperties.getS3().getBucket(), fileName));
        } catch  (AmazonClientException e) {
            throw new GeneralException(ErrorStatus._S3_DELETE_FAILED);
        }
    }

    /**
     * URL 기준으로 파일 삭제
     */
    public void deleteFileByUrl(String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            amazonS3.deleteObject(new DeleteObjectRequest(awsProperties.getS3().getBucket(), fileName));
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus._S3_DELETE_FAILED);
        }
    }

    /**
     * URL에서 파일명 추출
     */
    public String extractFileNameFromUrl(String url) {
        try {
            String decodedUrl = URLDecoder.decode(url, "UTF-8");
            // CloudFront 도메인 이후 전체 경로 반환 (= S3 키)
            String domainSuffix = awsProperties.getCloudfront().getDomain().endsWith("/")  // ← 교체
                    ? awsProperties.getCloudfront().getDomain()
                    : awsProperties.getCloudfront().getDomain() + "/";
            if (decodedUrl.startsWith(domainSuffix)) {
                return decodedUrl.substring(domainSuffix.length());
            }
            // 기존 S3 URL 호환
            String bucketName = awsProperties.getS3().getBucket();  // ← bucket 교체
            return decodedUrl.substring(decodedUrl.indexOf(bucketName) + bucketName.length() + 1);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus._S3_EXTRACT_URL_FAILED);
        }
    }

    /**
     * 이미지 리사이징 공통 처리 (최대 800x800, 비율 유지)
     */
    private byte[] processImage(InputStream inputStream) throws IOException {
        // contentType 검증 제거 (validateImageBytes에서 이미 완료)
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(inputStream)
                .size(800, 800)
                .outputQuality(0.85)
                .outputFormat("jpg")
                .toOutputStream(output);
        return output.toByteArray();
    }

    // InputStream을 byte[]로 변환
    private byte[] readToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384]; // 16KB 버퍼

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    // 실제 바이트로 이미지 검증 (contentType 무시)
    private void validateImageBytes(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new GeneralException(ErrorStatus._S3_INVALID_IMAGE);
            }
            log.debug("이미지 검증 성공: {}x{}", image.getWidth(), image.getHeight());
        } catch (IOException e) {
            // IOException만 catch → 명확한 이미지 읽기 실패
            log.error("이미지 파싱 실패", e);
            throw new GeneralException(ErrorStatus._S3_INVALID_IMAGE);
        } catch (GeneralException e) {
            // 이미 GeneralException이면 그대로 re-throw (재포장 방지)
            throw e;
        }
    }

}
