package io.github.lightman314.lightmanscurrency.common.money;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import io.github.lightman314.lightmanscurrency.LCConfig;
import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class CoinData
{
    //Coin item
    public final Item coinItem;
    //Coins chain id
    public final String chain;
    //Value inputs
    public final Item worthOtherCoin;
    public final int worthOtherCoinCount;
    //Coin's display initial 'c','d', etc.
    private final String initialTranslation;
    //Coin's plural form
    private final String pluralTranslation;
    //Is this hidden or not
    public final boolean isHidden;

    private CoinData(Builder builder)
    {
        this.coinItem = builder.coinItem;
        this.chain = builder.chain;
        this.worthOtherCoin = builder.worthOtherCoin;
        this.worthOtherCoinCount = builder.worthOtherCoinCount;
        this.initialTranslation = builder.initialTranslation;
        this.pluralTranslation = builder.pluralTranslation;
        this.isHidden = builder.isHidden;
    }

    public long getValue()
    {
        return getValue(MoneyUtil.getMoneyData());
    }

    public long getValue(MoneyData moneyData)
    {
        if(moneyData == null)
            return 0;
        if(!this.convertsDownwards())
        {
            //LightmansCurrency.LOGGER.info("CoinData.getValue() returning 1 due to being a bottom-coin.");
            return 1;
        }
        CoinData otherCoinData = moneyData.getData(this.worthOtherCoin);
        if(otherCoinData != null)
        {
            //LightmansCurrency.LOGGER.info("CoinData.getValue() calculated value of " + this.worthOtherCoinCount * otherCoinData.getValue() + ".");
            return this.worthOtherCoinCount * otherCoinData.getValue(moneyData);
        }
        else
        {
            LightmansCurrency.LogError("CoinData.getValue() for " + Registries.ITEM.getId(this.coinItem) + " returning 1 due it's dependent coin (" + Registries.ITEM.getId(this.worthOtherCoin) + ") not being registered.");
            return 1;
        }
    }

    /**
     * Gets the display value in double format
     */
    public double getDisplayValue()
    {
        double coreValue = this.getValue();
        double baseValue = MoneyUtil.getValue(LCConfig.SERVER.valueBaseCoin.get());
        //LightmansCurrency.LogInfo("Core Value of " + this.getCoinItem().getRegistryName() + "=" + coreValue + "\nBase Value of " + Config.getBaseCoinItem().getRegistryName() + "=" + baseValue + "\nDisplay Value: " + coreValue/baseValue);
        return coreValue / baseValue;
    }

    public boolean convertsDownwards()
    {
        return this.worthOtherCoin != null && this.worthOtherCoinCount > 0;
    }

    public Pair<Item,Integer> getDownwardConversion()
    {
        return new Pair<>(this.worthOtherCoin, this.worthOtherCoinCount);
    }

    public MutableText getInitial()
    {
        if(this.initialTranslation != null && !this.initialTranslation.isBlank())
            return Text.translatable(this.initialTranslation);
        //LightmansCurrency.LogWarning("No initial found for the coin '" + this.coinItem.getRegistryName().toString() + "'.");
        return Text.literal(new ItemStack(this.coinItem).getName().getString().substring(0,1).toLowerCase());
    }

    public MutableText getPlural() {
        //Get plural form
        if(this.pluralTranslation != null && !this.pluralTranslation.isBlank())
            return Text.translatable(this.pluralTranslation);
        return MoneyUtil.getDefaultPlural(this.coinItem);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("coinitem", Registries.ITEM.getId(this.coinItem).toString());
        json.addProperty("chain", this.chain);
        if(this.worthOtherCoin != null && this.worthOtherCoinCount > 0)
        {
            JsonObject worth = new JsonObject();
            worth.addProperty("coin", Registries.ITEM.getId(this.worthOtherCoin).toString());
            worth.addProperty("count", this.worthOtherCoinCount);
            json.add("worth", worth);
        }
        if(this.initialTranslation != null && !this.initialTranslation.isBlank())
            json.addProperty("initial", this.initialTranslation);
        if(this.pluralTranslation != null && !this.pluralTranslation.isBlank())
            json.addProperty("plural", this.pluralTranslation);
        if(this.isHidden)
            json.addProperty("hidden", true);

        return json;
    }

    public static Builder getBuilder(ItemConvertible coinItem, String chain)
    {
        return new Builder(coinItem.asItem(), chain);
    }

    public static Builder getBuilder(JsonObject json)
    {
        //Coin Item
        Item coinItem = Registries.ITEM.get(new Identifier(json.get("coinitem").getAsString()));
        String chain = json.get("chain").getAsString();
        Builder builder = new Builder(coinItem, chain);
        //Relative Worth
        if(json.has("worth"))
        {
            JsonObject worthData = json.get("worth").getAsJsonObject();
            Item otherCoin = Registries.ITEM.get(new Identifier(worthData.get("coin").getAsString()));
            int count = worthData.get("count").getAsInt();
            builder.defineConversion(otherCoin, count);
        }
        //Initial
        if(json.has("initial"))
            builder.defineInitial(json.get("initial").getAsString());
        //Plural
        if(json.has("plural"))
            builder.definePluralForm(json.get("plural").getAsString());
        //Hidden
        if(json.has("hidden") && json.get("hidden").getAsBoolean())
            builder.setHidden();
        return builder;
    }

    public static class Builder
    {

        //The coin's item
        final Item coinItem;
        //The coin's conversion chain
        final String chain;
        //Defines its worth based on another coin's value
        Item worthOtherCoin = null;
        int worthOtherCoinCount = 0;
        //The shortened name of the coin
        String initialTranslation = "";
        //The plural name of the coin
        String pluralTranslation = "";

        //Whether it's publicly visible
        boolean isHidden = false;


        public Builder(@NotNull Item coinItem, String chain)
        {
            this.coinItem = coinItem;
            this.chain = chain;
        }

        /**
         * Defines what lesser coin can be converted into this one, and how many of those coins are worth 1 of this coin.
         */
        public Builder defineConversion(ItemConvertible otherCoin, int coinAmount)
        {
            this.worthOtherCoin = otherCoin.asItem();
            this.worthOtherCoinCount = coinAmount;
            if(this.worthOtherCoinCount > new ItemStack(this.worthOtherCoin).getMaxCount())
            {
                this.worthOtherCoinCount = new ItemStack(this.worthOtherCoin).getMaxCount();
                LightmansCurrency.LogError("Coin conversion for '" + Registries.ITEM.getId(this.coinItem).toString() + "' is larger than 1 stack of '" + Registries.ITEM.getId(this.worthOtherCoin).toString() + "'\nValue will be shrunk to " + this.worthOtherCoinCount);
            }
            return this;
        }

        /**
         * Defines the coins initial used in displaying the short form of a price/value;
         */
        public Builder defineInitial(String translationString)
        {
            this.initialTranslation = translationString;
            return this;
        }

        /**
         * Defines the coins plural name, used in displaying the tooltip.
         * Required as some languages have significant name changes when making an items name plural.
         */
        public Builder definePluralForm(String translationString) {
            this.pluralTranslation = translationString;
            return this;
        }

        public Builder setHidden()
        {
            this.isHidden = true;
            return this;
        }

        public CoinData build()
        {
            return new CoinData(this);
        }
    }

}