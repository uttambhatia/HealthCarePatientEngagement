package com.healthcare.medicalrecord.exception;

public class VersionConflictException extends RuntimeException {
    public VersionConflictException(String recordId, int expectedVersion, int actualVersion) {
        super("Medical record version conflict for id=" + recordId + " expectedVersion=" + expectedVersion + " actualVersion=" + actualVersion);
    }
}
