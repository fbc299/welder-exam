package com.welder.exam.service;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.projection.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.core.app.NotificationCompat;
import com.welder.exam.QuestionBank;
import com.welder.exam.R;
import java.util.*;

public class FloatingWindowService extends Service {

    private static final int NOTIFICATION_ID = 10001;
    private static final String CHANNEL_ID = "welder_exam_floating";

    private WindowManager.LayoutParams params;
    private View floatingView;
    private WindowManager windowManager;

    private TextView tvQuestion;
    private TextView tvAnswer;
    private TextView tvType;
    private TextView tvScore;
    private Button btnClose;
    private Button btnToggle;

    private QuestionBank questionBank;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    public static boolean isRunning = false;
    private boolean isCapturing = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long CAPTURE_INTERVAL = 2000; // 2秒检测一次

    private String lastCapturedText = "";
    private boolean answerVisible = false;

    @Override
    public void onCreate() {
        super.onCreate();
        questionBank = new QuestionBank();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (floatingView == null) {
            setupFloatingWindow();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID, "悬浮窗服务", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("用于焊工答题辅助的悬浮窗");
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, FloatingWindowService.class);
        notificationIntent.setAction("STOP");

        PendingIntent pi = PendingIntent.getService(this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("焊工答题助手")
            .setContentText("悬浮窗已启动")
            .setSmallIcon(R.drawable.ic_float)
            .setContentIntent(pi)
            .build();
    }

    private void setupFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate floating layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating, null);

        // Window type for overlay
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 20;
        params.y = 200;

        // Init views
        tvQuestion = floatingView.findViewById(R.id.tv_question);
        tvAnswer = floatingView.findViewById(R.id.tv_answer);
        tvType = floatingView.findViewById(R.id.tv_type);
        tvScore = floatingView.findViewById(R.id.tv_score);
        btnClose = floatingView.findViewById(R.id.btn_close);
        btnToggle = floatingView.findViewById(R.id.btn_toggle);

        btnClose.setOnClickListener(v -> stopSelf());

        btnToggle.setOnClickListener(v -> {
            if (answerVisible) {
                hideAnswer();
            } else {
                startCapture();
            }
        });

        // Make floating window draggable
        setupDraggable();

        windowManager.addView(floatingView, params);

        // Auto-start capture
        startCapture();
    }

    private void setupDraggable() {
        View dragHandle = floatingView.findViewById(R.id.drag_handle);
        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    params.x = (int) (getScreenWidth() - event.getRawX() - floatingView.getWidth() / 2);
                    params.y = (int) event.getRawY() - floatingView.getHeight() / 2;
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
            }
            return false;
        });
    }

    private int getScreenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private void startCapture() {
        if (isCapturing) return;

        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // Request permission dynamically if needed
        Intent permissionIntent = projectionManager.createScreenCaptureIntent();
        permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(permissionIntent);

        // Note: In real implementation, you'd use ActivityResultLauncher
        // For now, we assume permission is granted

        isCapturing = true;
        answerVisible = true;
        btnToggle.setText("关闭答题");

        startScreenCapture();
        scheduleCapture();
    }

    private void startScreenCapture() {
        if (projectionManager == null) {
            // Fallback: use a定时检测已有文字
            return;
        }

        MediaProjection projection = projectionManager.getMediaProjection(
            Activity.RESULT_OK, createProjectionIntent());
        if (projection == null) return;

        imageReader = ImageReader.newInstance(
            getScreenWidth(), getResources().getDisplayMetrics().heightPixels,
            PixelFormat.RGBA_8888, 2);

        virtualDisplay = projection.createVirtualDisplay(
            "ScreenCapture",
            getScreenWidth(), getResources().getDisplayMetrics().heightPixels,
            getResources().getDisplayMetrics().densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(), null, handler
        );
    }

    private Intent createProjectionIntent() {
        return new Intent();
    }

    private void scheduleCapture() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isCapturing) {
                    captureAndRecognize();
                    scheduleCapture();
                }
            }
        }, CAPTURE_INTERVAL);
    }

    private void captureAndRecognize() {
        // For a real implementation, this would use ML Kit Text Recognition
        // to read text from the screen capture
        // Since we can't easily capture other apps' content without accessibility,
        // we'll use a simpler approach: monitor clipboard or use AccessibilityService

        // For now, show ready state
        if (tvQuestion != null) {
            tvQuestion.setText("📖 正在检测题目...");
        }

        // Real implementation would:
        // 1. Capture screenshot via MediaProjection
        // 2. Use ML Kit TextRecognition to extract text
        // 3. Match against QuestionBank
        // 4. Display result in floating window
    }

    public void updateAnswer(String questionText) {
        if (questionBank == null || questionText == null) return;

        QuestionBank.MatchResult result = questionBank.findAnswer(questionText);

        if (result != null) {
            tvQuestion.setText("📋 " + questionText.substring(0, Math.min(50, questionText.length())) + "...");
            tvAnswer.setText("✅ 答案: " + result.answer);
            tvType.setText("📝 类型: " + result.type);
            tvScore.setText("🔍 匹配: " + (int)(result.score * 100) + "%");
        } else {
            tvQuestion.setText("📋 " + questionText.substring(0, Math.min(50, questionText.length())) + "...");
            tvAnswer.setText("❓ 未找到答案");
            tvType.setText("");
            tvScore.setText("");
        }
    }

    private void hideAnswer() {
        answerVisible = false;
        isCapturing = false;
        btnToggle.setText("开启答题");

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (tvQuestion != null) tvQuestion.setText("答题已关闭");
        if (tvAnswer != null) tvAnswer.setText("");
        if (tvType != null) tvType.setText("");
        if (tvScore != null) tvScore.setText("");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideAnswer();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
