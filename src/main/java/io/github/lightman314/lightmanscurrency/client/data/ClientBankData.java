package io.github.lightman314.lightmanscurrency.client.data;

import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.money.bank.BankAccount;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class ClientBankData {

    private static final Map<UUID, BankAccount> loadedBankAccounts = new HashMap<>();
    private static BankAccount.AccountReference lastSelectedAccount = null;

    public static BankAccount GetPlayerBankAccount(UUID playerID)
    {
        if(loadedBankAccounts.containsKey(playerID))
            return loadedBankAccounts.get(playerID);
        //Return an empty account until the server notifies us of the new accounts creation.
        LightmansCurrency.LogWarning("No bank account for player with id " + playerID.toString() + " is present on the client.");
        return new BankAccount();
    }

    public static void InitBankAccounts(Map<UUID,BankAccount> bankAccounts)
    {
        loadedBankAccounts.clear();
        bankAccounts.forEach((id,account) -> loadedBankAccounts.put(id, account));
    }

    public static void UpdateBankAccount(NbtCompound compound)
    {
        try {
            UUID owner = compound.getUuid("Player");
            BankAccount account = new BankAccount(compound);
            if(owner != null && account != null)
                loadedBankAccounts.put(owner, account);
        } catch(Exception e) { e.printStackTrace(); }
    }

    public static void UpdateLastSelectedAccount(BankAccount.AccountReference reference) { lastSelectedAccount = reference; }

    public static BankAccount.AccountReference GetLastSelectedAccount() { return lastSelectedAccount; }

    public static void onClientLogout(ClientPlayNetworkHandler handler, MinecraftClient client) {
        loadedBankAccounts.clear();
        lastSelectedAccount = null;
    }

}