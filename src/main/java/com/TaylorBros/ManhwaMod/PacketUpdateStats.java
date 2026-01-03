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
                // 1. Get the current mana BEFORE updating
                int oldMana = SystemData.getMana(player);

                // 2. Map and update the stat (Same logic as before)
                String nbtKey;
                switch (statType) {
                    case "STR" -> nbtKey = "manhwamod.strength";
                    case "HP"  -> nbtKey = "manhwamod.health";
                    case "DEF" -> nbtKey = "manhwamod.defense";
                    case "SPD" -> nbtKey = "manhwamod.speed";
                    case "MANA"-> nbtKey = "manhwamod.mana";
                    default    -> nbtKey = "manhwamod." + statType.toLowerCase();
                }

                int newVal = oldMana + (statType.equals("MANA") ? amount : 0);
                int currentStatVal = player.getPersistentData().getInt(nbtKey);
                player.getPersistentData().putInt(nbtKey, currentStatVal + amount);

                // 3. Deduct points
                SystemData.savePoints(player, currentPoints - amount);

                // 4. THE MILESTONE CHECK: Did they cross a multiple of 50?
                if (statType.equals("MANA")) {
                    int milestonesReached = (currentStatVal + amount) / 50;
                    int oldMilestones = currentStatVal / 50;

                    if (milestonesReached > oldMilestones) {
                        // Unlock a skill for every 50-point threshold crossed
                        for (int i = 0; i < (milestonesReached - oldMilestones); i++) {
                            int skillId = player.getRandom().nextInt(1000);
                            SystemData.unlockSkill(player, skillId, "Mana Milestone Reward", 0);
                        }
                    }
                }
                SystemData.sync(player);
            }
        });
        return true;
    }
}