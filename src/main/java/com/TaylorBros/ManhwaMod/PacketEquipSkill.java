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
            if (player != null) {
                // Simplified check: if skillId is 0 (clearing) or valid ID
                if (skillId >= 0) {
                    player.getPersistentData().putInt(SystemData.SLOT_PREFIX + slotId, skillId);
                    SystemData.sync(player);
                }
            }
        });
        return true;
    }
}