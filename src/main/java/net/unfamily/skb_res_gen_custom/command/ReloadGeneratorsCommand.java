package net.unfamily.skb_res_gen_custom.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.unfamily.skb_res_gen_custom.generator.GeneratorLoader;

/**
 * Command to reload generator definitions from JSON
 */
public class ReloadGeneratorsCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("skb_res_gen")
                .requires(source -> source.hasPermission(2)) // Require operator permissions for the whole command
                .then(Commands.literal("reload")
                    .executes(ReloadGeneratorsCommand::reload)
                )
        );
    }
    
    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // Reload generators
            GeneratorLoader.reload();
            
            int count = GeneratorLoader.getAllGenerators().size();
            source.sendSuccess(
                () -> Component.literal("§a[Skyblock Resource Generators] §fReloaded §e" + count + " §fcustom generators."),
                true
            );
            
            // Inform about recipe viewer visibility requirement
            source.sendSuccess(
                () -> Component.literal("§eNote: to make recipes visible in recipe viewers (JEI/REI/EMI), run '/reload'."),
                true
            );
            
            return 1; // Success
        } catch (Exception e) {
            source.sendFailure(
                Component.literal("§c[Skyblock Resource Generators] §fError during reload: " + e.getMessage())
            );
            return 0; // Failure
        }
    }
}

