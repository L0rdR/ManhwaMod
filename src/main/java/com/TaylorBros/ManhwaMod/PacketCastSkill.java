package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketCastSkill {
    private final int slotId;

    public PacketCastSkill(int slotId) {
        this.slotId = slotId;
    }

    public PacketCastSkill(FriendlyByteBuf buf) {
        this.slotId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slotId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 1. Get the skill ID from the specific slot (1-5)
            int skillId = player.getPersistentData().getInt("manhwamod.slot_" + this.slotId);
            if (skillId <= 0) return;

            // 2. Retrieve cost and current mana
            int cost = player.getPersistentData().getInt("manhwamod.skill_cost_" + skillId);
            int currentMana = SystemData.getCurrentMana(player);

            if (currentMana >= cost) {
                // 3. TRANSACTION: Drain the Mana
                SystemData.saveCurrentMana(player, currentMana - cost);

                // 4. EXECUTION: Trigger the visual/physical effect
                executeSkillEffect(player, skillId);
            } else {
                player.displayClientMessage(Component.literal("§cNot enough Mana!"), true);
            }
        });
        return true;
    }

    private void executeSkillEffect(ServerPlayer player, int skillId) {
        // Get the recipe string (e.g., "FIRE BLAST:COMMON:...")
        String recipe = player.getPersistentData().getString("manhwamod.skill_recipe_" + skillId);
        if (recipe.isEmpty()) return;

        // Extract the name and make it uppercase for easy matching
        String name = recipe.split(":")[0].toUpperCase();
        Level level = player.level();

        // Dispatcher: Determine effect based on name keywords
        if (name.contains("FIRE")) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        else if (name.contains("ICE") || name.contains("FROST")) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.5, 0.5, 0.05);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
        else if (name.contains("MANA") || name.contains("ARCANE")) {
            level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(), 40, 0.7, 0.7, 0.7, 0.2);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
        else {
            // Generic fallback effect
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5f, 1.0f);
        }

        // Send confirmation to the hotbar
        player.displayClientMessage(Component.literal("§b§l> §fCasting: §6" + name), true);
    }
}