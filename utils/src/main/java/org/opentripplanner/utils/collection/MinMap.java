package org.opentripplanner.utils.collection;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Provides a map-like data structure that tracks the smallest value for each key.
 */
public class MinMap<K, V> {

  private final Map<K, V> map = new HashMap<>();
  private final Comparator<V> comparator;

  private MinMap(Comparator<V> comparator) {
    this.comparator = comparator;
  }

  /**
   * Create a new MinMap with natural order comparison for values. The value must implement
   * the Comparable interface.
   */
  public static <Key, Value extends Comparable<Value>> MinMap<Key, Value> of() {
    return new MinMap<Key, Value>(Comparator.naturalOrder());
  }

  /**
   * Create a MinMap with a custom comparator for ordering.
   */
  public static <Key, Value> MinMap<Key, Value> of(Comparator<Value> comparator) {
    return new MinMap<>(comparator);
  }

  /**
   * Put the given key-value pair in the map if the map does not yet contain the key, or if the
   * value is less than the existing value for the same key.
   *
   * @see Map#put(Object, Object)
   * @return whether the key-value pair was inserted in the map.
   */
  public boolean putMin(K key, V value) {
    V oldValue = map.get(key);
    if (oldValue == null || comparator.compare(value, oldValue) < 0) {
      map.put(key, value);
      return true;
    }
    return false;
  }

  /**
   * @see Map#get(Object)
   */
  @Nullable
  public V get(K key) {
    return map.get(key);
  }

  /**
   * @see Map#values()
   */
  public Collection<V> values() {
    return map.values();
  }
}
