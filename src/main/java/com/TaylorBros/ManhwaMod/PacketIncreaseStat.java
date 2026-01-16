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

    public PacketIncreaseStat(String statName) { this.statName = statName; }
    public PacketIncreaseStat(FriendlyByteBuf buf) { this.statName = buf.readUtf(); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeUtf(statName); }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            int currentPoints = player.getPersistentData().getInt("manhwamod.stat_points");
            if (currentPoints <= 0) return;

            String key;
            switch (statName.toLowerCase()) {
                case "strength":     key = SystemData.STR; break;
                case "agility":      key = SystemData.SPD; break;
                case "vitality":     key = SystemData.HP;  break;
                case "defense":      key = SystemData.DEF; break; // NEW
                case "intelligence": key = "manhwamod.intelligence"; break;
                default:             key = "manhwamod." + statName.toLowerCase();
            }

            int currentVal = player.getPersistentData().getInt(key);
            player.getPersistentData().putInt(key, currentVal + 1);
            player.getPersistentData().putInt("manhwamod.stat_points", currentPoints - 1);

            applyBalancedStats(player, statName.toLowerCase(), currentVal + 1);

            player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
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
            double bonusSpeed = newVal * 0.0005;
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(baseSpeed * (1.0 + bonusSpeed));
        }
        if (stat.equals("intelligence")) {
            int baseMana = 100;
            int bonusMana = newVal * 2;
            player.getPersistentData().putInt(SystemData.MANA, baseMana + bonusMana);
        }
        // DEFENSE: 0.1 Armor per point. (10 Points = 1 Armor Icon)
        if (stat.equals("defense")) {
            // Vanilla cap is complicated, but base armor adds up.
            // 1000 Def = 100 Armor. (Full diamond is 20).
            player.getAttribute(Attributes.ARMOR).setBaseValue(newVal * 0.1);
        }
    }
}