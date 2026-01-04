package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import java.util.List;

public class PacketUpdateStats {
    private final int amount;
    private final String statType;

    public PacketUpdateStats(int amount, String statType) {
        this.amount = amount;
        this.statType = statType;
    }

    public PacketUpdateStats(FriendlyByteBuf buf) {
        this.amount = buf.readInt();
        this.statType = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(amount);
        buf.writeUtf(statType);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            int currentPoints = SystemData.getPoints(player);
            if (currentPoints >= amount) {
                // Map the Stat Type to your NBT keys
                String nbtKey = switch (statType) {
                    case "MANA" -> SystemData.MANA; // "manhwamod.mana"
                    case "STR" -> "manhwamod.strength";
                    case "HP" -> "manhwamod.health";
                    default -> "manhwamod." + statType.toLowerCase();
                };

                int currentVal = player.getPersistentData().getInt(nbtKey);
                int newVal = currentVal + amount;

                player.getPersistentData().putInt(nbtKey, newVal);
                SystemData.savePoints(player, currentPoints - amount);

                // Milestone logic for unique skills
                if (statType.equals("MANA")) {
                    int oldMilestones = currentVal / 50;
                    int newMilestones = newVal / 50;

                    if (newMilestones > oldMilestones) {
                        for (int i = 0; i < (newMilestones - oldMilestones); i++) {
                            generateUniqueSkill(player);
                        }
                    }
                }
                SystemData.sync(player);
            }
        });
        return true;
    }

    private void generateUniqueSkill(ServerPlayer player) {
        String recipe = "";
        boolean isDuplicate = true;

        // Loop until a unique Art is found
        while (isDuplicate) {
            SkillTags.Shape s = SkillTags.Shape.values()[player.getRandom().nextInt(SkillTags.Shape.values().length)];
            SkillTags.Element e = SkillTags.Element.values()[player.getRandom().nextInt(SkillTags.Element.values().length)];
            SkillTags.Modifier m = SkillTags.Modifier.values()[player.getRandom().nextInt(SkillTags.Modifier.values().length)];
            recipe = s.name() + ":" + e.name() + ":" + m.name();

            isDuplicate = false;
            // Check existing recipes in the bank
            for (int id : SystemData.getUnlockedSkills(player)) {
                if (player.getPersistentData().getString(SystemData.RECIPE_PREFIX + id).equals(recipe)) {
                    isDuplicate = true;
                    break;
                }
            }
        }

        int skillId = player.getRandom().nextInt(10000);
        SystemData.unlockSkill(player, skillId, recipe, 25);

        // Use SkillEngine to get the name for the notification
        String skillName = SkillEngine.getSkillName(recipe);
        player.displayClientMessage(Component.literal("§b§l[SYSTEM] §fNew Art Learned: §e" + skillName), true);
    }
}