package com.TaylorBros.ManhwaMod;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import team.lodestar.lodestone.systems.particle.world.options.WorldParticleOptions;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;
import team.lodestar.lodestone.registry.common.particle.LodestoneParticleRegistry;
import team.lodestar.lodestone.systems.particle.world.LodestoneWorldParticle;


@Mod.EventBusSubscriber(modid = "manhwamod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {



    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("skill_hud", SkillsHudOverlay.HUD_SKILLS);
    }


    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {

        registerWorld(event, ModParticles.SOFT_GLOW.get());
        registerWorld(event, ModParticles.SHARP_SPARK.get());
        registerWorld(event, ModParticles.SMOKE_CLOUD.get());
        registerWorld(event, ModParticles.SHOCKWAVE_RING.get());

        // Epic Fight slash sprites (now LodestoneWorldParticleType)
        registerWorld(event, ModParticles.EF_CUT1.get());
        registerWorld(event, ModParticles.EF_CUT2.get());
        registerWorld(event, ModParticles.EF_CUT3.get());
        registerWorld(event, ModParticles.EF_CUT4.get());

        registerWorld(event, ModParticles.EF_BR1.get());
        registerWorld(event, ModParticles.EF_BR2.get());
        registerWorld(event, ModParticles.EF_BR3.get());
        registerWorld(event, ModParticles.EF_BR4.get());
        registerWorld(event, ModParticles.EF_BR5.get());
    }

    private static void registerWorld(RegisterParticleProvidersEvent event, LodestoneWorldParticleType type) {
        event.registerSpriteSet(type, spriteSet ->
                (WorldParticleOptions options, ClientLevel level,
                 double x, double y, double z, double dx, double dy, double dz) ->
                        new LodestoneWorldParticle(level, options, (ParticleEngine.MutableSpriteSet) spriteSet,
                                x, y, z, dx, dy, dz)
        );
    }
}
