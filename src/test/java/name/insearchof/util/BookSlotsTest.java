package name.insearchof.util;

import name.insearchof.util.BookSlots.Step;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookSlotsTest {
    @Test
    void onlyTheHotbarAndOffhandAreWritable() {
        for (int slot = 0; slot <= 8; slot++) {
            assertTrue(BookSlots.isWritableSlot(slot), "slot " + slot);
        }
        for (int slot = 9; slot <= 39; slot++) {
            assertFalse(BookSlots.isWritableSlot(slot), "slot " + slot);
        }
        assertTrue(BookSlots.isWritableSlot(40));
        assertFalse(BookSlots.isWritableSlot(-1));
    }

    @Test
    void noBooksGiveNoSteps() {
        assertEquals(List.of(), BookSlots.plan(List.of(), Set.of(), 0));
    }

    @Test
    void booksInTheHotbarAreWrittenInPlace() {
        List<Step> steps = BookSlots.plan(List.of(0, 3, 8), Set.of(1, 2), 0);

        assertEquals(List.of(new Step(0, 0), new Step(3, 3), new Step(8, 8)), steps);
        assertFalse(steps.get(0).needsSwap());
    }

    @Test
    void theOffhandIsWrittenInPlace() {
        assertEquals(List.of(new Step(40, 40)), BookSlots.plan(List.of(40), Set.of(), 0));
    }

    @Test
    void theTenthBookIsSwappedIntoTheHotbarSlotOfTheLastWrittenBook() {
        // nine books fill the hotbar, the tenth is in the main inventory
        List<Step> steps = BookSlots.plan(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), Set.of(), 0);

        assertEquals(10, steps.size());
        assertEquals(new Step(8, 8), steps.get(8));
        assertEquals(new Step(9, 8), steps.get(9));
        assertTrue(steps.get(9).needsSwap());
    }

    @Test
    void mainInventoryBooksAllGoThroughTheSameHotbarSlot() {
        List<Step> steps = BookSlots.plan(List.of(2, 10, 20, 35), Set.of(0, 1), 0);

        assertEquals(List.of(new Step(2, 2), new Step(10, 2), new Step(20, 2), new Step(35, 2)), steps);
    }

    @Test
    void withoutHotbarBooksTheFirstEmptyHotbarSlotIsUsed() {
        List<Step> steps = BookSlots.plan(List.of(12, 13), Set.of(6, 4, 7), 0);

        assertEquals(List.of(new Step(12, 4), new Step(13, 4)), steps);
    }

    @Test
    void withoutAnEmptyHotbarSlotTheSelectedSlotIsUsed() {
        List<Step> steps = BookSlots.plan(List.of(12), Set.of(), 5);

        assertEquals(List.of(new Step(12, 5)), steps);
    }

    @Test
    void offhandBooksComeBeforeTheMainInventoryBooks() {
        List<Step> steps = BookSlots.plan(List.of(10, 40), Set.of(0), 0);

        assertEquals(List.of(new Step(40, 40), new Step(10, 0)), steps);
    }

    @Test
    void armorSlotsAreIgnored() {
        assertEquals(List.of(), BookSlots.plan(List.of(36, 37, 38, 39), Set.of(0), 0));
    }
}
