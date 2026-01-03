package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketSyncSystemData {
    private final boolean awakened;
    private final int points, str, hp, def, spd, mana, currentMana, level, xp;
    private final boolean isSystemPlayer;

    public PacketSyncSystemData(boolean awakened, int points, int str, int hp, int def, int spd, int mana, int currentMana, boolean isSystemPlayer, int level, int xp) {
        this.awakened = awakened;
        this.points = points;
        this.str = str;
        this.hp = hp;
        this.def = def;
        this.spd = spd;
        this.mana = mana;
        this.currentMana = currentMana;
        this.isSystemPlayer = isSystemPlayer;
        this.level = level;
        this.xp = xp;
    }

    public PacketSyncSystemData(FriendlyByteBuf buf) {
        this.awakened = buf.readBoolean();
        this.points = buf.readInt();
        this.str = buf.readInt();
        this.hp = buf.readInt();
        this.def = buf.readInt();
        this.spd = buf.readInt();
        this.mana = buf.readInt();
        this.currentMana = buf.readInt();
        this.isSystemPlayer = buf.readBoolean();
        this.level = buf.readInt();
        this.xp = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(awakened);
        buf.writeInt(points);
        buf.writeInt(str);
        buf.writeInt(hp);
        buf.writeInt(def);
        buf.writeInt(spd);
        buf.writeInt(mana);
        buf.writeInt(currentMana);
        buf.writeBoolean(isSystemPlayer);
        buf.writeInt(level);
        buf.writeInt(xp);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // CLIENT SIDE: Update the player's NBT so the StatusScreen can see it
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getPersistentData().putBoolean("manhwamod.awakened", awakened);
                mc.player.getPersistentData().putInt("manhwamod.points", points);
                mc.player.getPersistentData().putInt("manhwamod.strength", str);
                mc.player.getPersistentData().putInt("manhwamod.health_stat", hp);
                mc.player.getPersistentData().putInt("manhwamod.defense", def);
                mc.player.getPersistentData().putInt("manhwamod.speed", spd);
                mc.player.getPersistentData().putInt("manhwamod.mana", mana);
                mc.player.getPersistentData().putInt("manhwamod.current_mana", currentMana);
                mc.player.getPersistentData().putBoolean("manhwamod.is_system_player", isSystemPlayer);
                mc.player.getPersistentData().putInt("manhwamod.level", level);
                mc.player.getPersistentData().putInt("manhwamod.xp", xp);
            }
        });
        return true;
    }
}