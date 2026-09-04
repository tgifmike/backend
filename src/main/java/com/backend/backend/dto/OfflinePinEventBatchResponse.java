package com.backend.backend.dto;

public record OfflinePinEventBatchResponse(int accepted, int duplicates, int staleCredentials) {
}
