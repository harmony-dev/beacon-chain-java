package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.javatuples.Pair;

/**
 * Searches for node in {@link Field#NODE} and requests session resolving using {@link
 * Field#SESSION_LOOKUP}
 */
public class NodeSessionRequestHandler implements EnvelopeHandler {

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.NODE)) {
      return;
    }
    envelope.put(
        Field.SESSION_LOOKUP,
        Pair.with(
            ((NodeRecord) envelope.get(Field.NODE)).getNodeId(),
            (Runnable)
                () -> { // TODO: failure
                }));
  }
}
