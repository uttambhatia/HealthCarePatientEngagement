package com.healthcare.careplan.exception;

public class VersionConflictException extends RuntimeException {
    public VersionConflictException(String carePlanId, int expectedVersion, int actualVersion) {
        super("Version conflict for careplan=" + carePlanId + " expectedVersion=" + expectedVersion + " actualVersion=" + actualVersion);
    }
}
