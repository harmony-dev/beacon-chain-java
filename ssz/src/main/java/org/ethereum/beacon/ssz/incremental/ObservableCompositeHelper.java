package org.ethereum.beacon.ssz.incremental;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ObservableCompositeHelper implements UpdateListener, ObservableComposite {

  private static final String PARENT_OBSERVER_ID = "parent";

  public class ObsValue<C> {
    private C value;
    private final int index;

    public ObsValue(C value, int index) {
      this.index = index;
      set(value);
    }

    public void set(C val) {
      if (val instanceof ObservableComposite) {
        ((ObservableComposite)val).getUpdateListener(PARENT_OBSERVER_ID, () ->
            new UpdateListener() {
              @Override
              public void childUpdated(int childIndex) {
                ObservableCompositeHelper.this.childUpdated(index);
              }

              @Override
              public UpdateListener fork() {
                return this;
              }
            }
        );
      }
      value = val;
      childUpdated(index);
    }

    public C get() {
      return value;
    }
  }

  private Map<String, UpdateListener> listeners;
  private int childCounter = 0;

  public ObservableCompositeHelper() {
    this(new ConcurrentHashMap<>());
  }

  public ObservableCompositeHelper(Map<String, UpdateListener> listeners) {
    this.listeners = listeners;
  }

  @Override
  public UpdateListener getUpdateListener(String observerId, Supplier<UpdateListener> listenerFactory) {
    return listeners.computeIfAbsent(observerId, s -> listenerFactory.get());
  }

  @Override
  public ObservableCompositeHelper fork() {
    return new ObservableCompositeHelper(copyListeners());
  }

  public void addAllListeners(Map<String, UpdateListener> listeners) {
    listeners.putAll(listeners);
  }

  @Override
  public Map<String, UpdateListener> getAllUpdateListeners() {
    return copyListeners();
  }

  private Map<String, UpdateListener> copyListeners() {
    Map<String, UpdateListener> lCopies = new ConcurrentHashMap<>();
    for (Entry<String, UpdateListener> entry : listeners.entrySet()) {
      if (!PARENT_OBSERVER_ID.equals(entry.getKey())) {
        lCopies.put(entry.getKey(), entry.getValue().fork());
      }
    }
    return lCopies;
  }

  @Override
  public void childUpdated(int childIndex) {
    listeners.values().forEach(l -> l.childUpdated(childIndex));
  }

  public void childrenUpdated(int fromIdx, int count) {
    for (int i = 0; i < count; i++) {
      childUpdated(fromIdx + i);
    }
  }

  public <C> ObsValue<C> newValue(C initialValue) {
    return new ObsValue<>(initialValue, childCounter++);
  }
}
