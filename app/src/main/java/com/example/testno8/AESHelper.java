package com.example.testno8;

import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESHelper {

    private static final int KEYSIZE = 256;

    // 비밀 키를 생성하는 메서드
    public static SecretKey generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(KEYSIZE);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("비밀 키 생성 중 오류 발생", e);
        }
    }

    // 사용자 정의 비밀 키를 생성하는 메서드
    public static SecretKeySpec generateSecretKey(String customSecretKey) {
        return new SecretKeySpec(customSecretKey.getBytes(), "AES");
    }

    // 초기화 벡터(IV)를 생성하는 메서드
    public static IvParameterSpec generateIV() {
        byte[] iv = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    // 사용자 정의 초기화 벡터(IV)를 생성하는 메서드
    public static IvParameterSpec generateIV(String customIV) {
        return new IvParameterSpec(customIV.getBytes());
    }

    // 문자열을 암호화하는 메서드
    public static String encrypt(String textToEncrypt, SecretKey secretKey, IvParameterSpec iv) {
        try {
            byte[] plainText = textToEncrypt.getBytes();
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] encrypt = cipher.doFinal(plainText);
            return Base64.encodeToString(encrypt, Base64.DEFAULT);
        } catch (Exception e) {
            throw new RuntimeException("암호화 중 오류 발생", e);
        }
    }

    // 암호화된 문자열을 복호화하는 메서드
    public static String decrypt(String encryptedText, SecretKey secretKey, IvParameterSpec iv) {
        try {
            byte[] textToDecrypt = Base64.decode(encryptedText, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
            byte[] decrypt = cipher.doFinal(textToDecrypt);
            return new String(decrypt);
        } catch (Exception e) {
            throw new RuntimeException("복호화 중 오류 발생", e);
        }
    }

    // 파일을 암호화하는 메서드
    public static void encryptFile(File file, SecretKey secretKey, IvParameterSpec iv, File outputFile) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            FileInputStream inputStream = new FileInputStream(file);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    outputStream.write(output);
                }
            }
            byte[] finalOutput = cipher.doFinal();
            if (finalOutput != null) {
                outputStream.write(finalOutput);
            }

            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("파일 암호화 중 오류 발생", e);
        }
    }

    // 파일을 복호화하는 메서드
    public static void decryptFile(File file, SecretKey secretKey, IvParameterSpec iv, File outputFile) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            FileInputStream inputStream = new FileInputStream(file);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    outputStream.write(output);
                }
            }
            byte[] finalOutput = cipher.doFinal();
            if (finalOutput != null) {
                outputStream.write(finalOutput);
            }

            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException("파일 복호화 중 오류 발생", e);
        }
    }

    // 데이터를 Base64로 인코딩하는 메서드
    public static String encodeToBase64(byte[] dataToEncode) {
        return Base64.encodeToString(dataToEncode, Base64.DEFAULT);
    }

    // Base64로 인코딩된 데이터를 디코딩하는 메서드
    public static String decodeFromBase64(String dataToDecode) {
        return new String(Base64.decode(dataToDecode, Base64.DEFAULT));
    }
}