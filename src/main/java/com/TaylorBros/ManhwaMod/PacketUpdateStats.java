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
                String nbtKey = statType.equals("MANA") ? SystemData.MANA : "manhwamod." + statType.toLowerCase();
                if (statType.equals("HP")) nbtKey = "manhwamod.health";

                int currentVal = player.getPersistentData().getInt(nbtKey);
                // Business Logic: If they upgrade Mana, we increase the Max and the Current
                int increase = (statType.equals("MANA")) ? amount * 10 : amount;                int newVal = currentVal + increase;

                player.getPersistentData().putInt(nbtKey, newVal);
                SystemData.savePoints(player, currentPoints - amount);

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
        String skillName = "";
        boolean isDuplicate = true;

        // 1. Get current names to prevent visual duplicates
        java.util.List<String> ownedNames = new java.util.ArrayList<>();
        for (int id : SystemData.getUnlockedSkills(player)) {
            String existing = player.getPersistentData().getString(SystemData.RECIPE_PREFIX + id);
            ownedNames.add(SkillEngine.getSkillName(existing));
        }

        // 2. Roll until a unique NAME is found
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