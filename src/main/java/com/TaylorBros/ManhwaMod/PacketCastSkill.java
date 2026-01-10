package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.List;

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

            // 1. Get the Skill ID from the slot
            int skillId = player.getPersistentData().getInt(SystemData.SLOT_PREFIX + this.slotId);
            if (skillId <= 0) return;

            // 2. Get the recipe and cost
            String recipe = player.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);
            int cost = player.getPersistentData().getInt(SystemData.COST_PREFIX + skillId);
            int currentMana = SystemData.getCurrentMana(player);

            if (currentMana >= cost) {
                // 3. Deduct Mana
                SystemData.saveCurrentMana(player, currentMana - cost);

                // 4. USE THE ENGINE (This fixes particles and full names)
                SkillEngine.execute(player, skillId);
                // --- NEW: Record Cooldown Data ---
                // We use world time (ticks). 100 ticks = 5 seconds.
                player.getPersistentData().putLong(SystemData.LAST_USE_PREFIX + this.slotId, player.level().getGameTime());
                player.getPersistentData().putInt(SystemData.COOLDOWN_PREFIX + this.slotId, 100);

                SystemData.sync(player); // Sync to client so HUD sees the update

                // 5. Send the correct full-name message
                String fullName = SkillEngine.getSkillName(recipe);
                player.displayClientMessage(Component.literal("§b§l> §fCasting: §6" + fullName), true);
            } else {
                player.displayClientMessage(Component.literal("§cNot enough Mana!"), true);
            }
        });
        return true;
    }
}