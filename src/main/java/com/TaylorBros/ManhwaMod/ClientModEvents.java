package com.TaylorBros.ManhwaMod;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import team.lodestar.lodestone.systems.particle.world.LodestoneWorldParticle;
import team.lodestar.lodestone.systems.particle.world.options.WorldParticleOptions;

@Mod.EventBusSubscriber(modid = "manhwamod", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent

    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {

        event.registerAboveAll("skill_hud", SkillsHudOverlay.HUD_SKILLS);

    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {

        // 1. Soft Glow
        event.registerSpriteSet(ModParticles.SOFT_GLOW.get(), spriteSet ->
                (WorldParticleOptions options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) ->
                        new LodestoneWorldParticle(level, options, (ParticleEngine.MutableSpriteSet) spriteSet, x, y, z, dx, dy, dz)
        );

        // 2. Sharp Spark
        event.registerSpriteSet(ModParticles.SHARP_SPARK.get(), spriteSet ->
                (WorldParticleOptions options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) ->
                        new LodestoneWorldParticle(level, options, (ParticleEngine.MutableSpriteSet) spriteSet, x, y, z, dx, dy, dz)
        );

        // 3. Smoke Cloud
        event.registerSpriteSet(ModParticles.SMOKE_CLOUD.get(), spriteSet ->
                (WorldParticleOptions options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) ->
                        new LodestoneWorldParticle(level, options, (ParticleEngine.MutableSpriteSet) spriteSet, x, y, z, dx, dy, dz)
        );

        // 4. Shockwave Ring
        event.registerSpriteSet(ModParticles.SHOCKWAVE_RING.get(), spriteSet ->
                (WorldParticleOptions options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) ->
                        new LodestoneWorldParticle(level, options, (ParticleEngine.MutableSpriteSet) spriteSet, x, y, z, dx, dy, dz)
        );

        // 5. Crescent Slash
        event.registerSpriteSet(ModParticles.CRESCENT_SLASH.get(), spriteSet ->
                (WorldParticleOptions options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) ->
                        new LodestoneWorldParticle(level, options, (ParticleEngine.MutableSpriteSet) spriteSet, x, y, z, dx, dy, dz)
        );
    }
}