package Server;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActivityLog {
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final String filePath;
    private final long maxBytes;
    private final int maxBackups;

    public ActivityLog(String filePath) {
        this.filePath = filePath;
        this.maxBytes = Long.parseLong(System.getProperty("crest.audit.max.bytes", "1048576"));
        this.maxBackups = Integer.parseInt(System.getProperty("crest.audit.max.backups", "3"));
    }

    public void log(String msg) {
        logEvent("INFO", "app", "-", "-", msg);
    }

    public void logEvent(String level, String action, String actor, String target, String details) {
        exec.submit(() -> {
            rotateIfNeeded();
            String safeDetails = details == null ? "" : details.replace("\"", "\\\"");
            String line = String.format(
                    "{\"ts\":\"%s\",\"level\":\"%s\",\"action\":\"%s\",\"actor\":\"%s\",\"target\":\"%s\",\"details\":\"%s\"}%s",
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(LocalDateTime.now().atOffset(ZoneOffset.UTC)),
                    level,
                    action,
                    actor == null ? "-" : actor,
                    target == null ? "-" : target,
                    safeDetails,
                    System.lineSeparator()
            );
            try (FileWriter fw = new FileWriter(filePath, true)) {
                fw.write(line);
            } catch (Exception ignored) {}
        });
    }

    private void rotateIfNeeded() {
        try {
            File logFile = new File(filePath);
            if (!logFile.exists() || logFile.length() < maxBytes) {
                return;
            }
            for (int i = maxBackups; i >= 1; i--) {
                File src = new File(filePath + "." + i);
                if (!src.exists()) continue;
                if (i == maxBackups) {
                    src.delete();
                } else {
                    src.renameTo(new File(filePath + "." + (i + 1)));
                }
            }
            logFile.renameTo(new File(filePath + ".1"));
        } catch (Exception ignored) {}
    }
}
