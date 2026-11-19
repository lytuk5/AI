package vn.jovi.assistant;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipInputStream;

final class SpeechModel {
    static synchronized File directory(Context context, String cache, String asset, String modelName,
                                       BooleanSupplier active) throws IOException, InterruptedException {
        File folder = new File(context.getNoBackupFilesDir(), cache).getCanonicalFile();
        File ready = new File(folder, "ready");
        File model = new File(folder, modelName);
        if (ready.isFile()) return model;
        Files.createDirectories(folder.toPath());
        try (var zip = new ZipInputStream(context.getAssets().open(asset))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!active.getAsBoolean()) throw new InterruptedException();
                File target = new File(folder, entry.getName()).getCanonicalFile();
                if (!target.toPath().startsWith(folder.toPath())) throw new IOException("Invalid model path");
                if (entry.isDirectory()) {
                    Files.createDirectories(target.toPath());
                } else {
                    Files.createDirectories(target.toPath().getParent());
                    Files.copy(zip, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Files.createFile(ready.toPath());
        return model;
    }
}
