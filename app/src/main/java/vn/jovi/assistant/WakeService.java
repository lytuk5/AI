package vn.jovi.assistant;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

public final class WakeService extends Service {
    private static final String CHANNEL = "jovi_wake";
    private static final int ID = 18;
    private static final int SAMPLE_RATE = 16000;
    private static final String WAKE_GRAMMAR = """
            ["jovi jovi", "joe vee joe vee", "joe v joe v", "joey joey", "joby joby", "[unk]"]
            """;
    private static WakeService current;
    private static volatile int commandSessions;
    private volatile boolean capturing;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private volatile long pausedUntil;
    private PowerManager.WakeLock cpu;

    static boolean enabled() { return current != null; }

    static void pauseForCommand() {
        commandSessions++;
    }

    static void resumeWake() {
        if (--commandSessions == 0 && current != null) current.pausedUntil = 0;
        assert commandSessions >= 0;
    }

    static void whenMicrophoneReleased(Handler handler, java.util.function.BooleanSupplier active,
                                       Runnable ready, Runnable busy) {
        long deadline = SystemClock.elapsedRealtime() + 2000;
        Runnable check = new Runnable() {
            @Override public void run() {
                if (!active.getAsBoolean()) return;
                if (current == null || !current.capturing) ready.run();
                else if (SystemClock.elapsedRealtime() >= deadline) busy.run();
                else handler.postDelayed(this, 25);
            }
        };
        check.run();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (running) return START_NOT_STICKY;
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(
                CHANNEL, getString(R.string.wake_title), NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.wake_waiting))
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(open)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(ID, notification);
        }
        cpu = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Jovi:WakeListener");
        cpu.acquire();
        running = true;
        current = this;
        new Thread(this::listen, "Jovi wake word").start();
        return START_NOT_STICKY;
    }

    private void listen() {
        try (Model model = new Model(SpeechModel.directory(this, "jovi-wake-v1", "wake-model.zip",
                "vosk-model-small-en-us-0.15", () -> running).getAbsolutePath());
             Recognizer recognizer = new Recognizer(model, SAMPLE_RATE, WAKE_GRAMMAR)) {
            int size = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (size <= 0) throw new IllegalStateException("Unsupported wake microphone format: " + size);
            short[] audio = new short[1600];
            int retryDelay = 250;
            while (running) {
                if (commandSessions > 0 || pausedUntil > SystemClock.elapsedRealtime()) {
                    Thread.sleep(100);
                    continue;
                }
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    throw new SecurityException("Microphone permission revoked");
                }
                boolean detected = false;
                boolean silentStream = false;
                AudioRecord mic = null;
                capturing = true;
                try {
                    // Publish capture before checking whether a command has claimed the mic.
                    if (commandSessions > 0) continue;
                    mic = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(size, 6400));
                    recognizer.reset();
                    mic.startRecording();
                    if (mic.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                        throw new IllegalStateException("Wake microphone did not start recording");
                    }
                    int zeroSamples = 0;
                    while (running && commandSessions == 0 && pausedUntil <= SystemClock.elapsedRealtime()) {
                        int count = mic.read(audio, 0, audio.length);
                        if (count < 0) throw new IllegalStateException("AudioRecord read: " + count);
                        if (count == 0) continue;
                        zeroSamples += count;
                        for (int i = 0; i < count; i++) {
                            if (audio[i] != 0) {
                                zeroSamples = 0;
                                retryDelay = 250;
                                break;
                            }
                        }
                        // A stream of exact zeros can persist after the emulator loses its host input.
                        if (zeroSamples >= 4 * SAMPLE_RATE) {
                            silentStream = true;
                            break;
                        }
                        boolean complete = recognizer.acceptWaveForm(audio, count);
                        var result = new JSONObject(complete ? recognizer.getResult() : recognizer.getPartialResult());
                        if (isWakePhrase(result.optString(complete ? "text" : "partial"))) {
                            detected = true;
                            pausedUntil = SystemClock.elapsedRealtime() + 16000;
                            break;
                        }
                    }
                } finally {
                    if (mic != null) mic.release();
                    capturing = false;
                }
                if (running && silentStream) {
                    Thread.sleep(retryDelay);
                    retryDelay = Math.min(retryDelay * 2, 4000);
                }
                if (running && detected) {
                    handler.post(() -> {
                        if (!running) return;
                        Log.i("Jovi", "Wake phrase detected");
                        try {
                            if (!TikTokService.showWakePanel()) pausedUntil = 0;
                        } catch (RuntimeException error) {
                            Log.w("Jovi", "Could not show wake screen", error);
                            pausedUntil = 0;
                        }
                    });
                    while (running && pausedUntil > SystemClock.elapsedRealtime()) Thread.sleep(100);
                }
            }
        } catch (Exception | LinkageError error) {
            if (running) {
                Log.e("Jovi", "Wake listener stopped", error);
                handler.post(() -> {
                    android.widget.Toast.makeText(this, R.string.wake_failed, android.widget.Toast.LENGTH_LONG).show();
                    stopSelf();
                });
            }
        }
    }

    static boolean isWakePhrase(String value) {
        return switch (value) {
            case "jovi jovi", "jovie jovie", "joe vee joe vee", "joe v joe v",
                 "joey joey", "joby joby" -> true;
            default -> false;
        };
    }

    @Override public void onDestroy() {
        running = false;
        if (cpu != null && cpu.isHeld()) cpu.release();
        if (current == this) current = null;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
