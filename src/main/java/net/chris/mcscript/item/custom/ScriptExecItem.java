package net.chris.mcscript.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ScriptExecItem extends Item {
    private static final String SCRIPT_PATH = "Insert File Path Here";

    public ScriptExecItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide) {
            // Get all .mcsl files in the script directory
            File scriptDir = new File(SCRIPT_PATH);
            File[] scriptFiles = scriptDir.listFiles((dir, name) -> name.endsWith(".mcsl"));

            if (scriptFiles == null || scriptFiles.length == 0) {
                pPlayer.sendSystemMessage(Component.literal("No script files found in " + SCRIPT_PATH));
                return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand));
            }

            // Execute the first script file found
            File scriptFile = scriptFiles[0];
            pPlayer.sendSystemMessage(Component.literal("Executing script: " + scriptFile.getName()));

            try {
                String scriptContent = readFile(scriptFile.getPath());
                MCScriptInterpreter interpreter = new MCScriptInterpreter(pPlayer, pLevel);
                interpreter.interpret(scriptContent);

                pPlayer.sendSystemMessage(Component.literal("Script execution completed."));
            } catch (IOException e) {
                pPlayer.sendSystemMessage(Component.literal("Error reading script: " + e.getMessage()));
            } catch (Exception e) {
                pPlayer.sendSystemMessage(Component.literal("Error executing script: " + e.getMessage()));
            }
        }

        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }

    private String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
