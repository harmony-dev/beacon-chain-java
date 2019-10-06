package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.javatuples.Pair;

/**
 * Searches for node in {@link Field#NODE} and requests context resolving using {@link
 * Field#NEED_CONTEXT}
 */
public class NodeContextRequestHandler implements EnvelopeHandler {

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.NODE)) {
      return;
    }
    envelope.put(
        Field.NEED_CONTEXT,
        Pair.with(
            ((NodeRecord) envelope.get(Field.NODE)).getNodeId(),
            (Runnable)
                () -> { // TODO: failure
                }));
  }
}
