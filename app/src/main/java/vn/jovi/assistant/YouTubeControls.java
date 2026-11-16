package vn.jovi.assistant;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class YouTubeControls {
    private static final Pattern RESULT = Pattern.compile(
            "^(?:(?:mở|phát) (?:kết quả|video) (?:thứ )?|(?:open|play) (?:the )?)(nhất|hai|ba|tư|bốn|năm|first|second|third|fourth|fifth|[1-5])(?: (?:result|video))?$");
    private static final String ID = YouTubeSearch.PACKAGE + ":id/";

    static int resultNumber(String command) {
        Matcher match = RESULT.matcher(command);
        if (!match.matches()) return 0;
        return switch (match.group(1)) {
            case "first", "nhất", "1" -> 1;
            case "second", "hai", "2" -> 2;
            case "third", "ba", "3" -> 3;
            case "fourth", "tư", "bốn", "4" -> 4;
            default -> 5;
        };
    }

    static boolean openResult(AccessibilityNodeInfo root, int number) {
        List<AccessibilityNodeInfo> rows = new ArrayList<>();
        collect(root, node -> node.isClickable() && node.isVisibleToUser()
                && description(node).matches("(?s).*[–-] (?:play video|phát video)$"), rows);
        rows.sort(Comparator.comparingInt(node -> {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            return bounds.top;
        }));
        return rows.size() >= number && click(rows.get(number - 1));
    }

    static boolean playback(AccessibilityNodeInfo root, boolean pause) {
        if (root == null) return false;
        List<AccessibilityNodeInfo> buttons = new ArrayList<>();
        for (AccessibilityNodeInfo player : root.findAccessibilityNodeInfosByViewId(ID + "watch_player")) {
            collect(player, node -> node.isClickable() && node.isVisibleToUser()
                && switch (description(node)) {
                    case "pause", "tạm dừng", "tạm dừng video", "pause video" -> pause;
                    case "play", "phát", "phát video", "play video" -> !pause;
                    default -> false;
                }, buttons);
        }
        return buttons.size() == 1 && click(buttons.get(0));
    }

    static boolean revealPlayer(AccessibilityNodeInfo root) {
        if (root == null) return false;
        for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByViewId(ID + "player_overlays")) {
            if (node.isVisibleToUser() && click(node)) return true;
        }
        return false;
    }

    private static boolean click(AccessibilityNodeInfo node) {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private static String description(AccessibilityNodeInfo node) {
        CharSequence text = node.getContentDescription();
        return text == null ? "" : text.toString().trim().toLowerCase(Locale.ROOT);
    }

    private static void collect(AccessibilityNodeInfo node, Predicate<AccessibilityNodeInfo> accepts,
                                List<AccessibilityNodeInfo> matches) {
        if (node == null) return;
        if (accepts.test(node)) matches.add(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            collect(node.getChild(i), accepts, matches);
        }
    }
}
