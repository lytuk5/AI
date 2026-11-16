package vn.jovi.assistant;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;

public final class TikTokService extends AccessibilityService {
    static final String[] PACKAGES = {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill"};
    private Button control;
    private VoiceInput voice;

    @Override protected void onServiceConnected() {
        voice = new VoiceInput(this);
        showControl();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        showControl();
    }

    private boolean isTikTok(AccessibilityNodeInfo root) {
        return root != null && root.getPackageName() != null
                && Arrays.asList(PACKAGES).contains(root.getPackageName().toString());
    }

    private boolean isYouTube(AccessibilityNodeInfo root) {
        CharSequence name = root == null ? null : root.getPackageName();
        return name != null && YouTubeSearch.PACKAGE.contentEquals(name);
    }

    private boolean isSupported(AccessibilityNodeInfo root) {
        return isTikTok(root) || isYouTube(root);
    }

    private void showControl() {
        // The accessibility shortcut is shown only while a supported app is active.
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (!isSupported(root) || control != null) return;
        control = new Button(this);
        control.setText("Jovi");
        control.setOnClickListener(view -> listen());
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        getSystemService(WindowManager.class).addView(control, params);
        control.postDelayed(presenceCheck, 300);
    }

    private final Runnable presenceCheck = new Runnable() {
        @Override public void run() {
            if (control == null) return;
            if (isSupported(getRootInActiveWindow())) {
                control.postDelayed(this, 300);
            } else {
                voice.close();
                removeControl();
            }
        }
    };

    private void listen() {
        if (!isSupported(getRootInActiveWindow())) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            message("Mở Jovi để cấp quyền micrô trước.");
            return;
        }
        message("Đang nghe…");
        voice.listen(phrase -> {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (getSystemService(android.app.KeyguardManager.class).isDeviceLocked()) return;
            if (isYouTube(root)) {
                if (!YouTubeSearch.handle(this, phrase, this::message)) {
                    message("Thử nói “Tìm bài hát Believer”.");
                }
                return;
            }
            if (!isTikTok(root)) return;
            switch (VoiceInput.command(phrase)) {
                case "video tiếp theo", "lướt lên", "lướt sang video tiếp theo",
                     "lướt qua video tiếp theo", "lướt lên video tiếp theo" -> nextVideo();
                default -> message("Bạn có thể nói “video tiếp theo”.");
            }
        }, this::message);
    }

    private void nextVideo() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (!isTikTok(root) || !getSystemService(PowerManager.class).isInteractive()
                || getSystemService(android.app.KeyguardManager.class).isDeviceLocked()) return;
        Rect bounds = new Rect();
        root.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) return;
        Path swipe = new Path();
        swipe.moveTo(bounds.exactCenterX(), bounds.top + bounds.height() * 0.77f);
        swipe.lineTo(bounds.exactCenterX(), bounds.top + bounds.height() * 0.26f);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(swipe, 0, 300)).build();
        if (!dispatchGesture(gesture, null, null)) message("Chưa lướt được trên TikTok.");
    }

    private void message(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void removeControl() {
        if (control != null) {
            control.removeCallbacks(presenceCheck);
            getSystemService(WindowManager.class).removeView(control);
            control = null;
        }
    }

    @Override public void onInterrupt() {
        if (voice != null) voice.close();
        removeControl();
    }

    @Override public void onDestroy() {
        onInterrupt();
        super.onDestroy();
    }
}
