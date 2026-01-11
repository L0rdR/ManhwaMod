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

            // 1. Check Points
            int currentPoints = SystemData.getPoints(player);
            if (currentPoints < amount) return;

            // 2. Deduct Points
            SystemData.savePoints(player, currentPoints - amount);

            // 3. Update Stat (SWITCH on the Button Name to avoid NBT errors)
            switch (this.statType.toUpperCase()) {
                case "STR", "STRENGTH", "STRENGTH:" ->
                        SystemData.saveStrength(player, SystemData.getStrength(player) + amount);

                case "HP", "HEALTH", "HEALTH:" ->
                        SystemData.saveHealthStat(player, SystemData.getHealthStat(player) + amount);

                case "DEF", "DEFENSE", "DEFENSE:" ->
                        SystemData.saveDefense(player, SystemData.getDefense(player) + amount);

                case "SPD", "SPEED", "SPEED:" ->
                        SystemData.saveSpeed(player, SystemData.getSpeed(player) + amount);

                case "MANA", "MANA:" -> {
                    int currentMana = SystemData.getMana(player);
                    SystemData.saveMana(player, currentMana + amount);

                    // !!! PASTE YOUR SKILL GENERATION CODE HERE !!!
                    // The file you uploaded earlier was missing this logic,
                    // so I cannot restore it. Paste your generic/random skill check here.
                    // Example: if (currentMana + amount >= 10) SkillEngine.generate(player);
                }

                case "WIPE" -> {
                    player.getPersistentData().remove(SystemData.STR);
                    player.getPersistentData().remove(SystemData.HP);
                    player.getPersistentData().remove(SystemData.DEF);
                    player.getPersistentData().remove(SystemData.SPD);
                    player.getPersistentData().remove(SystemData.MANA);
                    player.getPersistentData().remove(SystemData.BANK);
                    player.getPersistentData().remove(SystemData.POINTS);
                    // Reset defaults
                    SystemData.saveStrength(player, 10);
                    SystemData.saveHealthStat(player, 20);
                    SystemData.saveDefense(player, 10);
                    SystemData.saveSpeed(player, 10);
                    SystemData.saveMana(player, 10);
                    SystemData.sync(player);
                }
            }

            // 4. Sync
            SystemData.sync(player);
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