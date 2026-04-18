package name.insearchof.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class InventoryUtils {
    private InventoryUtils() {
        // Utility class
    }

    public static int countWritableBooks(Player player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() == Items.WRITABLE_BOOK) {
                count++;
            }
        }
        return count;
    }
}
