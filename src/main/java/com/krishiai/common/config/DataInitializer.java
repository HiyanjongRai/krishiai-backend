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
import com.krishiai.location.entity.MunicipalityType;
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

    @Value("${app.admin.Fullname}")
    private String adminFullName;


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
            jdbcTemplate.execute("ALTER TABLE crops ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000)");
            jdbcTemplate.execute("ALTER TABLE crops ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("UPDATE crops SET is_default = FALSE WHERE is_default IS NULL");
            jdbcTemplate.execute("ALTER TABLE crops ALTER COLUMN is_default SET DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE crops ALTER COLUMN is_default SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE crop_categories ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("UPDATE crop_categories SET is_default = FALSE WHERE is_default IS NULL");
            jdbcTemplate.execute("ALTER TABLE crop_categories ALTER COLUMN is_default SET DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE crop_categories ALTER COLUMN is_default SET NOT NULL");
            jdbcTemplate.execute("ALTER TABLE locations ADD COLUMN IF NOT EXISTS code VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE locations ADD COLUMN IF NOT EXISTS municipality_type VARCHAR(40)");
            log.info("Successfully ensured master-data columns exist");
        } catch (Exception e) {
            log.warn("Could not ensure master-data columns: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE expert_profiles ALTER COLUMN application_status TYPE VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE expert_profiles ALTER COLUMN verification_status TYPE VARCHAR(50)");
            log.info("Successfully ensured expert_profiles status columns are VARCHAR(50)");
        } catch (Exception e) {
            log.warn("Could not alter column lengths on expert_profiles: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check");
            jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING', 'BLOCKED'))");
            log.info("Successfully updated users_status_check constraint on users table");
        } catch (Exception e) {
            log.warn("Could not update users_status_check constraint: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE expert_crop_expertises ALTER COLUMN crop_id DROP NOT NULL");
            jdbcTemplate.execute("ALTER TABLE expert_crop_expertises ALTER COLUMN verification_status TYPE VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE expert_crop_expertises DROP CONSTRAINT IF EXISTS expert_crop_expertises_verification_status_check");
            jdbcTemplate.execute("ALTER TABLE expert_crop_expertises DROP CONSTRAINT IF EXISTS uq_ece_profile_crop");
            jdbcTemplate.execute("UPDATE expert_crop_expertises SET verification_status = 'SELF_DECLARED' WHERE verification_status IN ('PENDING', 'UNVERIFIED') OR verification_status IS NULL");
            jdbcTemplate.execute("UPDATE expert_crop_expertises SET source_type = 'SELF_DECLARED' WHERE source_type IS NULL");
            jdbcTemplate.execute("UPDATE expert_crop_expertises SET verification_method = 'NONE' WHERE verification_method IS NULL");
            jdbcTemplate.execute("UPDATE expert_crop_expertises SET expertise_level = 'INTERMEDIATE' WHERE expertise_level IS NULL");
            log.info("Successfully migrated expert_crop_expertises schema and preserved existing data");
        } catch (Exception e) {
            log.warn("Could not alter expert_crop_expertises table (may not exist yet or already updated): {}", e.getMessage());
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
                adminFullName
        );
        userRepository.save(admin);
        log.info("Successfully seeded default platform admin: {}", normalizedEmail);
    }

    private void seedCropCategoriesAndCrops() {
        CropCategory fruits = upsertCategory("Fruits", "FRUIT", "Fruit crops including orchard and tropical fruits.", "apple");
        CropCategory cereals = upsertCategory("Cereal Grains", "CEREAL", "Major cereal and grain crops.", "wheat");
        CropCategory vegetables = upsertCategory("Vegetables", "VEGETABLE", "Vegetable crops grown for food production.", "carrot");
        CropCategory pulses = upsertCategory("Pulses & Legumes", "PULSE", "Pulse and legume crops.", "bean");
        CropCategory oilCrops = upsertCategory("Oil Crops", "OIL_CROP", "Crops primarily grown for edible and industrial oils.", "flower");
        CropCategory cashCrops = upsertCategory("Cash Crops & Spices", "CASH_CROP", "Commercial cash crops and important agricultural crops.", "sprout");
        CropCategory rootTubers = upsertCategory("Root & Tuber Crops", "ROOT_TUBER", "Root and tuber crops.", "layers");
        CropCategory spices = upsertCategory("Spices", "SPICE", "Spice and condiment crops.", "leaf");

        upsertCrop(fruits, "Apple", "Malus domestica", "स्याउ", "apple", "Apple crop.", "https://w7.pngwing.com/pngs/973/255/png-transparent-red-apple-apple-fruit-apple-natural-foods-food-grocery-store-thumbnail.png", "Apple");
        upsertCrop(fruits, "Mango", "Mangifera indica", "आँप", "mango", "Mango crop.", "https://w7.pngwing.com/pngs/446/952/png-transparent-ripe-mangos-banganapalle-alphonso-mango-fruit-benishan-mango-natural-foods-food-citrus-thumbnail.png", "Mango");
        upsertCrop(fruits, "Papaya", "Carica papaya", "मेवा", "papaya", "Papaya crop.", "https://w7.pngwing.com/pngs/118/507/png-transparent-several-ripe-papaya-fruits-papaya-auglis-seed-food-fruit-papaya-nutrition-eating-green-papaya-thumbnail.png", "Papaya", "Payapa");
        upsertCrop(cereals, "Rice", "Oryza sativa", "धान", "rice", "Major cereal and staple crop.", "https://w7.pngwing.com/pngs/914/984/png-transparent-paddy-rice-rice-rice-hedao-paddy-rice-hedao-thumbnail.png", "Rice", "Rice (Paddy)");
        upsertCrop(cereals, "Maize", "Zea mays", "मकै", "maize", "Primary upland cereal crop.", "https://w7.pngwing.com/pngs/639/23/png-transparent-crop-maize-cereal-barrix-agro-sciences-private-limited-vegetable-corn-maize-s-food-pumpkin-millet-thumbnail.png", "Maize", "Maize (Corn)");
        upsertCrop(cereals, "Wheat", "Triticum aestivum", "गहुँ", "wheat", "Winter cereal grain.", "https://w7.pngwing.com/pngs/979/140/png-transparent-brown-wheats-illustration-common-wheat-wheat-germ-oil-gluten-cereal-germ-food-wheat-oat-nutrition-whole-grain-thumbnail.png", "Wheat");
        upsertCrop(cereals, "Millet", "Eleusine coracana", "कोदो", "millet", "Finger millet cereal crop.", "https://w7.pngwing.com/pngs/419/116/png-transparent-finger-millet-cereal-seed-popcorn-popcorn-food-five-spice-powder-millet-thumbnail.png", "Millet");
        upsertCrop(rootTubers, "Potato", "Solanum tuberosum", "आलु", "potato", "Root and tuber staple crop.", "https://w7.pngwing.com/pngs/74/390/png-transparent-mashed-potato-french-fries-potato-wedges-baked-potato-potato-chip-vegetable-food-baking-tomato-thumbnail.png", "Potato");
        upsertCrop(oilCrops, "Mustard", "Brassica juncea", "तोरी", "mustard", "Winter oilseed crop.", "https://w7.pngwing.com/pngs/350/543/png-transparent-mustard-plant-rapeseed-brassica-rapa-brassica-juncea-mustard-plant-stem-cabbage-flower-thumbnail.png", "Mustard");
        upsertCrop(pulses, "Lentil", "Lens culinaris", "मसुर", "lentil", "Pulse and legume crop.", "https://w7.pngwing.com/pngs/551/510/png-transparent-legume-vegetarian-cuisine-mung-bean-lentil-mung-thumbnail.png", "Lentil");
        upsertCrop(cashCrops, "Sugarcane", "Saccharum officinarum", "उखु", "sugarcane", "Commercial sugar crop.", "https://w7.pngwing.com/pngs/525/609/png-transparent-green-sugarcane-sugarcane-saccharum-officinarum-icon-green-cane-cane-sugar-cane-real-shot-chart-food-green-apple-fruit-thumbnail.png", "Sugarcane");

        upsertCrop(vegetables, "Tomato", "Solanum lycopersicum", "गोलभेँडा", "tomato", "Commercial greenhouse and open-field vegetable.", "https://w7.pngwing.com/pngs/689/481/png-transparent-tomato-tomato-natural-foods-food-nightshade-family-thumbnail.png ","Tomato");
        upsertCrop(vegetables, "Cauliflower", "Brassica oleracea", "काउली", "cauliflower", "Winter brassica vegetable.", "https://w7.pngwing.com/pngs/31/763/png-transparent-white-cauliflower-frutti-di-bosco-vegetable-fruit-cauliflower-cauliflower-leaf-vegetable-food-cooking-thumbnail.png", "Cauliflower");
        upsertCrop(spices, "Large Cardamom", "Amomum subulatum", "अलैँची", "cardamom", "High-value spice crop.", "https://w1.pngwing.com/pngs/306/989/png-transparent-green-tea-cardamom-true-cardamom-spice-black-cardamom-garam-masala-food-nutmeg-thumbnail.png", "Large Cardamom");
        upsertCrop(cashCrops, "Tea", "Camellia sinensis", "चिया", "tea", "Specialty hill beverage crop.", "https://w7.pngwing.com/pngs/644/368/png-transparent-green-tea-herbal-tea-tea-bag-green-tea-leaf-tea-herbal-tea-thumbnail.png", "Tea");
    }

    private CropCategory upsertCategory(String name, String code, String description, String icon) {
        CropCategory category = cropCategoryRepository.findByCodeIgnoreCase(code)
                .or(() -> cropCategoryRepository.findByNameIgnoreCase(name))
                .orElseGet(CropCategory::new);
        category.setName(name);
        category.setCode(code);
        category.setDescription(description);
        category.setIcon(icon);
        category.setDefaultCategory(true);
        category.setActive(true);
        return cropCategoryRepository.save(category);
    }

    private void upsertCrop(CropCategory category, String name, String scientificName, String nepaliName, String icon, String desc, String imageUrl, String... aliases) {
        Crop crop = null;
        for (String alias : aliases) {
            crop = cropRepository.findByNameIgnoreCase(alias).orElse(null);
            if (crop != null) {
                break;
            }
        }
        if (crop == null) {
            crop = cropRepository.findByNameIgnoreCase(name).orElseGet(Crop::new);
        }
        crop.setCategory(category);
        crop.setName(name);
        crop.setScientificName(scientificName);
        crop.setNepaliName(nepaliName);
        crop.setEmoji(icon);
        crop.setDescription(desc);
        if (imageUrl != null && (crop.getImageUrl() == null || crop.getImageUrl().isBlank())) {
            crop.setImageUrl(imageUrl);
        }
        crop.setDefaultCrop(true);
        crop.setActive(true);
        cropRepository.save(crop);
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

        Location jhapa = getOrCreateLocation(koshi, "Jhapa", "झापा", LocationType.DISTRICT);
        Location ilam = getOrCreateLocation(koshi, "Ilam", "इलाम", LocationType.DISTRICT);
        Location morang = getOrCreateLocation(koshi, "Morang", "मोरङ", LocationType.DISTRICT);

        Location dhanusha = getOrCreateLocation(madhesh, "Dhanusha", "धनुषा", LocationType.DISTRICT);
        Location sarlahi = getOrCreateLocation(madhesh, "Sarlahi", "सर्लाही", LocationType.DISTRICT);

        Location kathmandu = getOrCreateLocation(bagmati, "Kathmandu", "काठमाडौँ", LocationType.DISTRICT);
        Location lalitpur = getOrCreateLocation(bagmati, "Lalitpur", "ललितपुर", LocationType.DISTRICT);
        Location bhaktapur = getOrCreateLocation(bagmati, "Bhaktapur", "भक्तपुर", LocationType.DISTRICT);
        Location chitwan = getOrCreateLocation(bagmati, "Chitwan", "चितवन", LocationType.DISTRICT);
        Location kavre = getOrCreateLocation(bagmati, "Kavrepalanchok", "काभ्रेपलाञ्चोक", LocationType.DISTRICT);

        Location kaski = getOrCreateLocation(gandaki, "Kaski", "कास्की", LocationType.DISTRICT);
        Location mustang = getOrCreateLocation(gandaki, "Mustang", "मुस्ताङ", LocationType.DISTRICT);

        Location rupandehi = getOrCreateLocation(lumbini, "Rupandehi", "रुपन्देही", LocationType.DISTRICT);
        Location kapilvastu = getOrCreateLocation(lumbini, "Kapilvastu", "कपिलवस्तु", LocationType.DISTRICT);

        Location jumla = getOrCreateLocation(karnali, "Jumla", "जुम्ला", LocationType.DISTRICT);
        Location kailali = getOrCreateLocation(sudurpashchim, "Kailali", "कैलाली", LocationType.DISTRICT);

        upsertMunicipality(kathmandu, "Kathmandu Metropolitan City", "काठमाडौँ महानगरपालिका", "KMC", MunicipalityType.METROPOLITAN_CITY);
        upsertMunicipality(lalitpur, "Lalitpur Metropolitan City", "ललितपुर महानगरपालिका", "LMC", MunicipalityType.METROPOLITAN_CITY);
        upsertMunicipality(bhaktapur, "Bhaktapur Municipality", "भक्तपुर नगरपालिका", "BHAKTAPUR_MUN", MunicipalityType.MUNICIPALITY);
        upsertMunicipality(chitwan, "Bharatpur Metropolitan City", "भरतपुर महानगरपालिका", "BPC", MunicipalityType.METROPOLITAN_CITY);
        upsertMunicipality(kavre, "Dhulikhel Municipality", "धुलिखेल नगरपालिका", "DHULIKHEL", MunicipalityType.MUNICIPALITY);
        upsertMunicipality(kaski, "Pokhara Metropolitan City", "पोखरा महानगरपालिका", "POKHARA", MunicipalityType.METROPOLITAN_CITY);
        upsertMunicipality(mustang, "Gharapjhong Rural Municipality", "घरपझोङ गाउँपालिका", "GHARAPJHONG", MunicipalityType.RURAL_MUNICIPALITY);
        upsertMunicipality(jhapa, "Birtamod Municipality", "बिर्तामोड नगरपालिका", "BIRTAMOD", MunicipalityType.MUNICIPALITY);
        upsertMunicipality(ilam, "Ilam Municipality", "इलाम नगरपालिका", "ILAM_MUN", MunicipalityType.MUNICIPALITY);
        upsertMunicipality(morang, "Biratnagar Metropolitan City", "विराटनगर महानगरपालिका", "BIRATNAGAR", MunicipalityType.METROPOLITAN_CITY);
        upsertMunicipality(dhanusha, "Janakpurdham Sub-Metropolitan City", "जनकपुरधाम उपमहानगरपालिका", "JANAKPURDHAM", MunicipalityType.SUB_METROPOLITAN_CITY);
        upsertMunicipality(sarlahi, "Lalbandi Municipality", "लालबन्दी नगरपालिका", "LALBANDI", MunicipalityType.MUNICIPALITY);
        upsertMunicipality(rupandehi, "Butwal Sub-Metropolitan City", "बुटवल उपमहानगरपालिका", "BUTWAL", MunicipalityType.SUB_METROPOLITAN_CITY);
        upsertMunicipality(kapilvastu, "Kapilvastu Municipality", "कपिलवस्तु नगरपालिका", "KAPILVASTU_MUN", MunicipalityType.MUNICIPALITY);
        upsertMunicipality(jumla, "Chandannath Municipality", "चन्दननाथ नगरपालिका", "CHANDANNATH", MunicipalityType.MUNICIPALITY);
        upsertMunicipality(kailali, "Dhangadhi Sub-Metropolitan City", "धनगढी उपमहानगरपालिका", "DHANGADHI", MunicipalityType.SUB_METROPOLITAN_CITY);
    }

    private Location getOrCreateLocation(Location parent, String name, String nepaliName, LocationType type) {
        Location location = locationRepository.findByNameAndParentAndTypeIgnoreCase(name, parent != null ? parent.getId() : null, type)
                .or(() -> locationRepository.findByNameAndType(name, type))
                .orElseGet(Location::new);
        location.setParent(parent);
        location.setName(name);
        location.setNepaliName(nepaliName);
        location.setType(type);
        location.setCode(name.toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", ""));
        location.setActive(true);
        return locationRepository.save(location);
    }

    private void upsertMunicipality(Location district, String name, String nepaliName, String code, MunicipalityType municipalityType) {
        Location municipality = locationRepository.findByNameAndParentAndTypeIgnoreCase(name, district.getId(), LocationType.MUNICIPALITY)
                .orElseGet(Location::new);
        municipality.setParent(district);
        municipality.setName(name);
        municipality.setNepaliName(nepaliName);
        municipality.setCode(code);
        municipality.setType(LocationType.MUNICIPALITY);
        municipality.setMunicipalityType(municipalityType);
        municipality.setActive(true);
        locationRepository.save(municipality);
    }

    private void seedExpertDocuments() {
        List<ExpertProfile> profiles = expertProfileRepository.findAll();
        for (ExpertProfile profile : profiles) {
            if (expertDocumentRepository.findByExpertProfileId(profile.getId()).isEmpty()) {
                String expName = profile.getUser() != null
                        ? (profile.getUser().getFullName() + "_" ).replaceAll("\\s+", "_")
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
