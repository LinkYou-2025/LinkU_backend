package com.umc.linkyou.awsS3.controller;

import com.umc.linkyou.awsS3.AwsS3Service;
import com.umc.linkyou.validation.annotation.ApiV1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@ApiV1
@RestController
@RequiredArgsConstructor
@RequestMapping("/s3")
public class AwsS3Controller {

    private final AwsS3Service awsS3Service;

    // 파일 업로드
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile multipartFile) {
        String fileUrl = awsS3Service.uploadFile(multipartFile);
        return ResponseEntity.ok(fileUrl);
    }

    // 파일 삭제
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

    @PostMapping(value = "/upload/{folder}", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadFile(
            @PathVariable String folder,
            @RequestParam("file") MultipartFile multipartFile) {
        String fileUrl = awsS3Service.uploadFile(multipartFile, folder);  // 폴더 전달!
        return ResponseEntity.ok(fileUrl);
    }

}
