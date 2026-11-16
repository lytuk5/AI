package vn.jovi.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

final class AppAccess {
    private static final String[] APPS = {"TikTok", "YouTube", "Messenger", "Zalo", "WhatsApp", "SMS"};
    private static final int ALL = (1 << APPS.length) - 1;

    static boolean allows(Context context, String app) {
        int selected = context.getSharedPreferences("app_access", Context.MODE_PRIVATE).getInt("selected", 0);
        for (int i = 0; i < APPS.length; i++) {
            if (APPS[i].equals(app)) return (selected & (1 << i)) != 0;
        }
        return false;
    }

    static boolean configured(Context context) {
        return context.getSharedPreferences("app_access", Context.MODE_PRIVATE).contains("selected");
    }

    static void choose(Activity activity, Runnable complete) {
        var preferences = activity.getSharedPreferences("app_access", Context.MODE_PRIVATE);
        int[] selected = {preferences.getInt("selected", 0)};
        boolean[] checked = new boolean[APPS.length];
        for (int i = 0; i < APPS.length; i++) checked[i] = (selected[0] & (1 << i)) != 0;
        new AlertDialog.Builder(activity)
                .setTitle(R.string.choose_apps)
                .setMultiChoiceItems(APPS, checked, (dialog, item, allowed) -> {
                    int bit = 1 << item;
                    selected[0] = allowed ? selected[0] | bit : selected[0] & ~bit;
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save_access, (dialog, which) -> {
                    preferences.edit().putInt("selected", selected[0] & ALL).apply();
                    complete.run();
                })
                .show();
    }
}
