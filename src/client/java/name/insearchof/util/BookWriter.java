package name.insearchof.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

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
    public static void writeAndSignBook(String title, String content, PlayerEntity player) {
        String[] pages = splitIntoPages(content);
        List<Integer> bookSlots = findAvailableBooks(player);

        int booksNeeded = calculateBooksNeeded(pages.length);
        if (bookSlots.size() < booksNeeded) {
            notifyPlayer(player, String.format("§c§lNot enough books! §r§7Need: %d, Have: %d",
                    booksNeeded, bookSlots.size()));
            return;
        }

        writeAsync(title, pages, bookSlots, player, booksNeeded);
    }

    private static void writeAsync(String title, String[] pages, List<Integer> bookSlots,
                                   PlayerEntity player, int booksNeeded) {
        new Thread(() -> {
            notifyPlayer(player, "§6§lStand still while writing books...");

            for (int bookIndex = 0; bookIndex < booksNeeded; bookIndex++) {
                int startPage = bookIndex * MAX_PAGES_PER_BOOK;
                int endPage = Math.min(startPage + MAX_PAGES_PER_BOOK, pages.length);

                String bookTitle = formatBookTitle(title, bookIndex, booksNeeded);
                List<String> bookPages = extractPages(pages, startPage, endPage);

                sendBookPacket(bookSlots.get(bookIndex), bookPages, bookTitle, player);

                notifyPlayer(player, String.format("§6Book %d of %d written", bookIndex + 1, booksNeeded));

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

    private static List<Integer> findAvailableBooks(PlayerEntity player) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
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

    private static void sendBookPacket(int slot, List<String> pages, String title, PlayerEntity player) {
        BookUpdateC2SPacket packet = new BookUpdateC2SPacket(slot, pages, Optional.of(title));
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(packet);
    }

    private static void notifyPlayer(PlayerEntity player, String message) {
        MinecraftClient.getInstance().execute(() ->
                player.sendMessage(Text.literal(message), true)
        );
    }

    private static void delayNextWrite() {
        try {
            Thread.sleep(WRITE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void completeWriting(PlayerEntity player) {
        notifyPlayer(player, "§a§lAll books written!");
        MinecraftClient.getInstance().execute(() ->
                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
        );
    }
}

