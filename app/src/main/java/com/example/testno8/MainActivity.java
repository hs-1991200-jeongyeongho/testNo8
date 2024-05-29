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
import com.google.protobuf.ByteString;
import okhttp3.*;
import java.io.*;
import java.util.Base64;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION_SEND_SMS = 1; // SMS 보내기 권한 요청 코드

    private static final int REQUEST_PERMISSION_POST_NOTIFICATIONS = 2; // 알림 권한 요청 코드
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

        if (requestCode == REQUEST_PERMISSION_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // SMS 보내기 권한이 허용된 경우 알림 및 SMS 보내기
                addNotificationChannel(); // 알림 채널 추가
                sendNotification(); // 알림 보내기
            } else {
                // SMS 보내기 권한이 거부된 경우 사용자에게 알림 또는 대체 로직을 제공할 수 있음
                Toast.makeText(this, "SMS 보내기 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PERMISSION_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // POST_NOTIFICATIONS 권한이 허용된 경우 알림 보내기
                sendNotification();
            } else {
                // POST_NOTIFICATIONS 권한이 거부된 경우 사용자에게 알림 또는 대체 로직을 제공할 수 있음
                Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }

        if (permissionToReadStorageAccepted) {
            transcribeAudioFromTphone();
        } else {
            finish();
        }
    }

    /*public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // SMS 보내기 권한이 허용된 경우 알림 및 SMS 보내기
                addNotificationChannel(); // 알림 채널 추가
                sendNotification(); // 알림 보내기
            } else {
                // SMS 보내기 권한이 거부된 경우 사용자에게 알림 또는 대체 로직을 제공할 수 있음
                Toast.makeText(this, "SMS 보내기 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PERMISSION_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // POST_NOTIFICATIONS 권한이 허용된 경우 알림 보내기
                sendNotification();
            } else {
                // POST_NOTIFICATIONS 권한이 거부된 경우 사용자에게 알림 또는 대체 로직을 제공할 수 있음
                Toast.makeText(this, "알림 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }*/

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
        //String keyFilePath = "android.resource://" + getPackageName() + "/" + R.raw.credentials;

        // JSON 키 파일을 사용하여 GoogleCredentials를 만듭니다.
        //GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(keyFilePath));

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

    // 알림 채널 추가 메서드
    private void addNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel name";
            String description = "Channel description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("default", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // 알림 보내기 메서드
    private void sendNotification() {
        // 알림 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_PERMISSION_POST_NOTIFICATIONS);
            return;
        }

        // 인텐트 생성
        Intent intent = new Intent(this, AlertActivity.class);
        intent.setAction("com.example.apptest.ACTION_SMS_DIALOG");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // 알림 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.baseline_notification_important_24)
                .setContentTitle("보이스 피싱 탐지")
                .setContentText("보이스 피싱이 감지 됐습니다. 조심하세요.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }

    // 권환 확인 후 알림 보내기 실행 메서드
    // 추후 AI 모델 연동 후 정확성(probability) 부분 변경 필요 임시로 일단 넣어둠
    /*
    private void checkAndSendSMS() {
        if (probability > 0.85) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                // SEND_SMS 권한이 없는 경우 권한 요청 다이얼로그 표시
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS}, REQUEST_PERMISSION_SEND_SMS);
            } else {
                // SEND_SMS 권한이 있는 경우 알림 및 SMS 보내기
                addNotificationChannel(); // 알림 채널 추가
                sendNotification(); // 알림 보내기
            }
        }
    } */


}