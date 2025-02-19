package com.star.adskiphelper;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.star.adskiphelper.service.AdSkipService;

public class MainActivity extends AppCompatActivity {

    private TextView tvServiceState;
    private Button btnOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        tvServiceState.setText(AdSkipService.isServiceRunning() ? "ON" : "OFF");
    }

    private void findViews(){
        tvServiceState = findViewById(R.id.tv_service_state);
        btnOpen = findViewById(R.id.btn_open);
    }

    private void initViews() {
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开启服务
                showOpenAccessibilityDialog();
            }
        });
    }

    /**
     * 打开无障碍权限的提示
     * */
    private void showOpenAccessibilityDialog(){
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("依次点击：无障碍->已下载应用->广告跳过助手")
                .setPositiveButton("取消", null)
                .setNeutralButton("好的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //跳转至系统界面
                        Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent_abs);
                    }
                })
                .create();
        dialog.show();
    }

}