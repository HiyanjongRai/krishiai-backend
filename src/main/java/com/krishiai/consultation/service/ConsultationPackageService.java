package com.krishiai.consultation.service;

import com.krishiai.common.exception.BadRequestException;
import com.krishiai.common.exception.ForbiddenException;
import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.consultation.dto.ConsultationPackageDto;
import com.krishiai.consultation.dto.CreateConsultationPackageRequest;
import com.krishiai.consultation.dto.UpdateConsultationPackageRequest;
import com.krishiai.consultation.entity.ConsultationPackage;
import com.krishiai.consultation.repository.ConsultationPackageRepository;
import com.krishiai.crop.entity.Crop;
import com.krishiai.crop.repository.CropRepository;
import com.krishiai.expert.entity.ExpertProfile;
import com.krishiai.expert.repository.ExpertProfileRepository;
import com.krishiai.payment.service.CommissionService;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationPackageService {

    private final ConsultationPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final CropRepository cropRepository;
    private final CommissionService commissionService;

    @Transactional
    public ConsultationPackageDto createPackage(Long expertId, CreateConsultationPackageRequest request) {
        User expert = userRepository.findById(expertId)
                .orElseThrow(() -> new ResourceNotFoundException("Expert not found"));

        if (expert.getRole() != UserRole.ROLE_EXPERT) {
            throw new ForbiddenException("Only experts can configure consultation packages");
        }

        // Verify that expert profile is verified
        ExpertProfile profile = expertProfileRepository.findByUserId(expertId).orElse(null);
        if (profile == null || !profile.isVerifiedExpert()) {
            throw new ForbiddenException("You must be an approved, verified expert before offering paid consultation packages");
        }

        // Validate pricing bounds and duration
        commissionService.validatePackage(request.getPrice(), request.getDurationHours());

        Crop crop = null;
        if (request.getCropId() != null) {
            crop = cropRepository.findById(request.getCropId())
                    .orElseThrow(() -> new ResourceNotFoundException("Crop not found with id: " + request.getCropId()));
        }

        ConsultationPackage pkg = new ConsultationPackage(
                expert,
                crop,
                request.getName().trim(),
                request.getDescription(),
                request.getPrice(),
                request.getDurationHours()
        );

        pkg = packageRepository.save(pkg);
        log.info("Created consultation package: id={}, expertId={}, price={}, durationHours={}",
                pkg.getId(), expertId, pkg.getPrice(), pkg.getDurationHours());

        return ConsultationPackageDto.fromEntity(pkg);
    }

    @Transactional
    public ConsultationPackageDto updatePackage(Long expertId, Long packageId, UpdateConsultationPackageRequest request) {
        ConsultationPackage pkg = packageRepository.findByIdWithDetails(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));

        if (!pkg.getExpert().getId().equals(expertId)) {
            throw new ForbiddenException("You do not own this consultation package");
        }

        if (request.getPrice() != null || request.getDurationHours() != null) {
            commissionService.validatePackage(
                    request.getPrice() != null ? request.getPrice() : pkg.getPrice(),
                    request.getDurationHours() != null ? request.getDurationHours() : pkg.getDurationHours()
            );
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            pkg.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            pkg.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            pkg.setPrice(request.getPrice());
        }
        if (request.getDurationHours() != null) {
            pkg.setDurationHours(request.getDurationHours());
        }
        if (request.getActive() != null) {
            pkg.setActive(request.getActive());
        }

        pkg = packageRepository.save(pkg);
        log.info("Updated consultation package: id={}, expertId={}", packageId, expertId);

        return ConsultationPackageDto.fromEntity(pkg);
    }

    @Transactional
    public void deletePackage(Long expertId, Long packageId) {
        ConsultationPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));

        if (!pkg.getExpert().getId().equals(expertId)) {
            throw new ForbiddenException("You do not own this consultation package");
        }

        // Soft delete by deactivating
        pkg.setActive(false);
        packageRepository.save(pkg);
        log.info("Deactivated consultation package id={} for expertId={}", packageId, expertId);
    }

    @Transactional(readOnly = true)
    public List<ConsultationPackageDto> listMyPackages(Long expertId) {
        return packageRepository.findByExpertIdOrderByCreatedAtDesc(expertId)
                .stream()
                .map(ConsultationPackageDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConsultationPackageDto> listPublicPackagesForExpert(Long expertId) {
        return packageRepository.findActivePackagesForExpertWithCrop(expertId)
                .stream()
                .map(ConsultationPackageDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConsultationPackageDto getPackageDetail(Long packageId) {
        ConsultationPackage pkg = packageRepository.findByIdWithDetails(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));
        return ConsultationPackageDto.fromEntity(pkg);
    }
}
