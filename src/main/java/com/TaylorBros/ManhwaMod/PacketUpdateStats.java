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
                // 1. Map the stat type to the correct NBT key
                String nbtKey = "manhwamod." + statType.toLowerCase();

                // SPECIAL CASE: Ensure HP maps to the correct key used in SystemData
                if (statType.equalsIgnoreCase("HP")) {
                    nbtKey = "manhwamod.healthstat";
                }

                // 2. Update the specific stat in NBT
                int currentVal = player.getPersistentData().getInt(nbtKey);
                player.getPersistentData().putInt(nbtKey, currentVal + amount);

                // 3. Deduct points
                SystemData.savePoints(player, currentPoints - amount);

                // 4. Random skill unlock logic
                if (player.getRandom().nextFloat() < 0.1f) {
                    int skillNumber = player.getRandom().nextInt(1000);
                    String fullRecipe = "Random Skill:" + skillNumber + ":Generated Power";
                    int randomCost = 10 + player.getRandom().nextInt(40);
                    SystemData.unlockSkill(player, skillNumber, fullRecipe, randomCost);
                }

                // 5. THE MISSING LINK: Sync the data to the client
                // This tells the Screen to refresh the numbers you see!
                SystemData.sync(player);
            }
        });
        return true;
    }
}