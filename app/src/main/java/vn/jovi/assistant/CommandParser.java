package vn.jovi.assistant;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

final class CommandParser {
    private static final Pattern SPACE = Pattern.compile("\\s+");
    private static final Pattern WAKE = Pattern.compile(
            "^(?:jovi|joby|giô vi|giôvi)(?:[\\s,]+|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern END = Pattern.compile("[.!?]+$");
    private static final Pattern OPEN = Pattern.compile("^(?:mở(?: ứng dụng)?|vào(?: ứng dụng)?|open(?: the app)?) (.+)$");

    static String stripWakeWord(String phrase) {
        String normalized = SPACE.matcher(Normalizer.normalize(phrase, Normalizer.Form.NFC).trim()).replaceAll(" ");
        return END.matcher(WAKE.matcher(normalized).replaceFirst("")).replaceFirst("").trim();
    }

    static String command(String phrase) {
        return stripWakeWord(phrase).toLowerCase(Locale.ROOT);
    }

    static String appCommand(String phrase) {
        String command = command(phrase);
        var open = OPEN.matcher(command);
        if (!open.matches()) return command;
        return "open " + switch (open.group(1)) {
            case "tik tok" -> "tiktok";
            case "you tube" -> "youtube";
            case "mess" -> "messenger";
            case "whats app" -> "whatsapp";
            case "tin nhắn", "sms" -> "messages";
            default -> open.group(1);
        };
    }
}
