package com.example.testno8;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Base64;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.telephony.SmsManager;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;


import org.tensorflow.lite.Interpreter;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_PERMISSION_SEND_SMS = 1; // SMS 보내기 권한 요청 코드
    private static final int REQUEST_PERMISSION_POST_NOTIFICATIONS = 2; // 알림 권한 요청 코드
    private static final int REQUEST_PERMISSION_MANAGE_EXTERNAL_STORAGE = 1001;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String TAG = "MainActivity";
    private static final String API_KEY = "YOUR_API_KEY";  // Replace with your actual API key
    private boolean permissionToReadStorageAccepted = false;
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};

    private TextView tv_output = findViewById(R.id.detect_result);

    private String detect_result_alert;

    private ActivityResultLauncher<String> getContentLauncher;
    private ActivityResultLauncher<Intent> manageStoragePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        tv_output = findViewById(R.id.detect_result); // TextView 초기화

        getContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(uri);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                                StringBuilder stringBuilder = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    stringBuilder.append(line).append("\n");
                                }
                                reader.close();
                                inputStream.close();

                                String fileContent = stringBuilder.toString();

                                // 텍스트 전처리 및 모델 입력 준비
                                float[] input = preprocessText(fileContent);
                                float[] output = new float[5];

                                // 모델 실행
                                Interpreter lite = getTfliteInterpreter("converted_model.tflite");
                                lite.run(input, output);

                                // 결과 처리 및 표시
                                displayOutput(output);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        // 권한 설정 화면 런처 초기화
        manageStoragePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            // 권한이 부여된 경우 파일 선택기 실행
                            getContentLauncher.launch("text/plain");
                        } else {
                            Log.e(TAG, "Manage External Storage permission not granted.");
                        }
                    }
                });

        Button detectBtn = findViewById(R.id.detect);
        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 권한 요청 및 파일 선택기 실행
                requestManageExternalStoragePermission();
            }
        });
    }

    private void requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                manageStoragePermissionLauncher.launch(intent);
            } else {
                // 이미 권한이 있는 경우 파일 선택기 실행
                getContentLauncher.launch("text/plain");
            }
        } else {
            // Android 10 이하에서는 기존 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_MANAGE_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // 권한이 부여된 경우 파일 선택기 실행
                    getContentLauncher.launch("text/plain");
                } else {
                    Log.e(TAG, "Manage External Storage permission not granted.");
                }
            }
        }
    }
/////////////
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    boolean permissionToReadStorageAccepted = requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED;

    if (requestCode == REQUEST_PERMISSION_MANAGE_EXTERNAL_STORAGE) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 권한이 부여된 경우 파일 선택기 실행
            getContentLauncher.launch("text/plain");
        } else {
            Log.e(TAG, "READ_EXTERNAL_STORAGE permission not granted.");
        }
    } else if (requestCode == REQUEST_PERMISSION_SEND_SMS) {
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
////////////
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

    // 이건 해보고 안되면 해야될수도 잘 모르겠음
    private float[] preprocessText(String text) {
        // 텍스트 전처리 로직 구현 (예: 단어 빈도수, 임베딩 등)
        // 여기서는 예시로 224 크기의 배열 반환
        return new float[224]; // 실제 모델의 입력 크기에 맞게 수정 필요
    }

    private void displayOutput(float[] output) {
        // output 배열의 내용을 TextView에 표시 (예시)
        StringBuilder result = new StringBuilder();
        for (float val : output) {
            result.append(val).append("\n");
        }
        tv_output.setText(result.toString());

        // 텍스트를 가져와서 checkAndSendSMS 호출
        detect_result_alert = tv_output.getText().toString();
        Log.i(TAG, "Result=" + detect_result_alert);
        checkAndSendSMS(detect_result_alert);
    }




    // 이 부분은 모델 연동 위해 필수 코드
    private Interpreter getTfliteInterpreter (String modelPath) {
        try {
            return new Interpreter (loadModelFile(MainActivity.this, modelPath));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 이 부분은 모델 연동 위해 필수 코드
    public MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
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

    private void checkAndSendSMS(String detect_result_alert) {
        int detect_int = Integer.parseInt(detect_result_alert);

        if (detect_int > 80) {
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
    }

}