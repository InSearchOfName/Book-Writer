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

    // Loaded on first use, so the class can be loaded without Fabric (unit tests)
    private static Double delaySeconds;

    private BookWriterConfig() {
        // Utility class
    }

    /**
     * @return the delay between writing two books, in seconds
     */
    public static double getDelaySeconds() {
        if (delaySeconds == null) {
            delaySeconds = load(configFile());
        }
        return delaySeconds;
    }

    /**
     * Sets the delay between writing two books and saves it. The value is clamped to the allowed range.
     */
    public static void setDelaySeconds(double seconds) {
        delaySeconds = clamp(seconds);
        save(configFile(), delaySeconds);
    }

    public static long getDelayMillis() {
        return Math.round(getDelaySeconds() * 1000);
    }

    private static double clamp(double seconds) {
        if (Double.isNaN(seconds)) {
            return DEFAULT_DELAY_SECONDS;
        }
        return Math.clamp(seconds, MIN_DELAY_SECONDS, MAX_DELAY_SECONDS);
    }

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("book-writer.json");
    }

    static double load(Path file) {
        if (!Files.exists(file)) {
            return DEFAULT_DELAY_SECONDS;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            Data data = GSON.fromJson(reader, Data.class);
            return data == null ? DEFAULT_DELAY_SECONDS : clamp(data.delaySeconds);
        } catch (IOException | RuntimeException e) {
            // Unreadable or invalid file: fall back to the default
            return DEFAULT_DELAY_SECONDS;
        }
    }

    static void save(Path file, double seconds) {
        Data data = new Data();
        data.delaySeconds = seconds;

        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            // The setting still applies for this session
        }
    }

    private static class Data {
        double delaySeconds = DEFAULT_DELAY_SECONDS;
    }
}
