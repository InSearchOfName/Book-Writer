package name.insearchof.command;

import name.insearchof.UI.BookWriterUI;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;

public class CommandHandler {
    private static final String WRITE_COMMAND = ".write";

    public static void register() {
        ClientSendMessageEvents.ALLOW_CHAT.register(CommandHandler::handleMessage);
    }

    private static boolean handleMessage(String message) {
        if (!message.equalsIgnoreCase(WRITE_COMMAND)) {
            return true;
        }

        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new BookWriterUI()));
        return false;
    }
}
