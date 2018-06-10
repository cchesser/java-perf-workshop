package cchesser.javaperf.workshop.cache;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * An LFU Cache with history tracking, modified
 * from Stack Overflow: https://stackoverflow.com/a/23668899 (used as a basis).
 *
 * @author JMonterrubio
 */
public class CleverCache<K, V> {

    private Map<K, CacheEntry<V>> innerCache;
    private int cacheLimit;

    public CleverCache(int cacheLimit) {
        this.cacheLimit = cacheLimit;
        innerCache = new HashMap<>(cacheLimit);
    }

    /**
     * Stores the given key value pair in the cache
     *
     * @param key   the unique identifier associated with the given value
     * @param value the value mapped to the given key
     */
    public void store(K key, V value) {
        if (isFull()) {
            K keyToRemove = getLFUKey();
            CleverCache<K, V>.CacheEntry<V> removedEntry = innerCache.remove(keyToRemove);
        }

        CacheEntry<V> newEntry = new CacheEntry<V>();
        newEntry.data = value;
        newEntry.frequency = 0;

        innerCache.put(key, newEntry);
    }

    /**
     * Retrieves the value associated with the given key from the cache.
     *
     * @param key the key that maps to that value
     * @return the value mapped to the key
     * @throws NullPointerException if the key doesn't exist
     */
    public V fetch(K key) {
        if (innerCache.containsKey(key)) {
            innerCache.get(key).frequency++;
        }

        return innerCache.get(key).data;
    }

    /**
     * Indicates whether a key exists in the cache or not
     *
     * @param key the key to check
     * @return <code>true</code> if the key is in the cache, <code>false</code>
     * otherwise.
     */
    public boolean exists(K key) {
        return innerCache.containsKey(key);
    }

    private boolean isFull() {
        return innerCache.size() == cacheLimit;
    }

    private K getLFUKey() {
        Optional<Entry<K, CleverCache<K, V>.CacheEntry<V>>> lfuEntry = innerCache.entrySet().stream()
                .collect(Collectors.minBy(Comparator.comparingInt(entry -> entry.getValue().frequency)));

        return lfuEntry.get().getKey();
    }

    private class CacheEntry<T> {
        private T data;
        private int frequency;
    }

}
