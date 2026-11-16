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
        listen.setOnClickListener(view -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
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
        voiceCard.addView(configure, listenSize);
        configure.setOnClickListener(view -> configure());
        setContentView(scroll);
        if (!AppAccess.configured(this)) configure();
    }

    private void configure() {
        AppAccess.choose(this, () -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 2);
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
        if (code == 1 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    private void startListening() {
        if (voice == null) voice = new VoiceInput(this);
        status.setText(R.string.listening);
        voice.listen(this::runCommand, status::setText);
    }

    private void runCommand(String phrase) {
        if (YouTubeSearch.handle(this, phrase, status::setText)) return;
        String command = VoiceInput.command(phrase);
        if (command.equals("mở tin nhắn") || command.equals("mở sms") || command.equals("open sms") || command.equals("open messages")) {
            if (!AppAccess.allows(this, "SMS")) {
                status.setText(R.string.access_required);
                return;
            }
            Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
            try {
                startActivity(sms);
            } catch (android.content.ActivityNotFoundException error) {
                status.setText(getString(R.string.no_sms));
            }
            return;
        }
        String[] packages = switch (command) {
            case "mở tiktok", "open tiktok", "vào tiktok" -> TikTokService.PACKAGES;
            case "mở youtube", "open youtube", "vào youtube" -> new String[]{YouTubeSearch.PACKAGE};
            case "mở messenger", "open messenger", "mở mess" -> new String[]{"com.facebook.orca"};
            case "mở zalo" -> new String[]{"com.zing.zalo"};
            case "mở whatsapp", "open whatsapp", "mở whats app" -> new String[]{"com.whatsapp"};
            default -> new String[0];
        };
        for (String name : packages) {
            String app = switch (name) {
                case "com.facebook.orca" -> "Messenger";
                case "com.zing.zalo" -> "Zalo";
                case "com.whatsapp" -> "WhatsApp";
                case YouTubeSearch.PACKAGE -> "YouTube";
                default -> "TikTok";
            };
            if (!AppAccess.allows(this, app)) {
                status.setText(R.string.access_required);
                return;
            }
            Intent launch = getPackageManager().getLaunchIntentForPackage(name);
            if (launch != null) {
                startActivity(launch);
                status.setText(getString(R.string.opened));
                return;
            }
        }
        status.setText(packages.length == 0 ? getString(R.string.unsupported) : getString(R.string.app_missing));
    }

    @Override protected void onStop() {
        if (voice != null) {
            voice.close();
            voice = null;
        }
        if (getString(R.string.listening).contentEquals(status.getText())) status.setText(R.string.ready);
        super.onStop();
    }
}
