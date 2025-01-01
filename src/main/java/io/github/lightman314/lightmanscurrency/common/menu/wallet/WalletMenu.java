package io.github.lightman314.lightmanscurrency.common.menu.wallet;

import io.github.lightman314.lightmanscurrency.common.core.ModMenus;
import io.github.lightman314.lightmanscurrency.common.menu.slots.BlacklistSlot;
import io.github.lightman314.lightmanscurrency.common.menu.slots.DisplaySlot;
import io.github.lightman314.lightmanscurrency.common.money.MoneyUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class WalletMenu extends WalletMenuBase {

    public WalletMenu(int windowId, PlayerInventory inventory, int walletStackIndex)
    {

        super(ModMenus.WALLET, windowId, inventory, walletStackIndex);

        //Player Inventory before coin slots for desync safety.
        //Should make the Player Inventory slot indexes constant regardless of the wallet state.
        for(int y = 0; y < 3; y++)
        {
            for(int x = 0; x < 9; x++)
            {
                int index = x + (y * 9) + 9;
                if(index == this.walletStackIndex)
                    this.addSlot(new DisplaySlot(this.inventory, index, 8 + x * 18, 32 + (y + getRowCount()) * 18));
                else
                    this.addSlot(new BlacklistSlot(this.inventory, index, 8 + x * 18, 32 + (y + getRowCount()) * 18, this.inventory, this.walletStackIndex));
            }
        }

        //Player hotbar
        for(int x = 0; x < 9; x++)
        {
            if(x == this.walletStackIndex)
                this.addSlot(new DisplaySlot(this.inventory, x, 8 + x * 18, 90 + getRowCount() * 18));
            else
                this.addSlot(new BlacklistSlot(this.inventory, x, 8 + x * 18, 90 + getRowCount() * 18, this.inventory, this.walletStackIndex));
        }

        //Coin Slots last as they may vary between client and server at times.
        this.addCoinSlots(18);

        this.addDummySlots(37 + getMaxWalletSlots());

    }

    @Override
    public ItemStack quickMove(PlayerEntity playerEntity, int index)
    {

        if(index + this.coinInput.size() == this.walletStackIndex)
            return ItemStack.EMPTY;

        ItemStack clickedStack = ItemStack.EMPTY;

        Slot slot = this.slots.get(index);

        if(slot != null && slot.hasStack())
        {
            ItemStack slotStack = slot.getStack();
            clickedStack = slotStack.copy();
            if(index < 36)
            {
                if(!this.insertItem(slotStack, 36, this.slots.size(), false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if(!this.insertItem(slotStack, 0, 36, true))
            {
                return ItemStack.EMPTY;
            }

            if(slotStack.isEmpty())
            {
                slot.setStack(ItemStack.EMPTY);
            }
            else
            {
                slot.markDirty();
            }
        }

        return clickedStack;

    }

    public void QuickCollectCoins()
    {
        Inventory inv = this.player.getInventory();
        for(int i = 0; i < inv.size(); ++i)
        {
            ItemStack item = inv.getStack(i);
            if(MoneyUtil.isCoin(item, false))
            {
                ItemStack result = this.PickupCoins(item);
                inv.setStack(i, result);
            }
        }
    }

}