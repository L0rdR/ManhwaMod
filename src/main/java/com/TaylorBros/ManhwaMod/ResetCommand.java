package com.TaylorBros.ManhwaMod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ResetCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("manhwa")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reset")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();

                            // Get the root NBT and the Persisted sub-tag
                            CompoundTag nbt = player.getPersistentData();
                            CompoundTag persisted = nbt.getCompound(Player.PERSISTED_NBT_TAG);

                            // Wipe all mod keys from BOTH the root and the persisted folder
                            String[] keys = {
                                    "manhwamod.awakened", "manhwamod.is_system_player",
                                    "manhwamod.level", "manhwamod.points", "manhwamod.strength",
                                    "manhwamod.health_stat", "manhwamod.defense", "manhwamod.speed",
                                    "manhwamod.mana", "manhwamod.current_mana"
                            };

                            for (String key : keys) {
                                nbt.remove(key);
                                persisted.remove(key);
                            }

                            // Re-save the cleaned persisted tag
                            nbt.put(Player.PERSISTED_NBT_TAG, persisted);

                            // Critical: Sync the empty data to the client
                            SystemData.sync(player);

                            context.getSource().sendSuccess(() ->
                                    Component.literal("§a[SYSTEM] Hard reset complete. Data expunged."), true);
                            return 1;
                        })
                )
        );
    }
}