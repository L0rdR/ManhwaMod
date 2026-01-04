package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

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
                // 1. Map to exact tags used in SystemData.java
                String nbtKey = switch (statType) {
                    case "STR" -> "manhwamod.strength";
                    case "HP" -> "manhwamod.health"; // FIXED: Matches SystemData tag
                    case "DEF" -> "manhwamod.defense";
                    case "SPD" -> "manhwamod.speed";
                    case "MANA" -> "manhwamod.mana";
                    default -> "manhwamod." + statType.toLowerCase();
                };

                int currentVal = player.getPersistentData().getInt(nbtKey);
                int newVal = currentVal + amount;

                // 2. Update stat and deduct points
                player.getPersistentData().putInt(nbtKey, newVal);
                SystemData.savePoints(player, currentPoints - amount);

                // 3. THE 50 MANA FIX: Check milestone boundaries
                // 3. THE 50 MANA FIX: Check milestone boundaries
                if (statType.equals("MANA")) {
                    int oldMilestones = currentVal / 50;
                    int newMilestones = newVal / 50;

                    if (newMilestones > oldMilestones) {
                        for (int i = 0; i < (newMilestones - oldMilestones); i++) {
                            // 1. GENERATE RANDOM TAGS FROM YOUR ENUMS
                            SkillTags.Shape shape = SkillTags.Shape.values()[player.getRandom().nextInt(SkillTags.Shape.values().length)];
                            SkillTags.Element element = SkillTags.Element.values()[player.getRandom().nextInt(SkillTags.Element.values().length)];
                            SkillTags.Modifier modifier = SkillTags.Modifier.values()[player.getRandom().nextInt(SkillTags.Modifier.values().length)];

                            // 2. CREATE THE RECIPE STRING (Shape:Element:Modifier)
                            String recipe = shape.name() + ":" + element.name() + ":" + modifier.name();

                            // 3. GENERATE A FORMATTED NAME USING YOUR ENGINE
                            String skillName = SkillEngine.getSkillName(recipe);
                            int skillId = player.getRandom().nextInt(1000);
                            int cost = 10 + player.getRandom().nextInt(40);

                            // 4. UNLOCK USING YOUR TAG SYSTEM
                            SystemData.unlockSkill(player, skillId, recipe, cost);

                            // 5. NOTIFY THE PLAYER
                            player.displayClientMessage(Component.literal("§b§l[SYSTEM] §fNew Art Learned: §e" + skillName), true);
                        }
                    }
                    SystemData.sync(player);
                }
            }
        }); // FIXED: Closes enqueueWork
        return true;
    } // FIXED: Closes handle
} // FIXED: Closes class