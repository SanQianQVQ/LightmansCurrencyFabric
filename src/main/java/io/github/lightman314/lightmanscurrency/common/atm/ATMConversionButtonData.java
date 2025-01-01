package io.github.lightman314.lightmanscurrency.common.atm;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.atm.icons.ItemIcon;
import io.github.lightman314.lightmanscurrency.common.atm.icons.SimpleArrowIcon;
import io.github.lightman314.lightmanscurrency.common.core.ModItems;
import net.minecraft.item.ItemConvertible;

public class ATMConversionButtonData {

    public final int xPos;
    public final int yPos;
    public final int width;
    public final String command;
    private final List<ATMIconData> icons;
    public ImmutableList<ATMIconData> getIcons() { return ImmutableList.copyOf(this.icons); }

    public static ATMConversionButtonData parse(JsonObject data) throws Exception { return new ATMConversionButtonData(data); }

    private ATMConversionButtonData(JsonObject data) throws Exception {
        this.xPos = data.get("x").getAsInt();
        this.yPos = data.get("y").getAsInt();
        this.width = data.get("width").getAsInt();
        this.command = data.get("command").getAsString();

        this.icons = new ArrayList<>();
        if(data.has("icons"))
        {
            JsonArray iconListData = data.getAsJsonArray("icons");
            for(int i = 0; i < iconListData.size(); ++i)
            {
                try {
                    JsonObject iconData = iconListData.get(i).getAsJsonObject();
                    this.icons.add(ATMIconData.parse(iconData));
                } catch(Exception e) { LightmansCurrency.LogError("Error parsing ATM Icon #" + String.valueOf(i + 1) + ".", e);}
            }
        }
        else
        {
            LightmansCurrency.LogWarning("ATM Button Data has no 'icons' entry. Button will be blank.");
        }
    }

    public ATMConversionButtonData(int xPos, int yPos, int width, String command, List<ATMIconData> icons) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.width = width;
        this.command = command;
        this.icons = icons;
    }

    public JsonObject save() {
        JsonObject data = new JsonObject();

        data.addProperty("x", this.xPos);
        data.addProperty("y", this.yPos);
        data.addProperty("width", this.width);
        data.addProperty("command", this.command);

        JsonArray iconListData = new JsonArray();
        for(int i = 0; i < this.icons.size(); ++i)
            iconListData.add(this.icons.get(i).save());
        data.add("icons", iconListData);

        return data;
    }

    public static List<ATMConversionButtonData> generateDefault() {
        return Lists.newArrayList(
                //Convert All
                convertAllUpDefault(5,34),
                convertAllDownDefault(89,34),
                //Copper <-> Iron
                convertSingle(6, 61, ModItems.COIN_IRON, ModItems.COIN_COPPER, "convertDown-lightmanscurrency:coin_iron"),
                convertSingle(6, 88, ModItems.COIN_COPPER, ModItems.COIN_IRON, "convertUp-lightmanscurrency:coin_copper"),
                //Iron <-> Gold
                convertSingle(41, 61, ModItems.COIN_GOLD, ModItems.COIN_IRON, "convertDown-lightmanscurrency:coin_gold"),
                convertSingle(41, 88, ModItems.COIN_IRON, ModItems.COIN_GOLD, "convertUp-lightmanscurrency:coin_iron"),
                //Gold <-> Emerald
                convertSingle(75, 61, ModItems.COIN_EMERALD, ModItems.COIN_GOLD, "convertDown-lightmanscurrency:coin_emerald"),
                convertSingle(75, 88, ModItems.COIN_GOLD, ModItems.COIN_EMERALD, "convertUp-lightmanscurrency:coin_gold"),
                //Emerald <-> Diamond
                convertSingle(109, 61, ModItems.COIN_DIAMOND, ModItems.COIN_EMERALD, "convertDown-lightmanscurrency:coin_diamond"),
                convertSingle(109, 88, ModItems.COIN_EMERALD, ModItems.COIN_DIAMOND, "convertUp-lightmanscurrency:coin_emerald"),
                //Diamond <-> Netherite
                convertSingle(144, 61, ModItems.COIN_NETHERITE, ModItems.COIN_DIAMOND, "convertDown-lightmanscurrency:coin_netherite"),
                convertSingle(144, 88, ModItems.COIN_DIAMOND, ModItems.COIN_NETHERITE, "convertUp-lightmanscurrency:coin_diamond")
        );
    }

    private static ATMConversionButtonData convertAllUpDefault(int x, int y) {
        return new ATMConversionButtonData(x, y, 82, "convertAllUp",
                Lists.newArrayList(
                        new ItemIcon(-2,1,ModItems.COIN_COPPER),
                        new SimpleArrowIcon(10,6,SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(12,1,ModItems.COIN_IRON),
                        new SimpleArrowIcon(24,6,SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(26,1,ModItems.COIN_GOLD),
                        new SimpleArrowIcon(38,6,SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(40,1,ModItems.COIN_EMERALD),
                        new SimpleArrowIcon(52,6,SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(54,1,ModItems.COIN_DIAMOND),
                        new SimpleArrowIcon(66,6, SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(68,1,ModItems.COIN_NETHERITE)
                )
        );
    }

    private static ATMConversionButtonData convertAllDownDefault(int x, int y) {
        return new ATMConversionButtonData(x, y, 82, "convertAllDown",
                Lists.newArrayList(
                        new ItemIcon(-2,1,ModItems.COIN_NETHERITE),
                        new SimpleArrowIcon(10,6,SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(12,1,ModItems.COIN_DIAMOND),
                        new SimpleArrowIcon(24,6,SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(26,1,ModItems.COIN_EMERALD),
                        new SimpleArrowIcon(38,6,SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(40,1,ModItems.COIN_GOLD),
                        new SimpleArrowIcon(52,6,SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(54,1,ModItems.COIN_IRON),
                        new SimpleArrowIcon(66,6,SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(68,1,ModItems.COIN_COPPER)
                )
        );
    }

    private static ATMConversionButtonData convertSingle(int x, int y, ItemConvertible from, ItemConvertible to, String command) {
        return new ATMConversionButtonData(x, y, 26, command,
                Lists.newArrayList(
                        new ItemIcon(-2,1,from),
                        new SimpleArrowIcon(10,6,SimpleArrowIcon.ArrowType.RIGHT),
                        new ItemIcon(12,1,to)
                )
        );
    }

}