package com.mindmate.mindmate_server.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AesGcmEncryptionServiceTest {
    private AesGcmEncryptionService encryptionService;
    private String secretKey;

    @BeforeEach
    void setup() {
        secretKey = AesGcmEncryptionService.generateNewKey();
        encryptionService = new AesGcmEncryptionService(secretKey);
    }

    @ParameterizedTest
    @DisplayName("다양한 문자열 암호화 및 복호화 테스트")
    @MethodSource("encryptionTestCases")
    void encryptAndDecrypt_ShouldWork(String original) {
        // when
        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);

        // then
        assertThat(encrypted).isNotEqualTo(original);
        assertThat(decrypted).isEqualTo(original);
    }

    static Stream<String> encryptionTestCases() {
        return Stream.of(
                "Hello, World!",
                "특수문자 테스트 !@#$%^&*()",
                "한글 테스트 가나다라마바사",
                "Empty string test",
                "1234567890",
                "Long text " + "a".repeat(1000)
        );
    }

    @Test
    @DisplayName("동일한 평문 암호화 -> 매번 다른 결과")
    void encrypt_ShouldProduceDifferentResults() {
        // given
        String plainText = "Same plain text";

        // when
        String encrypted1 = encryptionService.encrypt(plainText);
        String encrypted2 = encryptionService.encrypt(plainText);

        // then
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("잘못된 암호화 복호문 시 예외")
    void decrypt_InvalidCipherText_ShouldThrowException() {
        // given
        String invalidCiphertext = "invalid==";

        // when & then
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(invalidCiphertext));
    }

    @Test
    @DisplayName("다른 키로 암호화된 텍스트는 복호화 실패")
    void decrypt_WithDifferentKey_ShouldFail() {
        // given
        String plainText = "Secret message";
        String encrypted = encryptionService.encrypt(plainText);

        String differentKey = AesGcmEncryptionService.generateNewKey();
        AesGcmEncryptionService differentService = new AesGcmEncryptionService(differentKey);

        // when & then
        assertThrows(RuntimeException.class, () -> differentService.decrypt(encrypted));
    }
}