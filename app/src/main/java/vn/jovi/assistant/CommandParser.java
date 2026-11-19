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
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern OPEN = Pattern.compile("^(?:mo(?: ung dung)?|vao(?: ung dung)?|open(?: the app)?) (.+)$");

    static String stripWakeWord(String phrase) {
        String normalized = SPACE.matcher(Normalizer.normalize(phrase, Normalizer.Form.NFC).trim()).replaceAll(" ");
        return END.matcher(WAKE.matcher(normalized).replaceFirst("")).replaceFirst("").trim();
    }

    static String command(String phrase) {
        return stripWakeWord(phrase).toLowerCase(Locale.ROOT);
    }

    private static String foldAccents(String command) {
        return MARKS.matcher(Normalizer.normalize(command, Normalizer.Form.NFD))
                .replaceAll("").replace('đ', 'd');
    }

    static boolean soundsLikeOpenCommand(String command) {
        return OPEN.matcher(foldAccents(command)).matches();
    }

    static String appCommand(String phrase) {
        String command = command(phrase);
        var open = OPEN.matcher(foldAccents(command));
        if (!open.matches()) return command;
        return switch (open.group(1).replace(" ", "")) {
            case "tiktok", "tichtoc" -> "open tiktok";
            case "youtube", "iutup", "dutup", "youtup", "iutip", "dutip" -> "open youtube";
            case "zalo", "gialo" -> "open zalo";
            case "mess", "messenger" -> "open messenger";
            case "whatsapp", "oatap" -> "open whatsapp";
            case "tinnhan", "sms", "messages" -> "open messages";
            default -> command;
        };
    }
}
