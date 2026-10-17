package name.insearchof.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class BookWriter {
    private static final long SIGN_TIMEOUT_MS = 15000;
    private static final long SIGN_POLL_MS = 50;
    private static final long SETTLE_MS = 250;
    private static final int SWAP_ATTEMPTS = 3;

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

        List<BookSlots.Step> steps = BookSlots.plan(bookSlots, findEmptyHotbarSlots(player), player.getInventory().getSelectedSlot());
        writeAsync(title, pages, steps, player, booksNeeded, BookWriterConfig.getDelayMillis());
    }

    private static void writeAsync(String title, String[] pages, List<BookSlots.Step> steps,
                                   Player player, int booksNeeded, long delayMs) {
        new Thread(() -> {
            notifyPlayer( Component.literal("Stand still while writing books...").withStyle(ChatFormatting.GOLD));

            for (int bookIndex = 0; bookIndex < booksNeeded; bookIndex++) {
                int startPage = bookIndex * BookLayout.MAX_PAGES_PER_BOOK;
                int endPage = Math.min(startPage + BookLayout.MAX_PAGES_PER_BOOK, pages.length);

                String bookTitle = BookLayout.formatBookTitle(title, bookIndex, booksNeeded);
                List<String> bookPages = BookLayout.extractPages(pages, startPage, endPage);

                BookSlots.Step step = steps.get(bookIndex);
                if (step.needsSwap()) {
                    moveIntoHotbar(player, step);
                }
                sendBookPacket(step.writeSlot(), bookPages, bookTitle);

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

    private static Set<Integer> findEmptyHotbarSlots(Player player) {
        Set<Integer> slots = new HashSet<>();
        for (int i = 0; i < BookSlots.HOTBAR_SIZE; i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                slots.add(i);
            }
        }
        return slots;
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

    /**
     * Swaps a book from the main inventory into the hotbar slot it is written in, and makes sure that worked.
     * The server can ignore a swap that is sent while it is still syncing the inventory after the previous
     * book (the packet then carries an outdated state), so the swap is checked and repeated if needed.
     */
    private static void moveIntoHotbar(Player player, BookSlots.Step step) {
        // The previous book in this slot must be signed first, or the swap would move it out unsigned
        waitUntil(() -> !isWritableBook(player, step.writeSlot()));

        for (int attempt = 0; attempt < SWAP_ATTEMPTS; attempt++) {
            sleep(SETTLE_MS);
            swapIntoHotbar(player, step.bookSlot(), step.writeSlot());
            sleep(SETTLE_MS);

            if (isWritableBook(player, step.writeSlot())) {
                return;
            }
        }
    }

    private static boolean isWritableBook(Player player, int slot) {
        return Minecraft.getInstance().submit(() -> player.getInventory().getItem(slot).is(Items.WRITABLE_BOOK)).join();
    }

    private static void waitUntil(BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + SIGN_TIMEOUT_MS;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            sleep(SIGN_POLL_MS);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The server only edits books in the hotbar and offhand, so a book in the main inventory is
     * swapped with a hotbar slot first. This is queued before the book packet, so the server sees it first.
     */
    private static void swapIntoHotbar(Player player, int inventorySlot, int hotbarSlot) {
        var client = Minecraft.getInstance();
        if (client.gameMode == null) return;

        // Slots 9 to 35 of the inventory have the same numbers in the inventory menu
        client.execute(() -> client.gameMode.handleInventoryMouseClick(
            player.inventoryMenu.containerId, inventorySlot, hotbarSlot, ClickType.SWAP, player
        ));
    }

    private static void sendBookPacket(int slot, List<String> pages, String title) {
        var client = Minecraft.getInstance();
        if (client.getConnection() == null) return;

        ServerboundEditBookPacket packet = new ServerboundEditBookPacket(slot, pages, Optional.of(title));
        client.execute(() -> client.getConnection().send(packet));
    }

    private static void notifyPlayer( Component message) {
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setOverlayMessage(message, false));
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

