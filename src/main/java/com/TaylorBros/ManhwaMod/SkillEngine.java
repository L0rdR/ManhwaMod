package com.TaylorBros.ManhwaMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.TickTask;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// --- EPIC FIGHT IMPORTS ---
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = "manhwamod")
public class SkillEngine {

    private static final Map<UUID, PendingSkill> pendingSkills = new ConcurrentHashMap<>();
    private record PendingSkill(int slotId, int skillId, String recipe, int cost, int intelligence, long startTime) {}

    public static SkillTags.Element currentSkillElement = SkillTags.Element.NONE;

    // 1. TRIGGER
    public static void scheduleCast(ServerPlayer player, int slotId, int skillId, String recipe) {
        if (recipe == null || recipe.isEmpty()) return;

        int cost = player.getPersistentData().getInt(SystemData.COST_PREFIX + skillId);
        int intelligence = SystemData.getMana(player);

        String cleanRecipe = recipe.contains("|") ? recipe.split("\\|")[0] : recipe;
        String[] parts = cleanRecipe.split(":");
        if (parts.length < 3) return;

        SkillTags.Shape shape = SkillTags.Shape.valueOf(parts[0].toUpperCase().trim().replace(" ", "_"));

        if (!checkRequirements(player, shape)) return;
        if (SystemData.getCurrentMana(player) < cost) {
            player.displayClientMessage(Component.literal("§cNot enough Mana!"), true);
            return;
        }

        boolean animationStarted = performPhysicalAnimation(player, shape);

        if (animationStarted) {
            pendingSkills.put(player.getUUID(), new PendingSkill(slotId, skillId, recipe, cost, intelligence, player.level().getGameTime()));
        } else {
            // Vanilla Fallback
            player.server.tell(new TickTask(player.server.getTickCount() + 10, () ->
                    executePayload(player, new PendingSkill(slotId, skillId, recipe, cost, intelligence, 0), shape)
            ));
        }
    }

    // 2. MONITOR (CUSTOM TIMING PER SKILL)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.player;
        PendingSkill pending = pendingSkills.get(player.getUUID());
        if (pending == null) return;

        boolean fired = false;

        if (ModList.get().isLoaded("epicfight")) {
            try {
                PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
                if (patch != null) {
                    Object animator = patch.getAnimator();
                    StaticAnimation currentAnim = null;
                    float timer = 0f;

                    // Reflection to find Animation & Timer
                    try {
                        Method getAnim = null;
                        try { getAnim = animator.getClass().getMethod("getAnimation"); }
                        catch (NoSuchMethodException e) { getAnim = animator.getClass().getMethod("getLivingAnimation"); }

                        if (getAnim != null) currentAnim = (StaticAnimation) getAnim.invoke(animator);

                        Method getTimer = null;
                        try { getTimer = animator.getClass().getMethod("getTimer"); }
                        catch (NoSuchMethodException e) { getTimer = animator.getClass().getMethod("getAnimationTimer"); }

                        if (getTimer != null) timer = (float) getTimer.invoke(animator);

                    } catch (Exception e) {
                        forceExecute(player, pending);
                        return;
                    }

                    if (currentAnim != null) {
                        boolean isAttack = currentAnim instanceof AttackAnimation;

                        // Ignore idle/walk
                        if (!isAttack) {
                            if (player.level().getGameTime() - pending.startTime() > 30) forceExecute(player, pending);
                            return;
                        }

                        float progress = timer / currentAnim.getTotalTime();

                        // --- GET CUSTOM TIMING ---
                        String cleanRecipe = pending.recipe().contains("|") ? pending.recipe().split("\\|")[0] : pending.recipe();
                        SkillTags.Shape shape = SkillTags.Shape.valueOf(cleanRecipe.split(":")[0].toUpperCase().trim().replace(" ", "_"));

                        float triggerPoint = getTriggerPoint(shape);

                        // FIRE if we passed the specific trigger point for this shape
                        if (progress >= triggerPoint) {
                            executePayload(player, pending, shape);
                            pendingSkills.remove(player.getUUID());
                            fired = true;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (!fired && (player.level().getGameTime() - pending.startTime() > 20)) {
            fireSkill(player, pending);
        }
    }


    // DEFINE TIMINGS HERE
    private static float getTriggerPoint(SkillTags.Shape shape) {
        return switch (shape) {
            // Blink Strike: Teleport EARLY (40%) so swing finishes at target
            case BLINK_STRIKE -> 0.4f;

            // Dashes: Start movement early (20%)
            case DASH -> 0.2f;

            // Slashes: Hit at extension (60%)
            case SLASH, VERT_SLASH, HORIZ_SLASH -> 0.6f;

            // Heavy attacks: Hit late (75%)
            case SLASH_BARRAGE, IMPACT_BURST -> 0.75f;

            // Default for projectiles/magic
            default -> 0.5f;
        };
    }

    private static void fireSkill(ServerPlayer player, PendingSkill pending) {
        forceExecute(player, pending);
    }

    private static void forceExecute(ServerPlayer player, PendingSkill pending) {
        String cleanRecipe = pending.recipe().contains("|") ? pending.recipe().split("\\|")[0] : pending.recipe();
        SkillTags.Shape shape = SkillTags.Shape.valueOf(cleanRecipe.split(":")[0].toUpperCase().trim().replace(" ", "_"));
        executePayload(player, pending, shape);
        pendingSkills.remove(player.getUUID());
    }

    // 3. PAYLOAD
    private static void executePayload(ServerPlayer player, PendingSkill skill, SkillTags.Shape shape) {
        if (!checkRequirements(player, shape)) return;
        int currentMana = SystemData.getCurrentMana(player);
        if (currentMana < skill.cost()) return;

        SystemData.saveCurrentMana(player, currentMana - skill.cost());
        SystemData.sync(player);

        int totalCD = getBaseShapeCooldown(shape) + (int)(skill.cost() * 0.1f);
        player.getPersistentData().putLong(SystemData.LAST_USE_PREFIX + skill.slotId(), player.level().getGameTime());
        player.getPersistentData().putInt(SystemData.COOLDOWN_PREFIX + skill.slotId(), totalCD);

        player.displayClientMessage(Component.literal("§b§l> §fCasting: §6" + getSkillName(skill.recipe())), true);
        Messages.sendToAllTracking(new PacketPlayEffect(player.getId(), skill.recipe()), player);

        String cleanRecipe = skill.recipe().contains("|") ? skill.recipe().split("\\|")[0] : skill.recipe();
        String[] parts = cleanRecipe.split(":");
        SkillTags.Element element = SkillTags.Element.valueOf(parts[1].toUpperCase().trim());
        SkillTags.Modifier modifier = SkillTags.Modifier.valueOf(parts[2].toUpperCase().trim());

        float multi = 1.0f + (skill.cost() / 100.0f);
        SimpleParticleType p1 = getElementParticle(element);
        SimpleParticleType p2 = getModifierParticle(modifier);
        playSkillSounds(player, element, modifier, shape);

        switch (shape) {
            case SLASH -> runSlash(player, p1, 0, false, modifier, multi, element, skill.intelligence());
            case VERT_SLASH -> runSlash(player, p1, 0, true, modifier, multi, element, skill.intelligence());
            case HORIZ_SLASH -> runSlash(player, p1, 0, false, modifier, multi, element, skill.intelligence());
            case DASH -> runDash(player, p1, modifier, multi, element, skill.intelligence());
            case BLINK_STRIKE -> runBlinkStrike(player, p1, modifier, multi, element, skill.intelligence());
            case SLASH_BARRAGE -> runSlashBarrage(player, p1, modifier, multi, skill.cost(), element, skill.intelligence());
            case BALL -> runBall(player, p1, p2, modifier, multi, element, skill.intelligence());
            case RAY -> runRay(player, p1, p2, modifier, multi, element, skill.intelligence());
            case BEAM -> runBeam(player, p1, multi, modifier, element, skill.intelligence());
            case SINGLE -> runSingle(player, p1, multi, modifier, element, skill.intelligence());
            case PUNCH -> runPunch(player, p1, p2, modifier, multi, element, skill.intelligence());
            case WALL, SMOKE -> runInstantWall(player, p1, skill.cost(), multi, element, skill.intelligence());
            case SPIKES -> runSpikes(player, p1, p2, modifier, multi, skill.cost(), element, skill.intelligence());
            case BARRAGE -> runBarrage(player, p1, p2, modifier, multi, skill.cost(), element, skill.intelligence());
            case BARRAGE_PUNCH -> runBarragePunch(player, p1, p2, modifier, multi, skill.cost(), element, skill.intelligence());
            case RAIN -> runRain(player, p1, p2, modifier, multi, skill.cost(), element, skill.intelligence());
            case FLARE -> runProjectileFlare(player, p1, p2, modifier, multi, skill.cost(), element, skill.intelligence());
            case IMPACT_BURST, STAR -> runImpactBurst(player, p1, p2, modifier, multi, skill.cost(), element, skill.intelligence());
            case AOE, CIRCLE -> runAOE(player, p1, p2, modifier, multi, skill.cost(), element, skill.intelligence());
            case CONE -> runCone(player, p1, modifier, multi, element, skill.intelligence());
            case BOOMERANG -> runBoomerang(player, p1, p2, modifier, multi, skill.cost(), element, skill.intelligence());
            case BOLT -> runBolt(player, p1, p2, modifier, multi, element, skill.intelligence());
            case SPARK -> runPunch(player, p1, p2, modifier, multi, element, skill.intelligence());
            default -> runSingle(player, p1, multi, modifier, element, skill.intelligence());
        }
    }

    private static boolean checkRequirements(ServerPlayer player, SkillTags.Shape shape) {
        boolean isMartial = shape.name().contains("SLASH") || shape == SkillTags.Shape.BLINK_STRIKE || shape == SkillTags.Shape.DASH || shape == SkillTags.Shape.SLASH_BARRAGE;
        if (isMartial) {
            boolean hasWeapon = player.getMainHandItem().getItem() instanceof SwordItem || player.getMainHandItem().getItem() instanceof AxeItem || player.getMainHandItem().getItem() instanceof TridentItem;
            if (ModList.get().isLoaded("epicfight")) {
                CapabilityItem itemCap = EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
                if (itemCap instanceof WeaponCapability) hasWeapon = true;
            }
            if (!hasWeapon) return false;
            if (ModList.get().isLoaded("epicfight")) {
                PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
                if (patch != null && !patch.isBattleMode()) return false;
            }
        }
        return true;
    }

    private static boolean performPhysicalAnimation(ServerPlayer player, SkillTags.Shape shape) {
        if (ModList.get().isLoaded("epicfight")) {
            PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
            CapabilityItem itemCap = EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
            if (patch != null && itemCap instanceof WeaponCapability weaponCap) {
                StaticAnimation animToPlay = null;
                List<StaticAnimation> autos = weaponCap.getAutoAttckMotion((PlayerPatch<?>) patch);
                if (shape == SkillTags.Shape.HORIZ_SLASH && !autos.isEmpty()) animToPlay = autos.get(0);
                else if (shape == SkillTags.Shape.SLASH) animToPlay = autos.size() > 1 ? autos.get(1) : (autos.isEmpty() ? null : autos.get(0));
                else if (shape == SkillTags.Shape.VERT_SLASH) animToPlay = autos.size() > 2 ? autos.get(2) : (autos.isEmpty() ? null : autos.get(autos.size() - 1));
                else if (shape == SkillTags.Shape.DASH || shape == SkillTags.Shape.BLINK_STRIKE) animToPlay = autos.isEmpty() ? null : autos.get(0);
                else if (shape == SkillTags.Shape.SLASH_BARRAGE) animToPlay = autos.size() > 1 ? autos.get(1) : (autos.isEmpty() ? null : autos.get(0));

                if (animToPlay != null) {
                    patch.playAnimationSynchronized(animToPlay, 0.0f);
                    return true;
                }
            }
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        return false;
    }

    private static void runSlash(ServerPlayer p, SimpleParticleType p1, float yawOff, boolean vert, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) { Vec3 start = p.getEyePosition(); Vec3 look = p.getLookAngle(); for (int i = -3; i <= 3; i++) { double offset = i * 0.5; Vec3 pos = start.add(look.scale(3.5)).add(vert ? 0 : offset, vert ? offset : 0, 0); damageArea(p, pos, 2.5, 10.0f * multi, m, e, intel); } }
    private static void runBolt(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) { Vec3 targetPos = p.getEyePosition().add(p.getLookAngle().scale(4.0)); damageArea(p, targetPos, 3.0, 12.0f * multi, m, e, intel); }
    private static void runSingle(ServerPlayer p, SimpleParticleType p1, float multi, SkillTags.Modifier m, SkillTags.Element e, int intel) { Vec3 start = p.getEyePosition(); Vec3 look = p.getLookAngle(); for (int i = 1; i < 20; i++) { Vec3 pos = start.add(look.scale(i)); p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0); if (!p.level().noCollision(new AABB(pos, pos).inflate(0.5))) { damageArea(p, pos, 2.0, 6.0f * multi, m, e, intel); break; } } }
    private static void runBall(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) { Vec3 pos = p.getEyePosition().add(p.getLookAngle().scale(2.0)); p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 10, 0.5, 0.5, 0.5, 0.1); shootProjectile(p, p1, m, 8.0f * multi, 1.5, e, intel); }
    private static void shootProjectile(ServerPlayer p, SimpleParticleType particle, SkillTags.Modifier mod, float dmg, double speed, SkillTags.Element ele, int intel) { Vec3 start = p.getEyePosition(); Vec3 vel = p.getLookAngle().scale(speed); for (int i=0; i<40; i++) { final int tick = i; p.server.tell(new TickTask(p.server.getTickCount() + i, () -> { Vec3 current = start.add(vel.scale(tick)); p.serverLevel().sendParticles(particle, current.x, current.y, current.z, 2, 0.1, 0.1, 0.1, 0); damageArea(p, current, 1.5, dmg, mod, ele, intel); })); } }
    private static void runRay(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) { Vec3 start = p.getEyePosition(); Vec3 look = p.getLookAngle(); for (int i = 0; i < 30; i++) { Vec3 pos = start.add(look.scale(i)); p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0); if (i % 3 == 0) damageArea(p, pos, 1.5, 3.0f * multi, m, e, intel); } }
    private static void runBeam(ServerPlayer p, SimpleParticleType p1, float multi, SkillTags.Modifier m, SkillTags.Element e, int intel) { Vec3 start = p.getEyePosition(); Vec3 look = p.getLookAngle(); for (int i = 0; i < 50; i++) { Vec3 pos = start.add(look.scale(i)); p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 5, 0.2, 0.2, 0.2, 0); damageArea(p, pos, 2.0, 1.0f * multi, m, e, intel); } }
    private static void runPunch(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) { Vec3 pos = p.getEyePosition().add(p.getLookAngle().scale(2.0)); p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 10, 0.5, 0.5, 0.5, 0.2); p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 5, 0.3, 0.3, 0.3, 0.1); damageArea(p, pos, 3.0, 10.0f * multi, m, e, intel); }
    private static void runDash(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) { Vec3 look = p.getLookAngle().scale(2.5); p.setDeltaMovement(look); p.hurtMarked = true; damageArea(p, p.position(), 3.0, 5.0f * multi, m, e, intel); }
    private static void runInstantWall(ServerPlayer p, SimpleParticleType p1, int cost, float multi, SkillTags.Element e, int intel) { Vec3 pos = p.position().add(p.getLookAngle().scale(3)); for (int y = 0; y < 3; y++) { for (int x = -2; x <= 2; x++) { p.serverLevel().sendParticles(p1, pos.x + x, pos.y + y, pos.z, 5, 0.2, 0.2, 0.2, 0); damageArea(p, pos.add(x, y, 0), 1.5, 4.0f * multi, SkillTags.Modifier.NONE, e, intel); } } }
    private static void runSpikes(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) { Vec3 start = p.position(); Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize(); for (int i = 1; i < 10; i++) { final int dist = i; p.server.tell(new TickTask(p.server.getTickCount() + i, () -> { Vec3 pos = start.add(dir.scale(dist * 1.5)); p.serverLevel().sendParticles(p1, pos.x, pos.y, pos.z, 10, 0.2, 0.5, 0.2, 0.1); damageArea(p, pos, 2.0, 6.0f * multi, m, e, intel); })); } }
    private static void runBarrage(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) { for (int i = 0; i < 10; i++) { final int delay = i * 3; p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> { shootProjectile(p, p1, m, 3.0f * multi, 2.0, e, intel); p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5f, 1.5f); })); } }
    private static void runBarragePunch(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) { for (int i = 0; i < 8; i++) { final int delay = i * 2; p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> { Vec3 offset = new Vec3((Math.random()-0.5)*2, (Math.random()-0.5)*2, (Math.random()-0.5)*2); Vec3 pos = p.getEyePosition().add(p.getLookAngle().scale(2)).add(offset); p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 5, 0.2, 0.2, 0.2, 0); damageArea(p, pos, 2.0, 5.0f * multi, m, e, intel); })); } }
    private static void runSlashBarrage(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) { for (int i = 0; i < 5; i++) { final int index = i; final int delay = i * 4; p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> { runSlash(p, p1, 0, index % 2 == 0, m, multi, e, intel); })); } }
    private static void runRain(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) { Vec3 center = p.position(); for (int i = 0; i < 20; i++) { final int delay = i * 2; p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> { double x = center.x + (Math.random() - 0.5) * 10; double z = center.z + (Math.random() - 0.5) * 10; p.serverLevel().sendParticles(p1, x, center.y + 5, z, 5, 0.1, 1.0, 0.1, 0.2); damageArea(p, new Vec3(x, center.y, z), 3.0, 4.0f * multi, m, e, intel); })); } }
    private static void runBlinkStrike(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) { Vec3 dest = p.getEyePosition().add(p.getLookAngle().scale(8)); p.teleportTo(dest.x, dest.y, dest.z); damageArea(p, dest, 4.0, 10.0f * multi, m, e, intel); p.serverLevel().sendParticles(p1, dest.x, dest.y, dest.z, 20, 1.0, 1.0, 1.0, 0.1); }
    private static void runProjectileFlare(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) { shootProjectile(p, p1, m, 8.0f * multi, 1.0, e, intel); p.server.tell(new TickTask(p.server.getTickCount() + 10, () -> { Vec3 pos = p.getEyePosition().add(p.getLookAngle().scale(10)); p.serverLevel().sendParticles(p2, pos.x, pos.y, pos.z, 30, 2.0, 2.0, 2.0, 0.1); damageArea(p, pos, 5.0, 12.0f * multi, m, e, intel); })); }
    private static void runImpactBurst(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) { damageArea(p, p.position(), 6.0, 15.0f * multi, m, e, intel); p.serverLevel().sendParticles(p1, p.getX(), p.getY(), p.getZ(), 50, 3.0, 0.5, 3.0, 0.2); }
    private static void runCone(ServerPlayer p, SimpleParticleType p1, SkillTags.Modifier m, float multi, SkillTags.Element e, int intel) { Vec3 start = p.getEyePosition(); Vec3 dir = p.getLookAngle(); for (int i = 1; i < 8; i++) { Vec3 center = start.add(dir.scale(i)); double width = i * 0.5; p.serverLevel().sendParticles(p1, center.x, center.y, center.z, i * 2, width, 0.2, width, 0.05); damageArea(p, center, width, 5.0f * multi, m, e, intel); } }
    private static void runBoomerang(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element e, int intel) { shootProjectile(p, p1, m, 6.0f * multi, 1.5, e, intel); }
    private static void runAOE(ServerPlayer p, SimpleParticleType p1, SimpleParticleType p2, SkillTags.Modifier m, float multi, int cost, SkillTags.Element element, int intelligence) { double radius = 4.0 + (cost / 40.0); Vec3 center = p.position(); for (int t = 0; t < 80; t += 5) { final int delay = t; p.server.tell(new TickTask(p.server.getTickCount() + delay, () -> { if (p.isRemoved()) return; for (int degree = 0; degree < 360; degree += 20) { double rad = Math.toRadians(degree); p.serverLevel().sendParticles(p1, center.x + Math.cos(rad) * radius, center.y + 0.2, center.z + Math.sin(rad) * radius, 1, 0, 0, 0, 0); } p.serverLevel().sendParticles(p2, center.x, center.y + 0.5, center.z, 5, radius/2, 0.5, radius/2, 0.02); damageArea(p, center, radius, 1.5f * multi, m, element, intelligence); p.level().playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.3f, 1.5f); })); } }
    private static void damageArea(ServerPlayer player, Vec3 pos, double range, float baseDmg, SkillTags.Modifier mod, SkillTags.Element element, int manaStat) { float damageMulti = 1.0f + (manaStat * 0.005f); currentSkillElement = element; player.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(range), e -> e != player).forEach(target -> { target.hurt(player.damageSources().magic(), baseDmg * damageMulti); applyElementEffect(target, element, player, baseDmg, manaStat); applyModifier(target, mod, player); }); currentSkillElement = SkillTags.Element.NONE; }
    private static void playSkillSounds(ServerPlayer p, SkillTags.Element e, SkillTags.Modifier m, SkillTags.Shape shape) { net.minecraft.sounds.SoundEvent elementSound = switch(e) { case FIRE -> SoundEvents.FIRECHARGE_USE; case LAVA -> SoundEvents.GENERIC_BURN; case ICE -> SoundEvents.PLAYER_HURT_FREEZE; case WATER -> SoundEvents.PLAYER_SPLASH; case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_THUNDER; case EARTH -> SoundEvents.ROOTED_DIRT_BREAK; case WIND -> SoundEvents.ELYTRA_FLYING; case LIGHT -> SoundEvents.AMETHYST_BLOCK_CHIME; case SHADOW -> SoundEvents.WITHER_SHOOT; case VOID -> SoundEvents.ENDERMAN_TELEPORT; case FORCE -> SoundEvents.WARDEN_ATTACK_IMPACT; case ACID -> SoundEvents.GENERIC_DRINK; case POISON -> SoundEvents.SPIDER_STEP; default -> SoundEvents.PLAYER_ATTACK_SWEEP; }; net.minecraft.sounds.SoundEvent modSound = switch(m) { case EXPLODE -> SoundEvents.GENERIC_EXPLODE; case STUN -> SoundEvents.CONDUIT_ATTACK_TARGET; case LIFESTEAL -> SoundEvents.PLAYER_LEVELUP; case VAMPIRE -> SoundEvents.PIGLIN_BRUTE_CONVERTED_TO_ZOMBIFIED; case CHAIN -> SoundEvents.TRIDENT_THUNDER; case GRAVITY -> SoundEvents.WARDEN_SONIC_CHARGE; case WITHER -> SoundEvents.WITHER_SKELETON_DEATH; case EXECUTE -> SoundEvents.IRON_GOLEM_REPAIR; case BOUNCE -> SoundEvents.PISTON_EXTEND; case WEAKEN -> SoundEvents.WITHER_SHOOT; default -> null; }; p.level().playSound(null, p.getX(), p.getY(), p.getZ(), elementSound, SoundSource.PLAYERS, 0.8f, 1.0f); if (modSound != null) p.level().playSound(null, p.getX(), p.getY(), p.getZ(), modSound, SoundSource.PLAYERS, 0.6f, 1.4f); if (shape.name().contains("SLASH")) p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.5f, 0.8f); }
    private static void applyElementEffect(LivingEntity target, SkillTags.Element element, ServerPlayer source, float baseDmg, int manaStat) { float boostedMulti = 1.0f + (manaStat * 0.03f); int durationBoost = manaStat * 2; int potencyBoost = Math.min(5, manaStat / 20); switch (element) { case FIRE -> target.setSecondsOnFire(4 + (manaStat / 10)); case ICE -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + durationBoost, 2 + potencyBoost)); case LIGHTNING -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15 + (manaStat/5), 10)); case WATER -> { target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + durationBoost, 1 + potencyBoost)); target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40 + durationBoost, potencyBoost)); } case EARTH -> { target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 + durationBoost, 4)); target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60 + durationBoost, 2 + potencyBoost)); } case LAVA -> { target.setSecondsOnFire(8 + (manaStat / 5)); target.hurt(source.damageSources().onFire(), baseDmg * boostedMulti); } case WIND -> { float lift = 0.6f + (manaStat * 0.02f); target.setDeltaMovement(target.getDeltaMovement().add(0, lift, 0)); target.hurtMarked = true; } case SHADOW -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40 + durationBoost, 0)); case ACID -> { target.addEffect(new MobEffectInstance(MobEffects.POISON, 60 + durationBoost, 1 + potencyBoost)); target.getArmorSlots().forEach(s -> s.setDamageValue(s.getDamageValue() + 2)); } case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100 + durationBoost, potencyBoost)); case LIGHT -> { target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100 + durationBoost, 0)); if (target.isInvertedHealAndHarm()) target.hurt(source.damageSources().magic(), baseDmg * 1.5f); } case VOID -> { target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40 + durationBoost, 0)); target.hurt(source.damageSources().fellOutOfWorld(), baseDmg * boostedMulti); } case FORCE -> { Vec3 push = target.position().subtract(source.position()).normalize().scale(1.2 + (manaStat * 0.05)); target.setDeltaMovement(target.getDeltaMovement().add(push)); target.hurtMarked = true; } } }
    private static void applyModifier(LivingEntity t, SkillTags.Modifier m, ServerPlayer p) { switch (m) { case EXPLODE -> t.level().explode(null, t.getX(), t.getY(), t.getZ(), 2.0f, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE); case STUN -> { t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 10)); t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 10)); } case LIFESTEAL -> p.heal(1.0f); case WEAKEN -> { t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1)); t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1)); } case BOUNCE -> { Vec3 knockback = t.position().subtract(p.position()).normalize().add(0, 0.5, 0).scale(1.5); t.setDeltaMovement(knockback); t.hurtMarked = true; } case CHAIN -> { p.level().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(5.0), e -> e != p && e != t).stream().limit(3).forEach(next -> { next.hurt(p.damageSources().magic(), 4.0f); p.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, next.getX(), next.getY() + 1, next.getZ(), 5, 0.1, 0.1, 0.1, 0.01); }); } case VAMPIRE -> { p.heal(2.0f); p.serverLevel().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1, p.getZ(), 3, 0.2, 0.2, 0.2, 0.05); } case GRAVITY -> { Vec3 center = t.position(); p.level().getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(7.0), e -> e != p).forEach(victim -> { Vec3 pull = center.subtract(victim.position()).normalize().scale(0.8); victim.setDeltaMovement(victim.getDeltaMovement().add(pull.x, 0.2, pull.z)); victim.hurtMarked = true; }); } case WITHER -> t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1)); case EXECUTE -> { if (t.getHealth() < (t.getMaxHealth() * 0.25f)) { t.hurt(p.damageSources().magic(), 40.0f); p.serverLevel().sendParticles(ParticleTypes.SONIC_BOOM, t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0); } } case NONE -> {} } }
    private static SimpleParticleType getElementParticle(SkillTags.Element e) { return switch (e) { case FIRE -> ParticleTypes.FLAME; case ICE -> ParticleTypes.SNOWFLAKE; case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK; case VOID -> ParticleTypes.PORTAL; case FORCE -> ParticleTypes.SOUL_FIRE_FLAME; case WATER -> ParticleTypes.SPLASH; case EARTH -> ParticleTypes.MYCELIUM; case LAVA -> ParticleTypes.LAVA; case LIGHT -> ParticleTypes.END_ROD; case WIND -> ParticleTypes.CLOUD; case SHADOW -> ParticleTypes.SQUID_INK; case ACID -> ParticleTypes.SNEEZE; case POISON -> ParticleTypes.ENTITY_EFFECT; default -> ParticleTypes.SMOKE; }; }
    private static SimpleParticleType getModifierParticle(SkillTags.Modifier mod) { return switch (mod) { case EXPLODE -> ParticleTypes.LAVA; case STUN -> ParticleTypes.WARPED_SPORE; case LIFESTEAL -> ParticleTypes.HEART; case WEAKEN -> ParticleTypes.SMOKE; case BOUNCE -> ParticleTypes.POOF; case VAMPIRE -> ParticleTypes.DAMAGE_INDICATOR; case CHAIN -> ParticleTypes.ELECTRIC_SPARK; case GRAVITY -> ParticleTypes.REVERSE_PORTAL; case WITHER -> ParticleTypes.LARGE_SMOKE; case EXECUTE -> ParticleTypes.SOUL; default -> ParticleTypes.WHITE_ASH; }; }
    public static String getSkillName(String recipe) { if (recipe == null || recipe.isEmpty() || recipe.equals("0")) return "None"; if (recipe.contains("|")) { return recipe.split("\\|")[1]; } try { String[] parts = recipe.split(":"); return formatName(parts[2]) + " " + formatName(parts[1]) + " " + formatName(parts[0]); } catch (Exception e) { return "Unnamed Art"; } }
    private static String formatName(String text) { text = text.replace("_", " "); return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase(); }
    private static int getBaseShapeCooldown(SkillTags.Shape s) { return switch (s) { case PUNCH, DASH, SLASH -> 15; case VERT_SLASH, HORIZ_SLASH, SINGLE, CONE -> 25; case BEAM, BARRAGE, RAY -> 45; case IMPACT_BURST, FLARE, BOOMERANG, BALL, STAR, BOLT -> 60; case WALL, SPIKES, BLINK_STRIKE, SMOKE -> 80; case BARRAGE_PUNCH, SLASH_BARRAGE, RAIN, AOE, CIRCLE -> 140; default -> 40; }; }
    public static SkillTags.Element rollWeightedElement(ServerPlayer player) { Affinity playerAff = SystemData.getAffinity(player); if (player.getRandom().nextFloat() < 0.65f && playerAff != Affinity.NONE) { try { return SkillTags.Element.valueOf(playerAff.name()); } catch (Exception e) { return SkillTags.Element.values()[player.getRandom().nextInt(SkillTags.Element.values().length)]; } } return SkillTags.Element.values()[player.getRandom().nextInt(SkillTags.Element.values().length)]; }

    // 4. IMMEDIATE CAST (For EF Skills)
    public static void castImmediate(ServerPlayer player, String recipe, SkillTags.Shape shape) {
        // Basic calculations
        int skillId = 0;
        int cost = 20;
        int intelligence = SystemData.getMana(player);

        // Fire payload immediately
        PendingSkill dummy = new PendingSkill(0, 0, recipe, cost, intelligence, 0);
        executePayload(player, dummy, shape);
    }
}
