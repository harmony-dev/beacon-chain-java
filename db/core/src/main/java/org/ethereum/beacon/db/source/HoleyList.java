package org.ethereum.beacon.db.source;

import java.util.Optional;
import java.util.function.Function;

/**
 * Add-only list which can miss elements at some positions and its size is the maximal element index
 * Also can be treated as <code>Map&lt;Long, V&gt;</code> with maximal key tracking.
 */
public interface HoleyList<V> {

  /**
   * Maximal index of inserted element
   */
  long size();

  /**
   * Put element at index <code>idx</code>
   * Increases size if necessary
   * If value is null nothing is modified
   */
  void put(long idx, V value);

  /**
   * Returns element at index <code>idx</code>
   * Empty instance is returned if no element with this index
   */
  Optional<V> get(long idx);

  /**
   * Puts element with index <code>size()</code>
   */
  default void add(V value) {
    put(size(), value);
  }

  /**
   * Handy functional method to update existing value or put a default if no value exists yet
   * @return new value
   */
  default V update(long idx, Function<V, V> updater, V defaultValue) {
    V newVal = get(idx).map(updater).orElse(defaultValue);
    put(idx, newVal);
    return newVal;
  }

  /**
   * Handy functional method to update existing value
   * @return new value if existed
   */
  default Optional<V> update(long idx, Function<V, V> updater) {
    return get(idx).map(val -> {
      V newVal = updater.apply(val);
      put(idx, newVal);
      return newVal;
    });
  }
}
