package com.welder.exam.ui;

import android.content.*;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.appcompat.app.*;
import androidx.core.app.ActivityCompat;
import com.welder.exam.R;
import com.welder.exam.service.FloatingWindowService;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQUEST = 1001;

    private Button btnStartService;
    private Button btnStopService;
    private TextView tvStatus;
    private TextView tvInfo;

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                tvStatus.setText("❌ 悬浮窗权限被拒绝");
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartService = findViewById(R.id.btn_start_service);
        btnStopService = findViewById(R.id.btn_stop_service);
        tvStatus = findViewById(R.id.tv_status);
        tvInfo = findViewById(R.id.tv_info);

        btnStartService.setOnClickListener(v -> checkPermissionsAndStart());
        btnStopService.setOnClickListener(v -> stopFloatingService());

        // Check if service is running
        updateStatus();
    }

    private void checkPermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            overlayPermissionLauncher.launch(intent);
        } else {
            startFloatingService();
        }
    }

    private void startFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        startForegroundService(serviceIntent);
        tvStatus.setText("✅ 悬浮窗已启动");
        updateStatus();
    }

    private void stopFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingWindowService.class);
        serviceIntent.setAction("STOP");
        startService(serviceIntent);
        tvStatus.setText("⏹️ 悬浮窗已关闭");
        updateStatus();
    }

    private void updateStatus() {
        boolean isRunning = FloatingWindowService.isRunning;
        tvStatus.setText(isRunning ? "✅ 服务运行中" : "⏹️ 服务未运行");
        btnStartService.setEnabled(!isRunning);
        btnStopService.setEnabled(isRunning);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
