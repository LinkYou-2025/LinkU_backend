package com.umc.linkyou.awss3.controller.admin;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.validation.annotation.ApiAdmin;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@ApiAdmin
@ApiV1
@RestController("awsS3AdminController")
@RequiredArgsConstructor
@RequestMapping("/s3")
public class AwsS3Controller {

    private final AwsS3Service awsS3Service;

    // 파일 업로드
    @ApiErrorCode(errorStatus = {ErrorStatus._S3_FILE_EMPTY, ErrorStatus._S3_INVALID_IMAGE, ErrorStatus._S3_UPLOAD_FAILED})
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile multipartFile) {
        String fileUrl = awsS3Service.uploadFile(multipartFile);
        return ResponseEntity.ok(fileUrl);
    }


}
