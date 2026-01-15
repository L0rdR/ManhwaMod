package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketIncreaseStat {
    private final String statName;

    public PacketIncreaseStat(String statName) { this.statName = statName; }
    public PacketIncreaseStat(FriendlyByteBuf buf) { this.statName = buf.readUtf(); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeUtf(statName); }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 1. Check if player has points
            int currentPoints = player.getPersistentData().getInt("manhwamod.stat_points");
            if (currentPoints <= 0) return;

            // 2. Identify which stat to boost
            String key = "manhwamod." + statName.toLowerCase();
            int currentVal = player.getPersistentData().getInt(key);

            // 3. Apply changes (Cost 1 point, Gain 1 stat)
            player.getPersistentData().putInt(key, currentVal + 1);
            player.getPersistentData().putInt("manhwamod.stat_points", currentPoints - 1);

            // 4. Sync and Notify
            SystemData.sync(player);
            // Optional: Play a small "ding" sound to confirm
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
        });
        return true;
    }
}