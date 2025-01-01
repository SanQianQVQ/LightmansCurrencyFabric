package io.github.lightman314.lightmanscurrency.common.menu.traderstorage.auction;

import java.util.function.Function;

import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderStorageScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.TraderStorageClientTab;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderstorage.auction.AuctionTradeCancelClientTab;
import io.github.lightman314.lightmanscurrency.common.menu.TraderStorageMenu;
import io.github.lightman314.lightmanscurrency.common.menu.traderstorage.TraderStorageTab;
import io.github.lightman314.lightmanscurrency.common.traders.TraderData;
import io.github.lightman314.lightmanscurrency.common.traders.auction.AuctionHouseTrader;
import io.github.lightman314.lightmanscurrency.common.traders.auction.tradedata.AuctionTradeData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;

public class AuctionTradeCancelTab extends TraderStorageTab {

    public AuctionTradeCancelTab(TraderStorageMenu menu) { super(menu); }

    @Override
    @Environment(EnvType.CLIENT)
    public TraderStorageClientTab<?> createClientTab(TraderStorageScreen screen) { return new AuctionTradeCancelClientTab(screen, this); }

    @Override
    public boolean canOpen(PlayerEntity player) { return this.menu.getTrader() instanceof AuctionHouseTrader; }

    private int tradeIndex = -1;
    public int getTradeIndex() { return this.tradeIndex; }
    public AuctionTradeData getTrade() {
        if(this.menu.getTrader() instanceof AuctionHouseTrader trader)
        {
            if(this.tradeIndex >= trader.getTradeCount() || this.tradeIndex < 0)
            {
                this.menu.changeTab(TraderStorageTab.TAB_TRADE_BASIC);
                this.menu.sendMessage(this.menu.createTabChangeMessage(TraderStorageTab.TAB_TRADE_BASIC, null));
                return null;
            }
            return ((AuctionHouseTrader)this.menu.getTrader()).getTrade(this.tradeIndex);
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

    public void cancelAuction(boolean giveToPlayer) {
        TraderData t = this.menu.getTrader();
        if(t instanceof AuctionHouseTrader trader)
        {
            AuctionTradeData trade = trader.getTrade(this.tradeIndex);
            if(this.menu.isClient())
            {
                NbtCompound message = new NbtCompound();
                message.putBoolean("CancelAuction", giveToPlayer);
                this.menu.sendMessage(message);
                //Don't run the cancel interaction while on the client
                return;
            }
            if(trade.isOwner(this.menu.player))
            {
                trade.CancelTrade(trader, giveToPlayer, this.menu.player);
                trader.markTradesDirty();
                trader.markStorageDirty();
                NbtCompound message = new NbtCompound();
                message.putBoolean("CancelSuccess", true);
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
        if(message.contains("CancelAuction"))
        {
            this.cancelAuction(message.getBoolean("CancelAuction"));
        }
    }

}