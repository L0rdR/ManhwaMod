package com.TaylorBros.ManhwaMod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AwakeningCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("awakening")
                .requires(source -> source.hasPermission(2)) // Requires OP/Cheats
                .then(Commands.literal("add_points")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    // 1. Get current points and add the new amount
                                    int currentPoints = SystemData.getPoints(player);
                                    SystemData.savePoints(player, currentPoints + amount);

                                    // 2. Notify the player
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§b[SYSTEM] §fAdded §e" + amount + " §fpoints. Total: §e" + (currentPoints + amount)), true);

                                    return 1;
                                })
                        )
                )
        );
    }
}