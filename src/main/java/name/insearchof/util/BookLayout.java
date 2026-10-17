package name.insearchof.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Splits text into pages and books and works out how many are needed.
 * This class does not use Minecraft, so it can be unit tested.
 */
public class BookLayout {
    public static final int CHARS_PER_PAGE = 256;
    public static final int MAX_PAGES_PER_BOOK = 100;

    private BookLayout() {
        // Utility class
    }

    /**
     * Splits text into pages of at most {@link #CHARS_PER_PAGE} characters.
     * Empty text gives one empty page.
     */
    public static String[] splitIntoPages(String text) {
        List<String> pages = new ArrayList<>();

        for (int i = 0; i < text.length(); i += CHARS_PER_PAGE) {
            int endPos = Math.min(i + CHARS_PER_PAGE, text.length());
            pages.add(text.substring(i, endPos));
        }

        return pages.isEmpty() ? new String[]{""} : pages.toArray(new String[0]);
    }

    /**
     * @return how many pages the given number of characters takes (at least one)
     */
    public static int calculateTotalPages(int charCount) {
        int pages = (int) Math.ceil((double) charCount / CHARS_PER_PAGE);
        return Math.max(pages, 1);
    }

    /**
     * @return how many books the given number of pages takes
     */
    public static int calculateBooksNeeded(int pageCount) {
        return (int) Math.ceil((double) pageCount / MAX_PAGES_PER_BOOK);
    }

    /**
     * @return how many books the given number of characters takes (at least one)
     */
    public static int calculateBooksNeededForChars(int charCount) {
        return calculateBooksNeeded(calculateTotalPages(charCount));
    }

    /**
     * @return the title of a book; books of a multi-book text get a " (Part N)" suffix
     */
    public static String formatBookTitle(String baseTitle, int bookIndex, int totalBooks) {
        if (totalBooks == 1) {
            return baseTitle;
        }
        return baseTitle + " (Part " + (bookIndex + 1) + ")";
    }

    /**
     * @return a copy of the pages from startPage (inclusive) to endPage (exclusive)
     */
    public static List<String> extractPages(String[] allPages, int startPage, int endPage) {
        return new ArrayList<>(Arrays.asList(allPages).subList(startPage, endPage));
    }

    /**
     * @return how long writing takes: the delay is only waited between books, not after the last one
     */
    public static double estimateSeconds(int booksNeeded, double delaySeconds) {
        return Math.max(booksNeeded - 1, 0) * delaySeconds;
    }
}
