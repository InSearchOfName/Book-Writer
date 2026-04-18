package name.insearchof.UI;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import name.insearchof.event.BookWriterEvents;
import name.insearchof.util.InventoryUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;


public class BookWriterUI extends BaseOwoScreen<FlowLayout> {
    private static final int CHARS_PER_PAGE = 256;
    private static final int MAX_PAGES_PER_BOOK = 100;
    private static final int TITLE_MAX_LENGTH = 128;
    private static final int TEXT_MAX_LENGTH = 1000000;

    private static String storedText = "";
    private static String storedTitle = "Book";

    private TextAreaComponent titleField;
    private TextAreaComponent textField;
    private LabelComponent charCountLabel;

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.sizing(Sizing.fill(100), Sizing.fill(100));
        rootComponent
            .surface(Surface.VANILLA_TRANSLUCENT)
            .horizontalAlignment(HorizontalAlignment.LEFT)
            .verticalAlignment(VerticalAlignment.TOP)
            .padding(Insets.of(8));

        // Title section
        rootComponent.child(
            UIComponents.label(Component.literal("Book Title:"))
                .margins(Insets.bottom(3))
        );

        titleField = UIComponents.textArea(Sizing.fill(100), Sizing.fixed(20));
        titleField.maxLines(1);
        titleField.setCharacterLimit(TITLE_MAX_LENGTH);
        titleField.setValue(storedTitle);
        rootComponent.child(titleField.margins(Insets.bottom(8)));

        // Content section
        rootComponent.child(
            UIComponents.label(Component.literal("Content:"))
                .margins(Insets.bottom(3))
        );

        textField = UIComponents.textArea(Sizing.fill(100), Sizing.fill(55));
        textField.setCharacterLimit(TEXT_MAX_LENGTH);
        textField.setValue(storedText);
        rootComponent.child(textField.margins(Insets.bottom(8)));

        // Character count
        charCountLabel = UIComponents.label(Component.literal("Characters: 0"));
        charCountLabel.margins(Insets.bottom(6));
        rootComponent.child(charCountLabel);

        // Button container
        FlowLayout buttonContainer = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonContainer.horizontalAlignment(HorizontalAlignment.CENTER);
        buttonContainer.gap(10);

        buttonContainer.child(
            UIComponents.button(Component.literal("Write to Book"), button -> onWriteClick())
                        .sizing(Sizing.fixed(100), Sizing.fixed(20))
        );

        buttonContainer.child(
            UIComponents.button(Component.literal("Calculate"), button -> onCalculateClick())
                        .sizing(Sizing.fixed(100), Sizing.fixed(20))
        );

        buttonContainer.child(
            UIComponents.button(Component.literal("Clear"), button -> onClearClick())
                        .sizing(Sizing.fixed(100), Sizing.fixed(20))
        );

        rootComponent.child(buttonContainer);
    }

    @Override
    public void tick() {
        super.tick();
        // Update character count display
        if (charCountLabel != null && textField != null) {
            charCountLabel.text(Component.literal("Characters: " + textField.getValue().length()));
        }
    }

    private void onWriteClick() {
        String text = textField.getValue();
        String title = titleField.getValue();

        if (text.isEmpty()) {
            var client = Minecraft.getInstance();
            if (client.player != null) {
                client.gui.setOverlayMessage(Component.literal("No text to write!").withStyle(ChatFormatting.RED), false);
            }
            Minecraft.getInstance().setScreen(null);
            return;
        }

        var client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        int booksNeeded = calculateBooksNeeded(text.length());
        int booksAvailable = InventoryUtils.countWritableBooks(client.player);

        if (booksAvailable < booksNeeded) {
            Component message = Component
                    .literal(String.format("Not enough books! Need: %d, Have: %d", booksNeeded, booksAvailable))
                    .withStyle(ChatFormatting.RED);
                client.gui.setOverlayMessage(message, false);
            Minecraft.getInstance().setScreen(null);
            return;
        }

        BookWriterEvents.WRITE_BOOK.invoker().onWriteBook(client.player, title, text);
        storedText = text;
        storedTitle = title;
        Minecraft.getInstance().setScreen(null);
    }

    private void onCalculateClick() {
        String text = textField.getValue();
        int totalPages = calculateTotalPages(text.length());
        int booksNeeded = calculateBooksNeeded(text.length());

        var client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        int booksAvailable = InventoryUtils.countWritableBooks(client.player);
        Component message = formatCalculateMessage(booksNeeded, booksAvailable, totalPages);
        client.gui.setOverlayMessage(message, false);
        Minecraft.getInstance().setScreen(null);
    }

    private void onClearClick() {
        textField.setValue("");
        titleField.setValue("Book");
    }

    private int calculateTotalPages(int charCount) {
        int pages = (int) Math.ceil((double) charCount / CHARS_PER_PAGE);
        return Math.max(pages, 1);
    }

    private int calculateBooksNeeded(int charCount) {
        int totalPages = calculateTotalPages(charCount);
        return (int) Math.ceil((double) totalPages / MAX_PAGES_PER_BOOK);
    }

    private Component formatCalculateMessage(int booksNeeded, int booksAvailable, int totalPages) {
        if (booksAvailable < booksNeeded) {
            return Component
                    .literal(String.format("Not enough books! Need: %d, Have: %d", booksNeeded, booksAvailable))
                    .withStyle(ChatFormatting.RED);
        }
        return Component
                .literal(String.format("Books Needed: %d (%d pages)", booksNeeded, totalPages))
                .withStyle(ChatFormatting.GREEN);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}