package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.Random;
import java.util.function.Supplier;

public class PacketBuyItem {
    private final int itemId; // 0 = Skill, 1 = Mana, 2 = Gamble

    public PacketBuyItem(int itemId) {
        this.itemId = itemId;
    }

    public PacketBuyItem(FriendlyByteBuf buf) {
        this.itemId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.itemId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            int points = SystemData.getPoints(player);
            Random random = new Random();

            // --- ITEM 1: MYSTERY SKILL BOOK (Cost: 10) ---
            if (itemId == 0) {
                if (points >= 20) {
                    SystemData.savePoints(player, points - 20);

                    // Generate Random Skill
                    String recipe = generateSkill(player, random);
                    int newId = 1000 + random.nextInt(90000);
                    // Calculate Cost based on tier (approx logic)
                    int cost = 30 + random.nextInt(50);

                    // Unlock & Notify
                    SystemData.unlockSkill(player, newId, recipe, cost);

                    // Parse name for display
                    String displayName = recipe.contains("|") ? recipe.split("\\|")[1] : "Unknown Art";
                    SkillRanker.Rank rank = SkillRanker.getRank(recipe);

                    player.sendSystemMessage(Component.literal("§a[STORE] §fPurchased: " + rank.label + " - " + displayName));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                } else {
                    player.sendSystemMessage(Component.literal("§c[STORE] Not enough points! Need 20."));
                }
            }

            // --- ITEM 2: MANA ELIXIR (Cost: 5) ---
            else if (itemId == 1) {
                if (points >= 5) {
                    SystemData.savePoints(player, points - 5);
                    int maxMana = SystemData.getMana(player) * 10;
                    SystemData.saveCurrentMana(player, maxMana); // Full Restore

                    player.sendSystemMessage(Component.literal("§b[STORE] §fMana fully restored."));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                } else {
                    player.sendSystemMessage(Component.literal("§c[STORE] Not enough points! Need 5."));
                }
            }

            // --- ITEM 3: GAMBLER'S BOX (Cost: 1) ---
            else if (itemId == 2) {
                if (points >= 1) {
                    SystemData.savePoints(player, points - 1);
                    boolean win = random.nextBoolean();

                    if (win) {
                        SystemData.savePoints(player, SystemData.getPoints(player) + 2); // Get original + 1 profit
                        player.sendSystemMessage(Component.literal("§6[GAMBLE] §fWIN! +2 Points."));
                        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 1.0f, 2.0f);
                    } else {
                        player.sendSystemMessage(Component.literal("§c[GAMBLE] §fLoss."));
                        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1.0f, 0.5f);
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§c[STORE] Not enough points! Need 1."));
                }
            }
        });
        return true;
    }

    // Helper to generate skill (Copied logic to keep packet self-contained)
    private String generateSkill(ServerPlayer player, Random random) {
        SkillTags.Shape shape = SkillTags.Shape.values()[random.nextInt(SkillTags.Shape.values().length)];
        SkillTags.Modifier modifier = SkillTags.Modifier.values()[random.nextInt(SkillTags.Modifier.values().length)];
        SkillTags.Element element;
        Affinity playerAff = SystemData.getAffinity(player);

        if (random.nextFloat() < 0.65f && playerAff != Affinity.NONE) {
            try { element = SkillTags.Element.valueOf(playerAff.name()); }
            catch (Exception e) { element = SkillTags.Element.values()[random.nextInt(SkillTags.Element.values().length)]; }
        } else {
            element = SkillTags.Element.values()[random.nextInt(SkillTags.Element.values().length)];
        }

        String base = shape.name() + ":" + element.name() + ":" + modifier.name();

        String eName = SkillNamingEngine.getElementName(element.name());
        String sName = SkillNamingEngine.getShapeName(shape.name());
        String mName = SkillNamingEngine.getModifierName(modifier.name());

        return base + "|" + eName + " " + sName + " " + mName;
    }
}