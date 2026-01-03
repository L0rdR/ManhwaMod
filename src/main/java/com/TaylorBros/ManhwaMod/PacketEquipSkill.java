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
            // Inside PacketEquipSkill.java handle method:
            ServerPlayer player = context.getSender();
            if (player != null) {
                // CASCADE: This now correctly finds the method in SystemData
                if (SystemData.getUnlockedSkills(player).contains(skillId)) {
                    player.getPersistentData().putInt(SystemData.SLOT_PREFIX + slot, skillId);
                    SystemData.sync(player);
                }
            }
        return true;
    }
}