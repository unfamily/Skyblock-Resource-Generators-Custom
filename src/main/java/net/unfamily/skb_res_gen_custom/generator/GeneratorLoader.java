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
            // Delegate to the centralized ReadmeGenerator so content and formatting are consistent
            net.unfamily.skb_res_gen_custom.util.ReadmeGenerator.writeReadme(configPath, true);
        } catch (Exception e) {
            LOGGER.error("Failed to create README.md file via ReadmeGenerator: {}", e.getMessage());
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
            
            // Get name (required) - supports translation keys (Minecraft auto-handles fallback)
            if (!generatorJson.has("name") || !generatorJson.get("name").isJsonPrimitive()) {
                LOGGER.error("Custom generator definition missing or invalid 'name' field for base_id: {}", baseId);
                return;
            }
            String nameStr = generatorJson.get("name").getAsString();
            if (nameStr.trim().isEmpty()) {
                LOGGER.error("Custom generator definition 'name' cannot be empty for base_id: {}", baseId);
                return;
            }
            net.minecraft.network.chat.Component nameComponent = net.minecraft.network.chat.Component.translatable(nameStr);
            
            // Get recipe item (optional, defaults to null)
            String recipe = null;
            if (generatorJson.has("recipe") && generatorJson.get("recipe").isJsonPrimitive()) {
                recipe = generatorJson.get("recipe").getAsString();
            }
            
            // Create generator definition
            GeneratorDefinition definition = new GeneratorDefinition(baseId, nameComponent, creativeTab, output, times, stacks, recipe);
            
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
     * Get a generator definition by ID
     */
    public static GeneratorDefinition getGenerator(String baseId) {
        return GENERATORS.get(baseId);
    }
    
    /**
     * Get all generator definitions
     */
    public static Map<String, GeneratorDefinition> getAllGenerators() {
        return new HashMap<>(GENERATORS);
    }
    
    /**
     * Reload all definitions
     */
    public static void reload() {
        LOGGER.info("Reloading all custom generator definitions...");
        GENERATORS.clear();
        scanConfigDirectory();
    }
}

