package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketExtractSkill {
    private final int skillId;

    public PacketExtractSkill(int skillId) {
        this.skillId = skillId;
    }

    public PacketExtractSkill(FriendlyByteBuf buf) {
        this.skillId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.skillId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 1. Find the Blank Orb in inventory
            ItemStack orbStack = ItemStack.EMPTY;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                // Check for Skill Orb Item that has NO data tags (Blank)
                if (s.getItem() instanceof SkillOrbItem && !s.hasTag()) {
                    orbStack = s;
                    break;
                }
            }

            if (orbStack.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c[!] You need a Blank Skill Orb in your inventory."));
                return;
            }

            // 2. Get Skill Data from Player
            String recipe = player.getPersistentData().getString(SystemData.RECIPE_PREFIX + this.skillId);
            int cost = player.getPersistentData().getInt(SystemData.COST_PREFIX + this.skillId);

            if (recipe.isEmpty()) return;

            // 3. REMOVE Skill from Player Data
            String bank = player.getPersistentData().getString(SystemData.BANK);
            String idStr = "[" + this.skillId + "]";
            String newBank = bank.replace(idStr, "");
            player.getPersistentData().putString(SystemData.BANK, newBank);

            // Clear from equipped slots
            for (int i = 0; i < 5; i++) {
                if (player.getPersistentData().getInt(SystemData.SLOT_PREFIX + i) == this.skillId) {
                    player.getPersistentData().putInt(SystemData.SLOT_PREFIX + i, 0);
                }
            }

            player.getPersistentData().remove(SystemData.RECIPE_PREFIX + this.skillId);
            player.getPersistentData().remove(SystemData.COST_PREFIX + this.skillId);

            // 4. TRANSFORM THE ORB (The Fix)
            if (orbStack.getCount() == 1) {
                // Perfect Swap: Just write data to the existing item in the slot.
                // It will glow immediately.
                orbStack.getOrCreateTag().putString("StoredSkillRecipe", recipe);
                orbStack.getOrCreateTag().putInt("StoredSkillCost", cost);
            } else {
                // Stack Swap: Remove one blank, add one filled.
                orbStack.shrink(1);

                ItemStack filledOrb = new ItemStack(orbStack.getItem());
                filledOrb.getOrCreateTag().putString("StoredSkillRecipe", recipe);
                filledOrb.getOrCreateTag().putInt("StoredSkillCost", cost);

                if (!player.addItem(filledOrb)) {
                    player.drop(filledOrb, false);
                }
            }

            // 5. Sync & Notify
            SystemData.sync(player);
            player.sendSystemMessage(Component.literal("§b[SYSTEM] §fSkill Crystallized into Orb."));
        });
        return true;
    }
}