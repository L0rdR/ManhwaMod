package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketCastSkill {
    private final int slotId;

    public PacketCastSkill(int slotId) { this.slotId = slotId; }
    public PacketCastSkill(FriendlyByteBuf buf) { this.slotId = buf.readInt(); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeInt(slotId); }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Using Rule 1 Constants
            int skillId = player.getPersistentData().getInt(SystemData.SLOT_PREFIX + this.slotId);
            if (skillId <= 0) return;

            int cost = player.getPersistentData().getInt(SystemData.COST_PREFIX + skillId);
            int currentMana = SystemData.getCurrentMana(player);

            // Rule 2: Logic first (Mana Check)
            if (currentMana >= cost) {
                SystemData.saveCurrentMana(player, currentMana - cost);
                executeSkillEffect(player, skillId);
            } else {
                player.displayClientMessage(Component.literal("§cNot enough Mana!"), true);
            }
        });
        return true;
    }

    private void executeSkillEffect(ServerPlayer player, int skillId) {
        String recipe = player.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);
        if (recipe.isEmpty()) return;

        String name = recipe.split(":")[0].toUpperCase();
        ServerLevel level = player.serverLevel();

        // Rule 3: Fail-safe dispatcher
        if (name.contains("FIRE")) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.5, 0.5, 0.1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else if (name.contains("ICE") || name.contains("FROST")) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.5, 0.5, 0.05);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.2f);
        } else {
            // Fallback
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(), 25, 0.5, 0.5, 0.5, 0.1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
        }

        player.displayClientMessage(Component.literal("§b§l> §fCasting: §6" + name), true);
    }
}