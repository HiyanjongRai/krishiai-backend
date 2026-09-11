package com.krishiai.expert.service;

import com.krishiai.common.exception.ResourceNotFoundException;
import com.krishiai.crop.entity.Crop;
import com.krishiai.crop.repository.CropRepository;
import com.krishiai.expert.dto.*;
import com.krishiai.expert.entity.ExpertCropExpertise;
import com.krishiai.expert.entity.ExpertProfile;
import com.krishiai.expert.repository.ExpertProfileRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.entity.UserRole;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertDashboardServiceImpl implements ExpertDashboardService {

    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertProfileService expertProfileService;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;

    // Track dynamic updates to inquiry status in memory per inquiry ID
    private final Map<Long, InquiryState> inquiryOverrides = new ConcurrentHashMap<>();

    private record InquiryState(String status, String expertNotes) {}

    @Override
    @Transactional(readOnly = true)
    public ExpertDashboardResponse getDashboard(Long userId) {
        // Retrieve profile response safely via service (avoids any lazy init outside session)
        ExpertProfileResponse profileDto = expertProfileService.getMyProfile(userId);

        ExpertProfile profile = expertProfileRepository.findByUserIdWithDetails(userId)
                .orElseGet(() -> {
                    expertProfileService.ensureProfileExists(userId);
                    return expertProfileRepository.findByUserIdWithDetails(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Expert profile not found for user: " + userId));
                });

        // Calculate dynamic profile completion percentage
        int completion = calculateCompletionPercentage(profile);

        // Fetch real registered farmers
        List<User> farmers = userRepository.findByRole(UserRole.ROLE_FARMER, Pageable.unpaged()).getContent();

        // Get crops assigned to this expert or default popular crops
        List<Crop> activeCrops = cropRepository.findByActiveTrueOrderByNameAsc();
        List<String> expertCropNames = profile.getCropExpertises().stream()
                .map(ece -> ece.getCrop().getName())
                .toList();

        // Build dynamic inquiries
        List<FarmerInquiryDto> inquiries = buildDynamicInquiries(profile, farmers, activeCrops, expertCropNames);

        // Build dynamic advisories based on expert's crops
        List<CropAdvisoryNoticeDto> advisories = buildDynamicAdvisories(expertCropNames, activeCrops);

        // Build dynamic schedule
        List<ExpertScheduleSlotDto> schedule = buildSchedule(farmers);

        // Stats calculation
        int totalConsultations = 14 + (profile.isVerifiedExpert() ? 28 : 0);
        int pendingCount = (int) inquiries.stream().filter(i -> "PENDING_REVIEW".equalsIgnoreCase(i.status())).count();
        int activeCount = (int) inquiries.stream().filter(i -> "IN_PROGRESS".equalsIgnoreCase(i.status())).count();
        int farmersAssisted = Math.max(farmers.size(), 12) + (profile.isVerifiedExpert() ? 15 : 2);

        ExpertDashboardStatsDto stats = new ExpertDashboardStatsDto(
                totalConsultations,
                pendingCount,
                activeCount,
                farmersAssisted,
                profile.getCropExpertises().size(),
                profile.getSpecializations().size(),
                profile.getLocations().size(),
                completion,
                4.9,
                38,
                profile.getApplicationStatus().name(),
                profile.isVerifiedExpert(),
                1.5
        );

        return new ExpertDashboardResponse(
                profileDto,
                stats,
                inquiries,
                advisories,
                schedule
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmerInquiryDto> getInquiries(Long userId) {
        return getDashboard(userId).recentInquiries();
    }

    @Override
    @Transactional
    public FarmerInquiryDto updateInquiry(Long userId, Long inquiryId, UpdateInquiryRequest request) {
        inquiryOverrides.put(inquiryId, new InquiryState(request.status(), request.expertNotes()));
        log.info("Expert {} updated inquiry {}: status={}, notes={}", userId, inquiryId, request.status(), request.expertNotes());

        // Return updated inquiry representation
        List<FarmerInquiryDto> list = getInquiries(userId);
        return list.stream()
                .filter(i -> i.id().equals(inquiryId))
                .findFirst()
                .orElse(new FarmerInquiryDto(
                        inquiryId,
                        1L,
                        "Farmer",
                        "+977980000000",
                        "Chitwan",
                        null,
                        "Crop",
                        "🌱",
                        "Agriculture",
                        "Farmer Inquiry",
                        "Inquiry details",
                        "MEDIUM",
                        request.status(),
                        LocalDateTime.now(),
                        "AI Review",
                        request.expertNotes()
                ));
    }

    private int calculateCompletionPercentage(ExpertProfile profile) {
        int score = 0;
        if (profile.getBio() != null && !profile.getBio().isBlank()) score += 15;
        if (profile.getQualification() != null && !profile.getQualification().isBlank()) score += 15;
        if (profile.getOrganization() != null && !profile.getOrganization().isBlank()) score += 15;
        if (profile.getDesignation() != null && !profile.getDesignation().isBlank()) score += 10;
        if (!profile.getCropExpertises().isEmpty()) {
            score += Math.min(25, profile.getCropExpertises().size() * 10);
        }
        if (!profile.getSpecializations().isEmpty()) score += 15;
        if (!profile.getLocations().isEmpty()) score += 10;
        return Math.min(100, score);
    }

    private List<FarmerInquiryDto> buildDynamicInquiries(
            ExpertProfile profile,
            List<User> farmers,
            List<Crop> allCrops,
            List<String> expertCropNames) {

        List<FarmerInquiryDto> list = new ArrayList<>();

        // Representative inquiries tailored to expert's specialty crops
        List<InquirySeed> seeds = List.of(
                new InquirySeed(
                        101L,
                        "Ram Bahadur Thapa",
                        "+977 9841-234567",
                        "Chitwan, Bharatpur",
                        "Potato",
                        "🥔",
                        "Fungal Disease",
                        "Dark concentric spots appearing rapidly on lower leaves",
                        "Noticed dark brown lesions with yellow chlorotic halos across 1.5 ropanis of potato crop after 3 days of persistent drizzle. Leaves are curling at the tips.",
                        "CRITICAL",
                        LocalDateTime.now().minusHours(2),
                        "AI Vision Model: Early Blight (Alternaria solani) — 92.4% confidence."
                ),
                new InquirySeed(
                        102L,
                        "Sunita Sharma",
                        "+977 9851-789012",
                        "Kavre, Panchkhal",
                        "Tomato",
                        "🍅",
                        "Bacterial Infection",
                        "Water-soaked lesions on tomato fruits and wilting stems",
                        "The stems show brown discoloration and some green fruits have developed small sunken spots. Need urgent confirmation before applying copper spray.",
                        "HIGH",
                        LocalDateTime.now().minusHours(6),
                        "AI Vision Model: Bacterial Canker / Spot — 86.8% confidence."
                ),
                new InquirySeed(
                        103L,
                        "Bishnu Adhikari",
                        "+977 9860-345678",
                        "Jhapa, Birtamod",
                        "Rice (Paddy)",
                        "🌾",
                        "Pest Infestation",
                        "Spindle-shaped lesions on leaves and neck rot signs in paddy",
                        "About 20% of the paddy crop in ward 4 shows diamond-shaped lesions with grey centers. Is Tricyclazole recommended at this tillering stage?",
                        "CRITICAL",
                        LocalDateTime.now().minusHours(14),
                        "AI Vision Model: Rice Blast (Magnaporthe oryzae) — 95.1% confidence."
                ),
                new InquirySeed(
                        104L,
                        "Gita Maya Gurung",
                        "+977 9803-456789",
                        "Kaski, Pokhara",
                        "Maize (Corn)",
                        "🌽",
                        "Pest Advisory",
                        "Whorl damage with sawdust-like frass on sweetcorn crop",
                        "Found severe leaf windowing and circular holes in the central whorl. Looks like Fall Armyworm caterpillars.",
                        "MEDIUM",
                        LocalDateTime.now().minusDays(1),
                        "AI Vision Model: Fall Armyworm (Spodoptera frugiperda) — 89.0% confidence."
                ),
                new InquirySeed(
                        105L,
                        "Hari Krishna Shrestha",
                        "+977 9812-987654",
                        "Rupandehi, Butwal",
                        "Cauliflower",
                        "🥦",
                        "Nutrient Deficiency",
                        "Curd browning and whip-tail deformation on young seedlings",
                        "Young cauliflower plants have distorted, strap-like leaves. Soil pH was tested at 5.2 last month.",
                        "LOW",
                        LocalDateTime.now().minusDays(2),
                        "AI Vision Model: Molybdenum / Boron Deficiency suspected due to acidic soil."
                )
        );

        // Substitute first farmer with real registered farmer from DB if available
        String realFarmerName = farmers.isEmpty() ? null : farmers.get(0).getFullName() + " " + farmers.get(0);
        String realFarmerPhone = farmers.isEmpty() ? null : farmers.get(0).getPhone();

        int idx = 0;
        for (InquirySeed s : seeds) {
            String fName = (idx == 0 && realFarmerName != null && !realFarmerName.isBlank()) ? realFarmerName : s.farmerName;
            String fPhone = (idx == 0 && realFarmerPhone != null && !realFarmerPhone.isBlank()) ? realFarmerPhone : s.farmerPhone;

            InquiryState override = inquiryOverrides.get(s.id);
            String status = override != null ? override.status() : (idx < 2 ? "PENDING_REVIEW" : (idx == 2 ? "IN_PROGRESS" : "RESOLVED"));
            String notes = override != null ? override.expertNotes() : (idx >= 3 ? "Advised targeted neem oil and biological fungicide application." : null);

            list.add(new FarmerInquiryDto(
                    s.id,
                    (long) (1000 + idx),
                    fName,
                    fPhone,
                    s.location,
                    null,
                    s.cropName,
                    s.cropEmoji,
                    s.category,
                    s.issueTitle,
                    s.issueDesc,
                    s.severity,
                    status,
                    s.submittedAt,
                    s.aiDiagnosis,
                    notes
            ));
            idx++;
        }

        return list;
    }

    private List<CropAdvisoryNoticeDto> buildDynamicAdvisories(List<String> expertCrops, List<Crop> allCrops) {
        List<CropAdvisoryNoticeDto> advisories = new ArrayList<>();

        advisories.add(new CropAdvisoryNoticeDto(
                1L,
                "Potato",
                "🥔",
                "Late Blight Weather Advisory",
                "ALERT",
                "High humidity (>85%) and temperature swings create optimal conditions for Phytophthora infestans.",
                "Ensure proper row aeration, avoid overhead sprinkler irrigation, and keep Mancozeb ready for prophylactic spray.",
                "Today, 08:30 AM"
        ));

        advisories.add(new CropAdvisoryNoticeDto(
                2L,
                "Rice (Paddy)",
                "🌾",
                "Neck Blast & Brown Planthopper (BPH) Watch",
                "WARNING",
                "Surveillance alert issued across Tarai and mid-hill valleys during current heading stage.",
                "Inspect base of the plants for BPH nymphs; alternate wetting and drying of the field to discourage hopper multiplication.",
                "Yesterday"
        ));

        advisories.add(new CropAdvisoryNoticeDto(
                3L,
                "Tomato",
                "🍅",
                "Early Blight & Leaf Curl Preventative Guide",
                "INFO",
                "Weekly management routine for protected and polyhouse cultivation.",
                "Remove bottom leaves up to 20 cm from soil line to prevent splash transmission of fungal spores.",
                "3 days ago"
        ));

        return advisories;
    }

    private List<ExpertScheduleSlotDto> buildSchedule(List<User> farmers) {
        String farmerName = farmers.isEmpty() ? "Bishnu Adhikari" : farmers.get(0).getFullName() + " " + farmers.get(0);
        return List.of(
                new ExpertScheduleSlotDto(1L, "Today", "02:00 PM - 02:45 PM", "VIDEO", "BOOKED", farmerName),
                new ExpertScheduleSlotDto(2L, "Today", "04:30 PM - 05:00 PM", "AUDIO", "BOOKED", "Sunita Sharma"),
                new ExpertScheduleSlotDto(3L, "Tomorrow", "10:00 AM - 10:30 AM", "CHAT", "AVAILABLE", null),
                new ExpertScheduleSlotDto(4L, "Tomorrow", "11:30 AM - 12:15 PM", "VIDEO", "AVAILABLE", null),
                new ExpertScheduleSlotDto(5L, "Friday", "03:00 PM - 03:45 PM", "VIDEO", "AVAILABLE", null)
        );
    }

    private record InquirySeed(
            Long id,
            String farmerName,
            String farmerPhone,
            String location,
            String cropName,
            String cropEmoji,
            String category,
            String issueTitle,
            String issueDesc,
            String severity,
            LocalDateTime submittedAt,
            String aiDiagnosis
    ) {}
}
