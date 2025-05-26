package by.backend.model.enums;

public enum Difficulty {
    EASY("Kolay", 30, 3),
    MEDIUM("Orta", 25, 4),
    HARD("Zor", 20, 5);

    private final String label;
    private final int timeLimit;
    private final int optionCount;

    Difficulty(String label, int timeLimit, int optionCount) {
        this.label = label;
        this.timeLimit = timeLimit;
        this.optionCount = optionCount;
    }

    public String getLabel() {
        return label;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public int getOptionCount() {
        return optionCount;
    }
} 