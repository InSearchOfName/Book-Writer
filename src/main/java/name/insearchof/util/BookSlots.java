package name.insearchof.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Works out in which inventory slot each book is written.
 * The server only accepts book edits for the hotbar (slots 0 to 8) and the offhand (slot 40),
 * so books in the main inventory (slots 9 to 35) are swapped into a hotbar slot first.
 * This class does not use Minecraft, so it can be unit tested.
 */
public class BookSlots {
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_INVENTORY_END = 35;
    public static final int OFFHAND_SLOT = 40;

    private BookSlots() {
        // Utility class
    }

    /**
     * @param bookSlot  where the book is now
     * @param writeSlot the hotbar or offhand slot the book is written in; if it differs from
     *                  bookSlot, the book is swapped there first
     */
    public record Step(int bookSlot, int writeSlot) {
        public boolean needsSwap() {
            return bookSlot != writeSlot;
        }
    }

    /**
     * @return whether the server accepts book edits for this slot
     */
    public static boolean isWritableSlot(int slot) {
        return (slot >= 0 && slot < HOTBAR_SIZE) || slot == OFFHAND_SLOT;
    }

    /**
     * Plans the order in which the books are written: first the books that are already in the
     * hotbar or offhand, then the ones in the main inventory, which all go through one hotbar slot.
     *
     * @param bookSlots        the inventory slots that hold a writable book, in ascending order
     * @param emptyHotbarSlots the hotbar slots that hold no item
     * @param selectedSlot     the selected hotbar slot, used when no hotbar slot is empty
     */
    public static List<Step> plan(List<Integer> bookSlots, Set<Integer> emptyHotbarSlots, int selectedSlot) {
        List<Step> steps = new ArrayList<>();
        List<Integer> mainInventory = new ArrayList<>();
        int workSlot = -1;

        for (int slot : bookSlots) {
            if (isWritableSlot(slot)) {
                steps.add(new Step(slot, slot));
                if (slot < HOTBAR_SIZE) {
                    workSlot = slot;
                }
            } else if (slot >= HOTBAR_SIZE && slot <= MAIN_INVENTORY_END) {
                mainInventory.add(slot);
            }
        }

        if (mainInventory.isEmpty()) {
            return steps;
        }

        // A hotbar slot that already holds a written book is free again; otherwise use an empty one
        if (workSlot < 0) {
            workSlot = emptyHotbarSlots.stream().min(Integer::compare).orElse(selectedSlot);
        }

        for (int slot : mainInventory) {
            steps.add(new Step(slot, workSlot));
        }
        return steps;
    }
}
