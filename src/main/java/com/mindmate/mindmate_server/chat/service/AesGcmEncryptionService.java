package com.mindmate.mindmate_server.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmEncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // 인증 태그 길이
    private static final int IV_LENGTH_BYTE = 12; // IV (초기화 벡터) 길이
    private static final int AES_KEY_BIT = 256;

    private final SecretKey secretKey;

    public AesGcmEncryptionService(@Value("${encryption.secret-key}") String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }

    /**
     * 문자열 암호화하고 Base64로 인코딩하여 반환
     */
    public String encrypt(String platinText) {
        try {
            // 랜덤 IV 생성 -> 매 암호화마다 새로운 IV 사용
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            // 암호화 수행
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(platinText.getBytes(StandardCharsets.UTF_8));

            // IV와 암호문 결합
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("암호화 과정에서 오류가 발생했습니다.", e);
        }
    }

    /**
     * Base64로 인코딩된 암호문을 복호화하여 원본 문자열 반환
     */
    public String decrypt(String encryptedText) {
        try {
            byte[] cipherMessage = Base64.getDecoder().decode(encryptedText);

            // IV 추출
            ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            // 암호문 추출
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // 복호화 수행
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 복호화된 텍스트 반환
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("복호화 과정에서 오류가 발생했습니다.", e);
        }
    }

    /**
     * 새로운 AES 키 생성 (키 교체 시 사용)
     */
    public static String generateNewKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(AES_KEY_BIT);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("키 생성 중 오류가 발생했습니다.", e);
        }
    }
}
