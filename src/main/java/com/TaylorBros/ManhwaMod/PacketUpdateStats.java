package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.ArrayList;

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

            // 1. Identify which stat the button is trying to upgrade
            // Matches "STR", "HP", "DEF", "SPD", "MANA" from StatusScreen.java
            String nbtKey = switch (statType.toUpperCase()) {
                case "STR", "STRENGTH" -> SystemData.STR;
                case "HP", "HEALTH" -> SystemData.HP;
                case "DEF", "DEFENSE" -> SystemData.DEF;
                case "SPD", "SPEED" -> SystemData.SPD;
                case "MANA" -> SystemData.MANA;
                default -> "";
            };

            if (nbtKey.isEmpty()) return;

            // 2. Check Points and Deduct
            int currentPoints = SystemData.getPoints(player);
            if (currentPoints >= amount) {
                SystemData.savePoints(player, currentPoints - amount);

                // 3. Save the specific stat (Logic previously missing for HP/DEF/SPD)
                if (nbtKey.equals(SystemData.STR)) {
                    SystemData.saveStrength(player, SystemData.getStrength(player) + amount);
                } else if (nbtKey.equals(SystemData.HP)) {
                    SystemData.saveHealthStat(player, SystemData.getHealthStat(player) + amount);
                } else if (nbtKey.equals(SystemData.DEF)) {
                    SystemData.saveDefense(player, SystemData.getDefense(player) + amount);
                } else if (nbtKey.equals(SystemData.SPD)) {
                    SystemData.saveSpeed(player, SystemData.getSpeed(player) + amount);
                } else if (nbtKey.equals(SystemData.MANA)) {
                    SystemData.saveMana(player, SystemData.getMana(player) + amount);
                }

                // 4. Sync immediately so the screen updates
                SystemData.sync(player);
            }
        });
        return true;
    }

    private void generateUniqueSkill(ServerPlayer player) {
        String recipe = "";
        String skillName = "";
        boolean isDuplicate = true;

        List<String> ownedNames = new ArrayList<>();
        for (int id : SystemData.getUnlockedSkills(player)) {
            String existing = player.getPersistentData().getString(SystemData.RECIPE_PREFIX + id);
            ownedNames.add(SkillEngine.getSkillName(existing));
        }

        int safety = 0;
        while (isDuplicate && safety < 100) {
            SkillTags.Shape s = SkillTags.Shape.values()[player.getRandom().nextInt(SkillTags.Shape.values().length)];
            SkillTags.Element e = SkillTags.Element.values()[player.getRandom().nextInt(SkillTags.Element.values().length)];
            SkillTags.Modifier m = SkillTags.Modifier.values()[player.getRandom().nextInt(SkillTags.Modifier.values().length)];
            recipe = s.name() + ":" + e.name() + ":" + m.name();
            skillName = SkillEngine.getSkillName(recipe);

            if (!ownedNames.contains(skillName)) isDuplicate = false;
            safety++;
        }

        int skillId = player.getRandom().nextInt(100000);
        SystemData.unlockSkill(player, skillId, recipe, 25);
        player.displayClientMessage(Component.literal("§b§l[SYSTEM] §fNew Art Learned: §e" + skillName), true);
    }
}