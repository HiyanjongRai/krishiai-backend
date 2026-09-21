package com.krishiai.messaging.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.messaging.dto.ConsultationDetailDto;
import com.krishiai.messaging.dto.ConsultationRequestDto;
import com.krishiai.messaging.service.ConsultationLifecycleService;
import com.krishiai.security.userdetails.CustomUserDetails;
import com.krishiai.user.entity.UserRole;
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

@RestController
@RequestMapping("/api/v1/consultations")
@PreAuthorize("hasAnyAuthority('ROLE_FARMER', 'ROLE_EXPERT', 'ROLE_ADMIN')")
@Validated
@RequiredArgsConstructor
public class ConsultationFlowController {

    private final ConsultationLifecycleService consultationService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_FARMER')")
    public ResponseEntity<ApiResponse<ConsultationDetailDto>> requestConsultation(
            @Valid @RequestBody ConsultationRequestDto request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ConsultationDetailDto dto = consultationService.requestConsultation(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Consultation requested successfully", dto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsultationDetailDto>>> listConsultations(
            @AuthenticationPrincipal CustomUserDetails principal) {
        UserRole role = principal.getUser().getRole();
        List<ConsultationDetailDto> list = consultationService.listConsultationsForUser(principal.getUserId(), role);
        return ResponseEntity.ok(ApiResponse.success("Consultations retrieved", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsultationDetailDto>> getConsultation(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        UserRole role = principal.getUser().getRole();
        ConsultationDetailDto dto = consultationService.getConsultationDetail(principal.getUserId(), id, role);
        return ResponseEntity.ok(ApiResponse.success("Consultation retrieved", dto));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('ROLE_EXPERT')")
    public ResponseEntity<ApiResponse<ConsultationDetailDto>> acceptConsultation(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ConsultationDetailDto dto = consultationService.acceptConsultation(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Consultation accepted", dto));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_EXPERT')")
    public ResponseEntity<ApiResponse<ConsultationDetailDto>> rejectConsultation(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ConsultationDetailDto dto = consultationService.rejectConsultation(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Consultation rejected", dto));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ConsultationDetailDto>> completeConsultation(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        UserRole role = principal.getUser().getRole();
        ConsultationDetailDto dto = consultationService.completeConsultation(principal.getUserId(), id, role);
        return ResponseEntity.ok(ApiResponse.success("Consultation completed", dto));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_FARMER')")
    public ResponseEntity<ApiResponse<ConsultationDetailDto>> cancelConsultation(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ConsultationDetailDto dto = consultationService.cancelConsultation(principal.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Consultation cancelled", dto));
    }
}
