package io.github.lightman314.lightmanscurrency.common.traders.item.tradedata.restrictions;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import io.github.lightman314.lightmanscurrency.common.core.ModItems;
import io.github.lightman314.lightmanscurrency.common.items.TicketItem;
import io.github.lightman314.lightmanscurrency.common.menu.slots.ticket.TicketSlot;
import io.github.lightman314.lightmanscurrency.common.traders.item.storage.TraderItemStorage;
import io.github.lightman314.lightmanscurrency.common.traders.item.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TicketKioskRestriction extends ItemTradeRestriction{

    public static TicketKioskRestriction INSTANCE = new TicketKioskRestriction();

    private TicketKioskRestriction() {}

    @Override
    public ItemStack modifySellItem(ItemStack sellItem, String customName, ItemTradeData trade)
    {
        if(sellItem.getItem() instanceof TicketItem && !customName.isBlank())
            sellItem.setCustomName(Text.literal(customName));
        return sellItem;
    }

    @Override
    public boolean allowSellItem(ItemStack itemStack)
    {
        if(TicketItem.isMasterTicket(itemStack))
            return true;
        return InventoryUtil.ItemHasTag(itemStack, TicketItem.TICKET_MATERIAL_TAG) && itemStack.getItem() != ModItems.TICKET;
    }

    @Override
    public ItemStack filterSellItem(ItemStack itemStack)
    {
        if(TicketItem.isMasterTicket(itemStack))
            return TicketItem.CreateTicket(TicketItem.GetTicketID(itemStack), 1);
        else if(InventoryUtil.ItemHasTag(itemStack, TicketItem.TICKET_MATERIAL_TAG) && itemStack.getItem() != ModItems.TICKET)
            return itemStack;
        else
            return ItemStack.EMPTY;
    }

    @Override
    public boolean allowItemSelectItem(ItemStack itemStack)
    {
        Item item = itemStack.getItem();
        return InventoryUtil.ItemHasTag(itemStack, TicketItem.TICKET_MATERIAL_TAG) && item != ModItems.TICKET && item != ModItems.TICKET_MASTER;
    }

    @Override
    public boolean allowExtraItemInStorage(ItemStack itemStack) {
        return InventoryUtil.ItemHasTag(itemStack, TicketItem.TICKET_MATERIAL_TAG);
    }

    @Override
    public int getSaleStock(TraderItemStorage traderStorage, ItemStack... sellItemList) {
        int ticketCount = 0;
        boolean foundTicket = false;
        int minStock = Integer.MAX_VALUE;
        for(ItemStack sellItem : sellItemList) {
            //Always add item to the ticket count, even if it's not a ticket, as the non-ticket sell item will still subtract from the available printing materials.
            ticketCount += sellItem.getCount();
            if(sellItem.getItem() == ModItems.TICKET)
                foundTicket = true;
            else
                minStock = Math.min(this.getItemStock(sellItem, traderStorage), minStock);
        }
        if(foundTicket && ticketCount > 0)
            minStock = Math.min(this.getTicketStock(ticketCount, traderStorage), minStock);
        return minStock;
    }

    protected final int getTicketStock(int ticketCount, TraderItemStorage traderStorage)
    {
        return traderStorage.getItemTagCount(TicketItem.TICKET_MATERIAL_TAG, ModItems.TICKET_MASTER) / ticketCount;
    }

    @Override
    public void removeItemsFromStorage(TraderItemStorage traderStorage, ItemStack... sellItemList)
    {
        //Sort out the tickets, and remove "normal" items from storage.
        List<ItemStack> tickets = new ArrayList<>();
        List<ItemStack> ignoreIfPossible = new ArrayList<>();
        for(ItemStack sellItem : sellItemList)
        {
            if(sellItem.getItem() == ModItems.TICKET)
                tickets.add(sellItem);
            else
            {
                this.removeFromStorage(sellItem, traderStorage);
                ignoreIfPossible.add(sellItem);
            }
        }
        //Attempt to remove the tickets from storage "normally".
        //Keep track of how many need to be printed.
        int printCount = 0;
        for(ItemStack ticketStack : tickets)
        {
            printCount += ticketStack.getCount() - traderStorage.removeItem(ticketStack).getCount();
        }
        //Remove the printing materials for tickets that needed to be printed
        if(printCount > 0)
        {
            traderStorage.removeItemTagCount(TicketItem.TICKET_MATERIAL_TAG, printCount, ignoreIfPossible, ModItems.TICKET_MASTER);
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Pair<Identifier,Identifier> getEmptySlotBG()
    {
        return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, TicketSlot.EMPTY_TICKET_SLOT);
    }

}