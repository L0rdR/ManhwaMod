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
                String nbtKey = switch (statType) {
                    case "STR" -> "manhwamod.strength";
                    case "HP" -> "manhwamod.health";
                    case "DEF" -> "manhwamod.defense";
                    case "SPD" -> "manhwamod.speed";
                    case "MANA" -> SystemData.MANA; // Using your constant
                    default -> "manhwamod." + statType.toLowerCase();
                };

                int currentVal = player.getPersistentData().getInt(nbtKey);
                int newVal = currentVal + amount;

                player.getPersistentData().putInt(nbtKey, newVal);
                SystemData.savePoints(player, currentPoints - amount);

                if (statType.equals("MANA")) {
                    int oldMilestones = currentVal / 50;
                    int newMilestones = newVal / 50;

                    if (newMilestones > oldMilestones) {
                        for (int i = 0; i < (newMilestones - oldMilestones); i++) {
                            String recipe = "";
                            boolean isDuplicate = true;

                            // NO REPEAT LOGIC: Uses your SystemData.getUnlockedSkills
                            while (isDuplicate) {
                                SkillTags.Shape s = SkillTags.Shape.values()[player.getRandom().nextInt(SkillTags.Shape.values().length)];
                                SkillTags.Element e = SkillTags.Element.values()[player.getRandom().nextInt(SkillTags.Element.values().length)];
                                SkillTags.Modifier m = SkillTags.Modifier.values()[player.getRandom().nextInt(SkillTags.Modifier.values().length)];
                                recipe = s.name() + ":" + e.name() + ":" + m.name();

                                isDuplicate = false;
                                java.util.List<Integer> ownedIds = SystemData.getUnlockedSkills(player);
                                for (int id : ownedIds) {
                                    String existing = player.getPersistentData().getString(SystemData.RECIPE_PREFIX + id);
                                    if (existing.equals(recipe)) {
                                        isDuplicate = true;
                                        break;
                                    }
                                }
                            }

                            int skillId = player.getRandom().nextInt(1000);
                            SystemData.unlockSkill(player, skillId, recipe, 25);
                            player.displayClientMessage(Component.literal("§b§l[SYSTEM] §fUnique Art Learned: §e" + SkillEngine.getSkillName(recipe)), true);
                        }
                    }
                }
                SystemData.sync(player);
            }
        }); // This closes enqueueWork correctly
        return true;
    }
}