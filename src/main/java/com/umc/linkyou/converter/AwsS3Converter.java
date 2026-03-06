package com.umc.linkyou.converter;

import com.umc.linkyou.awss3.AwsS3Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public class AwsS3Converter {

    // 이미지 파일이 null이 아닌 경우에만 S3로 업로드한 URL을 리턴
    public static String toImageUrl(MultipartFile image, AwsS3Service awsS3Service) {
        return Optional.ofNullable(image)
                .filter(f -> !f.isEmpty())
                .map(awsS3Service::uploadFile)
                .orElse(null);
    }
}
