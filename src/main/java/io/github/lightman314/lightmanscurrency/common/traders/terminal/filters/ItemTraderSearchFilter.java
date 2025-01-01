package io.github.lightman314.lightmanscurrency.common.traders.terminal.filters;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.lightman314.lightmanscurrency.common.traders.TraderData;
import io.github.lightman314.lightmanscurrency.common.traders.item.ItemTraderData;
import io.github.lightman314.lightmanscurrency.common.traders.item.tradedata.ItemTradeData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;

public class ItemTraderSearchFilter extends TraderSearchFilter{

    @Override
    public boolean filter(TraderData data, String searchText) {

        //Search the items being sold
        if(data instanceof ItemTraderData)
        {
            List<ItemTradeData> trades = ((ItemTraderData)data).getTradeData();
            for(int i = 0; i < trades.size(); i++)
            {
                if(trades.get(i).isValid())
                {
                    ItemStack sellItem = trades.get(i).getSellItem(0);
                    ItemStack sellItem2 = trades.get(i).getSellItem(1);
                    //Search item name
                    if(sellItem.getName().getString().toLowerCase().contains(searchText))
                        return true;
                    if(sellItem2.getName().getString().toLowerCase().contains(searchText))
                        return true;
                    //Search custom name
                    if(trades.get(i).getCustomName(0).toLowerCase().contains(searchText))
                        return true;
                    if(trades.get(i).getCustomName(1).toLowerCase().contains(searchText))
                        return true;
                    //Search enchantments
                    AtomicBoolean foundEnchantment = new AtomicBoolean(false);
                    EnchantmentHelper.get(sellItem).forEach((enchantment, level) ->{
                        if(enchantment.getName(level).getString().toLowerCase().contains(searchText))
                            foundEnchantment.set(true);
                    });
                    EnchantmentHelper.get(sellItem2).forEach((enchantment, level) ->{
                        if(enchantment.getName(level).getString().toLowerCase().contains(searchText))
                            foundEnchantment.set(true);
                    });
                    if(foundEnchantment.get())
                        return true;

                    //Check the barter item if applicable
                    if(trades.get(i).isBarter())
                    {
                        ItemStack barterItem = trades.get(i).getBarterItem(0);
                        ItemStack barterItem2 = trades.get(i).getBarterItem(1);
                        //Search item name
                        if(barterItem.getName().getString().toLowerCase().contains(searchText))
                            return true;
                        if(barterItem2.getName().getString().toLowerCase().contains(searchText))
                            return true;
                        //Search enchantments
                        foundEnchantment.set(false);
                        EnchantmentHelper.get(barterItem).forEach((enchantment, level) ->{
                            if(enchantment.getName(level).getString().toLowerCase().contains(searchText))
                                foundEnchantment.set(true);
                        });
                        EnchantmentHelper.get(barterItem2).forEach((enchantment, level) ->{
                            if(enchantment.getName(level).getString().toLowerCase().contains(searchText))
                                foundEnchantment.set(true);
                        });
                        if(foundEnchantment.get())
                            return true;
                    }
                }
            }
        }
        return false;
    }

}