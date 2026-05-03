package name.insearchof;

import name.insearchof.command.CommandHandler;
import name.insearchof.event.BookWriterEventHandler;
import net.fabricmc.api.ClientModInitializer;

public class BookWriterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CommandHandler.register();
        BookWriterEventHandler.register();
    }
}