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
    private static final String READY = "Chạm nút bên dưới để nói lệnh.";
    private static final String LISTENING = "Đang nghe…";
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
        layout.addView(label("Trợ lý giọng nói", 16, 0xff345878, false));

        LinearLayout voiceCard = card(layout, 24, 0xffd7ecff);
        status = label(READY, 16, 0xff25496c, false);
        status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        voiceCard.addView(status);

        Button listen = action("Nói với Jovi", 0xff1761ae, 0xffffffff);
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
        examples.addView(label("Lệnh mẫu", 18, 0xff123358, true));
        examples.addView(label("“Mở TikTok”", 16, 0xff345878, false));
        examples.addView(label("“Tìm bài hát Believer trên YouTube”", 16, 0xff345878, false));

        LinearLayout settings = card(layout, 16, 0xffffffff);
        settings.addView(label("Điều khiển trong ứng dụng", 18, 0xff123358, true));
        settings.addView(label("Bật Trợ năng để hiện nút Jovi trong TikTok và YouTube.", 15, 0xff345878, false));
        Button setup = action("Mở cài đặt Trợ năng", 0xffd7ecff, 0xff123358);
        LinearLayout.LayoutParams setupSize = new LinearLayout.LayoutParams(-1, -2);
        setupSize.topMargin = dp(12);
        settings.addView(setup, setupSize);
        setup.setOnClickListener(view -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        setContentView(scroll);
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
        status.setText(LISTENING);
        voice.listen(this::runCommand, status::setText);
    }

    private void runCommand(String phrase) {
        if (YouTubeSearch.handle(this, phrase, status::setText)) return;
        String command = VoiceInput.command(phrase);
        if (command.equals("mở tin nhắn") || command.equals("mở sms")) {
            Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
            try {
                startActivity(sms);
            } catch (android.content.ActivityNotFoundException error) {
                status.setText("Máy chưa có ứng dụng SMS.");
            }
            return;
        }
        String[] packages = switch (command) {
            case "mở tiktok", "vào tiktok" -> TikTokService.PACKAGES;
            case "mở youtube", "vào youtube" -> new String[]{YouTubeSearch.PACKAGE};
            case "mở messenger", "mở mess" -> new String[]{"com.facebook.orca"};
            case "mở zalo" -> new String[]{"com.zing.zalo"};
            case "mở whatsapp", "mở whats app" -> new String[]{"com.whatsapp"};
            default -> new String[0];
        };
        for (String name : packages) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(name);
            if (launch != null) {
                startActivity(launch);
                status.setText("Đã mở ứng dụng.");
                return;
            }
        }
        status.setText(packages.length == 0 ? "Jovi chưa hỗ trợ lệnh này." : "Chưa tìm thấy ứng dụng.");
    }

    @Override protected void onStop() {
        if (voice != null) {
            voice.close();
            voice = null;
        }
        if (LISTENING.contentEquals(status.getText())) status.setText(READY);
        super.onStop();
    }
}
