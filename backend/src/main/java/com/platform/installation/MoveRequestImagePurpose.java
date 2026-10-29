package com.platform.installation;

import java.util.Locale;

public enum MoveRequestImagePurpose {
    APPLIANCE,
    INSTALLATION_ENV;

    public static MoveRequestImagePurpose from(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidMoveRequestImageException();
        }
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidMoveRequestImageException();
        }
    }
}
