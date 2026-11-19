package vn.jovi.assistant;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.util.Set;
import java.util.function.Consumer;

final class VietnameseVoiceInput {
    private static final int SAMPLE_RATE = 16000;
    private static final Set<String> OPEN_COMMANDS = Set.of("open youtube", "open tiktok",
            "open messenger", "open zalo", "open whatsapp", "open messages");
    private static final String OPEN_GRAMMAR = openGrammar();

    private static String openGrammar() {
        var phrases = new JSONArray().put("[unk]");
        for (String verb : new String[]{"mở", "mở ứng dụng", "vào", "vào ứng dụng"}) {
            for (String app : new String[]{"youtube", "iu túp", "du túp", "tích tóc",
                    "tin nhắn", "gia lô", "messenger", "oát áp"}) {
                phrases.put(verb + " " + app);
            }
        }
        return phrases.toString();
    }
    private final Context context;
    private final boolean appCommands;
    private final Consumer<String> partial;
    private final Handler handler;
    private final Runnable ready;
    private final Consumer<String> result;
    private final Consumer<String> error;
    private volatile boolean cancelled;
    private boolean recordingStarted;

    VietnameseVoiceInput(Context context, Handler handler, boolean appCommands, Runnable ready,
                         Consumer<String> partial, Consumer<String> result, Consumer<String> error) {
        this.context = context;
        this.handler = handler;
        this.appCommands = appCommands;
        this.ready = ready;
        this.partial = partial;
        this.result = result;
        this.error = error;
    }

    void start() {
        WakeService.pauseForCommand();
        WakeService.whenMicrophoneReleased(handler, () -> !cancelled, () -> {
            recordingStarted = true;
            new Thread(this::recognize, "Jovi Vietnamese speech").start();
        }, () -> finish("", R.string.voice_busy));
    }

    void cancel() {
        if (cancelled) return;
        cancelled = true;
        if (!recordingStarted) WakeService.resumeWake();
    }

    private void recognize() {
        String text = "";
        int message = R.string.no_command;
        try {
            var path = SpeechModel.directory(context, "jovi-voice-vi-v1", "voice-vi-model.zip",
                    "vosk-model-small-vn-0.4", () -> !cancelled);
            if (!cancelled) {
                try (Model model = new Model(path.getAbsolutePath());
                     Recognizer recognizer = new Recognizer(model, SAMPLE_RATE);
                     Recognizer appOpener = appCommands
                             ? new Recognizer(model, SAMPLE_RATE, OPEN_GRAMMAR) : null) {
                    if (appOpener != null) appOpener.setWords(true);
                    text = listen(recognizer, appOpener);
                }
            }
        } catch (SecurityException failure) {
            message = R.string.permission_hint;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception | LinkageError failure) {
            message = R.string.voice_busy;
            Log.w("Jovi", "Vietnamese speech recognition failed", failure);
        }
        String phrase = text;
        int errorMessage = message;
        handler.post(() -> finish(phrase, errorMessage));
    }

    @SuppressLint("MissingPermission") // Both voice entry points check RECORD_AUDIO before starting.
    private String listen(Recognizer recognizer, Recognizer appOpener) throws Exception {
        if (cancelled) return "";
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize <= 0) throw new IllegalStateException("Unsupported microphone format");
        AudioRecord mic = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(bufferSize, 6400));
        try {
            mic.startRecording();
            if (mic.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                throw new IllegalStateException("Vietnamese microphone did not start recording");
            }
            handler.post(() -> { if (!cancelled) ready.run(); });
            short[] samples = new short[1600];
            long deadline = SystemClock.elapsedRealtime() + 12000;
            JSONObject openResult = null;
            long lastPreview = 0;
            String preview = "";
            while (!cancelled && SystemClock.elapsedRealtime() < deadline) {
                int count = mic.read(samples, 0, samples.length);
                if (count < 0) throw new IllegalStateException("AudioRecord read: " + count);
                if (count == 0) continue;
                if (appOpener != null && appOpener.acceptWaveForm(samples, count)) {
                    JSONObject segment = new JSONObject(appOpener.getResult());
                    if (!segment.optString("text").isBlank()) openResult = segment;
                }
                if (recognizer.acceptWaveForm(samples, count)) {
                    String text = new JSONObject(recognizer.getResult()).optString("text").trim();
                    if (!text.isEmpty()) return chooseResult(text, appOpener, openResult);
                }
                long now = SystemClock.elapsedRealtime();
                if (partial != null && now - lastPreview >= 250) {
                    lastPreview = now;
                    String words = new JSONObject(recognizer.getPartialResult()).optString("partial");
                    if (!words.isBlank() && !words.equals(preview)) {
                        preview = words;
                        handler.post(() -> { if (!cancelled) partial.accept(words); });
                    }
                }
            }
            if (cancelled) return "";
            String text = new JSONObject(recognizer.getFinalResult()).optString("text").trim();
            return chooseResult(text, appOpener, openResult);
        } finally {
            mic.release();
        }
    }

    private static String chooseResult(String text, Recognizer appOpener, JSONObject completed)
            throws org.json.JSONException {
        if (appOpener == null || OPEN_COMMANDS.contains(CommandParser.appCommand(text))) return text;
        String words = CommandParser.command(text);
        if (!words.isEmpty() && !CommandParser.soundsLikeOpenCommand(words)) return text;

        JSONObject result = completed == null ? new JSONObject(appOpener.getFinalResult()) : completed;
        String candidate = result.optString("text");
        if (!OPEN_COMMANDS.contains(CommandParser.appCommand(candidate))) return text;

        var details = result.optJSONArray("result");
        if (details == null || details.length() == 0) return text;
        double total = 0;
        for (int i = 0; i < details.length(); i++) {
            double confidence = details.getJSONObject(i).optDouble("conf", 0);
            if (confidence < 0.6) return text;
            total += confidence;
        }
        return total / details.length() >= 0.8 ? candidate : text;
    }

    private void finish(String text, int message) {
        WakeService.resumeWake();
        if (cancelled) return;
        cancelled = true;
        if (text.isEmpty()) error.accept(context.getString(message));
        else result.accept(text);
    }
}
