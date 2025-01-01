package io.github.lightman314.lightmanscurrency.common.menu.traderinterface.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.TraderInterfaceScreen;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderinterface.TraderInterfaceClientTab;
import io.github.lightman314.lightmanscurrency.client.gui.screen.inventory.traderinterface.item.ItemStorageClientTab;
import io.github.lightman314.lightmanscurrency.common.blockentity.traderinterface.item.ItemTraderInterfaceBlockEntity;
import io.github.lightman314.lightmanscurrency.common.menu.TraderInterfaceMenu;
import io.github.lightman314.lightmanscurrency.common.menu.slots.SimpleSlot;
import io.github.lightman314.lightmanscurrency.common.menu.slots.trader.UpgradeInputSlot;
import io.github.lightman314.lightmanscurrency.common.menu.traderinterface.TraderInterfaceTab;
import io.github.lightman314.lightmanscurrency.common.traders.item.storage.TraderItemStorage;
import io.github.lightman314.lightmanscurrency.util.InventoryUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.Direction;

public class ItemStorageTab extends TraderInterfaceTab {

    public ItemStorageTab(TraderInterfaceMenu menu) { super(menu); }

    @Override
    @Environment(EnvType.CLIENT)
    public TraderInterfaceClientTab<?> createClientTab(TraderInterfaceScreen screen) { return new ItemStorageClientTab(screen, this); }

    @Override
    public boolean canOpen(PlayerEntity player) { return true; }

    //Eventually will add upgrade slots
    List<SimpleSlot> slots = new ArrayList<>();
    public List<? extends Slot> getSlots() { return this.slots; }

    @Override
    public void onTabOpen() { SimpleSlot.SetActive(this.slots); }

    @Override
    public void onTabClose() { SimpleSlot.SetInactive(this.slots); }

    @Override
    public void addStorageMenuSlots(Function<Slot,Slot> addSlot) {
        for(int i = 0; i < this.menu.getTraderInterface().getUpgradeInventory().size(); ++i)
        {
            SimpleSlot upgradeSlot = new UpgradeInputSlot(this.menu.getTraderInterface().getUpgradeInventory(), i, 176, 18 + 18 * i, this.menu.getTraderInterface(), this::onUpgradeModified);
            upgradeSlot.active = false;
            addSlot.apply(upgradeSlot);
            this.slots.add(upgradeSlot);
        }
    }

    private void onUpgradeModified() { this.menu.getTraderInterface().setUpgradeSlotsDirty(); }

    @Override
    public boolean quickMoveStack(ItemStack stack) {
        if(this.menu.getTraderInterface() instanceof ItemTraderInterfaceBlockEntity be) {
            TraderItemStorage storage = be.getItemBuffer();
            if(storage.getFittableAmount(stack) > 0)
            {
                storage.tryAddItem(stack);
                be.setItemBufferDirty();
                return true;
            }
        }
        return super.quickMoveStack(stack);
    }

    public void clickedOnSlot(int storageSlot, boolean isShiftHeld, boolean leftClick) {
        if(this.menu.getTraderInterface().canAccess(this.menu.player) && this.menu.getTraderInterface() instanceof ItemTraderInterfaceBlockEntity)
        {
            ItemTraderInterfaceBlockEntity be = (ItemTraderInterfaceBlockEntity)this.menu.getTraderInterface();
            TraderItemStorage storage = be.getItemBuffer();
            ItemStack heldItem = this.menu.getCursorStack();
            if(heldItem.isEmpty())
            {
                //Move item out of storage
                List<ItemStack> storageContents = storage.getContents();
                if(storageSlot >= 0 && storageSlot < storageContents.size())
                {
                    ItemStack stackToRemove = storageContents.get(storageSlot).copy();
                    ItemStack removeStack = stackToRemove.copy();

                    //Assume we're moving a whole stack for now
                    int tempAmount = Math.min(stackToRemove.getMaxCount(), stackToRemove.getCount());
                    stackToRemove.setCount(tempAmount);
                    int removedAmount = 0;

                    //Right-click, attempt to cut the stack in half
                    if(!leftClick)
                    {
                        if(tempAmount > 1)
                            tempAmount = tempAmount / 2;
                        stackToRemove.setCount(tempAmount);
                    }

                    if(isShiftHeld)
                    {
                        //Put the item in the players inventory. Will not throw overflow on the ground, so it will safely stop if the players inventory is full
                        this.menu.player.getInventory().insertStack(stackToRemove);
                        //Determine the amount actually added to the players inventory
                        removedAmount = tempAmount - stackToRemove.getCount();
                    }
                    else
                    {
                        //Put the item into the players hand
                        this.menu.setCursorStack(stackToRemove);
                        removedAmount = tempAmount;
                    }
                    //Remove the correct amount from storage
                    if(removedAmount > 0)
                    {
                        removeStack.setCount(removedAmount);
                        storage.removeItem(removeStack);
                        //Mark the storage dirty
                        be.setItemBufferDirty();
                    }
                }
            }
            else
            {
                //Move from hand to storage
                if(leftClick)
                {
                    storage.tryAddItem(heldItem);
                    //Mark the storage dirty
                    be.setItemBufferDirty();
                }
                else
                {
                    //Right click, only attempt to add 1 from the hand
                    ItemStack addItem = heldItem.copy();
                    addItem.setCount(1);
                    if(storage.addItem(addItem))
                    {
                        heldItem.decrement(1);
                        if(heldItem.isEmpty())
                            this.menu.setCursorStack(ItemStack.EMPTY);
                    }
                    //Mark the storage dirty
                    be.setItemBufferDirty();
                }
            }
            if(this.menu.isClient())
            {
                NbtCompound message = new NbtCompound();
                message.putInt("ClickedSlot", storageSlot);
                message.putBoolean("HeldShift", isShiftHeld);
                message.putBoolean("LeftClick", leftClick);
                this.menu.sendMessage(message);
            }
        }
    }

    public void quickTransfer(int type) {
        if(this.menu.getTraderInterface().canAccess(this.menu.player) && this.menu.getTraderInterface() instanceof ItemTraderInterfaceBlockEntity)
        {
            ItemTraderInterfaceBlockEntity be = (ItemTraderInterfaceBlockEntity)this.menu.getTraderInterface();
            TraderItemStorage storage = be.getItemBuffer();
            PlayerInventory inv = this.menu.player.getInventory();
            boolean changed = false;
            if(type == 0)
            {
                //Quick Deposit
                for(int i = 0; i < 36; ++i)
                {
                    ItemStack stack = inv.getStack(i);
                    int fillAmount = storage.getFittableAmount(stack);
                    if(fillAmount > 0)
                    {
                        //Remove the item from the players inventory
                        ItemStack fillStack = inv.removeStack(i, fillAmount);
                        //Put the item into storage
                        storage.forceAddItem(fillStack);
                    }
                }
            }
            else if(type == 1)
            {
                //Quick Extract
                List<ItemStack> itemList = InventoryUtil.copyList(storage.getContents());
                for(ItemStack stack : itemList)
                {
                    boolean keepTrying = true;
                    while(storage.getItemCount(stack) > 0 && keepTrying)
                    {
                        ItemStack transferStack = stack.copy();
                        int transferCount = Math.min(storage.getItemCount(stack), stack.getMaxCount());
                        transferStack.setCount(transferCount);
                        //Attempt to move the stack into the players inventory
                        int removedCount = InventoryUtil.safeGiveToPlayer(inv, transferStack);
                        if(removedCount > 0)
                        {
                            changed = true;
                            //Remove the transferred amount from storage
                            ItemStack removeStack = stack.copy();
                            removeStack.setCount(removedCount);
                            storage.removeItem(removeStack);
                        }
                        else
                            keepTrying = false;
                    }
                }
            }

            if(changed)
                be.setItemBufferDirty();

            if(this.menu.isClient())
            {
                NbtCompound message = new NbtCompound();
                message.putInt("QuickTransfer", type);
                this.menu.sendMessage(message);
            }

        }
    }

    public void toggleInputSlot(Direction side) {
        if(this.menu.getTraderInterface().canAccess(this.menu.player) && this.menu.getTraderInterface() instanceof ItemTraderInterfaceBlockEntity) {
            ItemTraderInterfaceBlockEntity be = (ItemTraderInterfaceBlockEntity)this.menu.getTraderInterface();
            be.getItemHandler().toggleInputSide(side);
            be.setHandlerDirty(be.getItemHandler());
        }
    }

    public void toggleOutputSlot(Direction side) {
        if(this.menu.getTraderInterface().canAccess(this.menu.player) && this.menu.getTraderInterface() instanceof ItemTraderInterfaceBlockEntity) {
            ItemTraderInterfaceBlockEntity be = (ItemTraderInterfaceBlockEntity)this.menu.getTraderInterface();
            be.getItemHandler().toggleOutputSide(side);
            be.setHandlerDirty(be.getItemHandler());
        }
    }



    @Override
    public void receiveMessage(NbtCompound message) {
        if(message.contains("ClickedSlot", NbtElement.INT_TYPE))
        {
            int storageSlot = message.getInt("ClickedSlot");
            boolean isShiftHeld = message.getBoolean("HeldShift");
            boolean leftClick = message.getBoolean("LeftClick");
            this.clickedOnSlot(storageSlot, isShiftHeld, leftClick);
        }
        if(message.contains("QuickTransfer"))
        {
            this.quickTransfer(message.getInt("QuickTransfer"));
        }
    }

}