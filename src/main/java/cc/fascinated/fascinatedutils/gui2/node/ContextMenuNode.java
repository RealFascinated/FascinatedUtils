package cc.fascinated.fascinatedutils.gui2.node;

import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ContextMenuNode extends PositionedNode {
    private static final int MENU_PADDING = 4;
    private static final int MENU_CORNER_RADIUS = 4;
    private static final int BUTTON_GAP = 1;
    private static final int MIN_MENU_WIDTH = 100;
    private static final int SEPARATOR_HEIGHT = 5;
    private static final int SEPARATOR_LINE_HEIGHT = 1;

    private sealed interface RowEntry permits ButtonEntry, SeparatorEntry {}
    private record ButtonEntry(ButtonNode button) implements RowEntry {}
    private record SeparatorEntry() implements RowEntry {}

    private final List<ButtonNode> buttons = new ArrayList<>();
    private final List<RowEntry> rows = new ArrayList<>();
    private int[] separatorYPositions = new int[0];
    private int preferredPositionX;
    private int preferredPositionY;
    private int menuPositionX;
    private int menuPositionY;
    private int menuWidth;
    private int menuHeight;
    private int buttonHeight = 20;
    private Runnable onClose = () -> {};
    private boolean closeOnButtonPress = true;

    public ContextMenuNode() {
        full();
    }

    public ContextMenuNode setPreferredPosition(float preferredPositionX, float preferredPositionY) {
        this.preferredPositionX = Math.round(preferredPositionX);
        this.preferredPositionY = Math.round(preferredPositionY);
        return this;
    }

    public ContextMenuNode setButtonHeight(float buttonHeight) {
        this.buttonHeight = Math.max(14, Math.round(buttonHeight));
        return this;
    }

    public ContextMenuNode setOnClose(Runnable onClose) {
        this.onClose = onClose == null ? () -> {} : onClose;
        return this;
    }

    public ContextMenuNode setCloseOnButtonPress(boolean closeOnButtonPress) {
        this.closeOnButtonPress = closeOnButtonPress;
        return this;
    }

    public ContextMenuNode setItems(List<Item> items) {
        clearChildren();
        buttons.clear();
        rows.clear();
        if (items == null || items.isEmpty()) {
            return this;
        }
        for (Item item : items) {
            if (item == null) {
                continue;
            }
            if (item.isSeparator()) {
                rows.add(new SeparatorEntry());
                continue;
            }
            ButtonNode button = new ButtonNode(item.label())
                    .setGhost(true)
                    .setLeftAlignLabel(true)
                    .setRightIcon(item.icon())
                    .setLabelColorArgb(item.labelColor() != null ? item.labelColor().apply(UiThemeRepository.get()) : null)
                    .setOnPress(() -> {
                        item.onPress().run();
                        if (closeOnButtonPress) {
                            onClose.run();
                        }
                    });
            buttons.add(button);
            rows.add(new ButtonEntry(button));
            addChild(button);
        }
        return this;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        if (button != 0) {
            return false;
        }
        if (!isInMenu(pointerX, pointerY)) {
            onClose.run();
            return true;
        }
        return true;
    }

    @Override
    public void layout(RenderFrame renderFrame, int positionX, int positionY, int width, int height) {
        bounds().set(positionX, positionY, width, height);

        int widestButton = MIN_MENU_WIDTH - (MENU_PADDING * 2);
        for (ButtonNode button : buttons) {
            widestButton = Math.max(widestButton, (int) Math.ceil(measureButtonWidth(renderFrame, button)));
        }

        menuWidth = widestButton + (MENU_PADDING * 2);
        int contentHeight = 0;
        boolean firstRow = true;
        for (RowEntry row : rows) {
            if (!firstRow) {
                contentHeight += BUTTON_GAP;
            }
            contentHeight += row instanceof SeparatorEntry ? SEPARATOR_HEIGHT : buttonHeight;
            firstRow = false;
        }
        menuHeight = contentHeight + (MENU_PADDING * 2);

        int maxMenuPositionX = positionX + Math.max(0, width - menuWidth);
        int maxMenuPositionY = positionY + Math.max(0, height - menuHeight);
        menuPositionX = Math.min(Math.max(preferredPositionX, positionX), maxMenuPositionX);
        menuPositionY = Math.min(Math.max(preferredPositionY, positionY), maxMenuPositionY);

        int rowY = menuPositionY + MENU_PADDING;
        int buttonWidth = Math.max(0, menuWidth - (MENU_PADDING * 2));
        List<Integer> separatorYList = new ArrayList<>();
        boolean firstLayoutRow = true;
        for (RowEntry row : rows) {
            if (!firstLayoutRow) {
                rowY += BUTTON_GAP;
            }
            if (row instanceof ButtonEntry buttonEntry) {
                ButtonNode button = buttonEntry.button();
                button.width(buttonWidth);
                button.height(buttonHeight);
                button.layout(renderFrame, menuPositionX + MENU_PADDING, rowY, buttonWidth, buttonHeight);
                rowY += buttonHeight;
            } else {
                separatorYList.add(rowY + (SEPARATOR_HEIGHT - SEPARATOR_LINE_HEIGHT) / 2);
                rowY += SEPARATOR_HEIGHT;
            }
            firstLayoutRow = false;
        }
        separatorYPositions = new int[separatorYList.size()];
        for (int separatorIndex = 0; separatorIndex < separatorYList.size(); separatorIndex++) {
            separatorYPositions[separatorIndex] = separatorYList.get(separatorIndex);
        }
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        if (rows.isEmpty()) {
            return;
        }
        renderFrame.drawRoundedRectFrame(menuPositionX, menuPositionY, menuWidth, menuHeight, MENU_CORNER_RADIUS, renderFrame.theme().panelBorder(), renderFrame.theme().panelFill(), 1);
        int separatorX = menuPositionX + MENU_PADDING;
        int separatorWidth = menuWidth - (MENU_PADDING * 2);
        for (int separatorY : separatorYPositions) {
            renderFrame.drawRect(separatorX, separatorY, separatorWidth, SEPARATOR_LINE_HEIGHT, renderFrame.theme().divider());
        }
    }

    private float measureButtonWidth(RenderFrame renderFrame, ButtonNode button) {
        return button.minimumWidth(renderFrame);
    }

    private boolean isInMenu(float pointerX, float pointerY) {
        return pointerX >= menuPositionX
                && pointerX <= menuPositionX + menuWidth
                && pointerY >= menuPositionY
                && pointerY <= menuPositionY + menuHeight;
    }

    public record Item(String label, Function<UiTheme, Integer> labelColor, Identifier icon, Runnable onPress) {
        private static final Item SEPARATOR = new Item("", null, null, () -> {});

        public static Item separator() {
            return SEPARATOR;
        }

        public boolean isSeparator() {
            return this == SEPARATOR;
        }

        public Item(String label, Function<UiTheme, Integer> labelColor, Runnable onPress) {
            this(label, labelColor, null, onPress);
        }

        public Item(String label, Identifier icon, Runnable onPress) {
            this(label, null, icon, onPress);
        }

        public Item(String label, Runnable onPress) {
            this(label, null, null, onPress);
        }

        public Item {
            label = label == null ? "" : label;
            onPress = onPress == null ? () -> {} : onPress;
        }
    }
}