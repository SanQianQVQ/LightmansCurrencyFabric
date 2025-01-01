package io.github.lightman314.lightmanscurrency.client.gui.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconButton;
import io.github.lightman314.lightmanscurrency.client.util.IconAndButtonUtil;
import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.traders.TraderData;
import io.github.lightman314.lightmanscurrency.common.traders.TraderSaveData;
import io.github.lightman314.lightmanscurrency.common.traders.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.common.traders.rules.*;
import io.github.lightman314.lightmanscurrency.common.traders.tradedata.TradeData;
import io.github.lightman314.lightmanscurrency.network.server.messages.trader.CMessageOpenStorage;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class TradeRuleScreen extends Screen {

    public static final Identifier GUI_TEXTURE = new Identifier(LightmansCurrency.MODID, "textures/gui/traderules.png");

    public final int xSize = 176;
    public final int ySize = 176;

    public final int guiLeft() { return (this.width - this.xSize) / 2; }
    public final int guiTop() { return (this.height - this.ySize) / 2; }

    private final long traderID;
    private final int tradeIndex;

    private TraderData getTrader() { return TraderSaveData.GetTrader(true, this.traderID); }
    private TradeData getTrade() { return this.getTrader().getTradeData().get(this.tradeIndex); }

    private boolean stillValid() {
        try {
            if(this.getTrader() == null || this.tradeIndex >= 0 && this.getTrade() == null)
                return false;
            if(!this.getTrader().hasPermission(this.client.player, Permissions.EDIT_TRADE_RULES))
                return false;
        } catch(Throwable t) { return false; }
        return true;
    }

    int openTab = -1;

    ButtonWidget managerTab;

    Map<Integer,ButtonWidget> tabButtons = new HashMap<>();

    List<ButtonWidget> toggleRuleButtons = new ArrayList<>();

    private List<TradeRule> getTradeRules() {
        try {
            if(this.tradeIndex < 0)
                return this.getTrader().getRules();
            else
                return this.getTrade().getRules();
        } catch(Throwable t) { return new ArrayList<>(); }
    }
    TradeRule currentRule()
    {
        if(this.openTab >= 0 && this.openTab < this.getTradeRules().size())
            return this.getTradeRules().get(this.openTab);
        return null;
    }
    TradeRule.GUIHandler currentGUIHandler = null;

    public TradeRuleScreen(long traderID, int tradeIndex)
    {
        super(Text.empty());
        this.traderID = traderID;
        this.tradeIndex = tradeIndex;
    }

    @Override
    public void init()
    {

        //Back button
        this.addDrawableChild(new IconButton(guiLeft() + this.xSize, guiTop(), this::PressBackButton, IconAndButtonUtil.ICON_BACK));
        this.managerTab = this.addDrawableChild(new IconButton(guiLeft(), guiTop() - 20, b -> this.PressTabButton(-1), IconAndButtonUtil.ICON_TRADE_RULES));

        this.refreshTabs();

        this.initManagerTab();

    }

    private void initManagerTab() {
        this.toggleRuleButtons.clear();
        int count = this.getTradeRules().size();
        for(int i = 0; i < count; ++i)
        {
            final int index = i;
            this.toggleRuleButtons.add(this.addDrawableChild(IconAndButtonUtil.checkmarkButton(guiLeft() + 20, guiTop() + 25 + (12 * i), this::PressManagerActiveButton, () -> {
                List<TradeRule> rules = this.getTradeRules();
                if(index < rules.size())
                    return rules.get(index).isActive();
                return false;
            })));
        }
    }

    private void closeManagerTab() {

        for(ButtonWidget b : this.toggleRuleButtons)
            this.removeCustomWidget(b);
        this.toggleRuleButtons.clear();

    }

    @Override
    public void render(DrawContext gui, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(gui);
        if(this.openTab >= 0)
        {
            TradeRule rule = this.currentRule();
            if(rule != null && !rule.isActive())
                gui.setShaderColor(1f, 0.5f, 0.5f, 1f);
            else
                gui.setShaderColor(0f, 1f, 0f, 1f);
        }
        else
            gui.setShaderColor(1f, 1f, 1f, 1f);


        //Render the background
        gui.drawTexture(GUI_TEXTURE, guiLeft(), guiTop(), 0, 0, this.xSize, this.ySize);

        gui.setShaderColor(1f, 1f, 1f, 1f);
        //Render the current rule
        if(this.currentGUIHandler!= null)
        {
            this.currentGUIHandler.renderTab(gui, mouseX, mouseY, partialTicks);
        }
        else
        {

            //If current rule is null, but open tab is not -1, reset to the manager tab
            if(this.openTab >= 0)
                this.openTab = -1;

            gui.drawText(this.textRenderer, Text.translatable("traderule.list.blurb").formatted(Formatting.BOLD), guiLeft() + 20, guiTop() + 10, 0xFFFFFF, false);

            List<TradeRule> rules = this.getTradeRules();
            for(int i = 0; i < this.getTradeRules().size(); ++i)
            {
                TradeRule rule = rules.get(i);
                MutableText name = rule.getName().formatted(rule.isActive() ? Formatting.GREEN : Formatting.RED).formatted(Formatting.BOLD);
                gui.drawText(this.textRenderer, name, guiLeft() + 32, guiTop() + 26 + (12 * i), 0xFFFFFF, false);
            }

        }

        //Render the buttons, etc
        super.render(gui, mouseX, mouseY, partialTicks);

        if(this.managerTab.isMouseOver(mouseX, mouseY))
        {
            gui.drawTooltip(this.textRenderer, Text.translatable("gui.button.lightmanscurrency.manager"), mouseX, mouseY);
        }
        else
        {
            final List<TradeRule> rules = this.getTradeRules();
            this.tabButtons.forEach((ruleIndex,thisTab) -> {
                if(thisTab.isMouseOver(mouseX, mouseY) && ruleIndex >= 0 && ruleIndex < rules.size())
                    gui.drawTooltip(this.textRenderer, rules.get(ruleIndex).getName(), mouseX, mouseY);
            });
        }
    }

    public void tick()
    {
        if(this.client == null)
            return;
        if(!this.stillValid())
        {
            this.client.setScreen(null);
            return;
        }
        if(this.currentGUIHandler != null)
        {
            this.currentGUIHandler.onScreenTick();
        }
        this.validateTabs();
    }

    void PressBackButton(ButtonWidget button) { new CMessageOpenStorage(this.traderID).sendToServer(); }

    void PressTabButton(int ruleIndex)
    {
        if(ruleIndex >= 0)
        {

            if(this.openTab == ruleIndex)
                return;

            if(this.currentGUIHandler != null)
            {
                this.currentGUIHandler.onTabClose();
                this.currentGUIHandler = null;
            }
            else
                this.closeManagerTab();

            this.openTab = ruleIndex;

            if(this.currentRule() != null)
            {
                this.currentGUIHandler = this.currentRule().createHandler(this, this::currentRule);
                this.currentGUIHandler.initTab();
            }
        }
        else
        {
            if(this.openTab < 0)
                return;

            if(this.currentGUIHandler != null)
            {
                this.currentGUIHandler.onTabClose();
                this.currentGUIHandler = null;
            }

            this.openTab = -1;

            this.initManagerTab();
        }

    }

    void PressManagerActiveButton(ButtonWidget button)
    {
        int ruleIndex = this.toggleRuleButtons.indexOf(button);
        if(ruleIndex >= 0)
        {
            List<TradeRule> rules = this.getTradeRules();
            if(ruleIndex < rules.size())
            {
                TradeRule rule = rules.get(ruleIndex);
                NbtCompound updateInfo = new NbtCompound();
                updateInfo.putBoolean("SetActive", !rule.isActive());
                this.sendUpdateMessage(rule, updateInfo);
            }
            this.refreshTabs();
        }
    }

    public void sendUpdateMessage(TradeRule rule, NbtCompound updateInfo) { if(rule != null) this.getTrader().sendTradeRuleMessage(this.tradeIndex, rule.type, updateInfo); }

    private void validateTabs()
    {
        List<TradeRule> rules = this.getTradeRules();
        int activeCount = 0;
        for(int i = 0; i < rules.size(); ++i)
        {
            TradeRule thisRule = rules.get(i);
            if(thisRule.isActive())
            {
                activeCount++;
                if(!this.tabButtons.containsKey(i))
                {
                    this.refreshTabs();
                    return;
                }
            }
        }
        if(activeCount != this.tabButtons.values().size())
            this.refreshTabs();
    }

    public void refreshTabs()
    {

        this.tabButtons.values().forEach(this::remove);
        this.tabButtons.clear();

        List<TradeRule> rules = this.getTradeRules();
        int buttonPos = 0;
        for(int i = 0; i < rules.size(); i++)
        {
            final int ruleIndex = i;
            TradeRule thisRule = rules.get(ruleIndex);
            if(thisRule.isActive())
            {
                this.tabButtons.put(ruleIndex, this.addDrawableChild(new IconButton(guiLeft() + 20 + 20 * buttonPos, guiTop() - 20, b -> this.PressTabButton(ruleIndex), thisRule.getButtonIcon())));
                buttonPos++;
            }
        }

    }

    //Public functions for easy trade rule renderer access
    public TextRenderer getFont() { return this.textRenderer; }

    public <T extends Element & Drawable & Selectable> T addCustomRenderable(T widget)
    {
        if(widget != null)
            this.addDrawableChild(widget);
        return widget;
    }

    public <T extends Element & Selectable> T addCustomWidget(T widget)
    {
        if(widget != null)
            this.addSelectableChild(widget);
        return widget;
    }

    public <T extends Element> void removeCustomWidget(T widget) { this.remove(widget); }

}