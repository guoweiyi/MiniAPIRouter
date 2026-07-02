package com.miniapi.router.core.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class CryptoUtils {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN = 12;
    private final byte[] keyBytes;
    private final SecureRandom random = new SecureRandom();

    public CryptoUtils(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        for (int i = 0; i < 32; i++) {
            key[i] = i < raw.length ? raw[i] : 0;
        }
        this.keyBytes = key;
    }

    public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("encrypt failed", e);
        }
    }

    public String decrypt(String enc) {
        if (enc == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(enc);
            byte[] iv = new byte[IV_LEN];
            byte[] cipherText = new byte[all.length - IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            System.arraycopy(all, IV_LEN, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("decrypt failed", e);
        }
    }

    public String mask(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        int len = apiKey.length();
        int prefix = Math.min(4, len / 4);
        return apiKey.substring(0, prefix) + "..." + apiKey.substring(len - 4);
    }
}
