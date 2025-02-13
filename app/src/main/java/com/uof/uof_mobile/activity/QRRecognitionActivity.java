package com.uof.uof_mobile.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.uof.uof_mobile.R;
import com.uof.uof_mobile.manager.SharedPreferenceManager;
import com.uof.uof_mobile.manager.SocketManager;
import com.uof.uof_mobile.other.Global;

import org.json.JSONObject;

public class QRRecognitionActivity extends AppCompatActivity {
    private IntentIntegrator qrScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrrecognition);

        init();
    }

    @Override
    protected void onDestroy() {
        Global.activities.remove(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    private void init() {
        for (Activity activity : Global.activities) {
            if (activity instanceof QRRecognitionActivity) {
                activity.finish();
            }
        }
        Global.activities.add(this);

        Intent qrRecognitionActivityIntent = getIntent();
        if (qrRecognitionActivityIntent.getStringExtra("targetIp") != null) {
            connectToStore(qrRecognitionActivityIntent.getStringExtra("targetIp"), Integer.valueOf(qrRecognitionActivityIntent.getStringExtra("targetPort")));
        } else {
            qrScan = new IntentIntegrator(this);
            qrScan.setOrientationLocked(false);
            qrScan.setPrompt("QR 코드를 인식해주세요");
            qrScan.setBeepEnabled(false);
            qrScan.initiateScan();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null && result.getContents() != null) {
            // 회사 종류에 따라 다른 Activity 표시
            String targetIp = result.getContents().substring(result.getContents().indexOf("Ip") + 3, result.getContents().indexOf("&"));
            int targetPort = Integer.parseInt(result.getContents().substring(result.getContents().indexOf("Port") + 5));

            connectToStore(targetIp, targetPort);
        } else {
            Toast.makeText(this, "QR 코드 인식에 실패했습니다", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void connectToStore(String targetIp, int targetPort) {
        try {
            Global.socketManager = new SocketManager();
            Global.socketManager.setSocket(targetIp, targetPort);

            // Pos 소켓 연결 - 매장 정보 불러오기
            new Thread(() -> {
                if (Global.socketManager.connect(2000)) {
                    JSONObject sendData = new JSONObject();

                    if (Global.User.type.equals("customer")) {
                        try {
                            sendData.accumulate("request_code", Global.Network.Request.STORE_PRODUCT_INFO);
                            Global.socketManager.send(sendData.toString());

                            String strRecvData = Global.socketManager.recv();

                            if (strRecvData == null) {
                                // 수신 데이터가 없을 경우
                                Toast.makeText(QRRecognitionActivity.this, "매장 연결 중 문제가 발생했습니다", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                // 수신 데이터가 있을 경우
                                JSONObject recvData = new JSONObject(strRecvData);

                                JSONObject companyData = recvData.getJSONObject("message").getJSONObject("company");

                                String companyType = companyData.getString("type");

                                if (companyType.equals("restaurant") || companyType.equals("pc")) {
                                    // 회사 종류 - 음식점 또는 PC방
                                    SharedPreferenceManager.open(QRRecognitionActivity.this, Global.SharedPreference.APP_DATA);
                                    SharedPreferenceManager.save(Global.SharedPreference.TEMP_MESSAGE, recvData.getJSONObject("message").toString());
                                    SharedPreferenceManager.close();
                                    startActivity(new Intent(QRRecognitionActivity.this, OrderingActivity.class));
                                    finish();
                                } else if (companyType.equals("movie")) {
                                    // 회사 종류 - 영화관
                                    SharedPreferenceManager.open(QRRecognitionActivity.this, Global.SharedPreference.APP_DATA);
                                    SharedPreferenceManager.save(Global.SharedPreference.TEMP_MESSAGE, recvData.getJSONObject("message").toString());
                                    SharedPreferenceManager.close();
                                    startActivity(new Intent(QRRecognitionActivity.this, MovieOrderingActivity.class));
                                    finish();
                                } else {
                                    runOnUiThread(() -> {
                                        Toast.makeText(QRRecognitionActivity.this, "등록되지 않은 매장입니다", Toast.LENGTH_SHORT).show();
                                        Global.socketManager.disconnect();
                                        finish();
                                    });
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                Toast.makeText(QRRecognitionActivity.this, "매장 연결 중 문제가 발생했습니다: " + e.toString(), Toast.LENGTH_SHORT).show();
                                Global.socketManager.disconnect();
                                finish();
                            });
                        }
                    } else if (Global.User.type.equals("uofpartner")) {
                        try {
                            sendData.accumulate("request_code", Global.Network.Request.QR_IMAGE);
                            Global.socketManager.send(sendData.toString());

                            String strRecvData = Global.socketManager.recv();

                            if (strRecvData == null) {
                                // 수신 데이터가 없을 경우
                                Toast.makeText(QRRecognitionActivity.this, "매장 연결 중 문제가 발생했습니다", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                // 수신 데이터가 있을 경우
                                JSONObject recvData = new JSONObject(strRecvData);
                                String responseCode = recvData.getString("response_code");

                                if (responseCode.equals(Global.Network.Response.QR_IMAGE_SUCCESS)) {
                                    SharedPreferenceManager.open(QRRecognitionActivity.this, Global.SharedPreference.APP_DATA);
                                    SharedPreferenceManager.save(Global.SharedPreference.QR_IMAGE, recvData.getJSONObject("message").getString("image"));
                                    SharedPreferenceManager.close();
                                    runOnUiThread(() -> {
                                        Toast.makeText(QRRecognitionActivity.this, "매장 QR코드를 저장했습니다", Toast.LENGTH_SHORT).show();
                                        Global.socketManager.disconnect();
                                        finish();
                                    });
                                } else if (responseCode.equals(Global.Network.Response.QR_IMAGE_FAILED)) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(QRRecognitionActivity.this, "QR코드 불러오기 실패", Toast.LENGTH_SHORT).show();
                                        Global.socketManager.disconnect();
                                        finish();
                                    });
                                } else {
                                    runOnUiThread(() -> {
                                        Toast.makeText(QRRecognitionActivity.this, "매장 연결 중 문제가 발생했습니다", Toast.LENGTH_SHORT).show();
                                        Global.socketManager.disconnect();
                                        finish();
                                    });
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                Toast.makeText(QRRecognitionActivity.this, "매장 연결 중 문제가 발생했습니다: " + e.toString(), Toast.LENGTH_SHORT).show();
                                Global.socketManager.disconnect();
                                finish();
                            });
                        }
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(QRRecognitionActivity.this, "매장 연결 중 문제가 발생했습니다", Toast.LENGTH_SHORT).show();
                        Global.socketManager.disconnect();
                        finish();
                    });
                }
            }).start();
        } catch (Exception e) {
            Toast.makeText(QRRecognitionActivity.this, "등록되지 않은 매장입니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}