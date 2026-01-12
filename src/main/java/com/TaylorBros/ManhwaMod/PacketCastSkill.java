package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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

            // 1. Get the Skill ID from the slot
            int skillId = player.getPersistentData().getInt(SystemData.SLOT_PREFIX + this.slotId);
            if (skillId <= 0) return;

            // ---------------------------------------------------------
            // 2. CHECK COOLDOWN (New Logic)
            // ---------------------------------------------------------
            long lastUse = player.getPersistentData().getLong(SystemData.LAST_USE_PREFIX + this.slotId);
            int cooldownTime = player.getPersistentData().getInt(SystemData.COOLDOWN_PREFIX + this.slotId);
            long timePassed = player.level().getGameTime() - lastUse;

            // If time passed is less than the required cooldown, stop here.
            if (timePassed < cooldownTime) {
                return;
            }
            // ---------------------------------------------------------

            // 3. Get the recipe and cost
            String recipe = player.getPersistentData().getString(SystemData.RECIPE_PREFIX + skillId);
            int cost = player.getPersistentData().getInt(SystemData.COST_PREFIX + skillId);
            int currentMana = SystemData.getCurrentMana(player);

            if (currentMana >= cost) {
                // 4. Deduct Mana
                SystemData.saveCurrentMana(player, currentMana - cost);

                // 5. USE THE ENGINE
                SkillEngine.execute(player, skillId);

                // 6. Record New Cooldown Data
                // Currently hardcoded to 100 ticks (5 seconds) as per your previous code
                player.getPersistentData().putLong(SystemData.LAST_USE_PREFIX + this.slotId, player.level().getGameTime());
                player.getPersistentData().putInt(SystemData.COOLDOWN_PREFIX + this.slotId, 100);

                SystemData.sync(player);

                // 7. Send success message
                String fullName = SkillEngine.getSkillName(recipe);
                player.displayClientMessage(Component.literal("§b§l> §fCasting: §6" + fullName), true);
            } else {
                player.displayClientMessage(Component.literal("§cNot enough Mana!"), true);
            }
        });
        return true;
    }
}