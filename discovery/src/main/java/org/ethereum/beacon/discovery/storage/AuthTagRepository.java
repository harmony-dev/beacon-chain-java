package org.ethereum.beacon.discovery.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.NodeSession;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory repository with authTags, corresponding sessions {@link NodeSession} and 2-way getters:
 * {@link #get(BytesValue)} and {@link #getTag(NodeSession)}
 *
 * <p>Expired authTags should be manually removed with {@link #expire(NodeSession)}
 */
public class AuthTagRepository {
  private static final Logger logger = LogManager.getLogger(AuthTagRepository.class);
  private Map<BytesValue, NodeSession> authTags = new ConcurrentHashMap<>();
  private Map<NodeSession, BytesValue> sessions = new ConcurrentHashMap<>();

  public synchronized void put(BytesValue authTag, NodeSession session) {
    logger.trace(
        () ->
            String.format(
                "PUT: authTag[%s] => nodeSession[%s]",
                authTag, session.getNodeRecord().getNodeId()));
    authTags.put(authTag, session);
    sessions.put(session, authTag);
  }

  public Optional<NodeSession> get(BytesValue authTag) {
    logger.trace(() -> String.format("GET: authTag[%s]", authTag));
    NodeSession session = authTags.get(authTag);
    return session == null ? Optional.empty() : Optional.of(session);
  }

  public Optional<BytesValue> getTag(NodeSession session) {
    logger.trace(() -> String.format("GET: session %s", session));
    BytesValue authTag = sessions.get(session);
    return authTag == null ? Optional.empty() : Optional.of(authTag);
  }

  public synchronized void expire(NodeSession session) {
    logger.trace(() -> String.format("REMOVE: session %s", session));
    BytesValue authTag = sessions.remove(session);
    logger.trace(
        () ->
            authTag == null
                ? "Session %s not found, was not removed"
                : String.format("Session %s removed with authTag[%s]", session, authTag));
    if (authTag != null) {
      authTags.remove(authTag);
    }
  }
}
