package com.umc.linkyou.awsS3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다.");
        }

        String fileName = createFileName(multipartFile.getOriginalFilename());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        try (InputStream inputStream = multipartFile.getInputStream()) {
            amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, metadata));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 파일 업로드 실패");
        }

        return getFileUrl(fileName);
    }

    public String uploadFile(MultipartFile multipartFile, String folder) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다.");
        }

        String fileName = folder + "/" + createFileName(multipartFile.getOriginalFilename());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        try (InputStream inputStream = multipartFile.getInputStream()) {
            amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, metadata));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 파일 업로드 실패");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일 형식: " + fileName);
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 파일 삭제 실패");
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 파일 삭제 실패");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL에서 파일명을 추출할 수 없습니다.");
        }
    }



}
