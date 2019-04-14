package org.ethereum.beacon.ssz.incremental;

import java.util.Map;
import java.util.function.Supplier;

public interface ObservableComposite {

  UpdateListener getUpdateListener(String observerId, Supplier<UpdateListener> listenerFactory);

  Map<String, UpdateListener> getAllUpdateListeners();
}
