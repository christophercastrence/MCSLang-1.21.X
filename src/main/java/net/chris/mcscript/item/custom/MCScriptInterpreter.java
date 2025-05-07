package net.chris.mcscript.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MCScriptInterpreter{
    private Map<String, Object> variables;
    private Map<String, Block> blockTypes;
    private Map<String, net.minecraft.world.entity.EntityType<?>> mobTypes;
    private Player player;
    private Level level;
    private boolean debug = true; // Set to true to see detailed debug messages

    public MCScriptInterpreter(Player player, Level level) {
        this.variables = new HashMap<>();
        this.blockTypes = new HashMap<>();
        this.mobTypes = new HashMap<>();
        this.player = player;
        this.level = level;

        // Initialize block types
        initializeBlockTypes();
        // Initialize mob types
        initializeMobTypes();
    }

    private void initializeBlockTypes() {
        // Add basic block types
        blockTypes.put("stone", Blocks.STONE);
        blockTypes.put("dirt", Blocks.DIRT);
        blockTypes.put("grass", Blocks.GRASS_BLOCK);
        blockTypes.put("oak_planks", Blocks.OAK_PLANKS);
        blockTypes.put("cobblestone", Blocks.COBBLESTONE);
        blockTypes.put("sand", Blocks.SAND);
        blockTypes.put("gravel", Blocks.GRAVEL);
        blockTypes.put("glass", Blocks.GLASS);
        blockTypes.put("obsidian", Blocks.OBSIDIAN);
        blockTypes.put("air", Blocks.AIR);

        if (debug) {
            sendMessage("Initialized " + blockTypes.size() + " block types");
        }
    }

    private void initializeMobTypes() {
        // Add 10 different mobs to the HashMap
        mobTypes.put("zombie", net.minecraft.world.entity.EntityType.ZOMBIE);
        mobTypes.put("skeleton", net.minecraft.world.entity.EntityType.SKELETON);
        mobTypes.put("creeper", net.minecraft.world.entity.EntityType.CREEPER);
        mobTypes.put("spider", net.minecraft.world.entity.EntityType.SPIDER);
        mobTypes.put("cow", net.minecraft.world.entity.EntityType.COW);
        mobTypes.put("pig", net.minecraft.world.entity.EntityType.PIG);
        mobTypes.put("sheep", net.minecraft.world.entity.EntityType.SHEEP);
        mobTypes.put("chicken", net.minecraft.world.entity.EntityType.CHICKEN);
        mobTypes.put("villager", net.minecraft.world.entity.EntityType.VILLAGER);
        mobTypes.put("enderman", net.minecraft.world.entity.EntityType.ENDERMAN);
    }

    public void interpret(String code) {
        String[] lines = code.split("\n");

        if (debug) sendMessage("Starting script execution with " + lines.length + " lines");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("//")) continue; // Skip empty lines and comments

            if (debug) sendMessage("Processing line: " + line);

            try {
                if (line.startsWith("Let")) {
                    handleVariableDeclaration(line);
                } else if (line.startsWith("For")) {
                    handleForLoop(line, lines);
                } else if (line.startsWith("While")) {
                    i = handleWhileLoop(lines, i);
                } else if (line.startsWith("placeBlock")) {
                    handlePlaceBlock(line);
                } else if (line.startsWith("placeWall")) {
                    handlePlaceWall(line);
                } else if (line.startsWith("print")) {
                    handlePrint(line);
                } else if (line.startsWith("spawn")) {
                    handleSpawn(line);
                }
            } catch (Exception e) {
                sendMessage("Error executing line: " + line + " - " + e.getMessage());
            }
        }
    }

    private void handleVariableDeclaration(String line) {
        try {
            String[] parts = line.split("=");
            if (parts.length != 2) {
                sendMessage("Error: Invalid variable declaration: " + line);
                return;
            }

            String varName = parts[0].replace("Let", "").trim();
            String valueStr = parts[1].trim();

            // Remove trailing semicolon if present
            if (valueStr.endsWith(";")) {
                valueStr = valueStr.substring(0, valueStr.length() - 1).trim();
            }

            // Handle string values
            if (valueStr.startsWith("\"") && valueStr.endsWith("\"")) {
                String value = valueStr.substring(1, valueStr.length() - 1);
                variables.put(varName, value);
                if (debug) sendMessage("Defined string variable: " + varName + " = \"" + value + "\"");
            }
            // Handle block types
            else if (valueStr.startsWith("block(") && valueStr.endsWith(")")) {
                String blockName = valueStr.substring(6, valueStr.length() - 1).trim();
                // Remove quotes if present
                if (blockName.startsWith("\"") && blockName.endsWith("\"")) {
                    blockName = blockName.substring(1, blockName.length() - 1);
                }

                if (!blockTypes.containsKey(blockName)) {
                    sendMessage("Error: Unknown block type: " + blockName);
                    return;
                }

                // Store the block reference
                variables.put(varName, blockName);
                if (debug) sendMessage("Defined block variable: " + varName + " = block(\"" + blockName + "\")");
            }
            // Handle arithmetic operations (new)
            else if (valueStr.contains("+") || valueStr.contains("-") ||
                    valueStr.contains("*") || valueStr.contains("/")) {

                // Evaluate the arithmetic expression
                int result = evaluateArithmeticExpression(valueStr);
                variables.put(varName, result);
                if (debug) sendMessage("Defined variable with arithmetic: " + varName + " = " + result);
            }
            // Handle integer values
            else {
                try {
                    int value = Integer.parseInt(valueStr);
                    variables.put(varName, value);
                    if (debug) sendMessage("Defined integer variable: " + varName + " = " + value);
                } catch (NumberFormatException e) {
                    // It's not an integer or a string, could be another variable
                    if (variables.containsKey(valueStr)) {
                        variables.put(varName, variables.get(valueStr));
                        if (debug) sendMessage("Copied variable: " + varName + " = " + variables.get(valueStr));
                    } else {
                        sendMessage("Error: Invalid value: " + valueStr);
                    }
                }
            }
        } catch (Exception e) {
            sendMessage("Error in variable declaration: " + e.getMessage());
        }
    }

    // New method to evaluate arithmetic expressions
    private int evaluateArithmeticExpression(String expression) {
        try {
            // Simple expression evaluator supporting basic operations
            // First, split the expression by operators
            String[] parts;
            int result = 0;

            if (expression.contains("+")) {
                parts = expression.split("\\+");
                // Get the left operand
                int leftValue = getValueForArithmetic(parts[0].trim());
                // Get the right operand
                int rightValue = getValueForArithmetic(parts[1].trim());
                // Perform addition
                result = leftValue + rightValue;

            } else if (expression.contains("-")) {
                parts = expression.split("-");
                // Get the left operand
                int leftValue = getValueForArithmetic(parts[0].trim());
                // Get the right operand
                int rightValue = getValueForArithmetic(parts[1].trim());
                // Perform subtraction
                result = leftValue - rightValue;

            } else if (expression.contains("*")) {
                parts = expression.split("\\*");
                // Get the left operand
                int leftValue = getValueForArithmetic(parts[0].trim());
                // Get the right operand
                int rightValue = getValueForArithmetic(parts[1].trim());
                // Perform multiplication
                result = leftValue * rightValue;

            } else if (expression.contains("/")) {
                parts = expression.split("/");
                // Get the left operand
                int leftValue = getValueForArithmetic(parts[0].trim());
                // Get the right operand
                int rightValue = getValueForArithmetic(parts[1].trim());
                // Perform division
                if (rightValue == 0) {
                    sendMessage("Error: Division by zero");
                    return 0;
                }
                result = leftValue / rightValue;
            }

            return result;
        } catch (Exception e) {
            sendMessage("Error evaluating arithmetic expression: " + e.getMessage());
            return 0;
        }
    }

    // Helper method to get numeric value for an operand
    private int getValueForArithmetic(String operand) {
        try {
            // If it's a number literal
            return Integer.parseInt(operand);
        } catch (NumberFormatException e) {
            // If it's a variable
            if (variables.containsKey(operand)) {
                Object value = variables.get(operand);
                if (value instanceof Integer) {
                    return (Integer) value;
                } else {
                    sendMessage("Error: Variable '" + operand + "' is not an integer");
                    return 0;
                }
            } else {
                sendMessage("Error: Undefined variable in arithmetic: " + operand);
                return 0;
            }
        }
    }

    private void handlePrint(String line) {
        // Simply call executePrint to handle the print statement
        executePrint(line);
    }

    private void executePrint(String line) {
        try {
            // Extract content between print( and )
            int startIndex = line.indexOf("print(");
            int endIndex = line.lastIndexOf(")");

            if (startIndex == -1 || endIndex == -1) {
                sendMessage("Error: Invalid print statement: " + line);
                return;
            }

            String printContent = line.substring(startIndex + 6, endIndex).trim();

            // Check if it's an arithmetic operation
            if (printContent.contains("+") || printContent.contains("-") ||
                    printContent.contains("*") || printContent.contains("/")) {
                // Evaluate the arithmetic expression
                int result = evaluateArithmeticExpression(printContent);
                sendMessage(String.valueOf(result));
                return;
            }

            // Handle variable printing
            if (variables.containsKey(printContent)) {
                Object value = variables.get(printContent);
                if (value instanceof String) {
                    // If it's a string variable, print it directly
                    sendMessage((String) value);
                } else {
                    // For other types, convert to string
                    sendMessage(value.toString());
                }
                return;
            }

            // Handle string literals
            if (printContent.startsWith("\"") && printContent.endsWith("\"")) {
                String toPrint = printContent.substring(1, printContent.length() - 1);
                sendMessage(toPrint);
                return;
            }

            // If we get here, it's an undefined variable or invalid expression
            sendMessage("Error: Undefined variable or invalid expression: " + printContent);
        } catch (Exception e) {
            sendMessage("Error in print statement: " + e.getMessage());
        }
    }

    private void handleForLoop(String forLine, String[] lines) {
        try {
            if (debug) {
                sendMessage("For loop variables: " + variables.toString());
            }

            // Extract variable name and end value from the For line
            int openParen = forLine.indexOf("(");
            int closeParen = forLine.indexOf(")");

            if (openParen == -1 || closeParen == -1) {
                sendMessage("Error: Missing parentheses in For loop");
                return;
            }

            String loopContent = forLine.substring(openParen + 1, closeParen).trim();
            String[] loopParts = loopContent.split("upto");

            if (loopParts.length != 2) {
                sendMessage("Error: Invalid For loop syntax, missing 'upto'");
                return;
            }

            String varName = loopParts[0].trim();
            String endVarName = loopParts[1].trim();

            // Verify both variables exist
            if (!variables.containsKey(varName)) {
                sendMessage("Error: Loop variable not defined: " + varName);
                return;
            }

            if (!variables.containsKey(endVarName)) {
                sendMessage("Error: End variable not defined: " + endVarName);
                return;
            }

            // Get start and end values
            int start = getIntValue(varName);
            int end = getIntValue(endVarName);

            if (debug) sendMessage("Starting loop from " + start + " to " + end);

            // Execute the loop
            for (int i = start; i <= end; i++) {
                variables.put(varName, i);
                if (debug && i % 10 == 0) sendMessage("Loop iteration: " + i);
                evaluateIfStatements(lines);
            }
        } catch (Exception e) {
            sendMessage("Error in For loop: " + e.getMessage());
        }
    }

    private int getIntValue(String varName) {
        Object value = variables.get(varName);
        if (value instanceof Integer) {
            return (Integer) value;
        } else {
            sendMessage("Error: Variable '" + varName + "' is not an integer: " + value);
            return 0; // Default to 0 to prevent crashes
        }
    }

    private void evaluateIfStatements(String[] lines) {
        boolean conditionMet = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Skip empty lines, comments, and other non-conditional statements
            if (line.isEmpty() || line.startsWith("//") ||
                    !(line.startsWith("If") || line.startsWith("Elif") || line.startsWith("Else"))) {
                continue;
            }

            // If we've already met a condition in this if-block, skip remaining conditions
            if (conditionMet && !line.startsWith("If")) {
                continue;
            }

            // If this is a new If statement, reset the conditionMet flag
            if (line.startsWith("If")) {
                conditionMet = false;
            }

            boolean shouldExecute = false;

            // Evaluate condition
            if (line.startsWith("If") || line.startsWith("Elif")) {
                if (!conditionMet) {
                    shouldExecute = evaluateCondition(line);
                    if (shouldExecute) {
                        conditionMet = true;
                    }
                }
            } else if (line.startsWith("Else") && !conditionMet) {
                shouldExecute = true;
                conditionMet = true;
            }

            // Execute the code if condition is met
            if (shouldExecute) {
                // Find the next statement (e.g., print statement or placeBlock statement)
                i++;
                while (i < lines.length) {
                    String nextLine = lines[i].trim();
                    if (nextLine.isEmpty() || nextLine.startsWith("//")) {
                        i++;
                        continue;
                    }

                    if (nextLine.startsWith("print")) {
                        executePrint(nextLine);
                        break;
                    } else if (nextLine.startsWith("placeBlock")) {
                        handlePlaceBlock(nextLine);
                        break;
                    } else {
                        // Unexpected code
                        sendMessage("Error: Expected print or placeBlock statement, found: " + nextLine);
                        break;
                    }
                }

                // Once we've executed a branch, break from the if-elif-else block
                if (line.startsWith("If")) {
                    break;
                }
            }
        }
    }

    private boolean evaluateCondition(String line) {
        try {
            // For "else" statements, always true
            if (line.startsWith("Else")) {
                return true;
            }

            // Extract the condition from inside the parentheses
            int startIndex = line.indexOf("(");
            int endIndex = line.indexOf(")");

            if (startIndex == -1 || endIndex == -1) {
                sendMessage("Error: Missing parentheses in condition");
                return false;
            }

            String condition = line.substring(startIndex + 1, endIndex).trim();

            if (debug) sendMessage("Evaluating condition: " + condition);

            // Handle modulo condition (x % y == z)
            if (condition.contains("%")) {
                String[] parts = condition.split("%");
                String varName = parts[0].trim();

                if (!variables.containsKey(varName)) {
                    sendMessage("Error: Variable not defined in condition: " + varName);
                    return false;
                }

                // Handle the right side (y == z)
                String[] rightParts = parts[1].split("==");
                if (rightParts.length != 2) {
                    sendMessage("Error: Invalid modulo condition format");
                    return false;
                }

                int divisor = Integer.parseInt(rightParts[0].trim());
                int expectedRemainder = Integer.parseInt(rightParts[1].trim());

                int value = getIntValue(varName);
                boolean result = (value % divisor == expectedRemainder);

                if (debug) sendMessage("Condition result: " + value + " % " + divisor + " == " + expectedRemainder + " is " + result);

                return result;
            }

            // Handle equality condition (x == y)
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                String leftPart = parts[0].trim();
                String rightPart = parts[1].trim();

                Object leftValue = getValueFromExpression(leftPart);
                Object rightValue = getValueFromExpression(rightPart);

                if (leftValue == null || rightValue == null) {
                    sendMessage("Error: Could not evaluate values in condition");
                    return false;
                }

                boolean result = leftValue.equals(rightValue);

                if (debug) sendMessage("Condition result: " + leftValue + " == " + rightValue + " is " + result);

                return result;
            }

            sendMessage("Error: Unsupported condition: " + condition);
            return false;
        } catch (Exception e) {
            sendMessage("Error evaluating condition: " + e.getMessage());
            return false;
        }
    }

    private Object getValueFromExpression(String expr) {
        // If it's a string literal
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return expr.substring(1, expr.length() - 1);
        }

        // If it's a number literal
        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            // If it's a variable
            if (variables.containsKey(expr)) {
                return variables.get(expr);
            }
        }

        sendMessage("Error: Could not evaluate expression: " + expr);
        return null;
    }

    private void handlePlaceBlock(String line) {
        try {
            // Extract parameters from placeBlock(x, y, z, blockType)
            int startIndex = line.indexOf("placeBlock(");
            int endIndex = line.lastIndexOf(")");

            if (startIndex == -1 || endIndex == -1) {
                sendMessage("Error: Invalid placeBlock statement: " + line);
                return;
            }

            String content = line.substring(startIndex + 11, endIndex).trim();
            String[] params = content.split(",");

            if (params.length != 4) {
                sendMessage("Error: placeBlock requires 4 parameters: x, y, z, blockType");
                return;
            }

            // Parse parameters
            int x = parseNumberParam(params[0].trim());
            int y = parseNumberParam(params[1].trim());
            int z = parseNumberParam(params[2].trim());
            String blockType = params[3].trim();

            // Remove quotes if present
            if (blockType.startsWith("\"") && blockType.endsWith("\"")) {
                blockType = blockType.substring(1, blockType.length() - 1);
            }

            // Check if block type exists
            if (!blockTypes.containsKey(blockType)) {
                sendMessage("Error: Unknown block type: " + blockType);
                return;
            }

            // Get player position
            BlockPos playerPos = player.blockPosition();

            // Calculate actual position relative to player
            BlockPos pos = new BlockPos(
                    playerPos.getX() + x,
                    playerPos.getY() + y,
                    playerPos.getZ() + z
            );

            // Place the block
            placeBlockAtPosition(pos, blockTypes.get(blockType));

        } catch (Exception e) {
            sendMessage("Error in placeBlock: " + e.getMessage());
        }
    }

    private void handlePlaceWall(String line) {
        try {
            // Extract parameters from placeWall(blockType, direction, width, height)
            int startIndex = line.indexOf("placeWall(");
            int endIndex = line.lastIndexOf(")");

            if (startIndex == -1 || endIndex == -1) {
                sendMessage("Error: Invalid placeWall statement: " + line);
                return;
            }

            String content = line.substring(startIndex + 10, endIndex).trim();
            String[] params = content.split(",");

            if (params.length != 4) {
                sendMessage("Error: placeWall requires 4 parameters: blockType, direction, width, height");
                return;
            }

            // Parse parameters
            String blockType = params[0].trim();
            String direction = params[1].trim();
            int width = parseNumberParam(params[2].trim());
            int height = parseNumberParam(params[3].trim());

            // Remove quotes if present
            if (blockType.startsWith("\"") && blockType.endsWith("\"")) {
                blockType = blockType.substring(1, blockType.length() - 1);
            }
            if (direction.startsWith("\"") && direction.endsWith("\"")) {
                direction = direction.substring(1, direction.length() - 1);
            }

            // Place the wall
            placeWallFromPlayer(blockType, direction, width, height);

        } catch (Exception e) {
            sendMessage("Error in placeWall: " + e.getMessage());
        }
    }

    private int parseNumberParam(String param) {
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            // It might be a variable
            if (variables.containsKey(param)) {
                Object value = variables.get(param);
                if (value instanceof Integer) {
                    return (Integer) value;
                }
            }
            throw new RuntimeException("Invalid number parameter: " + param);
        }
    }

    // Method to place a wall of blocks from player position
    public void placeWallFromPlayer(String blockType, String direction, int width, int height) {
        try {
            if (!blockTypes.containsKey(blockType)) {
                sendMessage("Error: Unknown block type: " + blockType);
                return;
            }

            Block block = blockTypes.get(blockType);
            Direction dir = getDirection(direction);

            if (dir == null) {
                sendMessage("Error: Invalid direction: " + direction);
                return;
            }

            // Get player position
            BlockPos playerPos = player.blockPosition();
            sendMessage("Player position: " + playerPos.getX() + ", " + playerPos.getY() + ", " + playerPos.getZ());

            // Determine wall orientation
            Direction horizontal = dir;
            Direction secondary = Direction.UP;

            if (dir == Direction.UP || dir == Direction.DOWN) {
                // If direction is up/down, use player's facing direction for horizontal axis
                horizontal = player.getDirection();
                secondary = dir;
            } else {
                // For horizontal walls, the secondary direction is up
                horizontal = dir;
                secondary = Direction.UP;
            }

            // Place the wall
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    // Calculate position based on directions
                    int offsetX = horizontal.getStepX() * w + secondary.getStepX() * h;
                    int offsetY = horizontal.getStepY() * w + secondary.getStepY() * h;
                    int offsetZ = horizontal.getStepZ() * w + secondary.getStepZ() * h;

                    BlockPos pos = playerPos.offset(offsetX, offsetY, offsetZ);
                    placeBlockAtPosition(pos, block);
                }
            }

            sendMessage("Placed " + (width * height) + " " + blockType + " blocks as a wall");
        } catch (Exception e) {
            sendMessage("Error placing wall: " + e.getMessage());
        }
    }

    private Direction getDirection(String dir) {
        switch (dir.toLowerCase()) {
            case "up": return Direction.UP;
            case "down": return Direction.DOWN;
            case "north": return Direction.NORTH;
            case "south": return Direction.SOUTH;
            case "east": return Direction.EAST;
            case "west": return Direction.WEST;
            default: return null;
        }
    }

    private void placeBlockAtPosition(BlockPos pos, Block block) {
        try {
            sendMessage("Attempting to place " + block.getName().getString() + " at " +
                    pos.getX() + ", " + pos.getY() + ", " + pos.getZ());

            if (level instanceof ServerLevel) {
                // Check player permissions
                if (!player.mayBuild()) {
                    sendMessage("You don't have permission to build here");
                    return;
                }

                // Try first method
                boolean success = level.setBlock(pos, block.defaultBlockState(), 3);

                // If first method fails, try alternative methods
                if (!success) {
                    // Try setting to air first, then the block
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    success = level.setBlock(pos, block.defaultBlockState(), 3);

                    // If still failing, one more attempt with different flags
                    if (!success) {
                        success = level.setBlockAndUpdate(pos, block.defaultBlockState());
                    }
                }

                sendMessage("Block placement " + (success ? "successful" : "failed"));
            } else {
                sendMessage("Error: Cannot place blocks on client side");
            }
        } catch (Exception e) {
            sendMessage("Error placing block: " + e.getMessage());
        }
    }

    private void sendMessage(String message) {
        if (player != null && level != null && !level.isClientSide) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    // Handler for spawn(mobType, x, y, z)
    private void handleSpawn(String line) {
        try {
            // Extract parameters from spawn(mobType, x, y, z)
            int startIndex = line.indexOf("spawn(");
            int endIndex = line.lastIndexOf(")");

            if (startIndex == -1 || endIndex == -1) {
                sendMessage("Error: Invalid spawn statement: " + line);
                return;
            }

            String content = line.substring(startIndex + 6, endIndex).trim();
            String[] params = content.split(",");

            if (params.length != 4) {
                sendMessage("Error: spawn requires 4 parameters: mobType, x, y, z");
                return;
            }

            String mobType = params[0].trim();
            int x = parseNumberParam(params[1].trim());
            int y = parseNumberParam(params[2].trim());
            int z = parseNumberParam(params[3].trim());

            // Remove quotes if present
            if (mobType.startsWith("\"") && mobType.endsWith("\"")) {
                mobType = mobType.substring(1, mobType.length() - 1);
            }

            spawnMobAtPosition(mobType, x, y, z);

        } catch (Exception e) {
            sendMessage("Error in spawn: " + e.getMessage());
        }
    }

    // Method to spawn a mob at a position relative to the player
    private void spawnMobAtPosition(String mobType, int x, int y, int z) {
        try {
            if (!(level instanceof ServerLevel)) {
                sendMessage("Error: Cannot spawn mobs on client side");
                return;
            }

            ServerLevel serverLevel = (ServerLevel) level;

            // Use the HashMap to get the entity type
            net.minecraft.world.entity.EntityType<?> entityType = mobTypes.get(mobType);

            if (entityType == null) {
                sendMessage("Error: Unknown mob type: " + mobType);
                return;
            }

            BlockPos playerPos = player.blockPosition();
            double spawnX = playerPos.getX() + x;
            double spawnY = playerPos.getY() + y;
            double spawnZ = playerPos.getZ() + z;

            net.minecraft.world.entity.Entity entity = entityType.create(serverLevel);

            if (entity == null) {
                sendMessage("Error: Could not create entity for type: " + mobType);
                return;
            }

            entity.moveTo(spawnX, spawnY, spawnZ, player.getYRot(), 0.0F);
            serverLevel.addFreshEntity(entity);

            sendMessage("Spawned " + mobType + " at (" + spawnX + ", " + spawnY + ", " + spawnZ + ")");
        } catch (Exception e) {
            sendMessage("Error spawning mob: " + e.getMessage());
        }
    }

    // Add this new method for While loops
    private int handleWhileLoop(String[] lines, int startLine) {
        try {
            String whileLine = lines[startLine];
            int openParen = whileLine.indexOf("(");
            int closeParen = whileLine.indexOf(")");
            int openBrace = whileLine.indexOf("{");

            if (openParen == -1 || closeParen == -1) {
                sendMessage("Error: Missing parentheses in While loop");
                return startLine;
            }

            String condition = whileLine.substring(openParen + 1, closeParen).trim();

            // Find the loop body
            List<String> loopBody = new ArrayList<>();
            int currentLine = startLine + 1;
            int braceCount = 1;

            // If there's no opening brace on the While line, look for it on the next line
            if (openBrace == -1) {
                String nextLine = lines[currentLine].trim();
                if (nextLine.equals("{")) {
                    currentLine++;
                } else {
                    sendMessage("Error: Expected '{' after While condition");
                    return startLine;
                }
            }

            // Collect all lines until matching closing brace
            while (currentLine < lines.length) {
                String line = lines[currentLine].trim();

                if (line.contains("{")) braceCount++;
                if (line.contains("}")) braceCount--;

                if (braceCount == 0) break;

                if (!line.equals("{") && !line.equals("}")) {
                    loopBody.add(line);
                }
                currentLine++;
            }

            if (debug) {
                sendMessage("While loop condition: " + condition);
                sendMessage("Loop body size: " + loopBody.size());
            }

            // Execute the loop
            while (evaluateWhileCondition(condition)) {
                for (String line : loopBody) {
                    if (line.startsWith("print")) {
                        executePrint(line);
                    } else if (line.contains("=")) {
                        handleAssignment(line);
                    } else if (line.startsWith("spawn")) {
                        handleSpawn(line);
                    }
                }
            }

            return currentLine;
        } catch (Exception e) {
            sendMessage("Error in While loop: " + e.getMessage());
            return startLine;
        }
    }

    private void handleAssignment(String line) {
        try {
            String[] parts = line.split("=");
            if (parts.length != 2) {
                sendMessage("Error: Invalid assignment");
                return;
            }

            String varName = parts[0].trim();
            String expression = parts[1].trim();

            // Remove semicolon if present
            if (expression.endsWith(";")) {
                expression = expression.substring(0, expression.length() - 1).trim();
            }

            // Handle arithmetic expressions
            if (expression.contains("+")) {
                String[] operands = expression.split("\\+");
                int left = getValueForArithmetic(operands[0].trim());
                int right = getValueForArithmetic(operands[1].trim());
                variables.put(varName, left + right);
                if (debug) sendMessage("Assigned " + varName + " = " + (left + right));
            } else {
                // Simple assignment
                if (variables.containsKey(expression)) {
                    variables.put(varName, variables.get(expression));
                } else {
                    try {
                        int value = Integer.parseInt(expression);
                        variables.put(varName, value);
                    } catch (NumberFormatException e) {
                        sendMessage("Error: Invalid value: " + expression);
                    }
                }
            }
        } catch (Exception e) {
            sendMessage("Error in assignment: " + e.getMessage());
        }
    }

    private boolean evaluateWhileCondition(String condition) {
        try {
            if (condition.contains("<")) {
                String[] parts = condition.split("<");
                int left = getValueForArithmetic(parts[0].trim());
                int right = getValueForArithmetic(parts[1].trim());
                return left < right;
            } else if (condition.contains(">")) {
                String[] parts = condition.split(">");
                int left = getValueForArithmetic(parts[0].trim());
                int right = getValueForArithmetic(parts[1].trim());
                return left > right;
            } else if (condition.contains("==")) {
                String[] parts = condition.split("==");
                int left = getValueForArithmetic(parts[0].trim());
                int right = getValueForArithmetic(parts[1].trim());
                return left == right;
            }
            return false;
        } catch (Exception e) {
            sendMessage("Error evaluating while condition: " + e.getMessage());
            return false;
        }
    }
}