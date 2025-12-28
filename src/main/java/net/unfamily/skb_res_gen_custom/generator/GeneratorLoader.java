package net.unfamily.skb_res_gen_custom.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads custom generator definitions from external JSON files
 */
public class GeneratorLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Map to store generator definitions
    private static final Map<String, GeneratorDefinition> GENERATORS = new HashMap<>();
    
    /**
     * Scans the configuration directory for generator definitions
     */
    public static void scanConfigDirectory() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Scanning configuration directory for custom generator definitions...");
        }
        
        try {
            // Get configured path
            String customGeneratorsPath = net.unfamily.skb_res_gen_custom.Config.customGeneratorsPath;
            if (customGeneratorsPath == null || customGeneratorsPath.trim().isEmpty()) {
                customGeneratorsPath = "kubejs/external_scripts/skb_res_gen_custom/"; // default path
            }
            
            // Create directory if it doesn't exist
            Path configPath = Paths.get(customGeneratorsPath);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Created directory for custom generator definitions: {}", configPath.toAbsolutePath());
                }
                
                // Create README
                createReadme(configPath);
                return;
            }
            
            if (!Files.isDirectory(configPath)) {
                LOGGER.warn("The path for custom generator definitions exists but is not a directory: {}", configPath);
                return;
            }
            
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Scanning directory for custom generator definitions: {}", configPath.toAbsolutePath());
            }
            
            // Always regenerate README
            createReadme(configPath);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Updated README.md");
            }
            
            // Clear previous definitions
            GENERATORS.clear();
            
            // Scan all JSON files in directory
            try (Stream<Path> files = Files.walk(configPath)) {
                files.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".json"))
                     .filter(path -> !path.getFileName().toString().startsWith("."))
                     .sorted() // Process in alphabetical order
                     .forEach(GeneratorLoader::scanConfigFile);
            }
            
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Custom generator definitions scan completed. Loaded {} definitions", GENERATORS.size());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error scanning custom generator definitions directory: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Creates README file in the configuration directory
     */
    private static void createReadme(Path configPath) {
        try {
            Path readmePath = configPath.resolve("README.md");
            
            String readmeContent = "# Custom Generators - External Configuration\n\n" +
                "This directory contains configuration files for custom resource generators.\n\n" +
                "## Format\n\n" +
                "The format is JSON with the following structure:\n\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"skb_res_gen_custom:skb_res_gen_custom\",\n" +
                "  \"generators\": [\n" +
                "    {\n" +
                "      \"base_id\": \"example-obsidian\",\n" +
                "      \"creative_tab\": true,\n" +
                "      \"output\": \"minecraft:obsidian\",\n" +
                "      \"times\": [500, 300, 100, 100, 50, 50],\n" +
                "      \"stacks\": [1, 1, 1, 16, 16, 64]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                "## Fields\n\n" +
                "### Required Fields\n" +
                "- `type`: Must be \"skb_res_gen_custom:skb_res_gen_custom\" (required)\n" +
                "- `generators`: Array of generator definitions (required)\n\n" +
                "### Generator Definition Fields\n" +
                "- `base_id`: Unique identifier for the generator (e.g., \"example-obsidian\")\n" +
                "  - Will create generators for all tiers: wooden, stone, iron, gold, diamond, netherite\n" +
                "  - Example: \"example-obsidian\" → \"wooden_example_obsidian_generator\", \"stone_example_obsidian_generator\", etc.\n" +
                "- `creative_tab`: Whether the generators should appear in the creative tab (true/false)\n" +
                "- `output`: The item to generate (e.g., \"minecraft:obsidian\", \"minecraft:diamond\")\n" +
                "- `times`: Array of 6 integers representing generation time in ticks for each tier\n" +
                "  - Order: [wooden, stone, iron, gold, diamond, netherite]\n" +
                "  - Example: [500, 300, 100, 100, 50, 50] means wooden takes 500 ticks (25 seconds)\n" +
                "  - 20 ticks = 1 second\n" +
                "- `stacks`: Array of 6 integers representing stack size generated for each tier\n" +
                "  - Order: [wooden, stone, iron, gold, diamond, netherite]\n" +
                "  - Example: [1, 1, 1, 16, 16, 64] means wooden generates 1 item, netherite generates 64\n\n" +
                "## Examples\n\n" +
                "### Example 1: Obsidian Generator\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"skb_res_gen_custom:skb_res_gen_custom\",\n" +
                "  \"generators\": [\n" +
                "    {\n" +
                "      \"base_id\": \"example-obsidian\",\n" +
                "      \"creative_tab\": true,\n" +
                "      \"output\": \"minecraft:obsidian\",\n" +
                "      \"times\": [500, 300, 100, 100, 50, 50],\n" +
                "      \"stacks\": [1, 1, 1, 16, 16, 64]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                "### Example 2: Multiple Generators\n" +
                "```json\n" +
                "{\n" +
                "  \"type\": \"skb_res_gen_custom:skb_res_gen_custom\",\n" +
                "  \"generators\": [\n" +
                "    {\n" +
                "      \"base_id\": \"example-obsidian\",\n" +
                "      \"creative_tab\": true,\n" +
                "      \"output\": \"minecraft:obsidian\",\n" +
                "      \"times\": [500, 300, 100, 100, 50, 50],\n" +
                "      \"stacks\": [1, 1, 1, 16, 16, 64]\n" +
                "    },\n" +
                "    {\n" +
                "      \"base_id\": \"example-tnt\",\n" +
                "      \"creative_tab\": false,\n" +
                "      \"output\": \"minecraft:tnt\",\n" +
                "      \"times\": [600, 400, 200, 150, 100, 50],\n" +
                "      \"stacks\": [1, 2, 4, 8, 16, 32]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n" +
                "```\n\n" +
                "## Important Notes\n\n" +
                "- All generators use the same 3D model and textures as the bedrock generator\n" +
                "- The `base_id` must be unique across all configuration files\n" +
                "- Both `times` and `stacks` arrays must have exactly 6 values\n" +
                "- All time and stack values must be positive integers\n" +
                "- Changes require a game restart to take effect\n" +
                "- Invalid configurations will be logged and skipped\n\n" +
                "## Tiers\n\n" +
                "The six tiers are:\n" +
                "1. Wooden - Slowest, generates least\n" +
                "2. Stone - Slow\n" +
                "3. Iron - Medium\n" +
                "4. Gold - Fast\n" +
                "5. Diamond - Very fast\n" +
                "6. Netherite - Fastest, generates most\n";
            
            Files.write(readmePath, readmeContent.getBytes());
            LOGGER.info("Created README.md file at {}", readmePath);
            
        } catch (IOException e) {
            LOGGER.error("Failed to create README.md file: {}", e.getMessage());
        }
    }
    
    /**
     * Scansiona un singolo file di configurazione
     */
    private static void scanConfigFile(Path configFile) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Scanning config file: {}", configFile);
        }
        
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            parseConfigFromStream(configFile.toString(), inputStream);
        } catch (Exception e) {
            LOGGER.error("Error reading custom generator definition file {}: {}", configFile, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Parsa la configurazione da un input stream
     */
    private static void parseConfigFromStream(String filePath, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject json = jsonElement.getAsJsonObject();
                parseConfigJson(filePath, json);
            } else {
                LOGGER.error("Invalid JSON in custom generator definition file: {}", filePath);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing custom generator definition file {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Parses configuration from a JSON object
     */
    private static void parseConfigJson(String filePath, JsonObject json) {
        try {
            // Check if this is a custom generator definition file
            if (!json.has("type") || !json.get("type").getAsString().equals("skb_res_gen_custom:skb_res_gen_custom")) {
                LOGGER.debug("Skipping file {} - not a custom generator definition", filePath);
                return;
            }
            
            // Process generators array
            processGeneratorsJson(json);
            
        } catch (Exception e) {
            LOGGER.error("Error processing custom generator definition {}: {}", filePath, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Processes generators array from a definition file
     */
    private static void processGeneratorsJson(JsonObject json) {
        if (!json.has("generators") || !json.get("generators").isJsonArray()) {
            LOGGER.error("Custom generator definition file missing 'generators' array");
            return;
        }
        
        JsonArray generatorsArray = json.getAsJsonArray("generators");
        for (JsonElement generatorElement : generatorsArray) {
            if (generatorElement.isJsonObject()) {
                processGeneratorDefinition(generatorElement.getAsJsonObject());
            }
        }
    }
    
    /**
     * Processes a single generator definition
     */
    private static void processGeneratorDefinition(JsonObject generatorJson) {
        try {
            // Get base ID (required)
            if (!generatorJson.has("base_id") || !generatorJson.get("base_id").isJsonPrimitive()) {
                LOGGER.error("Custom generator definition missing 'base_id' field");
                return;
            }
            
            String baseId = generatorJson.get("base_id").getAsString();
            
            // Get creative tab (default: true)
            boolean creativeTab = true;
            if (generatorJson.has("creative_tab")) {
                creativeTab = generatorJson.get("creative_tab").getAsBoolean();
            }
            
            // Get output (required)
            if (!generatorJson.has("output") || !generatorJson.get("output").isJsonPrimitive()) {
                LOGGER.error("Custom generator definition missing 'output' field for base_id: {}", baseId);
                return;
            }
            
            String output = generatorJson.get("output").getAsString();
            
            // Get times (required, must be array of 6 elements)
            if (!generatorJson.has("times") || !generatorJson.get("times").isJsonArray()) {
                LOGGER.error("Custom generator definition missing 'times' array for base_id: {}", baseId);
                return;
            }
            
            JsonArray timesArray = generatorJson.getAsJsonArray("times");
            if (timesArray.size() != 6) {
                LOGGER.error("Custom generator 'times' array must have exactly 6 values for base_id: {}", baseId);
                return;
            }
            
            int[] times = new int[6];
            for (int i = 0; i < 6; i++) {
                times[i] = timesArray.get(i).getAsInt();
            }
            
            // Get stacks (required, must be array of 6 elements)
            if (!generatorJson.has("stacks") || !generatorJson.get("stacks").isJsonArray()) {
                LOGGER.error("Custom generator definition missing 'stacks' array for base_id: {}", baseId);
                return;
            }
            
            JsonArray stacksArray = generatorJson.getAsJsonArray("stacks");
            if (stacksArray.size() != 6) {
                LOGGER.error("Custom generator 'stacks' array must have exactly 6 values for base_id: {}", baseId);
                return;
            }
            
            int[] stacks = new int[6];
            for (int i = 0; i < 6; i++) {
                stacks[i] = stacksArray.get(i).getAsInt();
            }
            
            // Get name (required)
            if (!generatorJson.has("name") || !generatorJson.get("name").isJsonPrimitive()) {
                LOGGER.error("Custom generator definition missing 'name' field for base_id: {}", baseId);
                return;
            }
            String name = generatorJson.get("name").getAsString();
            
            // Create generator definition
            GeneratorDefinition definition = new GeneratorDefinition(baseId, name, creativeTab, output, times, stacks);
            
            // Validate definition
            if (!definition.isValid()) {
                LOGGER.error("Invalid custom generator definition for base_id: {}", baseId);
                return;
            }
            
            // Register definition
            GENERATORS.put(baseId, definition);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Registered custom generator definition: {}", baseId);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing custom generator definition: {}", e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Ottieni una definizione di generatore tramite ID
     */
    public static GeneratorDefinition getGenerator(String baseId) {
        return GENERATORS.get(baseId);
    }
    
    /**
     * Ottieni tutte le definizioni di generatori
     */
    public static Map<String, GeneratorDefinition> getAllGenerators() {
        return new HashMap<>(GENERATORS);
    }
    
    /**
     * Ricarica tutte le definizioni
     */
    public static void reload() {
        LOGGER.info("Reloading all custom generator definitions...");
        GENERATORS.clear();
        scanConfigDirectory();
    }
}

