package io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.item;

import java.util.List;

import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderStorageScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.TraderStorageClientTab;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollBarWidget;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollBarWidget.IScrollable;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollListener;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollListener.IScrollListener;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.client.util.IconAndButtonUtil;
import io.github.lightman314.lightmanscurrency.client.util.ItemRenderUtil;
import io.github.lightman314.lightmanscurrency.common.menu.traderstorage.item.ItemStorageTab;
import io.github.lightman314.lightmanscurrency.common.traders.item.ItemTraderData;
import io.github.lightman314.lightmanscurrency.common.traders.item.storage.TraderItemStorage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class ItemStorageClientTab extends TraderStorageClientTab<ItemStorageTab> implements IScrollListener, IScrollable{

    private static final int X_OFFSET = 13;
    private static final int Y_OFFSET = 17;
    private static final int COLUMNS = 8;
    private static final int ROWS = 5;

    public ItemStorageClientTab(TraderStorageScreen screen, ItemStorageTab commonTab) { super(screen, commonTab); }

    int scroll = 0;

    ScrollBarWidget scrollBar;

    @Override
    public @NotNull IconData getIcon() { return IconAndButtonUtil.ICON_STORAGE; }

    @Override
    public MutableText getTooltip() { return Text.translatable("tooltip.lightmanscurrency.trader.storage"); }

    @Override
    public boolean tabButtonVisible() { return true; }

    @Override
    public boolean blockInventoryClosing() { return false; }

    @Override
    public void onOpen() {

        this.scrollBar = this.screen.addRenderableTabWidget(new ScrollBarWidget(this.screen.getGuiLeft() + X_OFFSET + (18 * COLUMNS), this.screen.getGuiTop() + Y_OFFSET, ROWS * 18, this));

        this.screen.addTabListener(new ScrollListener(this.screen.getGuiLeft(), this.screen.getGuiTop(), this.screen.getImageWidth(), 118, this));

        this.screen.addRenderableTabWidget(IconAndButtonUtil.quickInsertButton(this.screen.getGuiLeft() + 22, this.screen.getGuiTop() + Y_OFFSET + 18 * ROWS + 8, b -> this.commonTab.quickTransfer(0)));
        this.screen.addRenderableTabWidget(IconAndButtonUtil.quickExtractButton(this.screen.getGuiLeft() + 34, this.screen.getGuiTop() + Y_OFFSET + 18 * ROWS + 8, b -> this.commonTab.quickTransfer(1)));

    }

    @Override
    public void renderBG(DrawContext gui, int mouseX, int mouseY, float partialTicks) {

        gui.drawText(this.font, Text.translatable("gui.lightmanscurrency.storage"), this.screen.getGuiLeft() + 8, this.screen.getGuiTop() + 6, 0x404040, false);

        this.scrollBar.beforeWidgetRender(mouseY);

        if(this.menu.getTrader() instanceof ItemTraderData trader)
        {
            //Validate the scroll
            this.validateScroll();
            //Render each display slot
            int index = this.scroll * COLUMNS;
            TraderItemStorage storage = trader.getStorage();
            int hoverSlot = this.isMouseOverSlot(mouseX, mouseY) + (this.scroll * COLUMNS);
            for(int y = 0; y < ROWS; ++y)
            {
                int yPos = this.screen.getGuiTop() + Y_OFFSET + y * 18;
                for(int x = 0; x < COLUMNS; ++x)
                {
                    //Get the slot position
                    int xPos = this.screen.getGuiLeft() + X_OFFSET + x * 18;
                    //Render the slot background
                    gui.setShaderColor(1f, 1f, 1f, 1f);
                    gui.drawTexture(TraderScreen.GUI_TEXTURE, xPos, yPos, TraderScreen.WIDTH, 0, 18, 18);
                    //Render the slots item
                    if(index < storage.getSlotCount())
                        ItemRenderUtil.drawItemStack(gui, this.font, storage.getContents().get(index), xPos + 1, yPos + 1, this.getCountText(storage.getContents().get(index)));
                    if(index == hoverSlot)
                        HandledScreen.drawSlotHighlight(gui, xPos + 1, yPos + 1, 0);
                    index++;
                }
            }

            //Render the slot bg for the upgrade slots
            gui.setShaderColor(1f, 1f, 1f, 1f);
            for(Slot slot : this.commonTab.getSlots())
            {
                gui.drawTexture(TraderScreen.GUI_TEXTURE, this.screen.getGuiLeft() + slot.x - 1, this.screen.getGuiTop() + slot.y - 1, TraderScreen.WIDTH, 0, 18, 18);
            }
        }

    }

    private String getCountText(ItemStack stack) {
        int count = stack.getCount();
        if(count <= 1)
            return null;
        if(count >= 1000)
        {
            String countText = String.valueOf(count / 1000);
            if((count % 1000) / 100 > 0)
                countText += "." + ((count % 1000) / 100);
            return countText + "k";
        }
        return String.valueOf(count);
    }

    @Override
    public void renderTooltips(DrawContext gui, int mouseX, int mouseY) {

        if(this.menu.getTrader() instanceof ItemTraderData && this.menu.getCursorStack().isEmpty())
        {
            int hoveredSlot = this.isMouseOverSlot(mouseX, mouseY);
            if(hoveredSlot >= 0)
            {
                hoveredSlot += scroll * COLUMNS;
                TraderItemStorage storage = ((ItemTraderData)this.menu.getTrader()).getStorage();
                if(hoveredSlot < storage.getContents().size())
                {
                    ItemStack stack = storage.getContents().get(hoveredSlot);
                    List<Text> tooltip = ItemRenderUtil.getTooltipFromItem(stack);
                    tooltip.add(Text.translatable("tooltip.lightmanscurrency.itemstorage", stack.getCount()));
                    if(stack.getCount() >= 64)
                    {
                        if(stack.getCount() % 64 == 0)
                            tooltip.add(Text.translatable("tooltip.lightmanscurrency.itemstorage.stacks.single", stack.getCount() / 64));
                        else
                            tooltip.add(Text.translatable("tooltip.lightmanscurrency.itemstorage.stacks.multi", stack.getCount() / 64, stack.getCount() % 64));
                    }
                    gui.drawTooltip(this.font, tooltip, mouseX, mouseY);
                }
            }
        }
    }

    private void validateScroll() {
        if(this.scroll < 0)
            this.scroll = 0;
        if(this.scroll > this.getMaxScroll())
            this.scroll = this.getMaxScroll();
    }

    private int isMouseOverSlot(double mouseX, double mouseY) {

        int foundColumn = -1;
        int foundRow = -1;

        int leftEdge = this.screen.getGuiLeft() + X_OFFSET;
        int topEdge = this.screen.getGuiTop() + Y_OFFSET;
        for(int x = 0; x < COLUMNS && foundColumn < 0; ++x)
        {
            if(mouseX >= leftEdge + x * 18 && mouseX < leftEdge + (x * 18) + 18)
                foundColumn = x;
        }
        for(int y = 0; y < ROWS && foundRow < 0; ++y)
        {
            if(mouseY >= topEdge + y * 18 && mouseY < topEdge + (y * 18) + 18)
                foundRow = y;
        }
        if(foundColumn < 0 || foundRow < 0)
            return -1;
        return (foundRow * COLUMNS) + foundColumn;
    }

    private int totalStorageSlots() {
        if(this.menu.getTrader() instanceof ItemTraderData)
        {
            return ((ItemTraderData)this.menu.getTrader()).getStorage().getContents().size();
        }
        return 0;
    }

    private boolean canScrollDown() {
        return this.totalStorageSlots() - this.scroll * COLUMNS > ROWS * COLUMNS;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if(delta < 0)
        {
            if(this.canScrollDown())
                this.scroll++;
            else
                return false;
        }
        else if(delta > 0)
        {
            if(this.scroll > 0)
                scroll--;
            else
                return false;
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if(this.menu.getTrader() instanceof ItemTraderData)
        {
            int hoveredSlot = this.isMouseOverSlot(mouseX, mouseY);
            if(hoveredSlot >= 0)
            {
                hoveredSlot += this.scroll * COLUMNS;
                this.commonTab.clickedOnSlot(hoveredSlot, Screen.hasShiftDown(), button == 0);
                return true;
            }
        }
        this.scrollBar.onMouseClicked(mouseX, mouseY, button);
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrollBar.onMouseReleased(mouseX, mouseY, button);
        return false;
    }

    @Override
    public int currentScroll() { return this.scroll; }

    @Override
    public void setScroll(int newScroll) {
        this.scroll = newScroll;
        this.validateScroll();
    }

    @Override
    public int getMaxScroll() {
        return Math.max(((this.totalStorageSlots() - 1) / COLUMNS) - ROWS + 1, 0);
    }

}