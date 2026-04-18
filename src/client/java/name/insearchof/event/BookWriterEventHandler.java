package name.insearchof.event;

import name.insearchof.util.BookWriter;
import net.minecraft.world.entity.player.Player;

public class BookWriterEventHandler {
    private BookWriterEventHandler() {
        // Utility class
    }

    public static void register() {
        BookWriterEvents.WRITE_BOOK.register(BookWriterEventHandler::handleWriteBook);
    }

    private static void handleWriteBook(Player player, String title, String content) {
        BookWriter.writeAndSignBook(title, content, player);
    }
}
