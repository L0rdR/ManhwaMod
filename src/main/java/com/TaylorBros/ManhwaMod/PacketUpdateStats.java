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
        // 1. Roll the dice for each category
        SkillTags.Shape shape = SkillTags.Shape.values()[(int)(Math.random() * SkillTags.Shape.values().length)];
        SkillTags.Element element = SkillTags.Element.values()[(int)(Math.random() * SkillTags.Element.values().length)];
        SkillTags.Modifier modifier = SkillTags.Modifier.values()[(int)(Math.random() * SkillTags.Modifier.values().length)];

        // 2. Save the recipe as a string
        String recipe = shape.name() + ":" + element.name() + ":" + modifier.name();
        player.getPersistentData().putString("manhwamod.skill_recipe_" + skillNumber, recipe);

        // 3. Roll a random cost between 15 and Max Mana
        int maxMana = player.getPersistentData().getInt("manhwamod.max_mana");
        if (maxMana < 20) maxMana = 20;
        int randomCost = 15 + (int)(Math.random() * (maxMana - 15 + 1));

        // 4. Save the cost (Fixed variable name from newSkillId to skillNumber)
        player.getPersistentData().putInt("manhwamod.skill_cost_" + skillNumber, randomCost);

        // 5. System Announcements
        player.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fNew Skill Manifested!"));
        player.sendSystemMessage(Component.literal("§6§l> §e" + element + " " + shape + " §7(" + modifier + ")"));
        player.sendSystemMessage(Component.literal("§b[System] §fMana Cost: §3" + randomCost));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.5f);
    }
}