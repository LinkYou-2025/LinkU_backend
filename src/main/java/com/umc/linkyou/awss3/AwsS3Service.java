package com.umc.linkyou.awss3;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    private final AwsProperties awsProperties;
    private final S3Client s3Client;  // SDK v2

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

                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(awsProperties.s3().bucket())
                        .key(fileName)
                        .contentType("image/jpeg")
                        .contentLength((long) resizedBytes.length)
                        .build();

                try {
                    s3Client.putObject(request, RequestBody.fromBytes(resizedBytes));
                } catch (SdkException e) {
                    log.error("S3 업로드 실패: {}", fileName, e);
                    throw new GeneralException(ErrorStatus._S3_UPLOAD_FAILED);
                }
            }
        } catch (IOException e) {
            log.error("업로드 IO 실패: {}", fileName, e);
            throw new GeneralException(ErrorStatus._S3_UPLOAD_FAILED);
        }

        return getFileUrl(fileName);
    }

    public String uploadFile(MultipartFile multipartFile, String folder) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new GeneralException(ErrorStatus._S3_FILE_EMPTY);
        }

        String fileName = folder + "/" + createFileName(multipartFile.getOriginalFilename());
        try (InputStream originalStream = multipartFile.getInputStream()) {
            byte[] originalBytes = readToByteArray(originalStream);
            validateImageBytes(originalBytes);

            try (InputStream resizedStream = new ByteArrayInputStream(originalBytes)) {
                byte[] resizedBytes = processImage(resizedStream);

                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(awsProperties.s3().bucket())
                        .key(fileName)
                        .contentType("image/jpeg")
                        .contentLength((long) resizedBytes.length)
                        .build();

                try {
                    s3Client.putObject(request, RequestBody.fromBytes(resizedBytes));
                } catch (SdkException e) {
                    log.error("S3 폴더 업로드 실패: {}", fileName, e);
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
     * 기존 이미지를 새 이미지로 교체한다.
     * 새 이미지를 먼저 업로드해 성공을 확인한 뒤 기존 이미지를 삭제한다. (업로드 실패 시 기존 이미지를 보존하기 위함)
     * 기존 이미지 삭제가 실패해도 새 이미지는 이미 정상 반영된 상태이므로 예외를 전파하지 않고 로그만 남긴다.
     * "이미지 수정" 성격의 도메인(예: Domain, UsersLinku)에서 공통으로 사용한다.
     */
    public String replaceFile(String oldFileUrl, MultipartFile newFile, String folder) {
        String newFileUrl = uploadFile(newFile, folder);
        if (oldFileUrl != null) {
            try {
                deleteFileByUrl(oldFileUrl);
            } catch (GeneralException e) {
                log.error("기존 이미지 삭제 실패 (새 이미지는 정상 반영됨): {}", oldFileUrl, e);
            }
        }
        return newFileUrl;
    }

    public void deleteFile(String fileName) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(awsProperties.s3().bucket())
                    .key(fileName)
                    .build();
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            throw new GeneralException(ErrorStatus._S3_DELETE_FAILED);
        }
    }

    public void deleteFileByUrl(String fileUrl) {
        String fileName = extractFileNameFromUrl(fileUrl);
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(awsProperties.s3().bucket())
                    .key(fileName)
                    .build();
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            throw new GeneralException(ErrorStatus._S3_DELETE_FAILED);
        }
    }

    public String getFileUrl(String fileName) {
        String domain = awsProperties.cloudfront().domain().trim();
        domain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        String key = fileName.startsWith("/") ? fileName.substring(1) : fileName;
        return domain + "/" + key;
    }

    public String extractFileNameFromUrl(String url) {
        try {
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
            String domainSuffix = awsProperties.cloudfront().domain().endsWith("/")
                    ? awsProperties.cloudfront().domain()
                    : awsProperties.cloudfront().domain() + "/";
            if (decodedUrl.startsWith(domainSuffix)) {
                return decodedUrl.substring(domainSuffix.length());
            }
            String bucketName = awsProperties.s3().bucket();
            int bucketIndex = decodedUrl.indexOf(bucketName);
            if (bucketIndex == -1) {
                throw new GeneralException(ErrorStatus._S3_EXTRACT_URL_FAILED);
            }
            return decodedUrl.substring(bucketIndex + bucketName.length() + 1);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus._S3_EXTRACT_URL_FAILED);
        }
    }

    private String createFileName(String originalName) {
        return UUID.randomUUID().toString() + getFileExtension(originalName);
    }

    private String getFileExtension(String fileName) {
        try {
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            throw new GeneralException(ErrorStatus._S3_INVALID_FILE);
        }
    }

    private byte[] processImage(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(inputStream)
                .size(800, 800)
                .outputQuality(0.85)
                .outputFormat("jpg")
                .toOutputStream(output);
        return output.toByteArray();
    }

    private byte[] readToByteArray(InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }

    private void validateImageBytes(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw new GeneralException(ErrorStatus._S3_INVALID_IMAGE);
            log.debug("이미지 검증 성공: {}x{}", image.getWidth(), image.getHeight());
        } catch (IOException e) {
            log.error("이미지 파싱 실패", e);
            throw new GeneralException(ErrorStatus._S3_INVALID_IMAGE);
        } catch (GeneralException e) {
            throw e;
        }
    }
}
