package name.insearchof.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BookWriter {
    private static final int CHARS_PER_PAGE = 256;
    private static final int MAX_PAGES_PER_BOOK = 100;
    private static final int WRITE_DELAY_MS = 3000;

    private BookWriter() {
        // Utility class
    }

    /**
     * Writes text to one or more books asynchronously
     *
     * @param title   The title for the book(s)
     * @param content The content to write
     * @param player  The player writing the book
     */
    public static void writeAndSignBook(String title, String content, Player player) {
        String[] pages = splitIntoPages(content);
        List<Integer> bookSlots = findAvailableBooks(player);

        int booksNeeded = calculateBooksNeeded(pages.length);
        if (bookSlots.size() < booksNeeded) {
            notifyPlayer(
                player,
                Component
                    .literal(String.format("Not enough books! Need: %d, Have: %d", booksNeeded, bookSlots.size()))
                    .withStyle(ChatFormatting.RED)
            );
            return;
        }

        writeAsync(title, pages, bookSlots, player, booksNeeded);
    }

    private static void writeAsync(String title, String[] pages, List<Integer> bookSlots,
                                   Player player, int booksNeeded) {
        new Thread(() -> {
            notifyPlayer(player, Component.literal("Stand still while writing books...").withStyle(ChatFormatting.GOLD));

            for (int bookIndex = 0; bookIndex < booksNeeded; bookIndex++) {
                int startPage = bookIndex * MAX_PAGES_PER_BOOK;
                int endPage = Math.min(startPage + MAX_PAGES_PER_BOOK, pages.length);

                String bookTitle = formatBookTitle(title, bookIndex, booksNeeded);
                List<String> bookPages = extractPages(pages, startPage, endPage);

                sendBookPacket(bookSlots.get(bookIndex), bookPages, bookTitle, player);

                notifyPlayer(
                    player,
                    Component.literal(String.format("Book %d of %d written", bookIndex + 1, booksNeeded)).withStyle(ChatFormatting.GOLD)
                );

                if (bookIndex < booksNeeded - 1) {
                    delayNextWrite();
                }
            }

            completeWriting(player);
        }).start();
    }

    private static String[] splitIntoPages(String text) {
        List<String> pages = new ArrayList<>();

        for (int i = 0; i < text.length(); i += CHARS_PER_PAGE) {
            int endPos = Math.min(i + CHARS_PER_PAGE, text.length());
            pages.add(text.substring(i, endPos));
        }

        return pages.isEmpty() ? new String[]{""} : pages.toArray(new String[0]);
    }

    private static List<Integer> findAvailableBooks(Player player) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.WRITABLE_BOOK) {
                slots.add(i);
            }
        }
        return slots;
    }

    private static int calculateBooksNeeded(int pageCount) {
        return (int) Math.ceil((double) pageCount / MAX_PAGES_PER_BOOK);
    }

    private static String formatBookTitle(String baseTitle, int bookIndex, int totalBooks) {
        if (totalBooks == 1) {
            return baseTitle;
        }
        return baseTitle + " (Part " + (bookIndex + 1) + ")";
    }

    private static List<String> extractPages(String[] allPages, int startPage, int endPage) {
        List<String> pages = new ArrayList<>();
        pages.addAll(Arrays.asList(allPages).subList(startPage, endPage));
        return pages;
    }

    private static void sendBookPacket(int slot, List<String> pages, String title, Player player) {
        var client = Minecraft.getInstance();
        if (client.getConnection() == null) return;

        ServerboundEditBookPacket packet = new ServerboundEditBookPacket(slot, pages, Optional.of(title));
        client.execute(() -> client.getConnection().send(packet));
    }

    private static void notifyPlayer(Player player, Component message) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setOverlayMessage(message, false));
    }

    private static void delayNextWrite() {
        try {
            Thread.sleep(WRITE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void completeWriting(Player player) {
        notifyPlayer(player, Component.literal("All books written!").withStyle(ChatFormatting.GREEN));
        Minecraft.getInstance().execute(() -> player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f));
    }
}

