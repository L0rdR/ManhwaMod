package com.TaylorBros.ManhwaMod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ManhwaMod.MODID)
public class SystemCombatEvents {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player attacker && event.getEntity() instanceof Player victim) {

            Affinity attackerAff = SystemData.getAffinity(attacker);
            Affinity victimAff = SystemData.getAffinity(victim);

            // Convert SkillEngine element to Affinity
            Affinity moveAff;
            try {
                moveAff = Affinity.valueOf(SkillEngine.currentSkillElement.name());
            } catch (Exception e) { return; }

            if (moveAff == Affinity.NONE) return;

            // --- 1. RESISTANCE (10% Damage Reduction) ---
            // If the victim's affinity is the same as the move hitting them
            if (victimAff.resists(moveAff)) {
                event.setAmount(event.getAmount() * 0.90f);
                victim.displayClientMessage(Component.literal("§b🛡 ELEMENTAL RESISTANCE"), true);
            }

            // --- 2. WEAKNESS (10% Extra Damage Taken) ---
            // If the victim is weak to the element hitting them
            if (victimAff.getWeaknesses().contains(moveAff)) {
                event.setAmount(event.getAmount() * 1.10f);
                victim.displayClientMessage(Component.literal("§c⚠ ELEMENTAL WEAKNESS"), true);
            }

            // --- 3. ADVANTAGE (10% Extra Damage Dealt) ---
            // If attacker uses their own element AND victim is weak to it
            if (moveAff == attackerAff && attackerAff.getStrengths().contains(victimAff)) {
                event.setAmount(event.getAmount() * 1.10f);
                attacker.displayClientMessage(Component.literal("§6⚔ ELEMENTAL ADVANTAGE"), true);
            }
        }
    }
}