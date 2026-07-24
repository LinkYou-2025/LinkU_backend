package com.umc.linkyou.awss3.controller.manager;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.validation.annotation.ApiManager;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@ApiManager
@ApiV1
@RestController("awsS3ManagerController")
@RequiredArgsConstructor
@RequestMapping("/s3")
public class AwsS3Controller {

    private final AwsS3Service awsS3Service;

    // 파일 삭제
    @ApiErrorCode(errorStatus = {ErrorStatus._S3_EXTRACT_URL_FAILED, ErrorStatus._S3_DELETE_FAILED})
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam String linkuImageUrl) {
        awsS3Service.deleteFileByUrl(linkuImageUrl);
        return ResponseEntity.ok("Deleted: " + linkuImageUrl);
    }

    // 파일 URL 조회
    @GetMapping("/url")
    public ResponseEntity<String> getFileUrl(@RequestParam String fileName) {
        String fileUrl = awsS3Service.getFileUrl(fileName);
        return ResponseEntity.ok(fileUrl);
    }

    @ApiErrorCode(errorStatus = {ErrorStatus._S3_FILE_EMPTY, ErrorStatus._S3_INVALID_IMAGE, ErrorStatus._S3_UPLOAD_FAILED})
    @PostMapping(value = "/upload/{folder}", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadFile(
            @PathVariable String folder,
            @RequestParam("file") MultipartFile multipartFile) {
        String fileUrl = awsS3Service.uploadFile(multipartFile, folder);  // 폴더 전달!
        return ResponseEntity.ok(fileUrl);
    }

}
