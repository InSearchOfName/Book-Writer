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
import java.util.List;
import java.util.Optional;

public class BookWriter {
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
        String[] pages = BookLayout.splitIntoPages(content);
        List<Integer> bookSlots = findAvailableBooks(player);

        int booksNeeded = BookLayout.calculateBooksNeeded(pages.length);
        if (bookSlots.size() < booksNeeded) {
            notifyPlayer(
                Component
                    .literal(String.format("Not enough books! Need: %d, Have: %d", booksNeeded, bookSlots.size()))
                    .withStyle(ChatFormatting.RED)
            );
            return;
        }

        writeAsync(title, pages, bookSlots, player, booksNeeded, BookWriterConfig.getDelayMillis());
    }

    private static void writeAsync(String title, String[] pages, List<Integer> bookSlots,
                                   Player player, int booksNeeded, long delayMs) {
        new Thread(() -> {
            notifyPlayer( Component.literal("Stand still while writing books...").withStyle(ChatFormatting.GOLD));

            for (int bookIndex = 0; bookIndex < booksNeeded; bookIndex++) {
                int startPage = bookIndex * BookLayout.MAX_PAGES_PER_BOOK;
                int endPage = Math.min(startPage + BookLayout.MAX_PAGES_PER_BOOK, pages.length);

                String bookTitle = BookLayout.formatBookTitle(title, bookIndex, booksNeeded);
                List<String> bookPages = BookLayout.extractPages(pages, startPage, endPage);

                sendBookPacket(bookSlots.get(bookIndex), bookPages, bookTitle);

                notifyPlayer(
                    Component.literal(String.format("Book %d of %d written", bookIndex + 1, booksNeeded)).withStyle(ChatFormatting.GOLD)
                );

                if (bookIndex < booksNeeded - 1) {
                    delayNextWrite(delayMs);
                }
            }

            completeWriting(player);
        }).start();
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

    private static void sendBookPacket(int slot, List<String> pages, String title) {
        var client = Minecraft.getInstance();
        if (client.getConnection() == null) return;

        ServerboundEditBookPacket packet = new ServerboundEditBookPacket(slot, pages, Optional.of(title));
        client.execute(() -> client.getConnection().send(packet));
    }

    private static void notifyPlayer( Component message) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.hud.setOverlayMessage(message, false));
    }

    private static void delayNextWrite(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void completeWriting(Player player) {
        notifyPlayer( Component.literal("All books written!").withStyle(ChatFormatting.GREEN));
        Minecraft.getInstance().execute(() -> player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f));
    }
}

