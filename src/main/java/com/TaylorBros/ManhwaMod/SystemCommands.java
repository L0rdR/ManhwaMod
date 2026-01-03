package com.TaylorBros.ManhwaMod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SystemCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("system")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("admin")
                        // 1. SET LEVEL
                        .then(Commands.literal("set_level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 1000))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int level = IntegerArgumentType.getInteger(context, "level");
                                            player.getPersistentData().putInt("manhwamod.level", level);
                                            player.getPersistentData().putInt("manhwamod.xp", 0);
                                            SystemData.sync(player);
                                            player.sendSystemMessage(Component.literal("§b§l[ADMIN] §fLevel set to: §e" + level));
                                            return 1;
                                        })
                                )
                        )
                        // 2. AWAKEN
                        .then(Commands.literal("awaken")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    SystemData.saveAwakening(player, true);
                                    player.getPersistentData().putBoolean("manhwamod.is_system_player", true);
                                    SystemData.sync(player);
                                    player.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fAwakening Forced."));
                                    return 1;
                                })
                        )
                        // 3. WIPE PLAYER
                        .then(Commands.literal("wipe_player")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    player.getPersistentData().putInt("manhwamod.level", 1);
                                    player.getPersistentData().putInt("manhwamod.xp", 0);
                                    player.getPersistentData().putInt("manhwamod.points", 5);
                                    player.getPersistentData().putBoolean("manhwamod.awakened", false);
                                    player.getPersistentData().putBoolean("manhwamod.is_system_player", false);
                                    player.getPersistentData().putBoolean("manhwamod.failed_system_trial", false);

                                    player.getPersistentData().putInt("manhwamod.strength", 10);
                                    player.getPersistentData().putInt("manhwamod.health_stat", 10);
                                    player.getPersistentData().putInt("manhwamod.defense", 10);
                                    player.getPersistentData().putInt("manhwamod.speed", 10);
                                    player.getPersistentData().putInt("manhwamod.mana", 10);

                                    SystemEvents.updatePlayerStats(player);
                                    SystemData.sync(player);
                                    player.sendSystemMessage(Component.literal("§c§l[ADMIN] §fCharacter data wiped."));
                                    return 1;
                                })
                        )
                        // 4. ADD POINTS
                        .then(Commands.literal("add_points")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 9999))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            int currentPoints = player.getPersistentData().getInt("manhwamod.points");
                                            player.getPersistentData().putInt("manhwamod.points", currentPoints + amount);
                                            SystemData.sync(player);
                                            player.sendSystemMessage(Component.literal("§b§l[ADMIN] §fAdded §e" + amount + " §fpoints."));
                                            return 1;
                                        })
                                )
                        )
                        // 5. BECOME PLAYER
                        .then(Commands.literal("become_player")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    player.getPersistentData().putBoolean("manhwamod.awakened", true);
                                    player.getPersistentData().putBoolean("manhwamod.is_system_player", true);
                                    player.getPersistentData().putBoolean("manhwamod.failed_system_trial", false);
                                    SystemData.sync(player);
                                    player.sendSystemMessage(Component.literal("§b§l[ADMIN] §fStatus: 'Player'."));
                                    return 1;
                                })
                        )
                )
        );
    }
}