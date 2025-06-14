package by.backend.model.enums;

import lombok.Getter;

/**
 * Türk aile yapısına uygun detaylı aile ilişki türleri
 * Cinsiyet ve generasyon bilgilerini içerir
 */
@Getter
public enum TurkishFamilyRelationType {
    // Ana-Baba (Ebeveynler)
    ANNE("Annesi", Gender.FEMALE, FamilyGeneration.PARENT, true),
    BABA("Babası", Gender.MALE, FamilyGeneration.PARENT, true),
    
    // Çocuklar
    KIZ_COCUK("Kızı", Gender.FEMALE, FamilyGeneration.CHILD, true),
    ERKEK_COCUK("Oğlu", Gender.MALE, FamilyGeneration.CHILD, true),
    
    // Kardeşler
    KIZ_KARDES("Kız Kardeşi", Gender.FEMALE, FamilyGeneration.SAME, true),
    ERKEK_KARDES("Erkek Kardeşi", Gender.MALE, FamilyGeneration.SAME, true),
    
    // Büyükanne-Büyükbaba
    BUYUKANNE("Büyükannesi", Gender.FEMALE, FamilyGeneration.GRANDPARENT, true),
    BUYUKBABA("Büyükbabası", Gender.MALE, FamilyGeneration.GRANDPARENT, true),
    NENE("Nenesi", Gender.FEMALE, FamilyGeneration.GRANDPARENT, true),
    DEDE("Dedesi", Gender.MALE, FamilyGeneration.GRANDPARENT, true),
    
    // Torunlar
    KIZ_TORUN("Kız Torunu", Gender.FEMALE, FamilyGeneration.GRANDCHILD, true),
    ERKEK_TORUN("Erkek Torunu", Gender.MALE, FamilyGeneration.GRANDCHILD, true),
    
    // Dayı-Teyze (Anne tarafı)
    DAYI("Dayısı", Gender.MALE, FamilyGeneration.PARENT, false, RelationshipSide.MATERNAL),
    TEYZE("Teyzesi", Gender.FEMALE, FamilyGeneration.PARENT, false, RelationshipSide.MATERNAL),
    
    // Amca-Hala (Baba tarafı)
    AMCA("Amcası", Gender.MALE, FamilyGeneration.PARENT, false, RelationshipSide.PATERNAL),
    HALA("Halası", Gender.FEMALE, FamilyGeneration.PARENT, false, RelationshipSide.PATERNAL),
    
    // Yeğenler
    YEGEN_KIZ("Kız Yeğeni", Gender.FEMALE, FamilyGeneration.CHILD, false),
    YEGEN_ERKEK("Erkek Yeğeni", Gender.MALE, FamilyGeneration.CHILD, false),
    
    // Kuzenler
    KUZEN_KIZ("Kız Kuzeni", Gender.FEMALE, FamilyGeneration.SAME, false),
    KUZEN_ERKEK("Erkek Kuzeni", Gender.MALE, FamilyGeneration.SAME, false),
    
    // Eşler
    ES_KADIN("Eşi", Gender.FEMALE, FamilyGeneration.SAME, true),
    ES_ERKEK("Eşi", Gender.MALE, FamilyGeneration.SAME, true),
    GELIN("Gelini", Gender.FEMALE, FamilyGeneration.SAME, false),
    DAMAT("Damadı", Gender.MALE, FamilyGeneration.SAME, false),
    
    // Kayın ailesi
    KAYNANA("Kaynanası", Gender.FEMALE, FamilyGeneration.PARENT, false),
    KAYNATA("Kaynatası", Gender.MALE, FamilyGeneration.PARENT, false),
    KAYIN_BIRADER("Kayınbiraderi", Gender.MALE, FamilyGeneration.SAME, false),
    BALDIZ("Baldızı", Gender.FEMALE, FamilyGeneration.SAME, false),
    GORUMCE("Görümcesi", Gender.FEMALE, FamilyGeneration.SAME, false),
    ENISTE("Eniştesi", Gender.MALE, FamilyGeneration.SAME, false);

    private final String turkishName;
    private final Gender gender;
    private final FamilyGeneration generation;
    private final boolean isDirectFamily;
    private final RelationshipSide side;

    TurkishFamilyRelationType(String turkishName, Gender gender, FamilyGeneration generation, boolean isDirectFamily) {
        this(turkishName, gender, generation, isDirectFamily, RelationshipSide.BOTH);
    }

    TurkishFamilyRelationType(String turkishName, Gender gender, FamilyGeneration generation, boolean isDirectFamily, RelationshipSide side) {
        this.turkishName = turkishName;
        this.gender = gender;
        this.generation = generation;
        this.isDirectFamily = isDirectFamily;
        this.side = side;
    }

    public String getTurkishName() { return turkishName; }
    public Gender getGender() { return gender; }
    public FamilyGeneration getGeneration() { return generation; }
    public boolean isDirectFamily() { return isDirectFamily; }
    public RelationshipSide getSide() { return side; }

    /**
     * Belirtilen cinsiyete uygun ilişki türlerini döndürür
     */
    public static TurkishFamilyRelationType[] getByGender(Gender gender) {
        return java.util.Arrays.stream(values())
                .filter(type -> type.gender == gender)
                .toArray(TurkishFamilyRelationType[]::new);
    }

    /**
     * Doğrudan aile üyesi olan ilişki türlerini döndürür
     */
    public static TurkishFamilyRelationType[] getDirectFamilyTypes() {
        return java.util.Arrays.stream(values())
                .filter(TurkishFamilyRelationType::isDirectFamily)
                .toArray(TurkishFamilyRelationType[]::new);
    }

    /**
     * Belirtilen generasyona ait ilişki türlerini döndürür
     */
    public static TurkishFamilyRelationType[] getByGeneration(FamilyGeneration generation) {
        return java.util.Arrays.stream(values())
                .filter(type -> type.generation == generation)
                .toArray(TurkishFamilyRelationType[]::new);
    }

    public enum FamilyGeneration {
        GRANDPARENT(-2, "Büyükanne/Büyükbaba"),
        PARENT(-1, "Ebeveyn"), 
        SAME(0, "Aynı Kuşak"),
        CHILD(1, "Çocuk"),
        GRANDCHILD(2, "Torun");

        private final int generationDifference;
        private final String description;

        FamilyGeneration(int generationDifference, String description) {
            this.generationDifference = generationDifference;
            this.description = description;
        }

        public int getGenerationDifference() { return generationDifference; }
        public String getDescription() { return description; }
    }

    public enum RelationshipSide {
        MATERNAL("Anne Tarafı"),
        PATERNAL("Baba Tarafı"), 
        BOTH("Her İki Taraf");

        private final String description;

        RelationshipSide(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }
} 