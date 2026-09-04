package com.backend.backend.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PinLockoutPolicyTest {
    private final PinLockoutPolicy policy = new PinLockoutPolicy();

    @Test
    void implementsExactProgressiveSchedule() {
        assertThat(policy.durationForFailure(1)).isZero();
        assertThat(policy.durationForFailure(4)).isZero();
        assertThat(policy.durationForFailure(5)).isEqualTo(Duration.ofMinutes(5));
        assertThat(policy.durationForFailure(6)).isEqualTo(Duration.ofMinutes(15));
        assertThat(policy.durationForFailure(7)).isEqualTo(Duration.ofHours(1));
        assertThat(policy.durationForFailure(8)).isEqualTo(Duration.ofHours(4));
        assertThat(policy.durationForFailure(9)).isEqualTo(Duration.ofHours(8));
        assertThat(policy.durationForFailure(10)).isEqualTo(Duration.ofHours(24));
        assertThat(policy.durationForFailure(100)).isEqualTo(Duration.ofHours(24));
    }
}
