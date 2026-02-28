package com.TaylorBros.ManhwaMod;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.awt.Color;

/**
 * Client-side visual dispatcher.
 *
 * - Martial shapes -> StrikeVisuals (EpicFight-style hit sprites / rush streaks)
 * - Everything else -> MagicVisuals (your Lodestone procedural VFX)
 *
 * PacketPlayEffect already calls this on the client, so this class should not do server-only work.
 */
public class SkillVisuals {

    public static void play(Player player, String skillName, String fullRecipeData) {
        if (player == null || player.level() == null || fullRecipeData == null || fullRecipeData.isEmpty()) return;

        Level level = player.level();

        // --- Parse recipe ---
        // Format you’re using: SHAPE:ELEMENT:MODIFIER|OptionalName
        String clean = fullRecipeData.contains("|") ? fullRecipeData.split("\\|")[0] : fullRecipeData;
        String[] parts = clean.split(":");

        String shapeName = parts.length > 0 ? parts[0].trim().toUpperCase() : "SINGLE";
        String elName    = parts.length > 1 ? parts[1].trim().toUpperCase() : "FORCE";

        SkillRanker.Rank rank = SkillRanker.getRank(fullRecipeData);
        float scale = 1.0f + (rank.ordinal() * 0.5f);

        Color c1 = getElementColor(elName);
        Color c2 = getFadeColor(elName);

        // SFX (both martial + magic)
        playRankSound(level, player, rank, shapeName);

        // Screen shake (server -> packets). Safe to keep: only runs if we’re on a ServerLevel.
        if (level instanceof ServerLevel server && rank.ordinal() >= SkillRanker.Rank.A.ordinal()) {
            float shake = rank.ordinal() >= SkillRanker.Rank.S.ordinal() ? 1.5f : 0.5f;
            for (Player p : server.players()) {
                if (p.distanceToSqr(player) < 900.0) {
                    Messages.sendToPlayer(new PacketScreenShake(shake, 20), (ServerPlayer) p);
                }
            }
        }

        // --- Dispatch ---
        if (isMartial(shapeName)) {
            StrikeVisuals.play(player, shapeName, c1, c2, scale, rank);
            System.out.println("[SkillVisuals] Routing to StrikeVisuals: " + shapeName);

        } else {
            MagicVisuals.play(player, shapeName, elName, c1, c2, scale, rank);
        }
    }

    // -------------------------------------------------------------------------
    // Colors / fallback particles
    // -------------------------------------------------------------------------

    static Color getElementColor(String elName) {
        return switch (elName) {
            case "FIRE", "LAVA" -> new Color(255, 100, 0);
            case "ICE", "WATER" -> new Color(100, 200, 255);
            case "LIGHTNING" -> new Color(200, 255, 50);
            case "VOID", "SHADOW" -> new Color(20, 0, 40);
            case "FORCE", "LIGHT" -> Color.WHITE;
            case "POISON", "ACID", "WIND" -> new Color(100, 255, 50);
            case "EARTH" -> new Color(139, 69, 19);
            default -> Color.WHITE;
        };
    }

    static Color getFadeColor(String elName) {
        return switch (elName) {
            case "FIRE" -> new Color(255, 200, 0);
            case "VOID" -> new Color(100, 0, 200);
            case "SHADOW" -> new Color(50, 0, 0);
            case "ICE" -> new Color(200, 255, 255);
            default -> Color.WHITE;
        };
    }

    static ParticleOptions getVanillaParticleFallback(String elName) {
        if (elName == null) return ParticleTypes.CRIT;
        return switch (elName) {
            case "FIRE", "LAVA" -> ParticleTypes.FLAME;
            case "ICE", "WATER" -> ParticleTypes.SNOWFLAKE;
            case "LIGHTNING" -> ParticleTypes.ELECTRIC_SPARK;
            case "VOID", "SHADOW" -> ParticleTypes.SQUID_INK;
            case "POISON", "ACID" -> ParticleTypes.ENTITY_EFFECT;
            case "EARTH" -> ParticleTypes.CRIT;
            case "WIND" -> ParticleTypes.CLOUD;
            case "FORCE", "LIGHT" -> ParticleTypes.ENCHANTED_HIT;
            default -> ParticleTypes.CRIT;
        };
    }

    static void playRankSound(Level level, Player player, SkillRanker.Rank rank, String shapeName) {
        if (shapeName == null) return;

        if (shapeName.contains("SLASH") || shapeName.contains("PUNCH")) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.2f);
        } else if (shapeName.contains("BOLT")) {
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.0f);
        } else if (shapeName.contains("EXPLODE") || shapeName.contains("IMPACT")) {
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.0f);
        }

        if (rank.ordinal() >= SkillRanker.Rank.S.ordinal()) {
            level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.2f, 1.0f);
        } else if (rank.ordinal() >= SkillRanker.Rank.B.ordinal()) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.5f);
        }
    }
    private static boolean isMartial(String shapeName) {
        return shapeName.equals("SLASH")
                || shapeName.equals("VERT_SLASH")
                || shapeName.equals("HORIZ_SLASH")
                || shapeName.equals("DASH")
                || shapeName.equals("BLINK_STRIKE")
                || shapeName.equals("SLASH_BARRAGE")
                || shapeName.equals("PUNCH")
                || shapeName.equals("BARRAGE_PUNCH");
    }
}
