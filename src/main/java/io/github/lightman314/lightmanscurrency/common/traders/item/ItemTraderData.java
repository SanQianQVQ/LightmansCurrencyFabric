package io.github.lightman314.lightmanscurrency.common.traders.item;

import java.util.ArrayList;
import java.util.List;

import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.commands.CommandLCAdmin;
import io.github.lightman314.lightmanscurrency.common.core.ModItems;
import io.github.lightman314.lightmanscurrency.common.items.UpgradeItem;
import io.github.lightman314.lightmanscurrency.common.menu.TraderStorageMenu;
import io.github.lightman314.lightmanscurrency.common.menu.traderstorage.TraderStorageTab;
import io.github.lightman314.lightmanscurrency.common.menu.traderstorage.item.ItemStorageTab;
import io.github.lightman314.lightmanscurrency.common.menu.traderstorage.item.ItemTradeEditTab;
import io.github.lightman314.lightmanscurrency.common.money.CoinValue;
import io.github.lightman314.lightmanscurrency.common.notifications.types.trader.OutOfStockNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.trader.item.ItemTradeNotification;
import io.github.lightman314.lightmanscurrency.common.notifications.types.trader.settings.AddRemoveTradeNotification;
import io.github.lightman314.lightmanscurrency.common.ownership.PlayerReference;
import io.github.lightman314.lightmanscurrency.common.traders.*;
import io.github.lightman314.lightmanscurrency.common.traders.item.handler.TraderItemHandler;
import io.github.lightman314.lightmanscurrency.common.traders.item.storage.TraderItemStorage;
import io.github.lightman314.lightmanscurrency.common.traders.item.tradedata.ItemTradeData;
import io.github.lightman314.lightmanscurrency.common.traders.item.tradedata.restrictions.ItemTradeRestriction;
import io.github.lightman314.lightmanscurrency.common.upgrades.UpgradeType;
import io.github.lightman314.lightmanscurrency.common.upgrades.types.capacity.CapacityUpgrade;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData;
import io.github.lightman314.lightmanscurrency.common.traders.TradeContext.TradeResult;
import io.github.lightman314.lightmanscurrency.common.traders.permissions.Permissions;
import io.github.lightman314.lightmanscurrency.common.traders.rules.TradeRule;
import io.github.lightman314.lightmanscurrency.common.traders.tradedata.TradeData;
import io.github.lightman314.lightmanscurrency.util.FileUtil;
import io.github.lightman314.lightmanscurrency.util.MathUtil;

public class ItemTraderData extends InputTraderData implements TraderItemStorage.ITraderItemFilter, ITradeSource<ItemTradeData> {

    public static final List<UpgradeType> ALLOWED_UPGRADES = Lists.newArrayList(UpgradeType.ITEM_CAPACITY);

    public static final int DEFAULT_STACK_LIMIT = 64 * 9;

    public static final Identifier TYPE = new Identifier(LightmansCurrency.MODID, "item_trader");

    TraderItemHandler itemHandler = new TraderItemHandler(this);

    protected TraderItemStorage storage = new TraderItemStorage(this);
    public final TraderItemStorage getStorage() { return this.storage; }

    public void markStorageDirty() { this.markDirty(this::saveStorage); }

    protected List<ItemTradeData> trades;

    @Override
    public boolean allowAdditionalUpgradeType(UpgradeType type) { return ALLOWED_UPGRADES.contains(type); }

    public ItemTraderData(){ this(TYPE); }
    protected ItemTraderData(Identifier type) {
        super(type);
        this.trades = ItemTradeData.listOfSize(1, true);
        this.validateTradeRestrictions();
    }

    public ItemTraderData(int tradeCount, World level, BlockPos pos) { this(TYPE, tradeCount, level, pos); }

    protected ItemTraderData(Identifier type, int tradeCount, World level, BlockPos pos)
    {
        super(type, level, pos);
        this.trades = ItemTradeData.listOfSize(tradeCount, true);
        this.validateTradeRestrictions();
    }

    @Override
    public void saveAdditional(NbtCompound compound) {
        super.saveAdditional(compound);

        this.saveStorage(compound);
        this.saveTrades(compound);

    }
    protected final void saveStorage(NbtCompound compound) {
        this.storage.save(compound, "ItemStorage");
    }

    protected final void saveTrades(NbtCompound compound) {
        ItemTradeData.saveAllData(compound, this.trades);
    }

    @Override
    public void loadAdditional(NbtCompound compound) {
        super.loadAdditional(compound);

        if(compound.contains("ItemStorage"))
            this.storage.load(compound, "ItemStorage");

        if(compound.contains(TradeData.DEFAULT_KEY))
        {
            this.trades = ItemTradeData.loadAllData(compound, !this.isPersistent());
            this.validateTradeRestrictions();
        }

    }

    @Override
    public int getTradeCount() { return this.trades.size(); }

    @Override
    public void addTrade(PlayerEntity requestor)
    {
        if(this.isClient())
            return;
        if(this.getTradeCount() >= TraderData.GLOBAL_TRADE_LIMIT)
            return;
        if(CommandLCAdmin.isAdminPlayer(requestor))
        {

            this.overrideTradeCount(this.getTradeCount() + 1);

            this.pushLocalNotification(new AddRemoveTradeNotification(PlayerReference.of(requestor), true, this.getTradeCount()));

        }
        else
            Permissions.PermissionWarning(requestor, "add a trade slot", Permissions.ADMIN_MODE);
    }

    public void removeTrade(PlayerEntity requestor)
    {
        if(this.isClient())
            return;
        if(this.getTradeCount() <= 1)
            return;
        if(CommandLCAdmin.isAdminPlayer(requestor))
        {

            this.overrideTradeCount(this.getTradeCount() - 1);

            this.pushLocalNotification(new AddRemoveTradeNotification(PlayerReference.of(requestor), false, this.getTradeCount()));

        }
        else
            Permissions.PermissionWarning(requestor, "remove a trade slot", Permissions.ADMIN_MODE);
    }

    public void overrideTradeCount(int newTradeCount)
    {
        if(this.getTradeCount() == MathUtil.clamp(newTradeCount, 1, TraderData.GLOBAL_TRADE_LIMIT))
            return;
        int tradeCount = MathUtil.clamp(newTradeCount, 1, TraderData.GLOBAL_TRADE_LIMIT);
        List<ItemTradeData> oldTrades = trades;
        trades = ItemTradeData.listOfSize(tradeCount, !this.isPersistent());
        //Write the old trade data into the array.
        for(int i = 0; i < oldTrades.size() && i < trades.size(); i++)
        {
            trades.set(i, oldTrades.get(i));
        }
        this.validateTradeRestrictions();
        //Send an update to the client
        if(this.isServer())
        {
            //Send update packet
            this.markTradesDirty();
        }

    }

    public final void validateTradeRestrictions() {
        for(int i = 0; i < this.trades.size(); ++i)
        {
            ItemTradeData trade = this.trades.get(i);
            trade.setRestriction(this.getTradeRestriction(i));
        }
    }

    protected ItemTradeRestriction getTradeRestriction(int tradeIndex) { return ItemTradeRestriction.NONE; }

    public ItemTradeData getTrade(int tradeSlot)
    {
        if(tradeSlot < 0 || tradeSlot >= this.trades.size())
        {
            LightmansCurrency.LogError("Cannot get trade in index " + tradeSlot + " from a trader with only " + this.trades.size() + " trades.");
            return new ItemTradeData(false);
        }
        return this.trades.get(tradeSlot);
    }

    @Override
    public List<ItemTradeData> getTradeData() { return new ArrayList<>(this.trades); }

    public int getTradeStock(int tradeSlot)
    {
        ItemTradeData trade = getTrade(tradeSlot);
        if(trade.sellItemsDefined())
        {
            if(this.isCreative())
                return Integer.MAX_VALUE;
            else
                return trade.stockCount(this);
        }
        return 0;
    }

    @Override
    public Storage<ItemVariant> getItemStorage(Direction relativeSide) { return this.itemHandler.getHandler(relativeSide); }

    @Override
    public IconData inputSettingsTabIcon() { return IconData.of(Items.HOPPER); }

    @Override
    public MutableText inputSettingsTabTooltip() { return Text.translatable("tooltip.lightmanscurrency.settings.iteminput"); }

    @Override
    public int inputSettingsTabColor() { return 0x00BF00; }

    @Override
    public int inputSettingsTextColor() { return 0xD0D0D0; }

    @Override
    public IconData getIcon() { return IconData.of(ModItems.TRADING_CORE); }

    @Override
    public boolean hasValidTrade() {
        for(ItemTradeData trade : this.trades)
        {
            if(trade.isValid())
                return true;
        }
        return false;
    }

    @Override
    protected void saveAdditionalToJson(JsonObject json) {

        JsonArray trades = new JsonArray();
        for(ItemTradeData trade : this.trades)
        {
            if(trade.isValid())
            {
                JsonObject tradeData = new JsonObject();
                tradeData.addProperty("TradeType", trade.getTradeType().name());
                if(trade.getSellItem(0).isEmpty())
                {
                    tradeData.add("SellItem", FileUtil.convertItemStack(trade.getSellItem(1)));
                    if(trade.hasCustomName(1))
                        tradeData.addProperty("DisplayName", trade.getCustomName(1));
                }
                else
                {
                    tradeData.add("SellItem", FileUtil.convertItemStack(trade.getSellItem(0)));
                    if(trade.hasCustomName(0))
                        tradeData.addProperty("DisplayName", trade.getCustomName(0));
                    if(!trade.getSellItem(1).isEmpty())
                    {
                        tradeData.add("SellItem2", FileUtil.convertItemStack(trade.getSellItem(1)));
                        if(trade.hasCustomName(1))
                            tradeData.addProperty("DisplayName2", trade.getCustomName(1));
                    }
                }

                if(trade.isSale() || trade.isPurchase())
                    tradeData.add("Price", trade.getCost().toJson());

                if(trade.isBarter())
                {
                    if(trade.getBarterItem(0).isEmpty())
                    {
                        tradeData.add("BarterItem", FileUtil.convertItemStack(trade.getBarterItem(1)));
                    }
                    else
                    {
                        tradeData.add("BarterItem", FileUtil.convertItemStack(trade.getBarterItem(0)));
                        if(!trade.getBarterItem(1).isEmpty())
                            tradeData.add("BarterItem2", FileUtil.convertItemStack(trade.getBarterItem(1)));
                    }
                }

                JsonArray ruleData = TradeRule.saveRulesToJson(trade.getRules());
                if(ruleData.size() > 0)
                    tradeData.add("Rules", ruleData);

                trades.add(tradeData);
            }
        }

        json.add("Trades", trades);

    }

    @Override
    protected void loadAdditionalFromJson(JsonObject json) throws Exception {

        if(!json.has("Trades"))
            throw new Exception("Item Trader must have a trade list.");

        JsonArray trades = json.get("Trades").getAsJsonArray();
        this.trades = new ArrayList<>();
        for(int i = 0; i < trades.size() && this.trades.size() < TraderData.GLOBAL_TRADE_LIMIT; ++i)
        {
            try {
                JsonObject tradeData = trades.get(i).getAsJsonObject();

                ItemTradeData newTrade = new ItemTradeData(false);
                //Sell Item
                newTrade.setItem(FileUtil.parseItemStack(tradeData.get("SellItem").getAsJsonObject()), 0);
                if(tradeData.has("SellItem2"))
                    newTrade.setItem(FileUtil.parseItemStack(tradeData.get("SellItem2").getAsJsonObject()), 1);
                //Trade Type
                if(tradeData.has("TradeType"))
                    newTrade.setTradeType(ItemTradeData.loadTradeType(tradeData.get("TradeType").getAsString()));
                //Trade Price
                if(tradeData.has("Price"))
                {
                    if(newTrade.isBarter())
                        LightmansCurrency.LogWarning("Price is being defined for a barter trade. Price will be ignored.");
                    else
                        newTrade.setCost(CoinValue.Parse(tradeData.get("Price")));
                }
                else if(!newTrade.isBarter())
                {
                    LightmansCurrency.LogWarning("Price is not defined on a non-barter trade. Price will be assumed to be free.");
                    newTrade.getCost().setFree(true);
                }
                if(tradeData.has("BarterItem"))
                {
                    if(newTrade.isBarter())
                    {
                        newTrade.setItem(FileUtil.parseItemStack(tradeData.get("BarterItem").getAsJsonObject()), 2);
                        if(tradeData.has("BarterItem2"))
                            newTrade.setItem(FileUtil.parseItemStack(tradeData.get("BarterItem2").getAsJsonObject()), 3);
                    }
                    else
                    {
                        LightmansCurrency.LogWarning("BarterItem is being defined for a non-barter trade. Barter item will be ignored.");
                    }
                }
                if(tradeData.has("DisplayName"))
                    newTrade.setCustomName(0, tradeData.get("DisplayName").getAsString());
                if(tradeData.has("DisplayName2"))
                    newTrade.setCustomName(1, tradeData.get("DisplayName2").getAsString());
                if(tradeData.has("Rules"))
                    newTrade.setRules(TradeRule.Parse(tradeData.get("Rules").getAsJsonArray()));

                this.trades.add(newTrade);

            } catch(Exception e) { LightmansCurrency.LogError("Error parsing item trade at index " + i, e); }
        }

        if(this.trades.size() == 0)
            throw new Exception("Trader has no valid trades!");

        this.storage = new TraderItemStorage.LockedTraderStorage(this);

    }

    @Override
    protected void saveAdditionalPersistentData(NbtCompound compound) {
        NbtList tradePersistentData = new NbtList();
        boolean tradesAreRelevant = false;
        for (ItemTradeData trade : this.trades) {
            NbtCompound ptTag = new NbtCompound();
            if (TradeRule.savePersistentData(ptTag, trade.getRules(), "RuleData"))
                tradesAreRelevant = true;
            tradePersistentData.add(ptTag);
        }
        if(tradesAreRelevant)
            compound.put("PersistentTradeData", tradePersistentData);
    }

    @Override
    protected void loadAdditionalPersistentData(NbtCompound compound) {
        if(compound.contains("PersistentTradeData"))
        {
            NbtList tradePersistentData = compound.getList("PersistentTradeData", NbtElement.COMPOUND_TYPE);
            for(int i = 0; i < tradePersistentData.size() && i < this.trades.size(); ++i)
            {
                ItemTradeData trade = this.trades.get(i);
                NbtCompound ptTag = tradePersistentData.getCompound(i);
                TradeRule.loadPersistentData(ptTag, trade.getRules(), "RuleData");
            }
        }
    }

    @Override
    protected void getAdditionalContents(List<ItemStack> results) {

        //Add item storage contents
        results.addAll(this.storage.getSplitContents());

    }

    @Override
    public TradeResult ExecuteTrade(TradeContext context, int tradeIndex) {
        ItemTradeData trade = this.getTrade(tradeIndex);
        //Abort if the trade is null
        if(trade == null)
        {
            LightmansCurrency.LogError("Trade at index " + tradeIndex + " is null. Cannot execute trade!");
            return TradeResult.FAIL_INVALID_TRADE;
        }

        //Abort if the trade is not valid
        if(!trade.isValid())
        {
            LightmansCurrency.LogWarning("Trade at index " + tradeIndex + " is not a valid trade. Cannot execute trade.");
            return TradeResult.FAIL_INVALID_TRADE;
        }

        if(!context.hasPlayerReference())
            return TradeResult.FAIL_NULL;

        //Check if the player is allowed to do the trade
        if(this.runPreTradeEvent(context.getPlayerReference(), trade).isCanceled())
            return TradeResult.FAIL_TRADE_RULE_DENIAL;

        //Get the cost of the trade
        CoinValue price = this.runTradeCostEvent(context.getPlayerReference(), trade).getCostResult();

        //Process a sale
        if(trade.isSale())
        {
            //Abort if not enough items in inventory
            if(!trade.hasStock(context) && !this.isCreative())
            {
                LightmansCurrency.LogDebug("Not enough items in storage to carry out the trade at index " + tradeIndex + ". Cannot execute trade.");
                return TradeResult.FAIL_OUT_OF_STOCK;
            }

            //Abort if not enough room to put the sold item
            if(!context.canFitItems(trade.getSellItem(0), trade.getSellItem(1)))
            {
                LightmansCurrency.LogDebug("Not enough room for the output item. Aborting trade!");
                return TradeResult.FAIL_NO_OUTPUT_SPACE;
            }

            if(!context.getPayment(price))
            {
                LightmansCurrency.LogDebug("Not enough money is present for the trade at index " + tradeIndex + ". Cannot execute trade.");
                return TradeResult.FAIL_CANNOT_AFFORD;
            }

            //We have enough money, and the trade is valid. Execute the trade
            //Get the trade itemStack
            //Give the trade item
            if(!context.putItem(trade.getSellItem(0)))//If there's not enough room to give the item to the output item, abort the trade
            {
                LightmansCurrency.LogError("Not enough room for the output item. Giving refund & aborting Trade!");
                //Give a refund
                context.givePayment(price);
                return TradeResult.FAIL_NO_OUTPUT_SPACE;
            }
            if(!context.putItem(trade.getSellItem(1)))
            {
                LightmansCurrency.LogError("Not enough room for the output item. Giving refund & aborting Trade!");
                //Give a refund
                context.collectItem(trade.getSellItem(0));
                context.givePayment(price);
                return TradeResult.FAIL_NO_OUTPUT_SPACE;
            }

            //Push Notification
            this.pushNotification(() -> new ItemTradeNotification(trade, price, context.getPlayerReference(), this.getNotificationCategory()));

            //Ignore editing internal storage if this is flagged as creative.
            if(!this.isCreative())
            {
                //Remove the sold items from storage
                trade.RemoveItemsFromStorage(this.getStorage());
                this.markStorageDirty();
                //Give the paid cost to storage
                this.addStoredMoney(price);

                //Push out of stock notification
                if(!trade.hasStock(this))
                    this.pushNotification(() -> new OutOfStockNotification(this.getNotificationCategory(), tradeIndex));

            }

            //Push the post-trade event
            this.runPostTradeEvent(context.getPlayerReference(), trade, price);

            return TradeResult.SUCCESS;

        }
        //Process a purchase
        else if(trade.isPurchase())
        {
            //Abort if not enough items in the item slots
            if(!context.hasItems(trade.getSellItem(0), trade.getSellItem(1)))
            {
                LightmansCurrency.LogDebug("Not enough items in the item slots to make the purchase.");
                return TradeResult.FAIL_CANNOT_AFFORD;
            }

            //Abort if not enough room to store the purchased items (unless we're creative)
            if(!trade.hasSpace(this) && !this.isCreative())
            {
                LightmansCurrency.LogDebug("Not enough room in storage to store the purchased items.");
                return TradeResult.FAIL_NO_INPUT_SPACE;
            }
            //Abort if not enough money to pay them back
            if(!trade.hasStock(context) && !this.isCreative())
            {
                LightmansCurrency.LogDebug("Not enough money in storage to pay for the purchased items.");
                return TradeResult.FAIL_OUT_OF_STOCK;
            }
            //Passed the checks. Take the item(s) from the input slot
            context.collectItem(trade.getSellItem(0));
            context.collectItem(trade.getSellItem(1));
            //Put the payment in the purchaser's wallet, coin slot, etc.
            context.givePayment(price);

            //Push Notification
            this.pushNotification(() -> new ItemTradeNotification(trade, price, context.getPlayerReference(), this.getNotificationCategory()));

            //Ignore editing internal storage if this is flagged as creative.
            if(!this.isCreative())
            {
                //Put the item in storage
                this.getStorage().forceAddItem(trade.getSellItem(0));
                this.getStorage().forceAddItem(trade.getSellItem(1));
                this.markStorageDirty();
                //Remove the coins from storage
                this.removeStoredMoney(price);

                //Push out of stock notification
                if(!trade.hasStock(this))
                    this.pushNotification(() -> new OutOfStockNotification(this.getNotificationCategory(), tradeIndex));

            }

            //Push the post-trade event
            this.runPostTradeEvent(context.getPlayerReference(), trade, price);

            return TradeResult.SUCCESS;

        }
        //Process a barter
        else if(trade.isBarter())
        {
            //Abort if not enough items in the item slots
            if(!context.hasItems(trade.getBarterItem(0), trade.getBarterItem(1)))
            {
                LightmansCurrency.LogDebug("Not enough items in the item slots to make the barter.");
                return TradeResult.FAIL_CANNOT_AFFORD;
            }
            //Abort if not enough room to store the purchased items (unless we're creative)
            if(!trade.hasSpace(this) && !this.isCreative())
            {
                LightmansCurrency.LogDebug("Not enough room in storage to store the purchased items.");
                return TradeResult.FAIL_NO_INPUT_SPACE;
            }
            //Abort if not enough items in inventory
            if(!trade.hasStock(context) && !this.isCreative())
            {
                LightmansCurrency.LogDebug("Not enough items in storage to carry out the trade at index " + tradeIndex + ". Cannot execute trade.");
                return TradeResult.FAIL_OUT_OF_STOCK;
            }

            //Passed the checks. Take the item(s) from the input slot
            context.collectItem(trade.getBarterItem(0));
            context.collectItem(trade.getBarterItem(1));
            //Check if there's room for the new items
            if(!context.putItem(trade.getSellItem(0)))
            {
                //Abort if no room for the sold item
                LightmansCurrency.LogDebug("Not enough room for the output item. Aborting trade!");
                context.putItem(trade.getBarterItem(0));
                context.putItem(trade.getBarterItem(1));
                return TradeResult.FAIL_NO_OUTPUT_SPACE;
            }
            if(!context.putItem(trade.getSellItem(1)))
            {
                //Abort if no room for the sold item
                LightmansCurrency.LogDebug("Not enough room for the output item. Aborting trade!");
                context.collectItem(trade.getSellItem(0));
                context.putItem(trade.getBarterItem(0));
                context.putItem(trade.getBarterItem(1));
                return TradeResult.FAIL_NO_OUTPUT_SPACE;
            }

            //Push Notification
            this.pushNotification(() -> new ItemTradeNotification(trade, price, context.getPlayerReference(), this.getNotificationCategory()));

            //Ignore editing internal storage if this is flagged as creative.
            if(!this.isCreative())
            {
                //Put the item in storage
                this.getStorage().forceAddItem(trade.getBarterItem(0));
                this.getStorage().forceAddItem(trade.getBarterItem(1));
                //Remove the item from storage
                trade.RemoveItemsFromStorage(this.getStorage());
                this.markStorageDirty();

                //Push out of stock notification
                if(!trade.hasStock(this))
                    this.pushNotification(() -> new OutOfStockNotification(this.getNotificationCategory(), tradeIndex));

            }

            //Push the post-trade event
            this.runPostTradeEvent(context.getPlayerReference(), trade, price);

            return TradeResult.SUCCESS;

        }

        return TradeResult.FAIL_INVALID_TRADE;
    }

    @Override
    public void addInteractionSlots(List<InteractionSlotData> interactionSlots) { }

    @Override
    public boolean canMakePersistent() { return true; }

    @Override
    public void initStorageTabs(TraderStorageMenu menu) {
        //Storage tab
        menu.setTab(TraderStorageTab.TAB_TRADE_STORAGE, new ItemStorageTab(menu));
        //Item Trade interaction tab
        menu.setTab(TraderStorageTab.TAB_TRADE_ADVANCED, new ItemTradeEditTab(menu));
    }

    @Override
    public boolean isItemRelevant(ItemStack item) {
        for(ItemTradeData trade : this.trades)
        {
            if(trade.allowItemInStorage(item))
                return true;
        }
        return false;
    }

    @Override
    public int getStorageStackLimit() {
        int limit = DEFAULT_STACK_LIMIT;
        for(int i = 0; i < this.getUpgrades().size(); ++i)
        {
            ItemStack stack = this.getUpgrades().getStack(i);
            if(stack.getItem() instanceof UpgradeItem upgradeItem)
            {
                if(this.allowUpgrade(upgradeItem) && upgradeItem.getUpgradeType() instanceof CapacityUpgrade)
                    limit += UpgradeItem.getUpgradeData(stack).getIntValue(CapacityUpgrade.CAPACITY);
            }
        }
        return limit;
    }

}