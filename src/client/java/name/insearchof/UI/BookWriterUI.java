package name.insearchof.UI;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.*;
import name.insearchof.event.BookWriterEvents;
import name.insearchof.util.BookLayout;
import name.insearchof.util.BookWriterConfig;
import name.insearchof.util.InventoryUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;


public class BookWriterUI extends BaseOwoScreen<FlowLayout> {
    private static final int TITLE_MAX_LENGTH = 128;
    private static final int TEXT_MAX_LENGTH = 1000000;

    private static String storedText = "";
    private static String storedTitle = "Book";

    private TextAreaComponent titleField;
    private TextAreaComponent textField;
    private LabelComponent charCountLabel;

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
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
            Components.label(Component.literal("Book Title:"))
                .margins(Insets.bottom(3))
        );

        titleField = Components.textArea(Sizing.fill(100), Sizing.fixed(20));
        titleField.maxLines(1);
        titleField.setCharacterLimit(TITLE_MAX_LENGTH);
        titleField.setValue(storedTitle);
        rootComponent.child(titleField.margins(Insets.bottom(8)));

        // Content section
        rootComponent.child(
            Components.label(Component.literal("Content:"))
                .margins(Insets.bottom(3))
        );

        textField = Components.textArea(Sizing.fill(100), Sizing.fill(35));
        textField.setCharacterLimit(TEXT_MAX_LENGTH);
        textField.setValue(storedText);
        rootComponent.child(textField.margins(Insets.bottom(6)));

        // Character count
        charCountLabel = Components.label(Component.literal("Characters: 0"));
        charCountLabel.margins(Insets.bottom(4));
        rootComponent.child(charCountLabel);

        // Delay between books
        DiscreteSliderComponent delaySlider = Components.discreteSlider(
            Sizing.fixed(220), BookWriterConfig.MIN_DELAY_SECONDS, BookWriterConfig.MAX_DELAY_SECONDS
        );
        delaySlider.snap(true).decimalPlaces(1);
        delaySlider.setFromDiscreteValue(BookWriterConfig.getDelaySeconds());
        delaySlider.message(value -> Component.literal("Delay between books: " + value + " seconds"));
        delaySlider.onChanged().subscribe(BookWriterConfig::setDelaySeconds);

        FlowLayout delayContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        delayContainer.horizontalAlignment(HorizontalAlignment.CENTER);
        delayContainer.margins(Insets.bottom(6));
        delayContainer.child(delaySlider.sizing(Sizing.fixed(220), Sizing.fixed(20)));
        rootComponent.child(delayContainer);

        // Button container
        FlowLayout buttonContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        buttonContainer.horizontalAlignment(HorizontalAlignment.CENTER);
        buttonContainer.gap(10);

        buttonContainer.child(
            Components.button(Component.literal("Write to Book"), button -> onWriteClick())
                        .sizing(Sizing.fixed(100), Sizing.fixed(20))
        );

        buttonContainer.child(
            Components.button(Component.literal("Calculate"), button -> onCalculateClick())
                        .sizing(Sizing.fixed(100), Sizing.fixed(20))
        );

        buttonContainer.child(
            Components.button(Component.literal("Clear"), button -> onClearClick())
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

        int booksNeeded = BookLayout.calculateBooksNeededForChars(text.length());
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
        int totalPages = BookLayout.calculateTotalPages(text.length());
        int booksNeeded = BookLayout.calculateBooksNeededForChars(text.length());

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

    private Component formatCalculateMessage(int booksNeeded, int booksAvailable, int totalPages) {
        if (booksAvailable < booksNeeded) {
            return Component
                    .literal(String.format("Not enough books! Need: %d, Have: %d", booksNeeded, booksAvailable))
                    .withStyle(ChatFormatting.RED);
        }
        double seconds = BookLayout.estimateSeconds(booksNeeded, BookWriterConfig.getDelaySeconds());
        return Component
                .literal(String.format("Books Needed: %d (%d pages), about %.0f seconds", booksNeeded, totalPages, seconds))
                .withStyle(ChatFormatting.GREEN);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}