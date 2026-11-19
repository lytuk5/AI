package vn.jovi.assistant;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.function.Consumer;

final class AppCommands {
    static void run(Context context, String phrase, Consumer<String> feedback) {
        if (context.getSystemService(KeyguardManager.class).isDeviceLocked()) {
            feedback.accept(context.getString(R.string.wake_unlock));
            return;
        }
        if (YouTubeSearch.handle(context, phrase, false, feedback)) return;
        String command = CommandParser.appCommand(phrase);
        if (command.equals("open messages")) {
            if (!AppAccess.allows(context, "SMS")) {
                feedback.accept(context.getString(R.string.access_required));
                return;
            }
            Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
            try {
                launch(context, sms);
                feedback.accept(context.getString(R.string.opened));
            } catch (android.content.ActivityNotFoundException error) {
                feedback.accept(context.getString(R.string.no_sms));
            }
            return;
        }
        String[] packages = switch (command) {
            case "open tiktok" -> TikTokService.PACKAGES;
            case "open youtube" -> new String[]{YouTubeSearch.PACKAGE};
            case "open messenger" -> new String[]{"com.facebook.orca"};
            case "open zalo" -> new String[]{"com.zing.zalo"};
            case "open whatsapp" -> new String[]{"com.whatsapp"};
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
            if (!AppAccess.allows(context, app)) {
                feedback.accept(context.getString(R.string.access_required));
                return;
            }
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(name);
            if (launch != null) {
                launch(context, launch);
                feedback.accept(context.getString(R.string.opened));
                return;
            }
        }
        feedback.accept(packages.length == 0 ? context.getString(R.string.unsupported, phrase)
                : context.getString(R.string.app_missing));
    }

    private static void launch(Context context, Intent intent) {
        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
