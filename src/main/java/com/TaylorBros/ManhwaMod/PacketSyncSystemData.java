package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketSyncSystemData {
    private final CompoundTag data;

    public PacketSyncSystemData(CompoundTag data) { this.data = data; }
    public PacketSyncSystemData(FriendlyByteBuf buf) { this.data = buf.readNbt(); }
    public void toBytes(FriendlyByteBuf buf) { buf.writeNbt(data); }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // This merges the Bank, Stats, and Slots into the client player
                mc.player.getPersistentData().merge(this.data);
            }
        });
        return true;
    }
}