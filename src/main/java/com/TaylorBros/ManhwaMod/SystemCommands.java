package com.TaylorBros.ManhwaMod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

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
                                    CompoundTag data = player.getPersistentData();

                                    data.putBoolean("manhwamod.awakened", false);
                                    data.putBoolean("manhwamod.is_system_player", false);
                                    data.putString("manhwamod.unlocked_skills", "");

                                    for (int i = 1; i <= 100; i++) {
                                        data.remove("manhwamod.skill_recipe_" + i);
                                        data.remove("manhwamod.skill_cost_" + i);
                                        if (i <= 5) data.putInt("manhwamod.slot_" + i, 0);
                                    }

                                    data.putInt("manhwamod.level", 1);
                                    data.putInt("manhwamod.xp", 0);
                                    data.putInt("manhwamod.strength", 10);
                                    data.putInt("manhwamod.health_stat", 10);
                                    data.putInt("manhwamod.defense", 10);
                                    data.putInt("manhwamod.speed", 10);
                                    data.putInt("manhwamod.mana", 10);

                                    SystemData.sync(player);
                                    player.sendSystemMessage(Component.literal("§c§l[SYSTEM] §fFull Profile Wipe Successful."));
                                    return 1;
                                })
                        )

                        // 4. LEARN SKILL
                        .then(Commands.literal("learn_skill")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int id = IntegerArgumentType.getInteger(context, "id");
                                            // Simple Solution: Fixed string format for name:color:desc
                                            SystemData.unlockSkill(player, id, "TestSkill_" + id + ":§bRARE:A powerful test ability.", 50);
                                            player.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fSkill #" + id + " added to Bank."));
                                            return 1;
                                        })
                                )
                        )

                        // 5. ADD POINTS
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

                        // 6. BECOME PLAYER
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
                ) // End admin
        ); // End system
    }
}