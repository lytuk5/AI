package vn.jovi.assistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private TextView status;
    private VoiceInput voice;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(this);
        title.setText("Jovi");
        title.setTextSize(36);
        layout.addView(title);
        status = new TextView(this);
        status.setText("Chạm để nói, ví dụ “Jovi, mở TikTok”.");
        layout.addView(status);
        Button listen = new Button(this);
        listen.setText("Nói với Jovi");
        listen.setOnClickListener(view -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        });
        layout.addView(listen);
        Button setup = new Button(this);
        setup.setText("Cài đặt điều khiển TikTok và YouTube");
        setup.setOnClickListener(view -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        layout.addView(setup);
        setContentView(layout);
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 1 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }

    private void startListening() {
        if (voice == null) voice = new VoiceInput(this);
        status.setText("Đang nghe…");
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
        super.onStop();
    }
}
