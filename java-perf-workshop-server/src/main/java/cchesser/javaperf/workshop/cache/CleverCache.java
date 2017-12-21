package cchesser.javaperf.workshop.cache;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A sorta LFU Cache, modified from Stack Overflow:
 * https://stackoverflow.com/a/23668899 (used as a basis).
 * 
 * @author JMonterrubio
 *
 */
public class CleverCache<K, V> {

	private Map<K, CacheEntry<V>> innerCache = new LinkedHashMap<>();
	private int cacheLimit;

	public CleverCache(int cacheLimit) {
		this.cacheLimit = cacheLimit;
	}

	public void store(K key, V value) {
		if (isFull()) {
			K keyToRemove = getLFUKey();
			innerCache.remove(keyToRemove);
		}

		CacheEntry<V> newEntry = new CacheEntry<V>();
		newEntry.data = value;
		newEntry.frequency = 0;

		innerCache.put(key, newEntry);
	}

	public V fetch(K key) {
		if (innerCache.containsKey(key)) {
			innerCache.get(key).frequency++;
		}

		return innerCache.get(key).data;
	}

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
