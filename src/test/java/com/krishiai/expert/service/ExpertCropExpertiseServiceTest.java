package com.krishiai.expert.service;

import com.krishiai.admin.dto.BatchExpertiseVerificationRequest;
import com.krishiai.admin.dto.BatchExpertiseVerificationResponse;
import com.krishiai.admin.repository.ExpertAuditLogRepository;
import com.krishiai.admin.service.AdminDashboardServiceImpl;
import com.krishiai.common.exception.BadRequestException;
import com.krishiai.crop.entity.Crop;
import com.krishiai.crop.repository.CropRepository;
import com.krishiai.expert.dto.AddCropExpertiseRequest;
import com.krishiai.expert.dto.AttachExpertiseEvidenceRequest;
import com.krishiai.expert.dto.CropExpertiseResponse;
import com.krishiai.expert.entity.*;
import com.krishiai.expert.repository.*;
import com.krishiai.location.repository.LocationRepository;
import com.krishiai.specialization.repository.SpecializationRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpertCropExpertiseServiceTest {

    @Mock
    private ExpertProfileRepository profileRepository;

    @Mock
    private ExpertCropExpertiseRepository cropExpertiseRepository;

    @Mock
    private ExpertSpecializationRepository specializationRepository;

    @Mock
    private ExpertLocationRepository locationRepository;

    @Mock
    private ExpertDocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CropRepository cropRepository;

    @Mock
    private SpecializationRepository specializationRepo;

    @Mock
    private LocationRepository locationRepo;

    @Mock
    private ExpertAuditLogRepository auditLogRepository;

    @InjectMocks
    private ExpertProfileServiceImpl expertProfileService;

    private AdminDashboardServiceImpl adminDashboardService;

    private User expertUser;
    private ExpertProfile expertProfile;
    private Crop tomatoCrop;
    private Crop potatoCrop;

    @BeforeEach
    void setUp() {
        adminDashboardService = new AdminDashboardServiceImpl(
                userRepository,
                profileRepository,
                cropRepository,
                auditLogRepository,
                cropExpertiseRepository
        );

        expertUser = new User();
        expertUser.setId(10L);
        expertUser.setEmail("expert@krishiai.com");
        expertUser.setFullName("Ram Prasad");

        expertProfile = ExpertProfile.createFor(expertUser);
        expertProfile.setId(100L);
        expertProfile.setVerifiedExpert(true);
        expertProfile.setVerificationStatus(ExpertVerificationStatus.VERIFIED);
        expertProfile.setApplicationStatus(ExpertApplicationStatus.APPROVED);

        tomatoCrop = new Crop();
        tomatoCrop.setId(1L);
        tomatoCrop.setName("Tomato");

        potatoCrop = new Crop();
        potatoCrop.setId(2L);
        potatoCrop.setName("Potato");
    }

    @Test
    @DisplayName("Expert adds crop expertise: starts as SELF_DECLARED and does not reset professional verification")
    void testAddCropExpertiseSelfDeclared() {
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(expertProfile));
        when(cropRepository.findById(1L)).thenReturn(Optional.of(tomatoCrop));
        when(cropExpertiseRepository.findByExpertProfileIdAndCropId(100L, 1L)).thenReturn(Optional.empty());
        when(cropExpertiseRepository.countByExpertProfileIdAndExpertiseType(100L, CropExpertiseType.PRIMARY)).thenReturn(1L);

        when(cropExpertiseRepository.save(any(ExpertCropExpertise.class))).thenAnswer(invocation -> {
            ExpertCropExpertise ece = invocation.getArgument(0);
            ece.setId(501L);
            return ece;
        });

        AddCropExpertiseRequest request = new AddCropExpertiseRequest(
                1L, null, CropExpertiseType.PRIMARY, ExpertiseLevel.ADVANCED, 5,
                "Specialized in tomato disease management", ExpertiseSourceType.SELF_DECLARED, null
        );

        CropExpertiseResponse response = expertProfileService.addOrUpdateCropExpertise(10L, request);

        assertNotNull(response);
        assertEquals("Tomato", response.cropName());
        assertEquals(CropExpertiseVerificationStatus.SELF_DECLARED, response.verificationStatus());
        assertEquals(CropExpertiseType.PRIMARY, response.expertiseType());

        // CRITICAL: Professional verification status must remain VERIFIED
        assertTrue(expertProfile.isVerifiedExpert());
        assertEquals(ExpertVerificationStatus.VERIFIED, expertProfile.getVerificationStatus());
        assertEquals(ExpertApplicationStatus.APPROVED, expertProfile.getApplicationStatus());
    }

    @Test
    @DisplayName("Expert adds expertise area: starts as SELF_DECLARED")
    void testAddAreaExpertise() {
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(expertProfile));
        when(cropExpertiseRepository.findByExpertProfileIdAndExpertiseAreaIgnoreCase(100L, "Pest Management"))
                .thenReturn(Optional.empty());

        when(cropExpertiseRepository.save(any(ExpertCropExpertise.class))).thenAnswer(invocation -> {
            ExpertCropExpertise ece = invocation.getArgument(0);
            ece.setId(502L);
            return ece;
        });

        AddCropExpertiseRequest request = new AddCropExpertiseRequest(
                null, "Pest Management", CropExpertiseType.AREA, ExpertiseLevel.SPECIALIST, 6,
                "Integrated pest management", ExpertiseSourceType.SELF_DECLARED, null
        );

        CropExpertiseResponse response = expertProfileService.addOrUpdateCropExpertise(10L, request);

        assertNotNull(response);
        assertEquals("Pest Management", response.cropName());
        assertEquals(CropExpertiseVerificationStatus.SELF_DECLARED, response.verificationStatus());
    }

    @Test
    @DisplayName("Expert attaches evidence: transitions status to EVIDENCE_SUBMITTED")
    void testAttachEvidence() {
        ExpertCropExpertise claim = new ExpertCropExpertise(expertProfile, tomatoCrop, CropExpertiseType.PRIMARY);
        claim.setId(501L);
        claim.setVerificationStatus(CropExpertiseVerificationStatus.SELF_DECLARED);

        ExpertDocument doc = new ExpertDocument(expertProfile, "EXPERIENCE", "Field Experience Letter",
                "letter.pdf", "application/pdf", "1.2 MB", "https://res.cloudinary.com/doc.pdf");
        doc.setId(901L);

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(expertProfile));
        when(cropExpertiseRepository.findById(501L)).thenReturn(Optional.of(claim));
        when(documentRepository.findById(901L)).thenReturn(Optional.of(doc));
        when(cropExpertiseRepository.save(any(ExpertCropExpertise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AttachExpertiseEvidenceRequest req = new AttachExpertiseEvidenceRequest(901L, ExpertiseSourceType.CERTIFICATE);
        CropExpertiseResponse response = expertProfileService.attachEvidenceToExpertise(10L, 501L, req);

        assertEquals(CropExpertiseVerificationStatus.EVIDENCE_SUBMITTED, response.verificationStatus());
        assertEquals(901L, response.evidenceDocumentId());
    }

    @Test
    @DisplayName("Admin batch verifies and rejects expertise claims")
    void testAdminBatchVerification() {
        ExpertCropExpertise claim1 = new ExpertCropExpertise(expertProfile, tomatoCrop, CropExpertiseType.PRIMARY);
        claim1.setId(501L);
        claim1.setVerificationStatus(CropExpertiseVerificationStatus.EVIDENCE_SUBMITTED);

        ExpertCropExpertise claim2 = new ExpertCropExpertise(expertProfile, potatoCrop, CropExpertiseType.SECONDARY);
        claim2.setId(502L);
        claim2.setVerificationStatus(CropExpertiseVerificationStatus.SELF_DECLARED);

        when(cropExpertiseRepository.findById(501L)).thenReturn(Optional.of(claim1));
        when(cropExpertiseRepository.findById(502L)).thenReturn(Optional.of(claim2));
        when(cropExpertiseRepository.save(any(ExpertCropExpertise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BatchExpertiseVerificationRequest batchReq = new BatchExpertiseVerificationRequest(
                List.of(
                        new BatchExpertiseVerificationRequest.Item(501L, "VERIFY", null, ExpertiseVerificationMethod.DOCUMENT_REVIEW),
                        new BatchExpertiseVerificationRequest.Item(502L, "REJECT", "Insufficient documentation provided", null)
                ),
                "Review of submitted claims"
        );

        BatchExpertiseVerificationResponse response = adminDashboardService.batchVerifyExpertises(batchReq, 1L, "admin@krishiai.com");

        assertEquals(2, response.totalProcessed());
        assertEquals(1, response.verifiedCount());
        assertEquals(1, response.rejectedCount());
        assertTrue(response.verifiedIds().contains(501L));
        assertTrue(response.rejectedIds().contains(502L));

        assertEquals(CropExpertiseVerificationStatus.VERIFIED, claim1.getVerificationStatus());
        assertEquals(1L, claim1.getVerifiedBy());
        assertNotNull(claim1.getVerifiedAt());

        assertEquals(CropExpertiseVerificationStatus.REJECTED, claim2.getVerificationStatus());
        assertEquals("Insufficient documentation provided", claim2.getRejectionReason());
    }

    @Test
    @DisplayName("Primary crop cap: enforces maximum 3 primary crops")
    void testPrimaryCropCapEnforced() {
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(expertProfile));
        when(cropRepository.findById(1L)).thenReturn(Optional.of(tomatoCrop));
        when(cropExpertiseRepository.findByExpertProfileIdAndCropId(100L, 1L)).thenReturn(Optional.empty());
        when(cropExpertiseRepository.countByExpertProfileIdAndExpertiseType(100L, CropExpertiseType.PRIMARY)).thenReturn(3L);

        AddCropExpertiseRequest request = new AddCropExpertiseRequest(
                1L, null, CropExpertiseType.PRIMARY, ExpertiseLevel.ADVANCED, 5,
                "Exceeding primary limit", ExpertiseSourceType.SELF_DECLARED, null
        );

        assertThrows(BadRequestException.class, () -> expertProfileService.addOrUpdateCropExpertise(10L, request));
    }

    @Test
    @DisplayName("Expert can remove self-declared expertise claim")
    void testRemoveSelfDeclaredExpertise() {
        ExpertCropExpertise claim = new ExpertCropExpertise(expertProfile, tomatoCrop, CropExpertiseType.PRIMARY);
        claim.setId(501L);
        claim.setVerificationStatus(CropExpertiseVerificationStatus.SELF_DECLARED);

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(expertProfile));
        when(cropExpertiseRepository.findById(501L)).thenReturn(Optional.of(claim));

        expertProfileService.removeExpertise(10L, 501L);

        verify(cropExpertiseRepository).delete(claim);
    }

    @Test
    @DisplayName("Expert cannot remove verified expertise claim")
    void testRemoveVerifiedExpertiseRejected() {
        ExpertCropExpertise claim = new ExpertCropExpertise(expertProfile, tomatoCrop, CropExpertiseType.PRIMARY);
        claim.setId(501L);
        claim.setVerificationStatus(CropExpertiseVerificationStatus.VERIFIED);

        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(expertProfile));
        when(cropExpertiseRepository.findById(501L)).thenReturn(Optional.of(claim));

        assertThrows(BadRequestException.class, () -> expertProfileService.removeExpertise(10L, 501L));
        verify(cropExpertiseRepository, never()).delete(any(ExpertCropExpertise.class));
    }
}
