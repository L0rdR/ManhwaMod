package com.TaylorBros.ManhwaMod;

import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "manhwamod");

    // --- BASE ASSETS ---
    public static final RegistryObject<LodestoneWorldParticleType> SOFT_GLOW =
            PARTICLES.register("soft_glow", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> SHARP_SPARK =
            PARTICLES.register("sharp_spark", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> SMOKE_CLOUD =
            PARTICLES.register("smoke_cloud", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> SHOCKWAVE_RING =
            PARTICLES.register("shockwave_ring", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> CRESCENT_SLASH =
            PARTICLES.register("crescent_slash", LodestoneWorldParticleType::new);

    // --- NEW ASSETS (Required for the new skills) ---
    public static final RegistryObject<LodestoneWorldParticleType> LIGHTNING_BOLT =
            PARTICLES.register("lightning_bolt", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> MAGIC_CIRCLE =
            PARTICLES.register("magic_circle", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> STAR_FLARE =
            PARTICLES.register("star_flare", LodestoneWorldParticleType::new); // If you haven't made this png yet, it will just show purple/black square, which is fine for testing.

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}