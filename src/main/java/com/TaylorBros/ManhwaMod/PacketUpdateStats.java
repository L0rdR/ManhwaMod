package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketUpdateStats {
    private final int amount;
    private final String statType;

    public PacketUpdateStats(int amount, String statType) {
        this.amount = amount;
        this.statType = statType;
    }

    public PacketUpdateStats(FriendlyByteBuf buf) {
        this.amount = buf.readInt();
        this.statType = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(amount);
        buf.writeUtf(statType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            int currentPoints = SystemData.getPoints(player);
            if (currentPoints >= amount) {
                // 1. Manually map the statType to the exact keys in SystemData.java
                String nbtKey;
                switch (statType) {
                    case "STR" -> nbtKey = "manhwamod.strength";
                    case "HP"  -> nbtKey = "manhwamod.health";
                    case "DEF" -> nbtKey = "manhwamod.defense";
                    case "SPD" -> nbtKey = "manhwamod.speed";
                    case "MANA"-> nbtKey = "manhwamod.mana";
                    default    -> nbtKey = "manhwamod." + statType.toLowerCase();
                }

                // 2. Update the stat using the correct key
                int currentVal = player.getPersistentData().getInt(nbtKey);
                player.getPersistentData().putInt(nbtKey, currentVal + amount);

                // 3. Deduct points
                SystemData.savePoints(player, currentPoints - amount);

                // 4. Random skill logic (unchanged)
                if (player.getRandom().nextFloat() < 0.1f) {
                    int skillNumber = player.getRandom().nextInt(1000);
                    String fullRecipe = "Random Skill:" + skillNumber + ":Generated Power";
                    int randomCost = 10 + player.getRandom().nextInt(40);
                    SystemData.unlockSkill(player, skillNumber, fullRecipe, randomCost);
                }

                // 5. Sync the data so the Screen updates
                SystemData.sync(player);
            }
        });
        return true;
    }
}