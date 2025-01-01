package io.github.lightman314.lightmanscurrency.common.menu.traderstorage.item;

import java.util.function.Function;

import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderStorageScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.TraderStorageClientTab;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.item.ItemTradeEditClientTab;
import io.github.lightman314.lightmanscurrency.common.menu.TraderStorageMenu;
import io.github.lightman314.lightmanscurrency.common.menu.traderstorage.TraderStorageTab;
import io.github.lightman314.lightmanscurrency.common.money.CoinValue;
import io.github.lightman314.lightmanscurrency.common.traders.item.ItemTraderData;
import io.github.lightman314.lightmanscurrency.common.traders.item.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.common.traders.permissions.Permissions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;

public class ItemTradeEditTab extends TraderStorageTab {

    public ItemTradeEditTab(TraderStorageMenu menu) { super(menu); }

    @Override
    @Environment(EnvType.CLIENT)
    public TraderStorageClientTab<?> createClientTab(TraderStorageScreen screen) { return new ItemTradeEditClientTab(screen, this); }

    @Override
    public boolean canOpen(PlayerEntity player) { return this.menu.getTrader().hasPermission(player, Permissions.EDIT_TRADES); }

    private int tradeIndex = -1;
    public int getTradeIndex() { return this.tradeIndex; }
    public ItemTradeData getTrade() {
        if(this.menu.getTrader() instanceof ItemTraderData trader)
        {
            if(this.tradeIndex >= trader.getTradeCount() || this.tradeIndex < 0)
            {
                this.menu.changeTab(TraderStorageTab.TAB_TRADE_BASIC);
                this.menu.sendMessage(this.menu.createTabChangeMessage(TraderStorageTab.TAB_TRADE_BASIC, null));
                return null;
            }
            return ((ItemTraderData)this.menu.getTrader()).getTrade(this.tradeIndex);
        }
        return null;
    }

    @Override
    public void onTabOpen() { }

    @Override
    public void onTabClose() { }

    @Override
    public void addStorageMenuSlots(Function<Slot, Slot> addSlot) { }

    public void setTradeIndex(int tradeIndex) { this.tradeIndex = tradeIndex; }

    public void setType(ItemTradeData.ItemTradeType type) {
        ItemTradeData trade = this.getTrade();
        if(trade != null)
        {
            trade.setTradeType(type);
            this.menu.getTrader().markTradesDirty();
            if(this.menu.isClient())
            {
                NbtCompound message = new NbtCompound();
                message.putInt("NewType", type.index);
                this.menu.sendMessage(message);
            }
        }
    }

    public void setCustomName(int selectedSlot, String customName) {
        ItemTradeData trade = this.getTrade();
        if(trade != null)
        {
            trade.setCustomName(selectedSlot, customName);
            this.menu.getTrader().markTradesDirty();
            if(this.menu.isClient())
            {
                NbtCompound message = new NbtCompound();
                message.putInt("Slot", selectedSlot);
                message.putString("CustomName", customName);
                this.menu.sendMessage(message);
            }
        }
    }

    public void setPrice(CoinValue price) {
        ItemTradeData trade = this.getTrade();
        if(trade != null)
        {
            trade.setCost(price);
            this.menu.getTrader().markTradesDirty();
            if(this.menu.isClient())
            {
                NbtCompound message = new NbtCompound();
                price.save(message, "NewPrice");
                this.menu.sendMessage(message);
            }
        }
    }

    public void setSelectedItem(int selectedSlot, ItemStack stack) {
        ItemTradeData trade = this.getTrade();
        if(trade != null)
        {
            trade.setItem(stack, selectedSlot);
            this.menu.getTrader().markTradesDirty();
            if(this.menu.isClient())
            {
                NbtCompound message = new NbtCompound();
                message.putInt("Slot", selectedSlot);
                message.put("NewItem", stack.writeNbt(new NbtCompound()));
                this.menu.sendMessage(message);
            }
        }
    }

    public void defaultInteraction(int slotIndex, ItemStack heldStack, int mouseButton) {
        ItemTradeData trade = this.getTrade();
        if(trade != null)
        {
            trade.onSlotInteraction(this, slotIndex, heldStack, mouseButton);
            if(this.menu.isClient())
            {
                NbtCompound message = new NbtCompound();
                message.putInt("Interaction", slotIndex);
                message.putInt("Button", mouseButton);
                message.put("Item", heldStack.writeNbt(new NbtCompound()));
                this.menu.sendMessage(message);
            }
        }

    }

    @Override
    public void receiveMessage(NbtCompound message) {
        if(message.contains("TradeIndex"))
        {
            this.tradeIndex = message.getInt("TradeIndex");
        }
        else if(message.contains("Slot"))
        {
            int slot = message.getInt("Slot");
            if(message.contains("CustomName"))
            {
                this.setCustomName(slot, message.getString("CustomName"));
            }
            else if(message.contains("NewItem"))
            {
                this.setSelectedItem(slot, ItemStack.fromNbt(message.getCompound("NewItem")));
            }
        }
        else if(message.contains("NewPrice"))
        {
            CoinValue price = new CoinValue();
            price.load(message, "NewPrice");
            this.setPrice(price);
        }
        else if(message.contains("NewType"))
        {
            this.setType(ItemTradeData.ItemTradeType.fromIndex(message.getInt("NewType")));
        }
        else if(message.contains("Interaction"))
        {
            int index = message.getInt("Interaction");
            int button = message.getInt("Button");
            this.defaultInteraction(index, ItemStack.fromNbt(message.getCompound("Item")), button);
        }
    }

}