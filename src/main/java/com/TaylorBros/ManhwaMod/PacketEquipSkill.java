package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketEquipSkill {
    private final int slotId;
    private final int skillId;

    public PacketEquipSkill(int slotId, int skillId) {
        this.slotId = slotId;
        this.skillId = skillId;
    }

    public PacketEquipSkill(FriendlyByteBuf buf) {
        this.slotId = buf.readInt();
        this.skillId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slotId);
        buf.writeInt(skillId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // SECURITY CHECK: Does the player actually own this skill in their bank?
            if (SystemData.getUnlockedSkills(player).contains(skillId)) {
                // Save to the specific slot (1-5)
                player.getPersistentData().putInt("manhwamod.slot_" + slotId, skillId);

                // Sync back to client to update the UI visuals
                SystemData.sync(player);
            }
        });
        return true;
    }
}