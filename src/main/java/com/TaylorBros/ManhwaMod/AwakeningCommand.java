package com.TaylorBros.ManhwaMod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AwakeningCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("awakening")
                .requires(source -> source.hasPermission(2))

                // --- EXISTING: ADD POINTS ---
                .then(Commands.literal("add_points")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int currentPoints = SystemData.getPoints(player);
                                    SystemData.savePoints(player, currentPoints + amount);

                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§b[SYSTEM] §fAdded §e" + amount + " §fpoints."), true);
                                    return 1;
                                })
                        )
                )

                // --- NEW: SET AFFINITY ---
                .then(Commands.literal("set_affinity")
                        .then(Commands.argument("element", StringArgumentType.string())
                                .executes(context -> {
                                    String elementInput = StringArgumentType.getString(context, "element").toUpperCase();
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    try {
                                        Affinity newAff = Affinity.valueOf(elementInput);
                                        SystemData.setAffinity(player, newAff); // Assuming saveAffinity exists in SystemData

                                        context.getSource().sendSuccess(() ->
                                                Component.literal("§b[SYSTEM] §fAffinity set to " + newAff.color + newAff.name()), true);
                                        return 1;
                                    } catch (IllegalArgumentException e) {
                                        context.getSource().sendFailure(Component.literal("§c[ERROR] Invalid Element! Use FIRE, WATER, etc."));
                                        return 0;
                                    }
                                })
                        )
                )
        );
    }
}