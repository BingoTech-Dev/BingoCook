package com.bingocook.cooking;

import java.io.Reader;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Loads configured cooking heat sources from data packs.
 *
 * <p>Files are read from {@code data/<namespace>/bingocook/heat_sources.json} in
 * every namespace. JSON format:
 * <pre>{@code {"replace": false, "values": ["minecraft:campfire", "#minecraft:campfires"]}}</pre>
 *
 * <p>Merge semantics match {@link ElementTypeLoader}.
 */
public final class HeatSourceLoader implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath("bingocook", "heat_sources");

    public static final HeatSourceLoader INSTANCE = new HeatSourceLoader();

    private static final String FILE_PATH = "bingocook/heat_sources.json";

    private HeatSourceLoader() {
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Set<HeatSourceEntry> loaded = new LinkedHashSet<>();
        Map<Identifier, List<Resource>> stacks = manager.listResourceStacks("bingocook",
                location -> location.getPath().equals(FILE_PATH));

        @SuppressWarnings("null")
        List<Identifier> fileIds = stacks.keySet().stream()
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
        for (Identifier fileId : fileIds) {
            List<Resource> resources = stacks.get(fileId);
            Resource resource = resources.get(resources.size() - 1);
            try (Reader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                boolean replace = json.has("replace") && json.get("replace").getAsBoolean();
                if (replace) {
                    loaded.clear();
                }
                for (JsonElement value : json.getAsJsonArray("values")) {
                    loaded.add(HeatSourceEntry.parse(value.getAsString()));
                }
            } catch (Exception exception) {
                LOGGER.error("Could not read heat sources from {}", resource.sourcePackId(), exception);
            }
        }

        HeatSourceManager.INSTANCE.onDatapackReload(Set.copyOf(loaded));
    }
}
