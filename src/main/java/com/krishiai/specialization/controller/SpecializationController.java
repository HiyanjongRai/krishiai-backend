package com.krishiai.specialization.controller;

import com.krishiai.common.response.ApiResponse;
import com.krishiai.specialization.dto.SpecializationResponse;
import com.krishiai.specialization.service.SpecializationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specializations")
@RequiredArgsConstructor
public class SpecializationController {

    private final SpecializationService specializationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SpecializationResponse>>> getAllSpecializations() {
        List<SpecializationResponse> list = specializationService.getAllActiveSpecializations();
        return ResponseEntity.ok(ApiResponse.success("Specializations retrieved successfully", list));
    }
}
