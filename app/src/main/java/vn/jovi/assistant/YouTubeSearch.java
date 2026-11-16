package vn.jovi.assistant;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class YouTubeSearch {
    static final String PACKAGE = "com.google.android.youtube";
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern REQUEST = Pattern.compile(
            "^(?:jovi[\\s,]+)?tìm(?: kiếm)?(?: bài hát| video| bài nhạc)?(?:\\s+(.+?))?[.!?]*$", FLAGS);
    private static final Pattern PLATFORM = Pattern.compile("(?:^|\\s+)(?:trên|ở) youtube$", FLAGS);

    static boolean handle(Context context, String phrase, Consumer<String> feedback) {
        Matcher request = REQUEST.matcher(phrase.trim());
        if (!request.matches()) return false;

        String query = request.group(1);
        if (query != null) query = PLATFORM.matcher(query).replaceFirst("").trim();
        if (query == null || query.isEmpty()) {
            feedback.accept("Bạn muốn tìm bài hát hoặc video nào?");
            return true;
        }

        Intent search = new Intent(Intent.ACTION_SEARCH)
                .setPackage(PACKAGE)
                .putExtra(SearchManager.QUERY, query)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(search);
        } catch (ActivityNotFoundException | SecurityException error) {
            feedback.accept("Không mở được tìm kiếm trong YouTube.");
        }
        return true;
    }
}
