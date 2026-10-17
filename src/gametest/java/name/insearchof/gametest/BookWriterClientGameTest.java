package name.insearchof.gametest;

import name.insearchof.event.BookWriterEvents;
import name.insearchof.util.BookLayout;
import name.insearchof.util.BookWriterConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs the real mod in a singleplayer world: the same event the "Write to Book" button invokes
 * is fired, and the signed books that end up in the inventory are checked.
 */
public class BookWriterClientGameTest implements FabricClientGameTest {
    private static final int WRITE_TIMEOUT_TICKS = 1200;

    @Override
    public void runTest(ClientGameTestContext context) {
        BookWriterConfig.setDelaySeconds(BookWriterConfig.MIN_DELAY_SECONDS);

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getClientLevel().waitForChunksRender();

            writesOneBook(context, singleplayer);
            writesALongTextAsSeveralBooks(context, singleplayer);
            writesMoreBooksThanTheHotbarHolds(context, singleplayer);
        }
    }

    private void writesOneBook(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("clear Player0");
        singleplayer.getServer().runCommand("give Player0 writable_book 1");
        context.waitFor(client -> countItems(client.player, Items.WRITABLE_BOOK) == 1);

        String text = "Hello from the client game test";
        write(context, "Test Book", text);

        waitForWrittenBooks(context, 1);
        List<WrittenBookContent> books = writtenBooks(context);

        assertEquals(1, books.size(), "number of written books");
        assertEquals("Test Book", books.get(0).title().raw(), "book title");
        assertEquals(List.of(text), pageTexts(books.get(0)), "book pages");
    }

    private void writesALongTextAsSeveralBooks(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("clear Player0");
        singleplayer.getServer().runCommand("give Player0 writable_book 2");
        context.waitFor(client -> countItems(client.player, Items.WRITABLE_BOOK) == 2);

        String text = "x".repeat(BookLayout.CHARS_PER_PAGE * (BookLayout.MAX_PAGES_PER_BOOK + 1));
        write(context, "Long Book", text);

        waitForWrittenBooks(context, 2);
        List<WrittenBookContent> books = writtenBooks(context);

        assertEquals(2, books.size(), "number of written books");
        assertEquals("Long Book (Part 1)", books.get(0).title().raw(), "first book title");
        assertEquals(BookLayout.MAX_PAGES_PER_BOOK, books.get(0).pages().size(), "pages in the first book");
        assertEquals("Long Book (Part 2)", books.get(1).title().raw(), "second book title");
        assertEquals(1, books.get(1).pages().size(), "pages in the second book");
    }

    private void writesMoreBooksThanTheHotbarHolds(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        // the server only edits books in the hotbar and offhand, so 3 of these 12 start in the main inventory
        int bookCount = 12;
        singleplayer.getServer().runCommand("clear Player0");
        singleplayer.getServer().runCommand("give Player0 writable_book " + bookCount);
        context.waitFor(client -> countItems(client.player, Items.WRITABLE_BOOK) == bookCount);

        String text = "y".repeat(BookLayout.CHARS_PER_PAGE * BookLayout.MAX_PAGES_PER_BOOK * bookCount);
        write(context, "Huge Book", text);

        waitForWrittenBooks(context, bookCount);
        List<WrittenBookContent> books = writtenBooks(context);

        assertEquals(bookCount, books.size(), "number of written books");
        Set<String> expectedTitles = new HashSet<>();
        for (int part = 1; part <= bookCount; part++) {
            expectedTitles.add("Huge Book (Part " + part + ")");
        }
        assertEquals(expectedTitles, books.stream().map(book -> book.title().raw()).collect(Collectors.toSet()), "book titles");
    }

    private void write(ClientGameTestContext context, String title, String text) {
        context.runOnClient(client ->
            BookWriterEvents.WRITE_BOOK.invoker().onWriteBook(client.player, title, text)
        );
    }

    private void waitForWrittenBooks(ClientGameTestContext context, int expected) {
        context.waitFor(client -> countItems(client.player, Items.WRITTEN_BOOK) == expected, WRITE_TIMEOUT_TICKS);
    }

    private List<WrittenBookContent> writtenBooks(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            List<WrittenBookContent> books = new ArrayList<>();
            Player player = client.player;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(Items.WRITTEN_BOOK)) {
                    books.add(stack.get(DataComponents.WRITTEN_BOOK_CONTENT));
                }
            }
            return books;
        });
    }

    private static List<String> pageTexts(WrittenBookContent content) {
        return content.pages().stream().map(page -> page.raw().getString()).toList();
    }

    private static int countItems(Player player, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) {
                count += player.getInventory().getItem(i).getCount();
            }
        }
        return count;
    }

    private static void assertEquals(Object expected, Object actual, String what) {
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
