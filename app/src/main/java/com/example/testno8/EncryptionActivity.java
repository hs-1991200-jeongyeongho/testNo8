package com.example.testno8;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class EncryptionActivity extends AppCompatActivity {

    private final int REQUEST_CODE_OPEN_DOCUMENT_ENCRYPT = 1;
    private final int REQUEST_CODE_OPEN_DOCUMENT_DECRYPT = 2;
    private final int REQUEST_CODE_SAVE_DOCUMENT = 3;
    private SecretKey secretKey;
    private IvParameterSpec iv;
    private TextView txtEncrypt;
    private TextView txtDecrypt;
    private Button btnEncrypt;
    private Button btnDecrypt;
    private Uri selectedFileUri;
    private Uri selectedOutputUri;
    private boolean isEncrypting = false;
    private String originalExtension;

    // 토스트 메시지를 표시하는 유틸리티 메서드
    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtEncrypt = findViewById(R.id.txt_encrypted);
        txtDecrypt = findViewById(R.id.txt_decrypted);
        btnEncrypt = findViewById(R.id.btn_encrypt);
        btnDecrypt = findViewById(R.id.btn_decrypt);

        // 비밀 키와 초기화 벡터 생성
        secretKey = AESHelper.generateSecretKey();
        iv = AESHelper.generateIV();

        // 암호화 버튼 클릭 리스너 설정
        btnEncrypt.setOnClickListener(view -> {
            isEncrypting = true;
            openFileSelector(REQUEST_CODE_OPEN_DOCUMENT_ENCRYPT);
        });

        // 복호화 버튼 클릭 리스너 설정
        btnDecrypt.setOnClickListener(view -> {
            isEncrypting = false;
            openFileSelector(REQUEST_CODE_OPEN_DOCUMENT_DECRYPT);
        });
    }

    // 파일 선택기를 열어 파일을 선택하도록 요청하는 메서드
    private void openFileSelector(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, requestCode);
    }

    // 파일 저장 위치를 선택하도록 요청하는 메서드
    private void openSaveFilePicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, isEncrypting ? "encryptedFile" : "decryptedFile." + originalExtension);
        startActivityForResult(intent, REQUEST_CODE_SAVE_DOCUMENT);
    }

    // 선택한 파일을 임시 파일로 만드는 메서드
    private File createFile(Uri uri) {
        try {
            FileInputStream inputStream = (FileInputStream) getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("temp", null, getCacheDir());
            if (inputStream != null) {
                FileOutputStream output = new FileOutputStream(tempFile);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                output.close();
            }
            originalExtension = getFileExtension(tempFile.getName());
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 파일 이름에서 확장자를 추출하는 메서드
    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    // 임시 파일을 선택한 출력 위치로 복사하는 메서드
    private void writeFile(File inputFile, Uri outputUri) {
        try {
            FileInputStream inputStream = new FileInputStream(inputFile);
            FileOutputStream outputStream = (FileOutputStream) getContentResolver().openOutputStream(outputUri);
            if (outputStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 파일 선택기 및 저장기에서 반환된 결과를 처리하는 메서드
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            switch (requestCode) {
                case REQUEST_CODE_OPEN_DOCUMENT_ENCRYPT:
                case REQUEST_CODE_OPEN_DOCUMENT_DECRYPT:
                    selectedFileUri = uri;
                    openSaveFilePicker();
                    break;
                case REQUEST_CODE_SAVE_DOCUMENT:
                    selectedOutputUri = uri;
                    File inputFile = createFile(selectedFileUri);
                    File tempOutputFile = null;
                    try {
                        tempOutputFile = File.createTempFile("temp_output", null, getCacheDir());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (tempOutputFile == null) return;
                    if (isEncrypting) {
                        AESHelper.encryptFile(inputFile, secretKey, iv, tempOutputFile);
                        txtEncrypt.setText("File encrypted successfully: " + selectedOutputUri.getPath());
                    } else {
                        AESHelper.decryptFile(inputFile, secretKey, iv, tempOutputFile);
                        txtDecrypt.setText("File decrypted successfully: " + selectedOutputUri.getPath());
                    }
                    writeFile(tempOutputFile, selectedOutputUri);
                    break;
            }
        }
    }
}