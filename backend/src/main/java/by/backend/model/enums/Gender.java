package by.backend.model.enums;

public enum Gender {
    ERKEK("Erkek"),
    KADIN("Kadın");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
} 