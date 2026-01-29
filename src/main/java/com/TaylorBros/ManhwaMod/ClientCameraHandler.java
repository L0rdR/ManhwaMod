package com.TaylorBros.ManhwaMod;

import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ManhwaMod.MODID, value = Dist.CLIENT)
public class ClientCameraHandler {

    private static float trauma = 0;
    private static int shakeTimer = 0;
    private static final Random random = new Random();

    public static void addTrauma(float amount, int duration) {
        trauma = Math.min(trauma + amount, 2.0f); // Cap trauma at 2.0
        shakeTimer = duration;
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTimer > 0 && trauma > 0) {
            shakeTimer--;

            // Shake Strength decays over time
            float shake = trauma * trauma * (shakeTimer / 20.0f);

            // Apply random jitter to Pitch (Up/Down) and Yaw (Left/Right)
            float pitchOffset = (random.nextFloat() - 0.5f) * shake * 2.0f;
            float yawOffset = (random.nextFloat() - 0.5f) * shake * 2.0f;
            float rollOffset = (random.nextFloat() - 0.5f) * shake * 1.5f;

            event.setPitch(event.getPitch() + pitchOffset);
            event.setYaw(event.getYaw() + yawOffset);
            event.setRoll(event.getRoll() + rollOffset);

            // Decay trauma slowly
            trauma = Math.max(0, trauma - 0.05f);
        }
    }
}