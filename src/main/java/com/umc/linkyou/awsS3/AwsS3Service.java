package com.umc.linkyou.awsS3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudfrontDomain;

    private final AmazonS3 amazonS3;

    /**
     * S3 파일 업로드 및 URL 반환
     */
    public String uploadFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new GeneralException(ErrorStatus._S3_FILE_EMPTY);
        }

        String fileName = createFileName(multipartFile.getOriginalFilename());
        try (InputStream inputStream = multipartFile.getInputStream()) {
            String safeContentType = Optional.ofNullable(multipartFile.getContentType())
                    .orElse("image/jpeg");
            byte[] resizedBytes = processImage(inputStream, safeContentType);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(resizedBytes.length);
            metadata.setContentType("image/jpeg");

            amazonS3.putObject(new PutObjectRequest(bucket, fileName,
                    new ByteArrayInputStream(resizedBytes), metadata));
        } catch (IOException e) {
            log.error("업로드 실패: {}", fileName, e);
            throw new GeneralException(ErrorStatus._S3_UPLOAD_FAILED);
        }

        return getFileUrl(fileName);
    }

    public String uploadFile(MultipartFile multipartFile, String folder) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new GeneralException(ErrorStatus._S3_FILE_EMPTY);
        }

        String fileName = folder + "/" + createFileName(multipartFile.getOriginalFilename());
        try (InputStream inputStream = multipartFile.getInputStream()) {

            String safeContentType = Optional.ofNullable(multipartFile.getContentType())
                    .orElse("image/jpeg");
            byte[] resizedBytes = processImage(inputStream, safeContentType);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(resizedBytes.length);
            metadata.setContentType("image/jpeg");

            amazonS3.putObject(new PutObjectRequest(bucket, fileName,
                    new ByteArrayInputStream(resizedBytes), metadata));
        } catch (IOException e) {
            log.error("폴더 업로드 실패: {}", fileName, e);
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
        String domain = cloudfrontDomain.trim().endsWith("/")
                ? cloudfrontDomain.substring(0, cloudfrontDomain.length() - 1)
                : cloudfrontDomain;
        String key = fileName.startsWith("/") ? fileName.substring(1) : fileName;
        return domain + "/" + key;
    }

    /**
     * 파일명 기준으로 파일 삭제
     */
    public void deleteFile(String fileName) {
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
        } catch (AmazonServiceException e) {
            throw new GeneralException(ErrorStatus._S3_DELETE_FAILED);
        }
    }

    /**
     * URL 기준으로 파일 삭제
     */
    public void deleteFileByUrl(String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
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
            String domainSuffix = cloudfrontDomain.endsWith("/")
                    ? cloudfrontDomain
                    : cloudfrontDomain + "/";
            if (decodedUrl.startsWith(domainSuffix)) {
                return decodedUrl.substring(domainSuffix.length());
            }
            // 기존 S3 URL 호환
            return decodedUrl.substring(decodedUrl.indexOf(bucket) + bucket.length() + 1);
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus._S3_EXTRACT_URL_FAILED);
        }
    }

    /**
     * 이미지 리사이징 공통 처리 (최대 800x800, 비율 유지)
     */
    private byte[] processImage(InputStream inputStream, String contentType) throws IOException {
        if (!contentType.startsWith("image/")) {
            throw new GeneralException(ErrorStatus._S3_INVALID_IMAGE);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(inputStream)
                .size(800, 800)  // 긴 변 800px 이하, 비율 유지
                .outputQuality(0.85)
                .outputFormat("jpg")
                .toOutputStream(output);

        return output.toByteArray();
    }

}
