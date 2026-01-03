package com.TaylorBros.ManhwaMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketSyncSystemData {
    private final boolean awakened;
    private final int points, str, hp, def, spd, mana, currentMana, level, xp;
    private final boolean isSystemPlayer;
    private final String unlockedSkills; // BUSINESS ADDITION: The Bank list

    public PacketSyncSystemData(boolean awakened, int points, int str, int hp, int def, int spd, int mana, int currentMana, boolean isSystemPlayer, int level, int xp, String unlockedSkills) {
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
        this.unlockedSkills = unlockedSkills;
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
        this.unlockedSkills = buf.readUtf(); // Read string from buffer
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
        buf.writeUtf(unlockedSkills); // Write string to buffer
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                var data = mc.player.getPersistentData();

                // 1. Sync the Core Flags & Bank
                data.putBoolean("manhwamod.awakened", awakened);
                data.putBoolean("manhwamod.is_system_player", isSystemPlayer);
                data.putString("manhwamod.unlocked_skills", unlockedSkills);

                // 2. THE WIPE LOGIC
                if (!awakened) {
                    // Reset Skills
                    data.putString("manhwamod.unlocked_skills", "");
                    for (int i = 1; i <= 5; i++) {
                        data.putInt("manhwamod.slot_" + i, 0);
                    }

                    // Reset Stats to Base 10
                    data.putInt("manhwamod.strength", 10);
                    data.putInt("manhwamod.mana", 10);
                    data.putInt("manhwamod.health_stat", 10);
                    data.putInt("manhwamod.defense", 10);
                    data.putInt("manhwamod.speed", 10);

                    // Close UI immediately if player is wiped while it's open
                    if (mc.screen instanceof StatusScreen || mc.screen instanceof AwakenedStatusScreen) {
                        mc.setScreen(null);
                    }
                } else {
                    // 3. Normal Stat Sync
                    data.putInt("manhwamod.points", points);
                    data.putInt("manhwamod.strength", str);
                    data.putInt("manhwamod.health_stat", hp);
                    data.putInt("manhwamod.defense", def);
                    data.putInt("manhwamod.speed", spd);
                    data.putInt("manhwamod.mana", mana);
                    data.putInt("manhwamod.current_mana", currentMana);
                }

                data.putInt("manhwamod.level", level);
                data.putInt("manhwamod.xp", xp);

                mc.player.refreshDisplayName();
            }
        });
        return true;
    }
}