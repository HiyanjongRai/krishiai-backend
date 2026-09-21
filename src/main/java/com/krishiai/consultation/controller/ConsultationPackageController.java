package com.krishiai.consultation.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.consultation.dto.ConsultationPackageDto;
import com.krishiai.consultation.dto.CreateConsultationPackageRequest;
import com.krishiai.consultation.dto.UpdateConsultationPackageRequest;
import com.krishiai.consultation.service.ConsultationPackageService;
import com.krishiai.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for consultation packages.
 * Experts manage their own packages; farmers/public view active packages.
 */
@RestController
@Validated
@RequiredArgsConstructor
public class ConsultationPackageController {

    private final ConsultationPackageService packageService;

    // ─── Expert: manage own packages ────────────────────────────────────────

    @PostMapping("/api/v1/expert/consultation-packages")
    @PreAuthorize("hasAuthority('ROLE_EXPERT')")
    public ResponseEntity<ApiResponse<ConsultationPackageDto>> createPackage(
            @Valid @RequestBody CreateConsultationPackageRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ConsultationPackageDto dto = packageService.createPackage(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Consultation package created successfully", dto));
    }

    @GetMapping("/api/v1/expert/consultation-packages")
    @PreAuthorize("hasAuthority('ROLE_EXPERT')")
    public ResponseEntity<ApiResponse<List<ConsultationPackageDto>>> listMyPackages(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<ConsultationPackageDto> packages = packageService.listMyPackages(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Packages retrieved", packages));
    }

    @PutMapping("/api/v1/expert/consultation-packages/{id}")
    @PreAuthorize("hasAuthority('ROLE_EXPERT')")
    public ResponseEntity<ApiResponse<ConsultationPackageDto>> updatePackage(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateConsultationPackageRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ConsultationPackageDto dto = packageService.updatePackage(principal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Package updated successfully", dto));
    }

    @DeleteMapping("/api/v1/expert/consultation-packages/{id}")
    @PreAuthorize("hasAuthority('ROLE_EXPERT')")
    public ResponseEntity<ApiResponse<Void>> deletePackage(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        packageService.deletePackage(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Package deactivated successfully", null));
    }

    // ─── Public: view active packages for an expert ─────────────────────────

    @GetMapping("/api/v1/experts/{expertId}/consultation-packages")
    public ResponseEntity<ApiResponse<List<ConsultationPackageDto>>> listExpertPackages(
            @PathVariable @Positive Long expertId) {
        List<ConsultationPackageDto> packages = packageService.listPublicPackagesForExpert(expertId);
        return ResponseEntity.ok(ApiResponse.success("Expert packages retrieved", packages));
    }

    @GetMapping("/api/v1/consultation-packages/{id}")
    public ResponseEntity<ApiResponse<ConsultationPackageDto>> getPackageDetail(
            @PathVariable @Positive Long id) {
        ConsultationPackageDto dto = packageService.getPackageDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Package details retrieved", dto));
    }
}
