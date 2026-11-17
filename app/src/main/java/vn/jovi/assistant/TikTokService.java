package vn.jovi.assistant;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;

public final class TikTokService extends AccessibilityService {
    static final String[] PACKAGES = {"com.zhiliaoapp.musically", "com.ss.android.ugc.trill"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Button control;
    private VoiceInput voice;
    private static TikTokService active;

    static boolean available() { return active != null; }

    static boolean openWakeScreen() {
        if (active == null) return false;
        active.startActivity(new android.content.Intent(active, MainActivity.class)
                .setAction("vn.jovi.assistant.WAKE")
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP));
        return true;
    }

    @Override protected void onServiceConnected() {
        active = this;
        voice = new VoiceInput(this);
        scheduleWindowCheck();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        scheduleWindowCheck();
    }

    private boolean isTikTok(AccessibilityNodeInfo root) {
        return AppAccess.allows(this, "TikTok") && root != null && root.getPackageName() != null
                && Arrays.asList(PACKAGES).contains(root.getPackageName().toString());
    }

    private boolean isYouTube(AccessibilityNodeInfo root) {
        CharSequence name = root == null ? null : root.getPackageName();
        return AppAccess.allows(this, "YouTube") && name != null && YouTubeSearch.PACKAGE.contentEquals(name);
    }

    private boolean isSupported(AccessibilityNodeInfo root) {
        return isTikTok(root) || isYouTube(root);
    }

    private final Runnable windowCheck = () -> {
        if (isSupported(getRootInActiveWindow()) && getSystemService(PowerManager.class).isInteractive()
                && !getSystemService(android.app.KeyguardManager.class).isDeviceLocked()) {
            showControl();
        } else {
            if (voice != null) voice.close();
            removeControl();
        }
    };

    private void scheduleWindowCheck() {
        handler.removeCallbacks(windowCheck);
        handler.postDelayed(windowCheck, 180);
    }

    private void showControl() {
        if (control != null) return;
        Button button = new Button(this);
        button.setText(R.string.app_name);
        button.setOnClickListener(view -> listen());
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        try {
            getSystemService(WindowManager.class).addView(button, params);
            control = button;
        } catch (WindowManager.BadTokenException | WindowManager.InvalidDisplayException error) {
            android.util.Log.w("Jovi", "Could not attach voice control", error);
        }
    }

    private void listen() {
        if (!isSupported(getRootInActiveWindow())) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            message(getString(R.string.permission_hint));
            return;
        }
        message(getString(R.string.listening));
        voice.listen(phrase -> {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (getSystemService(android.app.KeyguardManager.class).isDeviceLocked()) return;
            if (isYouTube(root)) {
                handleYouTube(root, phrase);
                return;
            }
            if (!isTikTok(root)) return;
            switch (VoiceInput.command(phrase)) {
                case "next video", "swipe up", "video tiếp theo", "lướt lên", "lướt sang video tiếp theo",
                     "lướt qua video tiếp theo", "lướt lên video tiếp theo" -> nextVideo();
                default -> message(getString(R.string.next_hint));
            }
        }, this::message);
    }

    private void handleYouTube(AccessibilityNodeInfo root, String phrase) {
        if (YouTubeSearch.handle(this, phrase, true, this::message)) return;
        String command = VoiceInput.command(phrase);
        int result = YouTubeControls.resultNumber(command);
        if (result != 0) {
            if (!YouTubeControls.openResult(root, result)) {
                message(getString(R.string.result_missing, result));
            }
            return;
        }
        switch (command) {
            case "pause", "pause video", "tạm dừng", "dừng video" -> playback(root, true);
            case "resume", "resume playback", "phát tiếp", "tiếp tục phát" -> playback(root, false);
            default -> message(getString(R.string.youtube_hint));
        }
    }

    private void playback(AccessibilityNodeInfo root, boolean pause) {
        if (YouTubeControls.playback(root, pause)) return;
        if (!YouTubeControls.revealPlayer(root)) {
            message(getString(R.string.player_missing));
            return;
        }
        retryPlayback(root.getWindowId(), pause, 4);
    }

    private void retryPlayback(int windowId, boolean pause, int remaining) {
        handler.postDelayed(() -> {
            AccessibilityNodeInfo current = getRootInActiveWindow();
            if (!isYouTube(current) || current.getWindowId() != windowId
                    || getSystemService(android.app.KeyguardManager.class).isDeviceLocked()) return;
            if (YouTubeControls.playback(current, pause)) return;
            if (remaining > 1) retryPlayback(windowId, pause, remaining - 1);
            else message(getString(R.string.player_missing));
        }, 200);
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
                .addStroke(new GestureDescription.StrokeDescription(swipe, 0, 190)).build();
        if (!dispatchGesture(gesture, null, null)) message(getString(R.string.swipe_failed));
    }

    private void message(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void removeControl() {
        if (control != null) {
            try {
                getSystemService(WindowManager.class).removeView(control);
            } catch (IllegalArgumentException error) {
                android.util.Log.w("Jovi", "Voice control already detached", error);
            }
            control = null;
        }
    }

    @Override public void onInterrupt() {
        handler.removeCallbacksAndMessages(null);
        if (voice != null) voice.close();
        removeControl();
    }

    @Override public void onDestroy() {
        onInterrupt();
        if (active == this) {
            active = null;
            stopService(new android.content.Intent(this, WakeService.class));
        }
        super.onDestroy();
    }
}
