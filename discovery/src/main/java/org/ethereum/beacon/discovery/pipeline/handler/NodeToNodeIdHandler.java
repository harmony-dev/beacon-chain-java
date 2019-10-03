package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.javatuples.Pair;

import static org.ethereum.beacon.discovery.pipeline.PipelineImpl.INCOMING;
import static org.ethereum.beacon.discovery.pipeline.handler.NodeIdContextHandler.NEED_CONTEXT;

public class NodeToNodeIdHandler implements EnvelopeHandler {

  public static final String NODE = "node";

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(NODE)) {
      return;
    }
    envelope.put(
        NEED_CONTEXT,
        Pair.with(
            ((NodeRecord) envelope.get(NODE)).getNodeId(),
            (Runnable)
                () -> { // TODO: failure
                }));
  }
}
