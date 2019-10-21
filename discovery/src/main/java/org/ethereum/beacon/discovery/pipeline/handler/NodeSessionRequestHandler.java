package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import org.javatuples.Pair;

/**
 * Searches for node in {@link Field#NODE} and requests session resolving using {@link
 * Field#SESSION_LOOKUP}
 */
public class NodeSessionRequestHandler implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(NodeSessionRequestHandler.class);

  @Override
  public void handle(Envelope envelope) {
    logger.trace(
        () ->
            String.format(
                "Envelope %s in NodeSessionRequestHandler, checking requirements satisfaction",
                envelope.getId()));
    if (!HandlerUtil.requireField(Field.NODE, envelope)) {
      return;
    }
    logger.trace(
        () ->
            String.format(
                "Envelope %s in NodeSessionRequestHandler, requirements are satisfied!",
                envelope.getId()));

    envelope.put(
        Field.SESSION_LOOKUP,
        Pair.with(((NodeRecord) envelope.get(Field.NODE)).getNodeId(), (Runnable) () -> {}));
  }
}
