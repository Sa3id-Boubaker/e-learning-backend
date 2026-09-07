package com.test.courseservice.controller;

import com.test.courseservice.dto.CertificateResponse;
import com.test.courseservice.dto.PageResponse;
import com.test.courseservice.security.AuthenticatedUser;
import com.test.courseservice.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/api/courses/{courseId}/certificate")
    public ResponseEntity<CertificateResponse> generateCertificate(@PathVariable String courseId,
                                                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        CertificateService.CertificateGenerationResult result = certificateService.generateCertificate(courseId, currentUser);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping("/api/courses/{courseId}/certificate")
    public ResponseEntity<CertificateResponse> getMyCertificateForCourse(@PathVariable String courseId,
                                                                         @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(certificateService.getMyCertificateForCourse(courseId, currentUser));
    }

    @GetMapping("/api/certificates/my")
    public ResponseEntity<PageResponse<CertificateResponse>> getMyCertificates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(certificateService.getMyCertificates(page, size, currentUser));
    }

    @GetMapping("/api/certificates/{certificateNumber}")
    public ResponseEntity<CertificateResponse> verifyCertificate(@PathVariable String certificateNumber) {
        return ResponseEntity.ok(certificateService.verifyCertificate(certificateNumber));
    }

    @GetMapping("/api/certificates")
    public ResponseEntity<PageResponse<CertificateResponse>> listAllCertificates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(certificateService.listAllCertificates(page, size));
    }

    @GetMapping("/api/certificates/course/{courseId}")
    public ResponseEntity<PageResponse<CertificateResponse>> listCertificatesByCourse(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(certificateService.listCertificatesByCourse(courseId, page, size, currentUser));
    }
}