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
import net.minecraft.text.Text;
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
                UIComponents.label(Text.literal("Book Title:"))
                .margins(Insets.bottom(3))
        );

        titleField = UIComponents.textArea(Sizing.fill(100), Sizing.fixed(20));
        titleField.maxLines(1);
        titleField.setMaxLength(TITLE_MAX_LENGTH);
        titleField.text(storedTitle);
        rootComponent.child(titleField.margins(Insets.bottom(8)));

        // Content section
        rootComponent.child(
                UIComponents.label(Text.literal("Content:"))
                .margins(Insets.bottom(3))
        );

        textField = UIComponents.textArea(Sizing.fill(100), Sizing.fill(55));
        textField.setMaxLength(TEXT_MAX_LENGTH);
        textField.text(storedText);
        rootComponent.child(textField.margins(Insets.bottom(8)));

        // Character count
        charCountLabel = UIComponents.label(Text.literal("Characters: 0"));
        charCountLabel.margins(Insets.bottom(6));
        rootComponent.child(charCountLabel);

        // Button container
        FlowLayout buttonContainer = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonContainer.horizontalAlignment(HorizontalAlignment.CENTER);
        buttonContainer.gap(10);

        buttonContainer.child(
                UIComponents.button(Text.literal("Write to Book"), button -> onWriteClick())
                        .sizing(Sizing.fixed(100), Sizing.fixed(20))
        );

        buttonContainer.child(
                UIComponents.button(Text.literal("Calculate"), button -> onCalculateClick())
                        .sizing(Sizing.fixed(100), Sizing.fixed(20))
        );

        buttonContainer.child(
                UIComponents.button(Text.literal("Clear"), button -> onClearClick())
                        .sizing(Sizing.fixed(100), Sizing.fixed(20))
        );

        rootComponent.child(buttonContainer);
    }

    @Override
    public void tick() {
        super.tick();
        // Update character count display
        if (charCountLabel != null && textField != null) {
            charCountLabel.text(Text.literal("Characters: " + textField.getText().length()));
        }
    }

    private void onWriteClick() {
        String text = textField.getText();
        String title = titleField.getText();

        if (text.isEmpty()) {
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("§cNo text to write!"), true);
            }
            this.close();
            return;
        }

        if (this.client == null || this.client.player == null) {
            return;
        }

        int booksNeeded = calculateBooksNeeded(text.length());
        int booksAvailable = InventoryUtils.countWritableBooks(this.client.player);

        if (booksAvailable < booksNeeded) {
            String message = String.format("§c§lNot enough books! §r§7Need: %d, Have: %d", booksNeeded, booksAvailable);
            this.client.player.sendMessage(Text.literal(message), true);
            this.close();
            return;
        }

        BookWriterEvents.WRITE_BOOK.invoker().onWriteBook(this.client.player, title, text);
        storedText = text;
        storedTitle = title;
        this.close();
    }

    private void onCalculateClick() {
        String text = textField.getText();
        int totalPages = calculateTotalPages(text.length());
        int booksNeeded = calculateBooksNeeded(text.length());

        if (this.client == null || this.client.player == null) {
            return;
        }

        int booksAvailable = InventoryUtils.countWritableBooks(this.client.player);
        String message = formatCalculateMessage(booksNeeded, booksAvailable, totalPages);
        this.client.player.sendMessage(Text.literal(message), true);
        this.close();
    }

    private void onClearClick() {
        textField.text("");
        titleField.text("Book");
    }

    private int calculateTotalPages(int charCount) {
        int pages = (int) Math.ceil((double) charCount / CHARS_PER_PAGE);
        return Math.max(pages, 1);
    }

    private int calculateBooksNeeded(int charCount) {
        int totalPages = calculateTotalPages(charCount);
        return (int) Math.ceil((double) totalPages / MAX_PAGES_PER_BOOK);
    }

    private String formatCalculateMessage(int booksNeeded, int booksAvailable, int totalPages) {
        if (booksAvailable < booksNeeded) {
            return String.format("§c§lNot enough books! §r§7Need: %d, Have: %d", booksNeeded, booksAvailable);
        }
        return String.format("§a§lBooks Needed: %d §r§7(%d pages)", booksNeeded, totalPages);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}