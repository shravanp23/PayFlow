package com.payflow.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSignerTest {

    private final HmacSigner signer = new HmacSigner("test-secret-key");

    @Test
    void sameInputProducesSameSignature() {
        String payload = "{\"eventId\":\"evt_1\"}";
        assertThat(signer.sign(payload)).isEqualTo(signer.sign(payload));
    }

    @Test
    void differentInputProducesDifferentSignature() {
        assertThat(signer.sign("a")).isNotEqualTo(signer.sign("b"));
    }

    @Test
    void verifyAcceptsCorrectSignature() {
        String payload = "{\"eventId\":\"evt_1\"}";
        String sig = signer.sign(payload);
        assertThat(signer.verify(payload, sig)).isTrue();
    }

    @Test
    void verifyRejectsTamperedPayload() {
        String payload = "{\"eventId\":\"evt_1\"}";
        String sig = signer.sign(payload);
        assertThat(signer.verify("{\"eventId\":\"evt_2\"}", sig)).isFalse();
    }

    @Test
    void verifyRejectsNullSignature() {
        assertThat(signer.verify("payload", null)).isFalse();
    }
}
