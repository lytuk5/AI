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

import java.io.File;
import java.nio.file.Files;
import java.util.zip.ZipInputStream;

public final class WakeService extends Service {
    private static final String CHANNEL = "jovi_wake";
    private static final int ID = 18;
    private static final int SAMPLE_RATE = 16000;
    private static WakeService current;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private volatile long pausedUntil;
    private volatile long wakeAt;
    private PowerManager.WakeLock cpu;

    static boolean enabled() { return current != null; }

    static void pauseForCommand() {
        if (current != null) current.pausedUntil = SystemClock.elapsedRealtime() + 16000;
    }

    static void resumeWake() {
        if (current != null) current.pausedUntil = 0;
    }

    static boolean consumeWake() {
        if (current == null || current.wakeAt == 0
                || SystemClock.elapsedRealtime() - current.wakeAt > 4000) return false;
        current.wakeAt = 0;
        return true;
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

    private File modelDirectory() throws Exception {
        File folder = new File(getNoBackupFilesDir(), "jovi-wake-v1").getCanonicalFile();
        File ready = new File(folder, "ready");
        File model = new File(folder, "vosk-model-small-en-us-0.15");
        if (ready.isFile()) return model;
        Files.createDirectories(folder.toPath());
        try (var zip = new ZipInputStream(getAssets().open("wake-model.zip"))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!running) throw new InterruptedException();
                File target = new File(folder, entry.getName()).getCanonicalFile();
                if (!target.toPath().startsWith(folder.toPath())) throw new SecurityException("Invalid model path");
                if (entry.isDirectory()) {
                    Files.createDirectories(target.toPath());
                } else {
                    Files.createDirectories(target.toPath().getParent());
                    Files.copy(zip, target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Files.createFile(ready.toPath());
        return model;
    }

    private void listen() {
        try (Model model = new Model(modelDirectory().getAbsolutePath());
             Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {
            int size = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            short[] audio = new short[1600];
            while (running) {
                if (pausedUntil > SystemClock.elapsedRealtime()) {
                    Thread.sleep(100);
                    continue;
                }
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    throw new SecurityException("Microphone permission revoked");
                }
                AudioRecord mic = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(size, 6400));
                boolean detected = false;
                try {
                    recognizer.reset();
                    mic.startRecording();
                    while (running && pausedUntil <= SystemClock.elapsedRealtime()) {
                        int count = mic.read(audio, 0, audio.length);
                        if (count < 0) throw new IllegalStateException("AudioRecord read: " + count);
                        boolean complete = recognizer.acceptWaveForm(audio, count);
                        var result = new JSONObject(complete ? recognizer.getResult() : recognizer.getPartialResult());
                        if (isWakePhrase(result.optString(complete ? "text" : "partial"))) {
                            detected = true;
                            pausedUntil = SystemClock.elapsedRealtime() + 16000;
                            break;
                        }
                    }
                } finally {
                    mic.release();
                }
                if (running && detected) {
                    handler.post(() -> {
                        if (!running) return;
                        wakeAt = SystemClock.elapsedRealtime();
                        try {
                            if (!TikTokService.openWakeScreen()) resumeWake();
                        } catch (RuntimeException error) {
                            Log.w("Jovi", "Could not show wake screen", error);
                            resumeWake();
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
