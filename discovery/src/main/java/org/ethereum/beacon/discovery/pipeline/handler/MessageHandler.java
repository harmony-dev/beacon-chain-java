package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.DiscoveryV5MessageProcessor;
import org.ethereum.beacon.discovery.MessageProcessor;
import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.message.DiscoveryMessage;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;

public class MessageHandler implements EnvelopeHandler {
  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.MESSAGE)) {
      return;
    }
    if (!envelope.contains(Field.SESSION)) {
      return;
    }

    NodeSession session = (NodeSession) envelope.get(Field.SESSION);
    DiscoveryMessage message = (DiscoveryMessage) envelope.get(Field.MESSAGE);
    // TODO: optimize
    MessageProcessor messageProcessor = new MessageProcessor(new DiscoveryV5MessageProcessor());
    messageProcessor.handleIncoming(message, session);
  }
}
