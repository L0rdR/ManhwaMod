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

    // --- STRIKE SPRITES (Epic Fight-style) ---
    public static final RegistryObject<LodestoneWorldParticleType> EF_CUT1 = PARTICLES.register("ef_cut1", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> EF_CUT2 = PARTICLES.register("ef_cut2", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> EF_CUT3 = PARTICLES.register("ef_cut3", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> EF_CUT4 = PARTICLES.register("ef_cut4", LodestoneWorldParticleType::new);

    public static final RegistryObject<LodestoneWorldParticleType> EF_BR1 = PARTICLES.register("ef_br1", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> EF_BR2 = PARTICLES.register("ef_br2", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> EF_BR3 = PARTICLES.register("ef_br3", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> EF_BR4 = PARTICLES.register("ef_br4", LodestoneWorldParticleType::new);
    public static final RegistryObject<LodestoneWorldParticleType> EF_BR5 = PARTICLES.register("ef_br5", LodestoneWorldParticleType::new);

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
