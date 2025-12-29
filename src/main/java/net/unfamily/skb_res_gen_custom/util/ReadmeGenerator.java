package net.unfamily.skb_res_gen_custom.util;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ReadmeGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String README_NAME = "README.md";

    private ReadmeGenerator() {}

    /**
     * Write README into the given directory if missing.
     * If overwrite == true, it will replace existing README.
     */
    public static void writeReadme(Path dir, boolean overwrite) {
        try {
            if (dir == null) return;
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path readme = dir.resolve(README_NAME);
            if (Files.exists(readme) && !overwrite) {
                LOGGER.debug("README already exists at {}, skipping", readme.toAbsolutePath());
                return;
            }

            String content = """
                # Skyblock Resource Generators - generators.json guide

                Location
                - Place your configuration file at: `run/kubejs/external_scripts/skb_res_gen_custom/generators.json`
                  (the mod reads that path by default; you can change it in the mod config).

                Top-level format
                - JSON object with:
                  - `type`: must be `"skb_res_gen_custom:skb_res_gen_custom"`
                  - `generators`: array of generator definitions

                Generator entry fields
                - `base_id` (string, required): unique id for this generator (e.g. `obsidian`).
                - `name` (string, required): readable name shown in the tooltip.
                - `output` (string, required): item that the generator produces (e.g. `minecraft:obsidian`).
                - `times` (array of 6 ints, required): ticks per generation for tiers
                   (order: wooden, stone, iron, gold, diamond, netherite).
                - `stacks` (array of 6 ints, required): amount produced per cycle for each tier.
                - `creative_tab` (boolean, optional, default true): show in creative tab.
                - `recipe` (string, optional): item id used to convert an empty generator (e.g. `minecraft:obsidian`).
                   If present, right-clicking an empty generator with this item will convert it to the custom generator.

                Validation
                - `times` and `stacks` MUST have exactly 6 numeric entries.
                - `base_id` and `name` must be non-empty.

                Texture naming & placement
                - Place textures under: `assets/skb_res_gen_custom/textures/block/`
                - Filename convention: `{base_id}_{tier}.png` (e.g. `obsidian_gold.png`)
                - Provide all 6 files per `base_id` (wooden, stone, iron, gold, diamond, netherite).
                - If a texture is missing, the mod falls back to `textures/block/default_texture.png`.

                Texture layout (visual, 64x64 atlas)
                - Combined image size: 64x64 pixels (4x4 grid of 16x16 tiles).
                - The renderer expects the **core** to be duplicated and the **frame** to be present once.
                - The following ASCII grid shows the recommended placement (each cell = 16x16):

                +----------+----------+
                |          |          |
                |   CORE   |   CORE   |
                |          |          |
                +----------+----------+
                |          |          |
                |          |  FRAME   |
                |          |          |
                +----------+----------+

                Legend:
                - CORE  = Core (duplicated 16x16 cells; both must contain the same core texture)
                - FRAME = Frame (single 16x16 cell)
                - empty = unused / transparent

                Interpretation:
                - CORE occupies the top-left two cells (duplicated).
                - FRAME occupies the first cell of the second row.
                - Remaining cells may be transparent or used for variants.

                Behavior notes
                - JEI visualization: the mod registers visual recipes so JEI shows the conversion like the native mod.
                - Actual conversion: handled by the mod's event handler when you right-click an empty generator with the configured `recipe` item.
                - Items created by `/give` or conversion are produced via the same factory method, so they contain the correct DataComponent/NBT.
                - When placed, the BlockEntity reads the ItemStack (DataComponent/NBT) and configures itself.

                Commands
                - `/skb_res_gen reload` — reloads generator JSONs at runtime.
                - `/skb_res_gen give` — gives configured generator items (useful for testing).

                Example generator entry

                ```json
                {
                  "type": "skb_res_gen_custom:skb_res_gen_custom",
                  "generators": [
                    {
                      "base_id": "obsidian",
                      "name": "Obsidian",
                      "creative_tab": true,
                      "output": "minecraft:obsidian",
                      "times": [200, 150, 100, 80, 60, 40],
                      "stacks": [1, 2, 3, 4, 5, 6],
                      "recipe": "minecraft:obsidian"
                    },
                    {
                      "base_id": "red_sand",
                      "name": "Red Sand",
                      "creative_tab": true,
                      "output": "minecraft:red_sand",
                      "times": [400, 300, 200, 150, 100, 80],
                      "stacks": [1, 1, 2, 2, 4, 4],
                      "recipe": "minecraft:red_sand"
                    }
                  ]
                }
                ```

                Tips
                - Always include the `name` for readable tooltips.
                - Provide the six combined textures to avoid fallback.
                - Use the `recipe` field if you want players to convert empty generators in-world by right-click.
                """;

            Files.writeString(readme, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LOGGER.info("Wrote README for custom generators at {}", readme.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to write README for custom generators: {}", e.getMessage(), e);
        }
    }
}


