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
            "^(?:(?:mở|phát) (?:kết quả|video) (?:(?:thứ|số) )?|(?:open|play) (?:the )?(?:(?:result|video) )?)(nhất|một|đầu tiên|hai|ba|tư|bốn|năm|first|second|third|fourth|fifth|[1-5])(?: (?:result|video))?$");
    private static final Pattern VIDEO_ROW = Pattern.compile("(?s).*[–-] (?:play video|phát video)$");
    private static final String ID = YouTubeSearch.PACKAGE + ":id/";

    static int resultNumber(String command) {
        Matcher match = RESULT.matcher(command);
        if (!match.matches()) return 0;
        return switch (match.group(1)) {
            case "first", "nhất", "một", "đầu tiên", "1" -> 1;
            case "second", "hai", "2" -> 2;
            case "third", "ba", "3" -> 3;
            case "fourth", "tư", "bốn", "4" -> 4;
            default -> 5;
        };
    }

    static boolean openResult(AccessibilityNodeInfo root, int number) {
        List<AccessibilityNodeInfo> rows = new ArrayList<>();
        collect(root, node -> node.isClickable() && node.isVisibleToUser()
                && VIDEO_ROW.matcher(description(node)).matches(), rows);
        Rect viewport = bounds(root);
        var unique = new java.util.HashMap<Rect, AccessibilityNodeInfo>();
        for (AccessibilityNodeInfo row : rows) {
            Rect area = bounds(row);
            if (area.isEmpty()) continue;
            Rect visible = new Rect(area);
            if (visible.intersect(viewport) && visible.height() * 2 >= area.height()) {
                unique.putIfAbsent(area, row);
            }
        }
        var ordered = new ArrayList<>(unique.entrySet());
        ordered.sort(Comparator.<java.util.Map.Entry<Rect, AccessibilityNodeInfo>>comparingInt(e -> e.getKey().top)
                .thenComparingInt(e -> e.getKey().left));
        return number > 0 && ordered.size() >= number && click(ordered.get(number - 1).getValue());
    }

    private static Rect bounds(AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return bounds;
    }

    static boolean playback(AccessibilityNodeInfo root, boolean pause) {
        if (root == null) return false;
        List<AccessibilityNodeInfo> buttons = new ArrayList<>();
        for (AccessibilityNodeInfo player : root.findAccessibilityNodeInfosByViewId(ID + "watch_player")) {
            collect(player, node -> node.isClickable() && node.isVisibleToUser()
                && switch (description(node)) {
                    case "pause", "tạm dừng", "tạm dừng video", "pause video" -> true;
                    case "play", "phát", "phát video", "play video" -> true;
                    default -> false;
                }, buttons);
        }
        if (buttons.size() != 1) return false;
        AccessibilityNodeInfo button = buttons.get(0);
        boolean currentlyPlaying = switch (description(button)) {
            case "pause", "tạm dừng", "tạm dừng video", "pause video" -> true;
            default -> false;
        };
        return currentlyPlaying != pause || click(button);
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
