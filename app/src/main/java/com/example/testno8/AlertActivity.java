package com.example.testno8;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class AlertActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showSendSmsDialog();
    }

    // 알림 클릭 시 AlertDialog로 메시지 전송 여부 묻기
    private void showSendSmsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("메시지 전송")
                .setMessage("보이스 피싱이 감지되었습니다. 메시지를 전송하시겠습니까?")
                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 권한이 있는지 확인
                        if (ContextCompat.checkSelfPermission(AlertActivity.this, Manifest.permission.SEND_SMS)
                                == PackageManager.PERMISSION_GRANTED) {
                            sendSms(); // SMS 보내기
                        } else {
                            Toast.makeText(AlertActivity.this, "SMS 보내기 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                        }
                        finish();
                    }
                })
                .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    // SMS 보내기 메서드
    private void sendSms() {
        try {
            String phoneNumber = "010-8937-5475";
            String message = "보이스 피싱 탐지";

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            Toast.makeText(this, "SMS가 전송되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            // 보안 예외 처리
            e.printStackTrace();
            Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}

