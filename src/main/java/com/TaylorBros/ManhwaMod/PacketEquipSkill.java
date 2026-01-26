package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.List;

public class PacketEquipSkill {
    private final int slotId; // 0 to 4
    private final int skillId; // The Unique ID (e.g., 10452)

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

            // 1. Verify Ownership
            List<Integer> unlocked = SystemData.getUnlockedSkills(player);
            if (!unlocked.contains(skillId)) {
                player.sendSystemMessage(Component.literal("§cError: You do not possess this art."));
                return;
            }

            // 2. Equip
            if (slotId >= 0 && slotId < 5) {
                player.getPersistentData().putInt(SystemData.SLOT_PREFIX + slotId, skillId);
                // 3. Reset Cooldown for that slot to prevent glitches
                player.getPersistentData().putLong(SystemData.LAST_USE_PREFIX + slotId, 0);
                SystemData.sync(player);
            }
        });
        return true;
    }
}