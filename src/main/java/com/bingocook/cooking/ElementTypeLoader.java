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
 * Loads the set of cooking element types from data packs.
 *
 * <p>Files are read from {@code data/<namespace>/bingocook/element_types.json} in
 * every namespace. JSON format:
 * <pre>{@code {"replace": false, "values": ["bingocook:fruit", ...]}}</pre>
 *
 * <p>Merge semantics:
 * <ul>
 * <li>Files from different namespaces merge into one shared set.</li>
 * <li>For the same path (same namespace), only the highest-priority pack's file is
 * read - a later pack fully overrides an earlier one.</li>
 * <li>{@code "replace": true} clears the set accumulated so far before applying
 * this file's values - this is how packs remove mod-shipped default elements.</li>
 * <li>Namespaces are processed in lexicographic order of their IDs so that replace
 * behaviour across namespaces is deterministic.</li>
 * </ul>
 *
 * <p>Registered through
 * {@link net.neoforged.neoforge.event.AddServerReloadListenersEvent} on the
 * NeoForge event bus (see {@link CookingEvents}); the loaded set is server-side
 * only. The item data maps are loaded by NeoForge's own loader, so this listener
 * needs no dependency edges.
 */
public final class ElementTypeLoader implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath("bingocook", "element_types");

    public static final ElementTypeLoader INSTANCE = new ElementTypeLoader();

    /** Path of the data file relative to each namespace: {@code data/<namespace>/bingocook/element_types.json}. */
    private static final String FILE_PATH = "bingocook/element_types.json";

    private volatile Set<CookingElement> elements = Set.of();

    private ElementTypeLoader() {
    }

    public Set<CookingElement> getElements() {
        return elements;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Set<CookingElement> loaded = new LinkedHashSet<>();
        // FileToIdConverter cannot be used here: it treats the prefix as a directory, but
        // element_types.json IS the file. List via the "bingocook" directory prefix and
        // filter by the exact path instead.
        Map<Identifier, List<Resource>> stacks = manager.listResourceStacks("bingocook",
                location -> location.getPath().equals(FILE_PATH));

        List<Identifier> fileIds = stacks.keySet().stream()
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
        for (Identifier fileId : fileIds) {
            List<Resource> resources = stacks.get(fileId);
            // Highest-priority pack comes last; a later pack overrides an earlier one at the same path.
            Resource resource = resources.get(resources.size() - 1);
            try (Reader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                boolean replace = json.has("replace") && json.get("replace").getAsBoolean();
                if (replace) {
                    loaded.clear();
                }
                for (JsonElement value : json.getAsJsonArray("values")) {
                    loaded.add(new CookingElement(Identifier.parse(value.getAsString())));
                }
            } catch (Exception exception) {
                LOGGER.error("Could not read element types from {}", resource.sourcePackId(), exception);
            }
        }

        this.elements = Set.copyOf(loaded);
    }
}
