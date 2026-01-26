package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.function.Supplier;

public class PacketIncreaseStat {
    private final String statName;
    private final int amount; // Multiplier

    public PacketIncreaseStat(String statName, int amount) {
        this.statName = statName;
        this.amount = amount;
    }

    public PacketIncreaseStat(FriendlyByteBuf buf) {
        this.statName = buf.readUtf();
        this.amount = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(statName);
        buf.writeInt(amount);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            int currentPoints = player.getPersistentData().getInt(SystemData.POINTS);
            if (currentPoints < amount) return;

            String key;
            switch (statName.toLowerCase()) {
                case "strength":     key = SystemData.STR; break;
                case "agility":      key = SystemData.SPD; break;
                case "vitality":     key = SystemData.HP;  break;
                case "defense":      key = SystemData.DEF; break;
                case "intelligence": key = SystemData.MANA; break;
                default:             key = "manhwamod." + statName.toLowerCase();
            }

            int currentVal = player.getPersistentData().getInt(key);
            player.getPersistentData().putInt(key, currentVal + amount);
            player.getPersistentData().putInt(SystemData.POINTS, currentPoints - amount);

            applyBalancedStats(player, statName.toLowerCase(), currentVal + amount);

            float pitch = (amount > 1) ? 1.2f : 1.0f;
            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, pitch);
            SystemData.sync(player);
        });
        return true;
    }

    private void applyBalancedStats(ServerPlayer player, String stat, int newVal) {
        if (stat.equals("vitality")) {
            double baseHealth = 20.0;
            double bonusHealth = newVal * 0.5;
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(baseHealth + bonusHealth);
            if (player.getHealth() < player.getMaxHealth()) player.setHealth(player.getHealth() + 0.5f);
        }
        if (stat.equals("agility")) {
            double baseSpeed = 0.10000000149011612;
            double bonusSpeed = newVal * 0.002;
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed * (1.0 + bonusSpeed));
        }
        if (stat.equals("defense")) {
            player.getAttribute(Attributes.ARMOR).setBaseValue(newVal * 0.1);
        }
    }
}