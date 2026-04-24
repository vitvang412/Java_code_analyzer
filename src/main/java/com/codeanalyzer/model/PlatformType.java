package com.codeanalyzer.model;

public enum PlatformType {
    CODEFORCES("Codeforces"),
    VJUDGE("VJudge");

    private final String displayName;

    PlatformType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
