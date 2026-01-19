package com.application.employee.service.enums;

public enum VisaStatus {
    H1B("H1B"),
    OPT("OPT"),
    GREEN_CARD("Green Card"),
    H4_EAD("H4 EAD"),
    GREEN_CARD_EAD("Green Card EAD"),
    US_CITIZEN("US Citizen"),
    OPT_EXTENSION("OPT Extension");

    private final String displayName;

    VisaStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static VisaStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (VisaStatus status : VisaStatus.values()) {
            if (status.name().equalsIgnoreCase(value) || 
                status.displayName.equalsIgnoreCase(value) ||
                value.replaceAll("\\s+", "_").equalsIgnoreCase(status.name())) {
                return status;
            }
        }
        // Try to match common variations
        String normalized = value.toUpperCase().replaceAll("\\s+", "_");
        if (normalized.equals("H4EAD")) normalized = "H4_EAD";
        if (normalized.equals("GREENCARDEAD")) normalized = "GREEN_CARD_EAD";
        if (normalized.equals("OPTEXTENSION")) normalized = "OPT_EXTENSION";
        if (normalized.equals("USCITIZEN")) normalized = "US_CITIZEN";
        if (normalized.equals("GREENCARD")) normalized = "GREEN_CARD";
        
        try {
            return VisaStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

