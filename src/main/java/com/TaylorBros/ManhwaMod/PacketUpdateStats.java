package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

public class PacketUpdateStats {
    private final int amount;
    private final String statName;

    public PacketUpdateStats(int amount, String statName) {
        this.amount = amount;
        this.statName = statName;
    }

    public PacketUpdateStats(FriendlyByteBuf buf) {
        this.amount = buf.readInt();
        this.statName = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(amount);
        buf.writeUtf(statName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            int points = SystemData.getPoints(player);
            int toAdd = Math.min(points, amount);

            if (toAdd > 0) {
                player.getPersistentData().putInt("manhwamod.points", points - toAdd);

                String internalKey = switch(statName.toUpperCase()) {
                    case "STR", "STRENGTH" -> "manhwamod.strength";
                    case "HP", "HEALTH" -> "manhwamod.health_stat";
                    case "DEF", "DEFENSE" -> "manhwamod.defense";
                    case "SPD", "SPEED" -> "manhwamod.speed";
                    case "MANA" -> "manhwamod.mana";
                    default -> "manhwamod." + statName.toLowerCase();
                };

                int oldVal = player.getPersistentData().getInt(internalKey);
                int newVal = oldVal + toAdd;
                player.getPersistentData().putInt(internalKey, newVal);

                // --- MANA MILESTONE CHECK ---
                if (statName.equalsIgnoreCase("MANA")) {
                    if (newVal / 50 > oldVal / 50) {
                        int skillNumber = newVal / 50;
                        // CALL THE MANIFEST METHOD HERE
                        manifestNewSkill(player, skillNumber);
                    }
                }

                if (statName.equalsIgnoreCase("HP") || statName.equalsIgnoreCase("health")) {
                    SystemEvents.updatePlayerStats(player);
                }

                SystemData.sync(player);
            }
        });
        return true;
    }

    public static void manifestNewSkill(ServerPlayer player, int skillNumber) {
        // 1. Generate the tags (Ensures SkillTags class is used correctly)
        SkillTags.Shape shape = SkillTags.Shape.values()[(int)(Math.random() * SkillTags.Shape.values().length)];
        SkillTags.Element element = SkillTags.Element.values()[(int)(Math.random() * SkillTags.Element.values().length)];
        SkillTags.Modifier modifier = SkillTags.Modifier.values()[(int)(Math.random() * SkillTags.Modifier.values().length)];

        // 2. Format the Recipe String: "NAME:RARITY:DESC"
        // This is the string the UI will split by ":" to get the name
        String skillName = element.name() + " " + shape.name();
        String rarity = modifier.name();
        String description = "A manifested power of " + element.name();
        String fullRecipe = skillName + ":" + rarity + ":" + description;

        // 3. CRITICAL: Save to the NBT key the UI expects
        // If this key is different by even one letter, the UI will show "Skill #1"
        player.getPersistentData().putString("manhwamod.skill_recipe_" + skillNumber, fullRecipe);

        // Save the cost too for the casting logic
        int randomCost = 15 + (int)(Math.random() * 20);
        player.getPersistentData().putInt("manhwamod.skill_cost_" + skillNumber, randomCost);

        // 4. Update the Bank List
        SystemData.unlockSkill(player, skillNumber, fullRecipe, randomCost);

        // 5. Feedback and Sync
        player.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fNew Skill: §6" + skillName));

        // MUST SYNC so the client UI sees the new recipe string immediately
        SystemData.sync(player);
    }
}