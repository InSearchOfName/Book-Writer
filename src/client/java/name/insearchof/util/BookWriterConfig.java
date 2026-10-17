package name.insearchof.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class BookWriterConfig {
    public static final double MIN_DELAY_SECONDS = 1.0;
    public static final double MAX_DELAY_SECONDS = 10.0;
    public static final double DEFAULT_DELAY_SECONDS = 3.0;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("book-writer.json");

    private static double delaySeconds = load();

    private BookWriterConfig() {
        // Utility class
    }

    /**
     * @return the delay between writing two books, in seconds
     */
    public static double getDelaySeconds() {
        return delaySeconds;
    }

    /**
     * Sets the delay between writing two books and saves it. The value is clamped to the allowed range.
     */
    public static void setDelaySeconds(double seconds) {
        delaySeconds = clamp(seconds);
        save();
    }

    public static long getDelayMillis() {
        return Math.round(delaySeconds * 1000);
    }

    private static double clamp(double seconds) {
        if (Double.isNaN(seconds)) {
            return DEFAULT_DELAY_SECONDS;
        }
        return Math.max(MIN_DELAY_SECONDS, Math.min(MAX_DELAY_SECONDS, seconds));
    }

    private static double load() {
        if (!Files.exists(CONFIG_FILE)) {
            return DEFAULT_DELAY_SECONDS;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            Data data = GSON.fromJson(reader, Data.class);
            return data == null ? DEFAULT_DELAY_SECONDS : clamp(data.delaySeconds);
        } catch (IOException | RuntimeException e) {
            // Unreadable or invalid file: fall back to the default
            return DEFAULT_DELAY_SECONDS;
        }
    }

    private static void save() {
        Data data = new Data();
        data.delaySeconds = delaySeconds;

        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            // The setting still applies for this session
        }
    }

    private static class Data {
        double delaySeconds = DEFAULT_DELAY_SECONDS;
    }
}
