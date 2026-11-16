package vn.jovi.assistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

final class AppAccess {
    private static final String[] APPS = {"TikTok", "YouTube", "Messenger", "Zalo", "WhatsApp", "SMS"};
    private static final String[] LEGACY_ORDER = {"TikTok", "YouTube", "Messenger", "Zalo", "WhatsApp", "SMS"};
    private static final String ALLOWED = "allowed_apps";

    private static java.util.Set<String> selected(android.content.SharedPreferences preferences) {
        var saved = preferences.getStringSet(ALLOWED, null);
        if (saved != null) return saved;
        var migrated = new java.util.HashSet<String>();
        int mask = preferences.getInt("selected", 0);
        for (int i = 0; i < LEGACY_ORDER.length; i++) {
            if ((mask & (1 << i)) != 0) migrated.add(LEGACY_ORDER[i]);
        }
        if (preferences.contains("selected")) {
            preferences.edit().putStringSet(ALLOWED, migrated).remove("selected").apply();
        }
        return migrated;
    }

    static boolean allows(Context context, String app) {
        return selected(context.getSharedPreferences("app_access", Context.MODE_PRIVATE)).contains(app);
    }

    static boolean configured(Context context) {
        var preferences = context.getSharedPreferences("app_access", Context.MODE_PRIVATE);
        return preferences.contains(ALLOWED) || preferences.contains("selected");
    }

    static void choose(Activity activity, Runnable complete) {
        var preferences = activity.getSharedPreferences("app_access", Context.MODE_PRIVATE);
        var selected = new java.util.HashSet<>(selected(preferences));
        boolean[] checked = new boolean[APPS.length];
        for (int i = 0; i < APPS.length; i++) checked[i] = selected.contains(APPS[i]);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.choose_apps)
                .setMultiChoiceItems(APPS, checked, (dialog, item, allowed) -> {
                    if (allowed) selected.add(APPS[item]);
                    else selected.remove(APPS[item]);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save_access, (dialog, which) -> {
                    preferences.edit().putStringSet(ALLOWED, selected).apply();
                    complete.run();
                })
                .show();
    }
}
