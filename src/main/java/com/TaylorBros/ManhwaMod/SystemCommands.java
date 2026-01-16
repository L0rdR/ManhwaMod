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
                                            player.getPersistentData().putInt(SystemData.LEVEL, level);
                                            player.getPersistentData().putInt(SystemData.XP, 0);
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
                                    player.getPersistentData().putBoolean(SystemData.IS_SYSTEM, true);
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

                                    data.putBoolean(SystemData.AWAKENED, false);
                                    data.putBoolean(SystemData.IS_SYSTEM, false);
                                    data.putString(SystemData.BANK, "");

                                    for (int i = 0; i <= 100; i++) {
                                        data.remove(SystemData.RECIPE_PREFIX + i);
                                        data.remove(SystemData.COST_PREFIX + i);
                                        if (i < 5) data.putInt(SystemData.SLOT_PREFIX + i, 0);
                                    }

                                    // Reset Stats
                                    data.putInt(SystemData.LEVEL, 1);
                                    data.putInt(SystemData.XP, 0);
                                    data.putInt(SystemData.STR, 10);
                                    data.putInt(SystemData.HP, 10);
                                    data.putInt(SystemData.DEF, 10);
                                    data.putInt(SystemData.SPD, 10);
                                    data.putInt(SystemData.MANA, 10);

                                    // FIXED: Reset the correct variable used by Phone UI
                                    data.putInt("manhwamod.stat_points", 0);
                                    // Also clear the old one just in case
                                    data.putInt(SystemData.POINTS, 0);

                                    player.getPersistentData().remove("manhwamod.quest_date");
                                    player.getPersistentData().remove("manhwamod.quest_kills");
                                    player.getPersistentData().remove("manhwamod.quest_dist");
                                    player.getPersistentData().remove("manhwamod.quest_done");

                                    SystemData.sync(player);
                                    player.sendSystemMessage(Component.literal("§c§l[SYSTEM] §fFull Profile Wipe Successful."));
                                    return 1;
                                })
                        )

                        // 4. LEARN SKILL
                        .then(Commands.literal("learn_skill")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1, 100000))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int id = IntegerArgumentType.getInteger(context, "id");
                                            SystemData.unlockSkill(player, id, "TestSkill_" + id + ":§bRARE:A test ability.", 50);
                                            player.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fSkill #" + id + " added to Bank."));
                                            return 1;
                                        })
                                )
                        )

                        // 5. ADD POINTS (FIXED)
                        .then(Commands.literal("add_points")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 9999))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int amount = IntegerArgumentType.getInteger(context, "amount");

                                            // FIXED: Use "manhwamod.stat_points" to match Phone UI
                                            int currentPoints = player.getPersistentData().getInt("manhwamod.stat_points");
                                            player.getPersistentData().putInt("manhwamod.stat_points", currentPoints + amount);

                                            SystemData.sync(player);
                                            player.sendSystemMessage(Component.literal("§b§l[ADMIN] §fAdded §e" + amount + " §fstat points."));
                                            return 1;
                                        })
                                )
                        )

                        // 6. BECOME PLAYER
                        .then(Commands.literal("become_player")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    player.getPersistentData().putBoolean("manhwamod.is_system_player", true);

                                    player.getPersistentData().remove("manhwamod.quest_date");
                                    player.getPersistentData().remove("manhwamod.quest_kills");
                                    player.getPersistentData().remove("manhwamod.quest_dist");
                                    player.getPersistentData().remove("manhwamod.quest_done");

                                    DailyQuestData.checkAndReset(player);
                                    SystemData.sync(player);

                                    player.sendSystemMessage(Component.literal("§b§l[SYSTEM] §fWelcome, Player."));
                                    return 1;
                                })
                        )
                )
        );
    }
}