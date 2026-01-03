package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketCastSkill {
    private final int slotNumber;

    public PacketCastSkill(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public PacketCastSkill(FriendlyByteBuf buf) {
        this.slotNumber = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slotNumber);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 1. Check if a skill is actually bound to the key pressed (1-5)
            String slotKey = "manhwamod.slot_" + slotNumber;
            if (player.getPersistentData().contains(slotKey)) {
                int skillId = player.getPersistentData().getInt(slotKey);

                // 2. Use 'current_mana' (the bar) instead of 'mana' (the permanent stat)
                int currentPool = player.getPersistentData().getInt("manhwamod.current_mana");
                int cost = player.getPersistentData().getInt("manhwamod.skill_cost_" + skillId);

                if (cost <= 0) cost = 20; // Default if not set

                // 3. The Transaction
                if (currentPool >= cost) {
                    // Take from the bar!
                    player.getPersistentData().putInt("manhwamod.current_mana", currentPool - cost);

                    // Execute the skill
                    SkillEngine.execute(player, skillId);

                    // Tell the ManaOverlay to update its blue bar
                    SystemData.sync(player);
                } else {
                    player.sendSystemMessage(Component.literal("§c[!] Not enough Mana! Need: " + cost));
                }
            } else {
                player.sendSystemMessage(Component.literal("§c[!] Slot " + slotNumber + " is empty!"));
            }
        });
        return true;
    }
}