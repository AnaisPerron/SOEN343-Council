package com.council.tenderapp.dlv.entities;

import java.util.Locale;

public enum AssignmentStatus {
    OFFERED, ACCEPTED, COMPLETED, ENDED;

    public String value() { return name().toLowerCase(Locale.ROOT); }
    public boolean isActive() { return this == OFFERED || this == ACCEPTED; }
}
