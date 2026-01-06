package com.TaylorBros.ManhwaMod;

import com.TaylorBros.ManhwaMod.ManhwaMod;
import com.TaylorBros.ManhwaMod.SkillsHudOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = ManhwaMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("skill_hud", SkillsHudOverlay.HUD_SKILLS);
    }
}
