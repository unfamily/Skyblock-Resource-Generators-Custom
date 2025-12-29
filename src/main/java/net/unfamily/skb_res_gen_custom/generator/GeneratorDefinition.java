package net.unfamily.skb_res_gen_custom.generator;

import java.util.Arrays;

/**
 * Custom generator definition
 */
public class GeneratorDefinition {
    private final String baseId;
    private final String name; // Readable name (required)
    private final boolean creativeTab;
    private final String output;
    private final int[] times;
    private final int[] stacks;
    private final String recipe; // Item ID for conversion (e.g., "minecraft:obsidian")
    
    public GeneratorDefinition(String baseId, String name, boolean creativeTab, String output, int[] times, int[] stacks, String recipe) {
        this.baseId = baseId;
        this.name = name;
        this.creativeTab = creativeTab;
        this.output = output;
        this.times = times;
        this.stacks = stacks;
        this.recipe = recipe;
    }
    
    public String getBaseId() {
        return baseId;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isCreativeTab() {
        return creativeTab;
    }
    
    public String getOutput() {
        return output;
    }
    
    public int[] getTimes() {
        return times;
    }
    
    public int[] getStacks() {
        return stacks;
    }
    
    public String getRecipe() {
        return recipe;
    }
    
    /**
     * Validates the generator definition
     */
    public boolean isValid() {
        if (baseId == null || baseId.trim().isEmpty()) {
            return false;
        }
        
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        if (output == null || output.trim().isEmpty()) {
            return false;
        }
        
        // Must have exactly 6 values for times and stacks (wooden, stone, iron, gold, diamond, netherite)
        if (times == null || times.length != 6) {
            return false;
        }
        
        if (stacks == null || stacks.length != 6) {
            return false;
        }
        
        // All values must be positive
        for (int time : times) {
            if (time <= 0) {
                return false;
            }
        }
        
        for (int stack : stacks) {
            if (stack <= 0) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return "GeneratorDefinition{" +
                "baseId='" + baseId + '\'' +
                ", name='" + name + '\'' +
                ", creativeTab=" + creativeTab +
                ", output='" + output + '\'' +
                ", times=" + Arrays.toString(times) +
                ", stacks=" + Arrays.toString(stacks) +
                ", recipe='" + recipe + '\'' +
                '}';
    }
}

