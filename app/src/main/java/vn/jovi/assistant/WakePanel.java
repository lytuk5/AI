package vn.jovi.assistant;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.LocaleManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class WakePanel {
    private final Context context;
    private final WindowManager windows;
    private VoiceInput voice;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable dismiss = this::close;
    private LinearLayout panel;

    WakePanel(Context context) {
        this.context = context;
        windows = context.getSystemService(WindowManager.class);
    }

    boolean canShow() {
        return context.getSystemService(PowerManager.class).isInteractive()
                && !context.getSystemService(KeyguardManager.class).isKeyguardLocked();
    }

    boolean show() {
        if (!canShow() || context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) return false;
        close();

        Context display = localizedContext();
        var card = new LinearLayout(display);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), 0, dp(12), dp(12));
        card.setBackground(shape(0xfff5f9ff, 20));
        card.setElevation(dp(8));
        card.setAccessibilityPaneTitle(display.getString(R.string.app_name));

        var header = new LinearLayout(display);
        header.setGravity(Gravity.CENTER_VERTICAL);
        var title = label(display, 16, 0xff123358);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setText(R.string.app_name);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        var closeButton = new Button(display);
        closeButton.setText("×");
        closeButton.setTextSize(22);
        closeButton.setTextColor(0xff123358);
        closeButton.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        closeButton.setPadding(0, 0, 0, 0);
        closeButton.setContentDescription(display.getString(R.string.wake_close));
        closeButton.setOnClickListener(view -> close());
        header.addView(closeButton, new LinearLayout.LayoutParams(dp(48), dp(48)));
        card.addView(header);

        var transcript = label(display, 15, 0xff123358);
        transcript.setBackground(shape(0xffdcecff, 14));
        transcript.setPadding(dp(10), dp(8), dp(10), dp(8));
        transcript.setMaxLines(4);
        transcript.setEllipsize(TextUtils.TruncateAt.END);
        transcript.setVisibility(View.GONE);
        card.addView(transcript, new LinearLayout.LayoutParams(-1, -2));

        var status = label(display, 13, 0xff345878);
        status.setPadding(0, dp(6), 0, 0);
        status.setText(display.getString(R.string.voice_preparing));
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        card.addView(status, new LinearLayout.LayoutParams(-1, -2));

        int screen = context.getResources().getConfiguration().screenWidthDp;
        var params = new WindowManager.LayoutParams(dp(Math.min(272, Math.max(1, screen - 24))), -2,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = dp(12);
        params.y = dp(8);
        if (Build.VERSION.SDK_INT >= 30) params.setFitInsetsTypes(WindowInsets.Type.systemBars()
                | WindowInsets.Type.displayCutout());
        params.setTitle("Jovi wake panel");
        try {
            windows.addView(card, params);
        } catch (WindowManager.BadTokenException | WindowManager.InvalidDisplayException failure) {
            android.util.Log.w("Jovi", "Could not show wake panel", failure);
            return false;
        }

        panel = card;
        voice = new VoiceInput(display, true);
        voice.listen(() -> status.setText(display.getString(R.string.listening)), text -> showTranscript(transcript, text),
                text -> {
                    if (!canShow()) {
                        close();
                        return;
                    }
                    showTranscript(transcript, text);
                    AppCommands.run(display, text, status::setText);
                    handler.postDelayed(dismiss, 7000);
                }, error -> {
                    status.setText(error);
                    handler.postDelayed(dismiss, 7000);
                });
        return true;
    }

    void close() {
        handler.removeCallbacks(dismiss);
        if (voice != null) {
            voice.close();
            voice = null;
        }
        if (panel != null) {
            var attached = panel;
            panel = null;
            if (attached.isAttachedToWindow()) windows.removeView(attached);
        }
    }

    private Context localizedContext() {
        if (Build.VERSION.SDK_INT < 33) return context;
        var locales = context.getSystemService(LocaleManager.class).getApplicationLocales();
        if (locales.isEmpty()) return context;
        var config = new Configuration(context.getResources().getConfiguration());
        config.setLocales(locales);
        return context.createConfigurationContext(config);
    }

    private static void showTranscript(TextView transcript, String text) {
        if (text == null || text.isBlank()) return;
        transcript.setText(text);
        transcript.setVisibility(View.VISIBLE);
    }

    private static TextView label(Context display, int size, int color) {
        var label = new TextView(display);
        label.setTextSize(size);
        label.setTextColor(color);
        return label;
    }

    private GradientDrawable shape(int color, int radius) {
        var shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
