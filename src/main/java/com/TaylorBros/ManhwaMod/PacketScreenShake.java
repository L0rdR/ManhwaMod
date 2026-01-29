package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketScreenShake {
    private final float intensity;
    private final int duration;

    public PacketScreenShake(float intensity, int duration) {
        this.intensity = intensity;
        this.duration = duration;
    }

    public PacketScreenShake(FriendlyByteBuf buf) {
        this.intensity = buf.readFloat();
        this.duration = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(intensity);
        buf.writeInt(duration);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client-Side Logic: Apply trauma
            ClientCameraHandler.addTrauma(intensity, duration);
        });
        return true;
    }
}