package com.TaylorBros.ManhwaMod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class AwakeningCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("awakening")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add_points")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
                                    Player player = (Player) serverPlayer; // Handle as base Player for SystemData

                                    // Using SystemData centralized logic
                                    int currentPoints = SystemData.getPoints(player);
                                    SystemData.savePoints(player, currentPoints + amount);

                                    context.getSource().sendSuccess(() ->
                                            Component.literal("§b[SYSTEM] §fAdded §e" + amount + " §fpoints."), true);
                                    return 1;
                                })
                        )
                )
        );
    }
}