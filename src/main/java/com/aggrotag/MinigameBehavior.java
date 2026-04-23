package com.aggrotag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MinigameBehavior {
    SHOW_ALL("Show Everything"),
    HIDE_NAMES("Show Max Hits Only"),
    DISABLE_ENTIRELY("Disable Plugin");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}
