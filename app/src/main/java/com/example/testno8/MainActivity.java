package com.example.testno8;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String TAG = "MainActivity";
    private static final String API_KEY = "YOUR_API_KEY";  // Replace with your actual API key
    private boolean permissionToReadStorageAccepted = false;
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionToReadStorageAccepted = requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (permissionToReadStorageAccepted) {
            transcribeAudioFromTphone();
        } else {
            finish();
        }
    }

    private void transcribeAudioFromTphone() {
        // T전화 녹음 파일 경로 (예시 경로, 실제 경로는 확인 필요)
        String filePath = "/storage/emulated/0/Call/20240527.wav";

        // T전화에서 녹음된 파일을 Google STT로 변환
        try {
            transcribeAudio(filePath);
        } catch (IOException e) {
            Log.e(TAG, "Error reading audio file: " + e.getMessage());
        }
    }

    private void transcribeAudio(String filePath) throws IOException {
        // JSON 키 파일의 경로 (클라이언트 인증 정보) 현재 credentials.json 파일이 없기 때문에 R.raw.credentials 부분 오류
        String keyFilePath = "android.resource://" + getPackageName() + "/" + R.raw.credentials;

        // JSON 키 파일을 사용하여 GoogleCredentials를 만듭니다.
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(keyFilePath));

        OkHttpClient client = new OkHttpClient();
        File audioFile = new File(filePath);
        byte[] audioBytes = new byte[(int) audioFile.length()];
        try (FileInputStream fis = new FileInputStream(audioFile)) {
            fis.read(audioBytes);
        }

        String audioContent = Base64.getEncoder().encodeToString(audioBytes);

        JsonObject config = new JsonObject();
        config.addProperty("encoding", "LINEAR16");
        config.addProperty("sampleRateHertz", 16000);
        config.addProperty("languageCode", "ko-KR");

        JsonObject audio = new JsonObject();
        audio.addProperty("content", audioContent);

        JsonObject request = new JsonObject();
        request.add("config", config);
        request.add("audio", audio);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                new Gson().toJson(request)
        );

        Request httpRequest = new Request.Builder()
                .url("https://speech.googleapis.com/v1/speech:recognize?key=" + API_KEY)
                .post(body)
                .build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error during API request: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "API request failed: " + response.body().string());
                    return;
                }

                String responseBody = response.body().string();
                Log.i(TAG, "Transcription response: " + responseBody);

                // 변환된 텍스트를 파일에 저장
                saveTranscriptionToFile(responseBody);
            }
        });
    }

    private void saveTranscriptionToFile(String transcription) {
        // 변환된 텍스트를 저장할 파일 경로 설정
        String outputFile = "/storage/emulated/0/Call/transcription.txt";

        try {
            FileWriter writer = new FileWriter(outputFile);
            writer.write(transcription);
            writer.close();
            Log.i(TAG, "Transcription saved to file: " + outputFile);
        } catch (IOException e) {
            Log.e(TAG, "Error saving transcription to file: " + e.getMessage());
        }
    }
}