package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketEquipSkill {
    private final int libraryIndex;
    private final int forcedSlot;

    public PacketEquipSkill(int libraryIndex) {
        this(libraryIndex, -1);
    }

    public PacketEquipSkill(int libraryIndex, int forcedSlot) {
        this.libraryIndex = libraryIndex;
        this.forcedSlot = forcedSlot;
    }

    public PacketEquipSkill(FriendlyByteBuf buf) {
        this.libraryIndex = buf.readInt();
        this.forcedSlot = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(libraryIndex);
        buf.writeInt(forcedSlot);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // --- CASE 1: CLEARING A SLOT (Right-Click) ---
            if (libraryIndex == -1 && forcedSlot != -1) {
                player.getPersistentData().remove("manhwamod.slot_" + forcedSlot);
                player.sendSystemMessage(Component.literal("§e[SYSTEM] §fSlot " + forcedSlot + " cleared."));
            }
            // --- CASE 2: EQUIPPING A SKILL (Left-Click) ---
            else {
                for (int i = 1; i <= 5; i++) {
                    String key = "manhwamod.slot_" + i;
                    // Check if skill is already equipped elsewhere to prevent duplicates
                    if (player.getPersistentData().getInt(key) == libraryIndex) {
                        player.sendSystemMessage(Component.literal("§c[!] Skill already equipped."));
                        return;
                    }

                    if (!player.getPersistentData().contains(key)) {
                        player.getPersistentData().putInt(key, libraryIndex);
                        player.sendSystemMessage(Component.literal("§b[SYSTEM] §fSkill #" + libraryIndex + " bound to Slot " + i));
                        break;
                    }
                }
            }
            SystemData.sync(player);
        });
        return true;
    }
}