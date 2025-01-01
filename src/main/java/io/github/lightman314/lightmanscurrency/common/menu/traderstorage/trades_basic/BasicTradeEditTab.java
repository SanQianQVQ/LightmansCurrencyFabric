package io.github.lightman314.lightmanscurrency.common.menu.traderstorage.trades_basic;

import java.util.function.Function;

import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderStorageScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.BasicTradeEditClientTab;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.TraderStorageClientTab;
import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.menu.TraderStorageMenu;
import io.github.lightman314.lightmanscurrency.common.menu.traderstorage.TraderStorageTab;
import io.github.lightman314.lightmanscurrency.common.traders.tradedata.TradeData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;

public class BasicTradeEditTab extends TraderStorageTab {

    public BasicTradeEditTab(TraderStorageMenu menu) { super(menu); }

    public static final int INTERACTION_INPUT = 0;
    public static final int INTERACTION_OUTPUT = 1;
    public static final int INTERACTION_OTHER = 2;

    TraderStorageMenu.IClientMessage clientHandler = null;

    @Override
    @Environment(EnvType.CLIENT)
    public TraderStorageClientTab<?> createClientTab(TraderStorageScreen screen) { return new BasicTradeEditClientTab<>(screen, this); }

    public void setClientHandler(TraderStorageMenu.IClientMessage clientHandler) { this.clientHandler = clientHandler; }

    @Override
    public boolean canOpen(PlayerEntity player) { return true; }

    @Override
    public void onTabOpen() { }

    @Override
    public void onTabClose() { }

    @Override
    public void addStorageMenuSlots(Function<Slot, Slot> addSlot) { }

    public void sendOpenTabMessage(int newTab, @Nullable NbtCompound additionalData) {
        NbtCompound message = this.menu.createTabChangeMessage(newTab, additionalData);
        if(this.clientHandler != null)
            this.clientHandler.selfMessage(message);
        this.menu.sendMessage(message);
    }

    public void sendInputInteractionMessage(int tradeIndex, int interactionIndex, int button, ItemStack heldItem) {
        NbtCompound message = new NbtCompound();
        message.putInt("TradeIndex", tradeIndex);
        message.putInt("InteractionType", INTERACTION_INPUT);
        message.putInt("InteractionIndex", interactionIndex);
        message.putInt("Button", button);
        NbtCompound itemTag = new NbtCompound();
        heldItem.writeNbt(itemTag);
        message.put("HeldItem", itemTag);
        //LightmansCurrency.LogInfo("Trade Input Interaction sent.\nIndex: " + tradeIndex + "\nInteractionIndex: " + interactionIndex + "\nButton: " + button + "\nHeld Item: " + heldItem.getCount() + "x " + heldItem.getItem().getRegistryName().toString());
        this.menu.sendMessage(message);
    }

    public void sendOutputInteractionMessage(int tradeIndex, int interactionIndex, int button, ItemStack heldItem) {
        NbtCompound message = new NbtCompound();
        message.putInt("TradeIndex", tradeIndex);
        message.putInt("InteractionType", INTERACTION_OUTPUT);
        message.putInt("InteractionIndex", interactionIndex);
        message.putInt("Button", button);
        NbtCompound itemTag = new NbtCompound();
        heldItem.writeNbt(itemTag);
        message.put("HeldItem", itemTag);
        //LightmansCurrency.LogInfo("Trade Output Interaction sent.\nIndex: " + tradeIndex + "\nInteractionIndex: " + interactionIndex + "\nButton: " + button + "\nHeld Item: " + heldItem.getCount() + "x " + heldItem.getItem().getRegistryName().toString());
        this.menu.sendMessage(message);
    }

    public void sendOtherInteractionMessage(int tradeIndex, int mouseX, int mouseY, int button, ItemStack heldItem) {
        NbtCompound message = new NbtCompound();
        message.putInt("TradeIndex", tradeIndex);
        message.putInt("InteractionType", INTERACTION_OTHER);
        message.putInt("Button", button);
        message.putInt("MouseX", mouseX);
        message.putInt("MouseY", mouseX);
        NbtCompound itemTag = new NbtCompound();
        heldItem.writeNbt(itemTag);
        message.put("HeldItem", itemTag);
        //LightmansCurrency.LogInfo("Trade Misc Interaction sent.\nIndex: " + tradeIndex + "\nMouse: " + mouseX + "," + mouseY + "\nButton: " + button + "\nHeld Item: " + heldItem.getCount() + "x " + heldItem.getItem().getRegistryName().toString());
        this.menu.sendMessage(message);
    }

    public void addTrade() {

        if(this.menu.getTrader() != null)
        {
            this.menu.getTrader().addTrade(this.menu.player);
            if(this.menu.isClient())
            {
                NbtCompound message = new NbtCompound();
                message.putBoolean("AddTrade", true);
                this.menu.sendMessage(message);
            }
        }

    }

    public void removeTrade() {

        if(this.menu.getTrader() != null)
        {
            this.menu.getTrader().removeTrade(this.menu.player);
            if(this.menu.isClient())
            {
                NbtCompound message = new NbtCompound();
                message.putBoolean("RemoveTrade", true);
                this.menu.sendMessage(message);
            }
        }

    }

    @Override
    public void receiveMessage(NbtCompound message) {
        if(message.contains("TradeIndex", NbtElement.INT_TYPE))
        {
            int tradeIndex = message.getInt("TradeIndex");
            int interaction = message.getInt("InteractionType");
            int interactionIndex = message.contains("InteractionIndex", NbtElement.INT_TYPE) ? message.getInt("InteractionIndex") : 0;
            int button = message.getInt("Button");
            int mouseX = message.contains("MouseX", NbtElement.INT_TYPE) ? message.getInt("MouseX") : 0;
            int mouseY = message.contains("MouseY", NbtElement.INT_TYPE) ? message.getInt("MouseY") : 0;
            ItemStack heldItem = ItemStack.fromNbt(message.getCompound("HeldItem"));
            TradeData trade = this.menu.getTrader().getTradeData().get(tradeIndex);
            switch(interaction) {
                case INTERACTION_INPUT:
                    trade.onInputDisplayInteraction(this, this.clientHandler, interactionIndex, button, heldItem);
                    //LightmansCurrency.LogInfo("Trade Input Interaction received.\nIndex: " + tradeIndex + "\nInteractionIndex: " + interactionIndex + "\nButton: " + button + "\nHeld Item: " + heldItem.getCount() + "x " + heldItem.getItem().getRegistryName().toString());
                    break;
                case INTERACTION_OUTPUT:
                    trade.onOutputDisplayInteraction(this, this.clientHandler, interactionIndex, button, heldItem);
                    //LightmansCurrency.LogInfo("Trade Output Interaction received.\nIndex: " + tradeIndex + "\nInteractionIndex: " + interactionIndex + "\nButton: " + button + "\nHeld Item: " + heldItem.getCount() + "x " + heldItem.getItem().getRegistryName().toString());
                    break;
                case INTERACTION_OTHER:
                    trade.onInteraction(this, this.clientHandler, mouseX, mouseY, button, heldItem);
                    //LightmansCurrency.LogInfo("Trade Misc Interaction received.\nIndex: " + tradeIndex + "\nMouse: " + mouseX + "," + mouseY + "\nButton: " + button + "\nHeld Item: " + heldItem.getCount() + "x " + heldItem.getItem().getRegistryName().toString());
                    break;
                default:
                    LightmansCurrency.LogWarning("Interaction Type " + interaction + " is not a valid interaction.");
            }
            this.menu.getTrader().markTradesDirty();
        }
        if(message.contains("AddTrade"))
            this.addTrade();
        if(message.contains("RemoveTrade"))
            this.removeTrade();
    }

}