package com.bingocook.cooking;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Holds the effective heat source set: datapack defaults plus runtime command
 * overlays. Runtime overlays are cleared on every data pack reload.
 */
public final class HeatSourceManager {
    public enum Source {
        DATAPACK,
        RUNTIME
    }

    public record AnnotatedEntry(HeatSourceEntry entry, Source source) {
    }

    public static final HeatSourceManager INSTANCE = new HeatSourceManager();

    private volatile Set<HeatSourceEntry> datapackLoaded = Set.of();
    private final Set<HeatSourceEntry> runtimeAdded = new LinkedHashSet<>();
    private final Set<HeatSourceEntry> runtimeRemoved = new LinkedHashSet<>();

    private HeatSourceManager() {
    }

    /**
     * Called by {@link HeatSourceLoader} after reloading datapack entries. Clears
     * all runtime command overlays.
     */
    public void onDatapackReload(Set<HeatSourceEntry> loaded) {
        synchronized (this) {
            this.datapackLoaded = Set.copyOf(loaded);
            this.runtimeAdded.clear();
            this.runtimeRemoved.clear();
        }
    }

    /**
     * @return the current effective heat source set.
     */
    public Set<HeatSourceEntry> effective() {
        synchronized (this) {
            LinkedHashSet<HeatSourceEntry> result = new LinkedHashSet<>(this.datapackLoaded);
            result.addAll(this.runtimeAdded);
            result.removeAll(this.runtimeRemoved);
            return Set.copyOf(result);
        }
    }

    /**
     * @return effective entries annotated with their origin for command listing.
     */
    public Set<AnnotatedEntry> effectiveAnnotated() {
        synchronized (this) {
            LinkedHashSet<AnnotatedEntry> result = new LinkedHashSet<>();
            for (HeatSourceEntry entry : this.datapackLoaded) {
                if (!this.runtimeRemoved.contains(entry)) {
                    result.add(new AnnotatedEntry(entry, Source.DATAPACK));
                }
            }
            for (HeatSourceEntry entry : this.runtimeAdded) {
                if (!this.runtimeRemoved.contains(entry)) {
                    result.add(new AnnotatedEntry(entry, Source.RUNTIME));
                }
            }
            return Set.copyOf(result);
        }
    }

    /**
     * Adds a runtime heat source (until the next {@code /reload}).
     *
     * @return true if the entry was newly added to the runtime overlay
     */
    public boolean addRuntime(HeatSourceEntry entry) {
        synchronized (this) {
            this.runtimeRemoved.remove(entry);
            return this.runtimeAdded.add(entry);
        }
    }

    /**
     * Removes a heat source from the effective set until the next {@code /reload}.
     *
     * @return true if the entry was present in the effective set
     */
    public boolean removeRuntime(HeatSourceEntry entry) {
        synchronized (this) {
            if (this.runtimeAdded.remove(entry)) {
                return true;
            }
            if (this.datapackLoaded.contains(entry) && !this.runtimeRemoved.contains(entry)) {
                return this.runtimeRemoved.add(entry);
            }
            return false;
        }
    }

    /**
     * @return a sorted, comma-separated summary of effective heat sources.
     */
    public String effectiveSummary() {
        return effectiveAnnotated().stream()
                .sorted((a, b) -> a.entry().raw().compareTo(b.entry().raw()))
                .map(annotated -> annotated.entry().raw() + " (" + annotated.source().name().toLowerCase() + ")")
                .collect(Collectors.joining(", "));
    }
}
