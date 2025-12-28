package net.unfamily.skb_res_gen_custom.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unfamily.skb_res_gen_custom.block.display.BedrockGeneratorDisplayItem;
import net.unfamily.skb_res_gen_custom.generator.GeneratorDefinition;
import net.unfamily.skb_res_gen_custom.generator.GeneratorLoader;
import net.unfamily.skb_res_gen_custom.init.ModBlocks;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Command to give custom generators to players
 * Syntax: /skb_res_gen give <player> <base_id> <tier> [amount]
 */
public class GiveGeneratorCommand {
    
    private static final List<String> TIERS = Arrays.asList(
        "wooden", "stone", "iron", "gold", "diamond", "netherite"
    );
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_GENERATORS = 
        (context, builder) -> SharedSuggestionProvider.suggest(
            GeneratorLoader.getAllGenerators().keySet(),
            builder
        );
    
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TIERS = 
        (context, builder) -> SharedSuggestionProvider.suggest(TIERS, builder);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("skb_res_gen")
                .then(Commands.literal("give")
                    .requires(source -> source.hasPermission(2)) // Requires operator permissions
                    .then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("base_id", StringArgumentType.string())
                            .suggests(SUGGEST_GENERATORS)
                            .then(Commands.argument("tier", StringArgumentType.string())
                                .suggests(SUGGEST_TIERS)
                                .executes(ctx -> giveGenerator(
                                    ctx,
                                    EntityArgument.getPlayers(ctx, "player"),
                                    StringArgumentType.getString(ctx, "base_id"),
                                    StringArgumentType.getString(ctx, "tier"),
                                    1
                                ))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                    .executes(ctx -> giveGenerator(
                                        ctx,
                                        EntityArgument.getPlayers(ctx, "player"),
                                        StringArgumentType.getString(ctx, "base_id"),
                                        StringArgumentType.getString(ctx, "tier"),
                                        IntegerArgumentType.getInteger(ctx, "amount")
                                    ))
                                )
                            )
                        )
                    )
                )
        );
    }
    
    private static int giveGenerator(CommandContext<CommandSourceStack> context,
                                     Collection<ServerPlayer> players,
                                     String baseId,
                                     String tier,
                                     int amount) {
        CommandSourceStack source = context.getSource();
        
        // Validate tier
        if (!TIERS.contains(tier.toLowerCase())) {
            source.sendFailure(
                Component.literal("§c[Skyblock Resource Generators] §fInvalid tier. " +
                    "Use: wooden, stone, iron, gold, diamond, netherite")
            );
            return 0;
        }
        
        // Check if generator exists
        GeneratorDefinition def = GeneratorLoader.getGenerator(baseId);
        if (def == null) {
            source.sendFailure(
                Component.literal("§c[Skyblock Resource Generators] §fGenerator not found: §e" + baseId)
            );
            return 0;
        }
        
        try {
            // Create configured stack
            ItemStack stack = BedrockGeneratorDisplayItem.createConfiguredStack(
                ModBlocks.RESOURCE_GENERATOR.get(),
                tier.toLowerCase(),
                baseId
            );
            stack.setCount(amount);
            
            // Give item to players
            int successCount = 0;
            for (ServerPlayer player : players) {
                boolean added = player.addItem(stack.copy());
                if (added) {
                    successCount++;
                }
            }
            
            final int finalSuccessCount = successCount;
            
            if (finalSuccessCount > 0) {
                String generatorName = def.getName();
                String tierCapitalized = tier.substring(0, 1).toUpperCase() + tier.substring(1);
                if (players.size() == 1) {
                    source.sendSuccess(
                        () -> Component.literal("§a[Skyblock Resource Generators] §fGave §e" + amount + 
                            "x " + tierCapitalized + " " + generatorName + " §fto §e" + 
                            players.iterator().next().getName().getString()),
                        true
                    );
                } else {
                    source.sendSuccess(
                        () -> Component.literal("§a[Skyblock Resource Generators] §fGave §e" + amount + 
                            "x " + tierCapitalized + " " + generatorName + " §fto §e" + 
                            finalSuccessCount + " §fplayers"),
                        true
                    );
                }
                return finalSuccessCount;
            }
            
            return 0;
            
        } catch (Exception e) {
            source.sendFailure(
                Component.literal("§c[Skyblock Resource Generators] §fError: " + e.getMessage())
            );
            return 0;
        }
    }
}

