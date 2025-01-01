package io.github.lightman314.lightmanscurrency.network.server.messages.wallet;

import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.items.WalletItem;
import io.github.lightman314.lightmanscurrency.common.money.wallet.WalletHandler;
import io.github.lightman314.lightmanscurrency.integration.trinketsapi.LCTrinketsAPI;
import io.github.lightman314.lightmanscurrency.network.LazyPacketData;
import io.github.lightman314.lightmanscurrency.network.client.messages.data.SMessageUpdateClientWallet;
import io.github.lightman314.lightmanscurrency.network.server.ClientToServerPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class CMessageEquipWalletFromHand extends ClientToServerPacket {

    public static final Identifier PACKET_ID = new Identifier(LightmansCurrency.MODID, "equip_wallet_from_hand");

    public CMessageEquipWalletFromHand() { super(PACKET_ID); }
    
    @Override
    protected void encode(LazyPacketData.Builder dataBuilder) { 
        // No data needed for this message
    }

    public static void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, LazyPacketData data, PacketSender responseSender) {
        // Only allow when Trinkets is disabled to prevent conflicts
        if(LCTrinketsAPI.isActive())
            return;
            
        ItemStack heldWallet = player.getStackInHand(Hand.MAIN_HAND);
        if(!WalletItem.isWallet(heldWallet))
            return;
            
        WalletHandler walletHandler = WalletHandler.getWallet(player);
        if(!walletHandler.getWallet().isEmpty())
            return; // Already has a wallet equipped
            
        // Equip the wallet using built-in system only
        walletHandler.setWalletBuiltIn(heldWallet);
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        
        // Sync to client
        new SMessageUpdateClientWallet(player.getUuid(), walletHandler).sendToAll();
        walletHandler.clean();
        
        LightmansCurrency.LogDebug("Player " + player.getName().getString() + " equipped wallet via keybind");
    }
}
