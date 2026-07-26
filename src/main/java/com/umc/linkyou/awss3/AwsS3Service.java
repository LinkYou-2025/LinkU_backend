package com.umc.linkyou.awss3;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.properties.AwsProperties;
import com.umc.linkyou.domain.Image;
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
public class AwsS3Service {

    private final AwsProperties awsProperties;
    private final S3Client s3Client;  // SDK v2
    private final String cloudFrontDomain;

    public AwsS3Service(AwsProperties awsProperties, S3Client s3Client) {
        this.awsProperties = awsProperties;
        this.s3Client = s3Client;
        this.cloudFrontDomain = normalizeDomain(awsProperties.cloudfront().domain());
    }

    private static String normalizeDomain(String domain) {
        String trimmed = domain.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

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

        return "/" + fileName;
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

        return "/" + fileName;
    }

    /**
     * 기존 이미지를 새 이미지로 교체한다.
     * 새 이미지를 먼저 업로드해 성공을 확인한 뒤 기존 이미지를 삭제한다. (업로드 실패 시 기존 이미지를 보존하기 위함)
     * 기존 이미지 삭제가 실패해도 새 이미지는 이미 정상 반영된 상태이므로 예외를 전파하지 않고 로그만 남긴다.
     * "이미지 수정" 성격의 도메인(예: Domain, UsersLinku)에서 공통으로 사용한다.
     *
     * @param oldKey 기존 이미지의 object key ("/"로 시작하는 상대 경로, 없으면 null)
     * @return 새로 업로드된 이미지의 object key ("/"로 시작하는 상대 경로)
     */
    public String replaceFile(String oldKey, MultipartFile newFile, String folder) {
        String newKey = uploadFile(newFile, folder);
        if (oldKey != null) {
            try {
                deleteFile(oldKey);
            } catch (GeneralException e) {
                log.error("기존 이미지 삭제 실패 (새 이미지는 정상 반영됨): {}", oldKey, e);
            }
        }
        return newKey;
    }

    /**
     * 이미지 엔티티가 가리키는 실제 파일을 S3에서 삭제한다. (Image row 자체는 지우지 않는다)
     */
    public void deleteFile(Image image) {
        if (image == null || !image.isS3()) return;
        deleteFile(image.getLocation());
    }

    public void deleteFile(String fileName) {
        String key = stripLeadingSlash(fileName);
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(awsProperties.s3().bucket())
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            throw new GeneralException(ErrorStatus._S3_DELETE_FAILED);
        }
    }

    /**
     * Image를 화면에 표시 가능한 URL로 변환한다.
     * S3 출처는 CloudFront 도메인을 붙여 URL을 만들고, EXTERNAL 출처는 저장된 값을 그대로 반환한다.
     * (서명된 URL/만료 시간 발급은 아직 도입 전이며, 추후 별도 작업으로 대체될 지점이다.)
     */
    public String resolveUrl(Image image) {
        if (image == null) return null;
        return image.isS3() ? getFileUrl(image.getLocation()) : image.getLocation();
    }

    private String stripLeadingSlash(String key) {
        if (key == null) return null;
        return key.startsWith("/") ? key.substring(1) : key;
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
        String key = fileName.startsWith("/") ? fileName.substring(1) : fileName;
        return cloudFrontDomain + "/" + key;
    }

    public String extractFileNameFromUrl(String url) {
        try {
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
            String domainSuffix = cloudFrontDomain + "/";
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
