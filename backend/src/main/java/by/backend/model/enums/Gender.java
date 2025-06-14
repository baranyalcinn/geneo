package by.backend.model.enums;

public enum Gender {
    MALE("Erkek"),
    FEMALE("Kadın");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
} 