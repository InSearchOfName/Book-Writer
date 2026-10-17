package name.insearchof.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookLayoutTest {
    private static final int CHARS_PER_PAGE = BookLayout.CHARS_PER_PAGE;
    private static final int MAX_PAGES_PER_BOOK = BookLayout.MAX_PAGES_PER_BOOK;

    @Test
    void limitsMatchMinecraft() {
        assertEquals(256, CHARS_PER_PAGE);
        assertEquals(100, MAX_PAGES_PER_BOOK);
    }

    @Test
    void emptyTextIsOneEmptyPage() {
        assertArrayEquals(new String[]{""}, BookLayout.splitIntoPages(""));
    }

    @Test
    void shortTextIsOnePage() {
        assertArrayEquals(new String[]{"Hello"}, BookLayout.splitIntoPages("Hello"));
    }

    @Test
    void exactlyOnePageOfCharactersStaysOnePage() {
        String text = "a".repeat(CHARS_PER_PAGE);

        assertEquals(1, BookLayout.splitIntoPages(text).length);
    }

    @Test
    void oneCharacterOverStartsASecondPage() {
        String text = "a".repeat(CHARS_PER_PAGE) + "b";

        String[] pages = BookLayout.splitIntoPages(text);

        assertEquals(2, pages.length);
        assertEquals("a".repeat(CHARS_PER_PAGE), pages[0]);
        assertEquals("b", pages[1]);
    }

    @Test
    void splitPagesJoinBackToTheOriginalText() {
        String text = "The quick brown fox. ".repeat(500);

        assertEquals(text, String.join("", BookLayout.splitIntoPages(text)));
    }

    @Test
    void totalPagesIsAtLeastOne() {
        assertEquals(1, BookLayout.calculateTotalPages(0));
        assertEquals(1, BookLayout.calculateTotalPages(1));
        assertEquals(1, BookLayout.calculateTotalPages(CHARS_PER_PAGE));
        assertEquals(2, BookLayout.calculateTotalPages(CHARS_PER_PAGE + 1));
    }

    @Test
    void exactlyOneFullBookOfPagesNeedsOneBook() {
        assertEquals(1, BookLayout.calculateBooksNeeded(1));
        assertEquals(1, BookLayout.calculateBooksNeeded(MAX_PAGES_PER_BOOK));
    }

    @Test
    void oneMorePageThanABookNeedsASecondBook() {
        assertEquals(2, BookLayout.calculateBooksNeeded(MAX_PAGES_PER_BOOK + 1));
        assertEquals(2, BookLayout.calculateBooksNeeded(MAX_PAGES_PER_BOOK * 2));
        assertEquals(3, BookLayout.calculateBooksNeeded(MAX_PAGES_PER_BOOK * 2 + 1));
    }

    @Test
    void booksNeededForCharactersCountsWholePages() {
        int fullBookChars = CHARS_PER_PAGE * MAX_PAGES_PER_BOOK;

        assertEquals(1, BookLayout.calculateBooksNeededForChars(0));
        assertEquals(1, BookLayout.calculateBooksNeededForChars(fullBookChars));
        assertEquals(2, BookLayout.calculateBooksNeededForChars(fullBookChars + 1));
    }

    @Test
    void theMaximumTextTakesTheExpectedNumberOfBooks() {
        // the .write screen allows 1,000,000 characters
        assertEquals(3907, BookLayout.calculateTotalPages(1_000_000));
        assertEquals(40, BookLayout.calculateBooksNeededForChars(1_000_000));
    }

    @Test
    void singleBookKeepsItsTitle() {
        assertEquals("My Story", BookLayout.formatBookTitle("My Story", 0, 1));
    }

    @Test
    void multipleBooksGetPartNumbers() {
        assertEquals("My Story (Part 1)", BookLayout.formatBookTitle("My Story", 0, 3));
        assertEquals("My Story (Part 3)", BookLayout.formatBookTitle("My Story", 2, 3));
    }

    @Test
    void extractPagesReturnsTheRequestedRange() {
        String[] pages = {"a", "b", "c", "d"};

        assertEquals(List.of("b", "c"), BookLayout.extractPages(pages, 1, 3));
        assertEquals(List.of("a", "b", "c", "d"), BookLayout.extractPages(pages, 0, 4));
        assertEquals(List.of(), BookLayout.extractPages(pages, 2, 2));
    }

    @Test
    void extractPagesRejectsARangeOutsideThePages() {
        String[] pages = {"a", "b"};

        assertThrows(IndexOutOfBoundsException.class, () -> BookLayout.extractPages(pages, 0, 3));
    }

    @Test
    void aLargeTextSplitsIntoFullBooksAndARemainder() {
        String text = "x".repeat(CHARS_PER_PAGE * (MAX_PAGES_PER_BOOK + 5));
        String[] pages = BookLayout.splitIntoPages(text);
        int booksNeeded = BookLayout.calculateBooksNeeded(pages.length);

        assertEquals(MAX_PAGES_PER_BOOK + 5, pages.length);
        assertEquals(2, booksNeeded);

        List<String> firstBook = BookLayout.extractPages(pages, 0, MAX_PAGES_PER_BOOK);
        List<String> secondBook = BookLayout.extractPages(pages, MAX_PAGES_PER_BOOK, pages.length);

        assertEquals(MAX_PAGES_PER_BOOK, firstBook.size());
        assertEquals(5, secondBook.size());
    }

    @Test
    void estimateOnlyWaitsBetweenBooks() {
        assertEquals(0.0, BookLayout.estimateSeconds(1, 3.0));
        assertEquals(6.0, BookLayout.estimateSeconds(3, 3.0));
        assertEquals(4.5, BookLayout.estimateSeconds(4, 1.5));
    }

    @Test
    void estimateForNoBooksIsZero() {
        assertEquals(0.0, BookLayout.estimateSeconds(0, 3.0));
    }
}
