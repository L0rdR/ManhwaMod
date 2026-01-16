package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketEquipSkill {
    private final int unlockedIndex; // Index in the "unlocked" list (0, 1, 2...)
    private final int slotId;        // Hotbar slot (0-4)

    public PacketEquipSkill(int unlockedIndex, int slotId) {
        this.unlockedIndex = unlockedIndex;
        this.slotId = slotId;
    }

    public PacketEquipSkill(FriendlyByteBuf buf) {
        this.unlockedIndex = buf.readInt();
        this.slotId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(unlockedIndex);
        buf.writeInt(slotId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 1. Validate that the player actually owns this index
            int total = player.getPersistentData().getInt("manhwamod.total_unlocked");
            if (unlockedIndex >= total || unlockedIndex < 0) return;

            // 2. Retrieve the GENERATED data from the library
            String recipe = player.getPersistentData().getString("manhwamod.unlocked_skill_" + unlockedIndex);
            int cost = player.getPersistentData().getInt("manhwamod.unlocked_cost_" + unlockedIndex);

            // 3. Create a unique ID for this slot assignment
            // (We add 1000 to distinct it from standard IDs, though strictly distinct IDs aren't critical here)
            int equipId = 1000 + unlockedIndex;

            // 4. Save to the Hotbar Slot
            player.getPersistentData().putInt(SystemData.SLOT_PREFIX + slotId, equipId);
            player.getPersistentData().putString(SystemData.RECIPE_PREFIX + equipId, recipe);
            player.getPersistentData().putInt(SystemData.COST_PREFIX + equipId, cost);

            // 5. Reset Cooldown for that slot
            player.getPersistentData().putLong(SystemData.LAST_USE_PREFIX + slotId, 0);
            player.getPersistentData().putInt(SystemData.COOLDOWN_PREFIX + slotId, 0);

            // 6. Sync
            SystemData.sync(player);
            player.displayClientMessage(Component.literal("§a[System] Art Equipped to Slot " + (slotId + 1)), true);
        });
        return true;
    }
}