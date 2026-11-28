package name.insearchof;

import name.insearchof.command.CommandHandler;
import net.fabricmc.api.ClientModInitializer;

public class BookWriterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CommandHandler.register();
    }
}