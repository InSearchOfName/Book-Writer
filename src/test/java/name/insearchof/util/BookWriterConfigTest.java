package name.insearchof.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookWriterConfigTest {
    @TempDir
    Path dir;

    @AfterEach
    void resetState() {
        BookWriterConfig.delaySeconds = null;
    }

    private Path fileWith(String content) throws IOException {
        return Files.writeString(dir.resolve("book-writer.json"), content);
    }

    @Test
    void missingFileGivesTheDefault() {
        assertEquals(BookWriterConfig.DEFAULT_DELAY_SECONDS, BookWriterConfig.load(dir.resolve("missing.json")));
    }

    @Test
    void invalidOrEmptyFileGivesTheDefault() throws IOException {
        assertEquals(BookWriterConfig.DEFAULT_DELAY_SECONDS, BookWriterConfig.load(fileWith("not json")));
        assertEquals(BookWriterConfig.DEFAULT_DELAY_SECONDS, BookWriterConfig.load(fileWith("")));
    }

    @Test
    void valuesOutsideTheRangeAreClamped() throws IOException {
        assertEquals(BookWriterConfig.MIN_DELAY_SECONDS, BookWriterConfig.load(fileWith("{\"delaySeconds\": 0.1}")));
        assertEquals(BookWriterConfig.MAX_DELAY_SECONDS, BookWriterConfig.load(fileWith("{\"delaySeconds\": 99}")));
    }

    @Test
    void savedValueIsLoadedAgain() {
        Path file = dir.resolve("saved.json");
        BookWriterConfig.save(file, 4.5);
        assertEquals(4.5, BookWriterConfig.load(file));
    }

    @Test
    void settingTheDelayClampsAndSavesIt() {
        Path file = dir.resolve("book-writer.json");
        BookWriterConfig.configFile = () -> file;

        BookWriterConfig.setDelaySeconds(99);
        assertEquals(BookWriterConfig.MAX_DELAY_SECONDS, BookWriterConfig.getDelaySeconds());
        assertEquals(BookWriterConfig.MAX_DELAY_SECONDS, BookWriterConfig.load(file));

        BookWriterConfig.setDelaySeconds(2.5);
        assertEquals(2500, BookWriterConfig.getDelayMillis());
    }

    @Test
    void delayIsLoadedFromTheFileOnFirstUse() throws IOException {
        Path file = fileWith("{\"delaySeconds\": 7}");
        BookWriterConfig.configFile = () -> file;
        assertEquals(7.0, BookWriterConfig.getDelaySeconds());
    }

    @Test
    void notANumberGivesTheDefault() {
        assertEquals(BookWriterConfig.DEFAULT_DELAY_SECONDS, BookWriterConfig.clamp(Double.NaN));
    }

    @Test
    void savingToAnUnwritablePathDoesNotThrow() {
        // a directory cannot be opened as a file
        BookWriterConfig.save(dir, 4.0);
    }
}
