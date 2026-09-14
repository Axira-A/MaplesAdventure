package dev.maplesadventure.client.message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared searchable/category browser for templates, tokens, modifiers and connectors. */
final class SearchableSelectionScreen<T> extends Screen {
    record SelectionItem<T>(T value, String categoryId, Component categoryLabel,
                            Component label, Component description, List<String> searchTerms) {
        SelectionItem {
            searchTerms = List.copyOf(searchTerms);
        }
        boolean matches(String query) {
            String normalized = query.toLowerCase(Locale.ROOT).trim();
            if (normalized.isEmpty()) return true;
            if (label.getString().toLowerCase(Locale.ROOT).contains(normalized)) return true;
            for (String term : searchTerms) if (term.toLowerCase(Locale.ROOT).contains(normalized)) return true;
            return false;
        }
    }

    private final Screen parent;
    private final List<SelectionItem<T>> source;
    private final T current;
    private final Consumer<T> onSelected;
    private EditBox search;
    private SelectionList list;
    private String category = "all";
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    SearchableSelectionScreen(Screen parent, Component title, List<SelectionItem<T>> source,
                              T current, Consumer<T> onSelected) {
        super(title);
        this.parent = parent;
        this.source = List.copyOf(source);
        this.current = current;
        this.onSelected = onSelected;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(600, Math.max(360, (int)(width * 0.66F)));
        panelHeight = Math.min(430, Math.max(270, (int)(height * 0.76F)));
        panelWidth = Math.min(panelWidth, width - 24);
        panelHeight = Math.min(panelHeight, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        search = addRenderableWidget(new EditBox(font, panelLeft + 16, panelTop + 30,
                panelWidth - 32, 18, Component.translatable("screen.maplesadventure.message.search")));
        search.setHint(Component.translatable("screen.maplesadventure.message.search_hint"));
        search.setMaxLength(64);
        search.setResponder(ignored -> refreshEntries());

        Map<String, Component> categories = new LinkedHashMap<>();
        categories.put("all", Component.translatable("screen.maplesadventure.message.category.all"));
        for (SelectionItem<T> item : source) categories.putIfAbsent(item.categoryId(), item.categoryLabel());
        int tabsY = panelTop + 54;
        int available = panelWidth - 32;
        int tabWidth = Math.max(48, Math.min(88, available / Math.max(1, categories.size())));
        int x = panelLeft + 16;
        for (Map.Entry<String, Component> entry : categories.entrySet()) {
            if (x + tabWidth > panelLeft + panelWidth - 16) break;
            String id = entry.getKey();
            Button button = Button.builder(entry.getValue(), ignored -> {
                category = id;
                refreshEntries();
            }).bounds(x, tabsY, tabWidth - 3, 18).build();
            addRenderableWidget(button);
            x += tabWidth;
        }

        int listTop = panelTop + 79;
        int listHeight = panelHeight - 116;
        list = addRenderableWidget(new SelectionList(minecraft, panelWidth - 32, listHeight, listTop, 30));
        list.setX(panelLeft + 16);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), ignored -> onClose())
                .bounds(panelLeft + panelWidth / 2 - 55, panelTop + panelHeight - 28, 110, 20).build());
        refreshEntries();
        setInitialFocus(search);
    }

    private void refreshEntries() {
        if (list == null || search == null) return;
        ArrayList<ChoiceEntry> entries = new ArrayList<>();
        ChoiceEntry currentEntry = null;
        for (SelectionItem<T> item : source) {
            if (!(category.equals("all") || category.equals(item.categoryId())) || !item.matches(search.getValue())) continue;
            ChoiceEntry entry = new ChoiceEntry(item);
            entries.add(entry);
            if (java.util.Objects.equals(item.value(), current)) currentEntry = entry;
        }
        list.replace(entries);
        if (currentEntry != null) list.setSelected(currentEntry);
    }

    private void choose(SelectionItem<T> item) {
        onSelected.accept(item.value());
        minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && list != null && list.getSelected() != null) {
            choose(list.getSelected().item);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xE0100D09);
        graphics.renderOutline(panelLeft, panelTop, panelWidth, panelHeight, 0xFF9E743C);
        graphics.drawCenteredString(font, title, width / 2, panelTop + 10, 0xFFE8CA8D);
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }

    private final class SelectionList extends ObjectSelectionList<ChoiceEntry> {
        SelectionList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
            super(minecraft, width, height, top, itemHeight);
            centerListVertically = false;
        }
        void replace(List<ChoiceEntry> entries) { replaceEntries(entries); setScrollAmount(0); }
        @Override public int getRowWidth() { return Math.max(80, getWidth() - 18); }
        @Override protected void renderListBackground(GuiGraphics graphics) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), 0xB00B0907);
        }
    }

    private final class ChoiceEntry extends ObjectSelectionList.Entry<ChoiceEntry> {
        private final SelectionItem<T> item;
        ChoiceEntry(SelectionItem<T> item) { this.item = item; }
        @Override public Component getNarration() { return item.label(); }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            SearchableSelectionScreen.this.list.setSelected(this);
            choose(item);
            return true;
        }
        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) graphics.fill(left, top, left + width, top + height, 0x604F3923);
            graphics.drawString(font, item.label(), left + 7, top + 4, 0xFFFFD58A, false);
            graphics.drawString(font, item.description(), left + 7, top + 16, 0xFF9F9587, false);
        }
    }
}
