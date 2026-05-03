package name.insearchof.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Player;

public class BookWriterEvents {
    public static final Event<WriteBook> WRITE_BOOK = EventFactory.createArrayBacked(
            WriteBook.class,
            listeners -> (player, title, content) -> {
                for (WriteBook listener : listeners) {
                    listener.onWriteBook(player, title, content);
                }
            }
    );

    @FunctionalInterface
    public interface WriteBook {
        void onWriteBook(Player player, String title, String content);
    }
}