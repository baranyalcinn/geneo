package by.backend.model.enums;

/**
 * Türk aile yapısına uygun detaylı aile ilişki türleri
 * Cinsiyet ve generasyon bilgilerini içerir
 */
public enum TurkishFamilyRelationType {
    // Ana-Baba (Ebeveynler)
    ANNE("Anne", Gender.KADIN, FamilyGeneration.PARENT, true),
    BABA("Baba", Gender.ERKEK, FamilyGeneration.PARENT, true),
    
    // Çocuklar
    KIZ_COCUK("Kız Çocuk", Gender.KADIN, FamilyGeneration.CHILD, true),
    ERKEK_COCUK("Erkek Çocuk", Gender.ERKEK, FamilyGeneration.CHILD, true),
    
    // Kardeşler
    KIZ_KARDES("Kız Kardeş", Gender.KADIN, FamilyGeneration.SAME, true),
    ERKEK_KARDES("Erkek Kardeş", Gender.ERKEK, FamilyGeneration.SAME, true),
    
    // Büyükanne-Büyükbaba
    BUYUKANNE("Büyükanne", Gender.KADIN, FamilyGeneration.GRANDPARENT, true),
    BUYUKBABA("Büyükbaba", Gender.ERKEK, FamilyGeneration.GRANDPARENT, true),
    NENE("Nene", Gender.KADIN, FamilyGeneration.GRANDPARENT, true),
    DEDE("Dede", Gender.ERKEK, FamilyGeneration.GRANDPARENT, true),
    
    // Torunlar
    KIZ_TORUN("Kız Torun", Gender.KADIN, FamilyGeneration.GRANDCHILD, true),
    ERKEK_TORUN("Erkek Torun", Gender.ERKEK, FamilyGeneration.GRANDCHILD, true),
    
    // Anne Tarafı Akrabalar
    DAYI("Dayı", Gender.ERKEK, FamilyGeneration.PARENT, false, RelationshipSide.MATERNAL),
    TEYZE("Teyze", Gender.KADIN, FamilyGeneration.PARENT, false, RelationshipSide.MATERNAL),
    
    // Baba Tarafı Akrabalar  
    AMCA("Amca", Gender.ERKEK, FamilyGeneration.PARENT, false, RelationshipSide.PATERNAL),
    HALA("Hala", Gender.KADIN, FamilyGeneration.PARENT, false, RelationshipSide.PATERNAL),
    
    // Yeğenler
    YEGEN_KIZ("Yeğen (Kız)", Gender.KADIN, FamilyGeneration.CHILD, false),
    YEGEN_ERKEK("Yeğen (Erkek)", Gender.ERKEK, FamilyGeneration.CHILD, false),
    
    // Kuzenler
    KUZEN_KIZ("Kuzen (Kız)", Gender.KADIN, FamilyGeneration.SAME, false),
    KUZEN_ERKEK("Kuzen (Erkek)", Gender.ERKEK, FamilyGeneration.SAME, false),
    
    // Evlilik İlişkileri
    ES_KADIN("Eş (Kadın)", Gender.KADIN, FamilyGeneration.SAME, true),
    ES_ERKEK("Eş (Erkek)", Gender.ERKEK, FamilyGeneration.SAME, true),
    GELIN("Gelin", Gender.KADIN, FamilyGeneration.SAME, false),
    DAMAT("Damat", Gender.ERKEK, FamilyGeneration.SAME, false),
    
    // Kayın İlişkileri
    KAYNANA("Kaynana", Gender.KADIN, FamilyGeneration.PARENT, false),
    KAYNATA("Kaynata", Gender.ERKEK, FamilyGeneration.PARENT, false),
    KAYIN_BIRADER("Kayınbirader", Gender.ERKEK, FamilyGeneration.SAME, false),
    BALDIZ("Baldız", Gender.KADIN, FamilyGeneration.SAME, false),
    GORUMCE("Görümce", Gender.KADIN, FamilyGeneration.SAME, false),
    ENISTE("Enişte", Gender.ERKEK, FamilyGeneration.SAME, false);

    private final String turkishName;
    private final Gender requiredGender;
    private final FamilyGeneration generation;
    private final boolean isDirectFamily;
    private final RelationshipSide side;

    TurkishFamilyRelationType(String turkishName, Gender requiredGender, 
                            FamilyGeneration generation, boolean isDirectFamily) {
        this(turkishName, requiredGender, generation, isDirectFamily, RelationshipSide.BOTH);
    }

    TurkishFamilyRelationType(String turkishName, Gender requiredGender, 
                            FamilyGeneration generation, boolean isDirectFamily, 
                            RelationshipSide side) {
        this.turkishName = turkishName;
        this.requiredGender = requiredGender;
        this.generation = generation;
        this.isDirectFamily = isDirectFamily;
        this.side = side;
    }

    public String getTurkishName() { return turkishName; }
    public Gender getRequiredGender() { return requiredGender; }
    public FamilyGeneration getGeneration() { return generation; }
    public boolean isDirectFamily() { return isDirectFamily; }
    public RelationshipSide getSide() { return side; }

    /**
     * Belirtilen cinsiyete uygun ilişki türlerini döndürür
     */
    public static TurkishFamilyRelationType[] getByGender(Gender gender) {
        return java.util.Arrays.stream(values())
                .filter(type -> type.requiredGender == gender)
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