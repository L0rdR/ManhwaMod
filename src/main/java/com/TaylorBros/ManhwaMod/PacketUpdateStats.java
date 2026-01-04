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
                // 1. Get mana BEFORE the update
                int oldMana = SystemData.getMana(player);

                // 2. Map to the EXACT keys in SystemData.java
                String nbtKey;
                switch (statType) {
                    case "STR" -> nbtKey = "manhwamod.strength";
                    case "HP"  -> nbtKey = "manhwamod.health";
                    case "DEF" -> nbtKey = "manhwamod.defense";
                    case "SPD" -> nbtKey = "manhwamod.speed";
                    case "MANA"-> nbtKey = "manhwamod.mana";
                    default    -> nbtKey = "manhwamod." + statType.toLowerCase();
                }

                // 3. Update the stat
                int currentStatVal = player.getPersistentData().getInt(nbtKey);
                int newStatVal = currentStatVal + amount;
                player.getPersistentData().putInt(nbtKey, newStatVal);

                // 4. Deduct points
                SystemData.savePoints(player, currentPoints - amount);

                // 5. THE MILESTONE CHECK: Logic for 50 mana points
                if (statType.equals("MANA")) {
                    int oldMilestones = currentStatVal / 50;
                    int newMilestones = newStatVal / 50;

                    if (newMilestones > oldMilestones) {
                        // Give a skill for every 50-point gap (supports multipliers)
                        for (int i = 0; i < (newMilestones - oldMilestones); i++) {
                            int skillId = player.getRandom().nextInt(1000);
                            SystemData.unlockSkill(player, skillId, "Mana Milestone Reward", 0);
                        }
                    }
                }

                // Sync is handled inside savePoints and unlockSkill
            }
        });
        return true;
    }
}