package com.krishiai.common.config;

import com.krishiai.crop.entity.Crop;
import com.krishiai.crop.entity.CropCategory;
import com.krishiai.crop.repository.CropCategoryRepository;
import com.krishiai.crop.repository.CropRepository;
import com.krishiai.expert.entity.ExpertDocument;
import com.krishiai.expert.entity.ExpertProfile;
import com.krishiai.expert.repository.ExpertDocumentRepository;
import com.krishiai.expert.repository.ExpertProfileRepository;
import com.krishiai.location.entity.Location;
import com.krishiai.location.entity.LocationType;
import com.krishiai.location.repository.LocationRepository;
import com.krishiai.specialization.entity.Specialization;
import com.krishiai.specialization.repository.SpecializationRepository;
import com.krishiai.user.entity.User;
import com.krishiai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CropCategoryRepository cropCategoryRepository;
    private final CropRepository cropRepository;
    private final SpecializationRepository specializationRepository;
    private final LocationRepository locationRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertDocumentRepository expertDocumentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.first-name}")
    private String adminFirstName;

    @Value("${app.admin.last-name}")
    private String adminLastName;

    @Override
    @Transactional
    public void run(String... args) {
        migrateSchema();
        seedAdminUser();
        seedCropCategoriesAndCrops();
        seedSpecializations();
        seedLocations();
        seedExpertDocuments();
    }

    private void migrateSchema() {
        try {
            jdbcTemplate.execute("ALTER TABLE expert_profiles ALTER COLUMN application_status TYPE VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE expert_profiles ALTER COLUMN verification_status TYPE VARCHAR(50)");
            log.info("Successfully ensured expert_profiles status columns are VARCHAR(50)");
        } catch (Exception e) {
            log.warn("Could not alter column lengths on expert_profiles: {}", e.getMessage());
        }
    }

    private void seedAdminUser() {
        String normalizedEmail = User.normaliseEmail(adminEmail);
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Admin user already exists with email: {}", normalizedEmail);
            return;
        }

        User admin = User.createAdmin(
                normalizedEmail,
                passwordEncoder.encode(adminPassword),
                adminFirstName,
                adminLastName
        );

        userRepository.save(admin);
        log.info("Successfully seeded default platform admin: {}", normalizedEmail);
    }

    private void seedCropCategoriesAndCrops() {
        CropCategory cereals = getOrCreateCategory("Cereal Grains", "CEREAL", "Staple grain crops like rice, wheat, and maize", "🌾");
        CropCategory vegetables = getOrCreateCategory("Vegetables", "VEGETABLE", "Fresh farm vegetables and roots", "🥦");
        CropCategory fruits = getOrCreateCategory("Fruits & Orchards", "FRUIT", "Horticultural fruit crops", "🍎");
        CropCategory cashCrops = getOrCreateCategory("Cash Crops & Spices", "CASH_CROP", "Commercial cash and spice crops", "☕");
        CropCategory pulses = getOrCreateCategory("Pulses & Legumes", "PULSE", "Protein-rich lentils and legumes", "🫘");

        getOrCreateCrop(cereals, "Rice (Paddy)", "Oryza sativa", "धान", "🌾", "Major staple cereal crop grown during monsoon season in Nepal.");
        getOrCreateCrop(cereals, "Wheat", "Triticum aestivum", "गहुँ", "🌾", "Winter cereal grain grown in plains and hills.");
        getOrCreateCrop(cereals, "Maize (Corn)", "Zea mays", "मकै", "🌽", "Primary upland food grain and livestock feed.");
        getOrCreateCrop(vegetables, "Potato", "Solanum tuberosum", "आलु", "🥔", "Major highland and valley vegetable staple.");
        getOrCreateCrop(vegetables, "Tomato", "Solanum lycopersicum", "गोलभेँडा", "🍅", "High-value commercial greenhouse and open field vegetable.");
        getOrCreateCrop(vegetables, "Cauliflower", "Brassica oleracea", "काउली", "🥦", "Widely cultivated winter brassica vegetable.");
        getOrCreateCrop(fruits, "Apple", "Malus domestica", "स्याउ", "🍎", "High-altitude temperate fruit cultivated in Himalayan districts.");
        getOrCreateCrop(fruits, "Mandarin Orange", "Citrus reticulata", "सुन्तला", "🍊", "Major mid-hill commercial citrus fruit.");
        getOrCreateCrop(cashCrops, "Large Cardamom", "Amomum subulatum", "अलैँची", "🌱", "High-value cash spice exported from eastern Nepal hills.");
        getOrCreateCrop(cashCrops, "Tea", "Camellia sinensis", "चिया", "🍵", "Specialty hill beverage crop grown in Ilam and eastern districts.");
        getOrCreateCrop(cashCrops, "Mustard", "Brassica campestris", "तोरी", "🌼", "Primary winter oilseed crop.");
        getOrCreateCrop(pulses, "Lentil", "Lens culinaris", "मुसुरो", "🫘", "Nutritious grain legume widely grown in Tarai plains.");
    }

    private CropCategory getOrCreateCategory(String name, String code, String description, String icon) {
        return cropCategoryRepository.findByCode(code)
                .orElseGet(() -> cropCategoryRepository.save(new CropCategory(name, code, description, icon)));
    }

    private void getOrCreateCrop(CropCategory category, String name, String scientificName, String nepaliName, String emoji, String desc) {
        if (!cropRepository.existsByNameIgnoreCase(name)) {
            cropRepository.save(new Crop(category, name, scientificName, nepaliName, emoji, desc));
        }
    }

    private void seedSpecializations() {
        createSpecIfNotExists("Soil Health & Fertility", "SOIL_HEALTH", "Soil testing, nutrition, pH balance, and organic matter management", "🌱");
        createSpecIfNotExists("Pest & Disease Management", "PEST_DISEASE", "Integrated pest management, fungal/bacterial blight diagnosis and control", "🔬");
        createSpecIfNotExists("Organic & Sustainable Farming", "ORGANIC_FARMING", "Bio-fertilizers, permaculture, pesticide-free cultivation techniques", "🍃");
        createSpecIfNotExists("Irrigation & Water Conservation", "IRRIGATION", "Drip irrigation, rainwater harvesting, drought management", "💧");
        createSpecIfNotExists("Agronomy & Crop Production", "AGRONOMY", "Crop rotation, varietal selection, optimal sowing and harvesting", "🌾");
        createSpecIfNotExists("Horticulture & Greenhouse Farming", "HORTICULTURE", "Protected polyhouse cultivation, nursery management, grafting", "🏡");
        createSpecIfNotExists("Post-Harvest Technology", "POST_HARVEST", "Storage, packaging, cold chain, and minimizing post-harvest loss", "📦");
        createSpecIfNotExists("Agri-Economics & Market Linkage", "AGRI_ECONOMICS", "Cost-benefit analysis, pricing, collective marketing and value chains", "📊");
    }

    private void createSpecIfNotExists(String name, String code, String description, String icon) {
        if (!specializationRepository.existsByCode(code)) {
            specializationRepository.save(new Specialization(name, code, description, icon));
        }
    }

    private void seedLocations() {
        Location koshi = getOrCreateLocation(null, "Koshi Province", "कोशी प्रदेश", LocationType.PROVINCE);
        Location madhesh = getOrCreateLocation(null, "Madhesh Province", "मधेश प्रदेश", LocationType.PROVINCE);
        Location bagmati = getOrCreateLocation(null, "Bagmati Province", "बागमती प्रदेश", LocationType.PROVINCE);
        Location gandaki = getOrCreateLocation(null, "Gandaki Province", "गण्डकी प्रदेश", LocationType.PROVINCE);
        Location lumbini = getOrCreateLocation(null, "Lumbini Province", "लुम्बिनी प्रदेश", LocationType.PROVINCE);
        Location karnali = getOrCreateLocation(null, "Karnali Province", "कर्णाली प्रदेश", LocationType.PROVINCE);
        Location sudurpashchim = getOrCreateLocation(null, "Sudurpashchim Province", "सुदूरपश्चिम प्रदेश", LocationType.PROVINCE);

        // Sample key agricultural districts
        getOrCreateLocation(koshi, "Jhapa", "झापा", LocationType.DISTRICT);
        getOrCreateLocation(koshi, "Ilam", "इलाम", LocationType.DISTRICT);
        getOrCreateLocation(koshi, "Morang", "मोरङ", LocationType.DISTRICT);

        getOrCreateLocation(madhesh, "Dhanusha", "धनुषा", LocationType.DISTRICT);
        getOrCreateLocation(madhesh, "Sarlahi", "सर्लाही", LocationType.DISTRICT);

        getOrCreateLocation(bagmati, "Kathmandu", "काठमाडौँ", LocationType.DISTRICT);
        getOrCreateLocation(bagmati, "Lalitpur", "ललितपुर", LocationType.DISTRICT);
        getOrCreateLocation(bagmati, "Bhaktapur", "भक्तपुर", LocationType.DISTRICT);
        getOrCreateLocation(bagmati, "Chitwan", "चितवन", LocationType.DISTRICT);
        getOrCreateLocation(bagmati, "Kavrepalanchok", "काभ्रेपलाञ्चोक", LocationType.DISTRICT);

        getOrCreateLocation(gandaki, "Kaski", "कास्की", LocationType.DISTRICT);
        getOrCreateLocation(gandaki, "Mustang", "मुस्ताङ", LocationType.DISTRICT);

        getOrCreateLocation(lumbini, "Rupandehi", "रुपन्देही", LocationType.DISTRICT);
        getOrCreateLocation(lumbini, "Kapilvastu", "कपिलवस्तु", LocationType.DISTRICT);

        getOrCreateLocation(karnali, "Jumla", "जुम्ला", LocationType.DISTRICT);
        getOrCreateLocation(sudurpashchim, "Kailali", "कैलाली", LocationType.DISTRICT);
    }

    private Location getOrCreateLocation(Location parent, String name, String nepaliName, LocationType type) {
        return locationRepository.findByNameAndType(name, type)
                .orElseGet(() -> locationRepository.save(new Location(parent, name, nepaliName, type)));
    }

    private void seedExpertDocuments() {
        List<ExpertProfile> profiles = expertProfileRepository.findAll();
        for (ExpertProfile profile : profiles) {
            if (expertDocumentRepository.findByExpertProfileId(profile.getId()).isEmpty()) {
                String expName = profile.getUser() != null
                        ? (profile.getUser().getFirstName() + "_" + profile.getUser().getLastName()).replaceAll("\\s+", "_")
                        : "Expert";
                expertDocumentRepository.save(new ExpertDocument(
                        profile,
                        "IDENTITY",
                        "National ID / Citizenship Card",
                        "Citizenship_" + expName + ".pdf",
                        "application/pdf",
                        "1.8 MB",
                        "/sample-docs/citizenship.pdf"
                ));
                expertDocumentRepository.save(new ExpertDocument(
                        profile,
                        "EDUCATION",
                        "Degree Certificate (" + (profile.getQualification() != null ? profile.getQualification() : "B.Sc. Agriculture") + ")",
                        "Degree_Certificate_" + expName + ".pdf",
                        "application/pdf",
                        "2.4 MB",
                        "/sample-docs/degree.pdf"
                ));
                expertDocumentRepository.save(new ExpertDocument(
                        profile,
                        "LICENSE",
                        "Nepal Agricultural Council License",
                        "Agriculture_Council_License_" + expName + ".pdf",
                        "application/pdf",
                        "950 KB",
                        "/sample-docs/license.pdf"
                ));
                expertDocumentRepository.save(new ExpertDocument(
                        profile,
                        "EXPERIENCE",
                        "Professional Agricultural Experience Certificate",
                        "Experience_Letter_" + expName + ".pdf",
                        "application/pdf",
                        "1.2 MB",
                        "/sample-docs/experience.pdf"
                ));
                log.info("Seeded verification documents for expert profile ID {}", profile.getId());
            }
        }

        // Ensure at least one profile has SUBMITTED status for admin verification queue
        if (expertProfileRepository.countByApplicationStatus(com.krishiai.expert.entity.ExpertApplicationStatus.SUBMITTED) == 0) {
            profiles.stream()
                    .filter(p -> p.getUser() != null && "expert@gmail.com".equalsIgnoreCase(p.getUser().getEmail()))
                    .findFirst()
                    .ifPresent(p -> {
                        p.setApplicationStatus(com.krishiai.expert.entity.ExpertApplicationStatus.SUBMITTED);
                        p.setSubmittedAt(java.time.LocalDateTime.now());
                        p.setVerifiedExpert(false);
                        expertProfileRepository.save(p);
                        log.info("Reset expert profile {} (expert@gmail.com) to SUBMITTED for verification queue", p.getId());
                    });
        }
    }
}
