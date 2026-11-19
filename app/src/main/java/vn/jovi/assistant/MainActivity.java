package vn.jovi.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.net.Uri;
import android.provider.Settings;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {


    private TextView status;
    private VoiceInput voice;
    private boolean systemVoicePending;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xffeaf5ff);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(24), dp(24), dp(28));
        scroll.addView(layout);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            }
            return insets;
        });

        android.widget.ImageView avatar = new android.widget.ImageView(this);
        avatar.setImageResource(R.drawable.jovi_avatar);
        avatar.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        avatar.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        avatar.setBackground(rounded(0xffeaf5ff));
        avatar.setClipToOutline(true);
        layout.addView(avatar, new LinearLayout.LayoutParams(dp(96), dp(96)));
        layout.addView(label("Jovi", 44, 0xff123358, true));
        layout.addView(label(getString(R.string.subtitle), 16, 0xff345878, false));

        LinearLayout voiceCard = card(layout, 24, 0xffd7ecff);
        status = label(getString(R.string.ready), 16, 0xff25496c, false);
        status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        voiceCard.addView(status);

        Button listen = action(getString(R.string.listen), 0xff1761ae, 0xffffffff);
        LinearLayout.LayoutParams listenSize = new LinearLayout.LayoutParams(-1, -2);
        listenSize.topMargin = dp(16);
        voiceCard.addView(listen, listenSize);
        boolean vietnamese = usesVietnamese();
        Button alternateVoice = action(getString(vietnamese ? R.string.offline_voice : R.string.system_voice),
                0xffd7ecff, 0xff123358);
        voiceCard.addView(alternateVoice, new LinearLayout.LayoutParams(-1, -2));
        listen.setOnClickListener(view -> {
            if (vietnamese) startSystemVoice();
            else startListeningWithPermission();
        });
        alternateVoice.setOnClickListener(view -> {
            if (vietnamese) startListeningWithPermission();
            else startSystemVoice();
        });

        LinearLayout examples = card(layout, 16, 0xffffffff);
        examples.addView(label(getString(R.string.examples), 18, 0xff123358, true));
        examples.addView(label(getString(R.string.example_tiktok), 16, 0xff345878, false));
        examples.addView(label(getString(R.string.example_youtube), 16, 0xff345878, false));

        LinearLayout settings = card(layout, 16, 0xffffffff);
        settings.addView(label(getString(R.string.controls), 18, 0xff123358, true));
        settings.addView(label(getString(R.string.accessibility_hint), 15, 0xff345878, false));
        Button setup = action(getString(R.string.accessibility_button), 0xffd7ecff, 0xff123358);
        LinearLayout.LayoutParams setupSize = new LinearLayout.LayoutParams(-1, -2);
        setupSize.topMargin = dp(12);
        settings.addView(setup, setupSize);
        setup.setOnClickListener(view -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            Button language = action(getString(R.string.language), 0xffd7ecff, 0xff123358);
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-1, -2);
            size.topMargin = dp(12);
            settings.addView(language, size);
            language.setOnClickListener(view -> startActivity(new Intent(
                    Settings.ACTION_APP_LOCALE_SETTINGS, Uri.parse("package:" + getPackageName()))));
        }
        Button configure = action(getString(R.string.configure_jovi), 0xffd7ecff, 0xff123358);
        voiceCard.addView(configure, new LinearLayout.LayoutParams(listenSize));
        configure.setOnClickListener(view -> configure());
        Button wake = action(getString(R.string.wake_setup), 0xffd7ecff, 0xff123358);
        voiceCard.addView(wake, new LinearLayout.LayoutParams(listenSize));
        wake.setOnClickListener(view -> configureWake());
        var roles = getSystemService(android.app.role.RoleManager.class);
        if (roles.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT)) {
            Button assistant = action(getString(R.string.set_default_assistant), 0xffd7ecff, 0xff123358);
            settings.addView(assistant, new LinearLayout.LayoutParams(setupSize));
            assistant.setOnClickListener(view -> {
                if (roles.isRoleHeld(android.app.role.RoleManager.ROLE_ASSISTANT)) {
                    status.setText(R.string.default_assistant_active);
                } else {
                    startActivityForResult(roles.createRequestRoleIntent(android.app.role.RoleManager.ROLE_ASSISTANT), 4);
                }
            });
        }
        setContentView(scroll);
        if (state == null && !AppAccess.configured(this)) configure();
    }

    @Override protected void onResume() {
        super.onResume();
        startWakeIfReady();
    }

    private void startWakeIfReady() {
        if (WakeService.enabled() || !TikTokService.configured(this)
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || (android.os.Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) {
            return;
        }
        try {
            startForegroundService(new Intent(this, WakeService.class));
            status.setText(R.string.wake_waiting);
        } catch (RuntimeException error) {
            status.setText(R.string.wake_failed);
        }
    }

    private void configureWake() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 6);
        } else if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        } else {
            showWakeSetup();
        }
    }

    private void showWakeSetup() {
        if (TikTokService.configured(this)) {
            startWakeIfReady();
            if (WakeService.enabled()) status.setText(R.string.wake_waiting);
        } else {
            status.setText(R.string.wake_accessibility);
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    private void finishWakeSetup() {
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5);
            return;
        }
        startWakeIfReady();
    }

    private void configure() {
        AppAccess.choose(this, () -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 2);
            } else {
                finishWakeSetup();
            }
        });
    }

    private LinearLayout card(LinearLayout parent, int margin, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(rounded(color));
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-1, -2);
        size.topMargin = dp(margin);
        parent.addView(card, size);
        return card;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return label;
    }

    private Button action(String text, int color, int textColor) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(textColor);
        button.setMinHeight(dp(54));
        button.setGravity(Gravity.CENTER);
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(0x33123358), rounded(color), null));
        return button;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(20));
        return background;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (code == 2) finishWakeSetup();
            if (code == 5) startWakeIfReady();
            if (code == 6) configureWake();
            if (code == 7) showWakeSetup();
        }
        if ((code == 1 || code == 2 || code == 6)
                && results.length > 0 && results[0] != PackageManager.PERMISSION_GRANTED) {
            status.setText(R.string.permission_hint);
        }
        if (code == 1 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    private boolean usesVietnamese() {
        return "vi".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
    }

    private void startListeningWithPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private void startSystemVoice() {
        if (systemVoicePending) return;
        if (voice != null) voice.close();
        systemVoicePending = true;
        WakeService.pauseForCommand();
        WakeService.whenMicrophoneReleased(status.getHandler(), () -> systemVoicePending,
                this::launchSystemVoice, () -> {
                    finishSystemVoice();
                    status.setText(R.string.voice_busy);
                });
    }

    private void launchSystemVoice() {
        Intent request = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,
                        usesVietnamese() ? "vi-VN" : "en-US")
                .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, getString(R.string.wake_voice_prompt));
        try {
            startActivityForResult(request, 3);
        } catch (android.content.ActivityNotFoundException | SecurityException error) {
            finishSystemVoice();
            status.setText(R.string.recognizer_missing);
        }
    }

    private void finishSystemVoice() {
        if (!systemVoicePending) return;
        systemVoicePending = false;
        WakeService.resumeWake();
    }

    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == 4) {
            status.setText(result == RESULT_OK ? R.string.default_assistant_active : R.string.default_assistant_declined);
            return;
        }
        if (code != 3) return;
        finishSystemVoice();
        if (result != RESULT_OK || data == null) return;
        var phrases = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
        if (phrases == null || phrases.isEmpty()) {
            status.setText(R.string.no_command);
            return;
        }
        runCommand(phrases.get(0));
    }

    private void startListening() {
        if (systemVoicePending) return;
        if (voice == null) voice = new VoiceInput(this);
        status.setText(R.string.voice_preparing);
        voice.listen(() -> status.setText(R.string.listening), this::runCommand, status::setText);
    }

    private void runCommand(String phrase) {
        AppCommands.run(this, phrase, status::setText);
    }

    @Override protected void onStop() {
        if (voice != null) {
            voice.close();
            voice = null;
        }
        if (getString(R.string.listening).contentEquals(status.getText())
                || getString(R.string.voice_preparing).contentEquals(status.getText())) {
            status.setText(R.string.ready);
        }
        super.onStop();
    }

    @Override protected void onDestroy() {
        finishSystemVoice();
        super.onDestroy();
    }
}
