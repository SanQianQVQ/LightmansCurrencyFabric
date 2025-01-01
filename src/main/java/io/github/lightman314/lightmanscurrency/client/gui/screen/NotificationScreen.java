package io.github.lightman314.lightmanscurrency.client.gui.screen;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.lightman314.lightmanscurrency.client.data.ClientNotificationData;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollBarWidget;
import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollBarWidget.IScrollable;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.notifications.MarkAsSeenButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.notifications.NotificationTabButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.tab.TabButton;
import io.github.lightman314.lightmanscurrency.client.util.ScreenUtil;
import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.notifications.Notification;
import io.github.lightman314.lightmanscurrency.common.notifications.NotificationCategory;
import io.github.lightman314.lightmanscurrency.common.notifications.NotificationData;
import io.github.lightman314.lightmanscurrency.network.server.messages.notifications.CMessageFlagNotificationsSeen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class NotificationScreen extends Screen implements IScrollable{

    public static void open() {
        if(RenderSystem.isOnRenderThread())
        {
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new NotificationScreen());
        }
        else
            ScreenUtil.safelyOpenScreen(new NotificationScreen());
    }

    public static final Identifier GUI_TEXTURE =  new Identifier(LightmansCurrency.MODID, "textures/gui/notifications.png");

    public final NotificationData getNotifications() { return ClientNotificationData.GetNotifications(); }

    public final int guiLeft() { return (this.width - this.xSize - TabButton.SIZE) / 2; }
    public final int guiTop() { return (this.height - this.ySize) / 2; }
    public final int xSize = 200;
    public final int ySize = 200;

    public final int TABS_PER_PAGE = 8;

    public final int NOTIFICATIONS_PER_PAGE = 8;
    public final int NOTIFICATION_HEIGHT = 22;

    List<NotificationTabButton> tabButtons;
    int tabScroll = 0;
    NotificationCategory selectedCategory = NotificationCategory.GENERAL;

    ScrollBarWidget notificationScroller = null;

    ButtonWidget buttonMarkAsSeen;

    int notificationScroll = 0;

    public NotificationScreen() { super(Text.empty()); }

    @Override
    public boolean shouldPause() { return false; }

    public List<NotificationCategory> getCategories() {
        List<NotificationCategory> categories = Lists.newArrayList(NotificationCategory.GENERAL);
        categories.addAll(this.getNotifications().getCategories().stream().filter(cat -> cat != NotificationCategory.GENERAL).toList());
        return categories;
    }

    public void reinit() {
        this.clearChildren();
        this.validateSelectedCategory();
        this.init();
    }

    @Override
    public void init() {

        this.tabButtons = new ArrayList<>();
        for(NotificationCategory cat : this.getCategories())
        {
            this.tabButtons.add(this.addDrawableChild(new NotificationTabButton(this::SelectTab, this.textRenderer, this::getNotifications, cat)));
        }
        this.positionTabButtons();

        this.notificationScroller = this.addDrawableChild(new ScrollBarWidget(this.guiLeft() + TabButton.SIZE + this.xSize - 15, this.guiTop() + 15, this.NOTIFICATIONS_PER_PAGE * this.NOTIFICATION_HEIGHT, this));

        this.buttonMarkAsSeen = this.addDrawableChild(new MarkAsSeenButton(this.guiLeft() + this.xSize  + TabButton.SIZE - 15, this.guiTop() + 4, Text.translatable("gui.button.notifications.mark_read"), this::markAsRead));

        this.tick();

    }

    private void validateSelectedCategory() {
        List<NotificationCategory> categories = this.getCategories();
        boolean categoryFound = false;
        for(int i = 0; i < categories.size() && !categoryFound; ++i)
        {
            if(categories.get(i).matches(this.selectedCategory))
                categoryFound = true;
        }
        if(!categoryFound || this.selectedCategory == null)
            this.selectedCategory = NotificationCategory.GENERAL;
    }

    private void positionTabButtons() {
        this.tabScroll = Math.min(this.tabScroll, this.getMaxTabScroll());
        int startIndex = this.tabScroll;
        int xPos = this.guiLeft();
        int yPos = this.guiTop();
        List<NotificationCategory> categories = this.getCategories();
        for(int i = 0; i < this.tabButtons.size(); ++i)
        {
            TabButton tab = this.tabButtons.get(i);
            if(i >= startIndex && i < startIndex + TABS_PER_PAGE)
            {
                tab.visible = true;
                tab.reposition(xPos, yPos, 3);
                if(i < categories.size()) //Use match code, as some categories are generated on get, and a new instance may have been generated due to reloading, etc.
                    tab.active = !categories.get(i).matches(this.selectedCategory);
                else
                    tab.active = true;
                yPos += TabButton.SIZE;
            }
            else
                tab.visible = false;
        }
    }

    @Override
    public void tick() {
        this.buttonMarkAsSeen.active = this.getNotifications().unseenNotification(this.selectedCategory);
    }

    @Override
    public void render(DrawContext gui, int mouseX, int mouseY, float partialTicks) {

        this.renderBackground(gui);

        //Render the background
        gui.setShaderColor(1f, 1f, 1f, 1f);
        int screenLeft = this.guiLeft() + TabButton.SIZE;
        gui.drawTexture(GUI_TEXTURE, screenLeft, this.guiTop(), 0, 0, this.xSize, this.ySize);

        this.notificationScroller.beforeWidgetRender(mouseY);

        //Render the current notifications
        this.notificationScroll = Math.min(this.notificationScroll, this.getMaxNotificationScroll());
        List<Notification> notifications = this.getNotifications().getNotifications(this.selectedCategory);
        Text tooltip = null;
        int index = this.notificationScroll;
        for(int y = 0; y < NOTIFICATIONS_PER_PAGE && index < notifications.size(); ++y)
        {
            Notification not = notifications.get(index++);
            int yPos = this.guiTop() + 15 + y * NOTIFICATION_HEIGHT;
            gui.setShaderColor(1f, 1f, 1f, 1f);

            int vPos = not.wasSeen() ? this.ySize : this.ySize + NOTIFICATION_HEIGHT;
            int textColor = not.wasSeen() ? 0xFFFFFF : 0x000000;

            gui.drawTexture(GUI_TEXTURE, screenLeft + 15, yPos, 0, vPos, 170, NOTIFICATION_HEIGHT);
            int textXPos = screenLeft + 17;
            int textWidth = 166;
            if(not.getCount() > 1)
            {
                //Render quantity text
                String countText = String.valueOf(not.getCount());
                int quantityWidth = this.textRenderer.getWidth(countText);
                gui.drawTexture(GUI_TEXTURE, screenLeft + 16 + quantityWidth, yPos, 170, vPos, 3, NOTIFICATION_HEIGHT);

                gui.drawText(this.textRenderer, countText, textXPos, yPos + (NOTIFICATION_HEIGHT / 2) - (this.textRenderer.fontHeight / 2), textColor, false);

                textXPos += quantityWidth + 2;
                textWidth -= quantityWidth + 2;
            }
            Text message = this.selectedCategory == NotificationCategory.GENERAL ? not.getGeneralMessage() : not.getMessage();
            List<OrderedText> lines = this.textRenderer.wrapLines(message, textWidth);
            if(lines.size() == 1)
            {
                gui.drawText(this.textRenderer, lines.get(0), textXPos, yPos + (NOTIFICATION_HEIGHT / 2) - (this.textRenderer.fontHeight / 2), textColor, false);
            }
            else
            {
                for(int l = 0; l < lines.size() && l < 2; ++l)
                    gui.drawText(this.textRenderer, lines.get(l), textXPos, yPos + 2 + l * 10, textColor, false);
                //Set the message as a tooltip if it's too large to fit and the mouse is hovering over the notification
                if(lines.size() > 2 && tooltip == null && mouseX >= screenLeft + 15 && mouseX < screenLeft + 185 && mouseY >= yPos && mouseY < yPos + NOTIFICATION_HEIGHT)
                    tooltip = message;
            }
        }

        //Render widgets
        super.render(gui, mouseX, mouseY, partialTicks);

        //Render tooltips
        for(NotificationTabButton tab : this.tabButtons)
            tab.renderTooltip(gui, mouseX, mouseY);

        if(tooltip != null)
            gui.drawOrderedTooltip(this.textRenderer, this.textRenderer.wrapLines(tooltip, 170), mouseX, mouseY);

    }

    private void SelectTab(ButtonWidget button) {
        int tabIndex = this.tabButtons.indexOf(button);
        if(tabIndex >= 0)
        {
            List<NotificationCategory> categories = this.getCategories();
            if(tabIndex < categories.size())
            {
                NotificationCategory newCategory = categories.get(tabIndex);
                if(!newCategory.matches(this.selectedCategory))
                {
                    this.selectedCategory = newCategory;
                    //Reset notification scroll as a new category was selected
                    this.notificationScroll = 0;
                    this.positionTabButtons();
                }
            }
        }
    }

    public int getMaxTabScroll() {
        return Math.max(0, this.tabButtons.size() - TABS_PER_PAGE);
    }

    public boolean tabScrolled(double delta) {
        if(delta < 0)
        {
            if(this.tabScroll < this.getMaxTabScroll())
            {
                this.tabScroll++;
                this.positionTabButtons();
            }
            else
                return false;
        }
        else if(delta > 0)
        {
            if(this.tabScroll > 0)
            {
                this.tabScroll--;
                this.positionTabButtons();
            }
            else
                return false;
        }
        return true;
    }

    public int getMaxNotificationScroll() {
        return Math.max(0, this.getNotifications().getNotifications(this.selectedCategory).size() - NOTIFICATIONS_PER_PAGE);
    }

    public boolean notificationScrolled(double delta) {
        if(delta < 0)
        {
            if(this.notificationScroll < this.getMaxNotificationScroll())
                this.notificationScroll++;
            else
                return false;
        }
        else if(delta > 0)
        {
            if(this.notificationScroll > 0)
                this.notificationScroll--;
            else
                return false;
        }
        return true;
    }

    public void markAsRead(ButtonWidget button) { new CMessageFlagNotificationsSeen(this.selectedCategory).sendToServer(); }

    @Override
    public int currentScroll() { return this.notificationScroll; }
    @Override
    public void setScroll(int newScroll) { this.notificationScroll = newScroll; }
    @Override
    public int getMaxScroll() { return this.getMaxNotificationScroll(); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        //If mouse is over the screen, scroll the notifications
        if(mouseX >= this.guiLeft() + TabButton.SIZE && mouseX < this.guiLeft() + this.xSize && mouseY >= this.guiTop() && mouseY < this.guiTop() + this.ySize)
        {
            if(this.notificationScrolled(delta))
                return true;
            //Don't scroll the tabs while the mouse is over the center of the screen.
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        else if(this.tabScrolled(delta)) //Otherwise scroll the tabs
            return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.notificationScroller.onMouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.notificationScroller.onMouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int mods) {
        //Manually close the screen when hitting the inventory key
        assert this.client != null;
        if (this.client.options.inventoryKey.matchesKey(key, scanCode)) {
            this.client.setScreen(null);
            return true;
        }
        return super.keyPressed(key, scanCode, mods);
    }

}