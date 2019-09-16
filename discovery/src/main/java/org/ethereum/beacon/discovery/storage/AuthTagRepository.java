package org.ethereum.beacon.discovery.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.NodeContext;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In memory repository with authTags, corresponding contexts {@link NodeContext} and 2-way getters:
 * {@link #get(BytesValue)} and {@link #getTag(NodeContext)}
 *
 * <p>Expired authTags should be manually removed with {@link #expire(NodeContext)}
 */
public class AuthTagRepository {
  private static final Logger logger = LogManager.getLogger(AuthTagRepository.class);
  private Map<BytesValue, NodeContext> authTags = new ConcurrentHashMap<>();
  private Map<NodeContext, BytesValue> contexts = new ConcurrentHashMap<>();

  public synchronized void put(BytesValue authTag, NodeContext context) {
    logger.trace(
        () ->
            String.format(
                "PUT: authTag[%s] => nodeContext[%s]",
                authTag, context.getNodeRecord().getNodeId()));
    authTags.put(authTag, context);
    contexts.put(context, authTag);
  }

  public Optional<NodeContext> get(BytesValue authTag) {
    logger.trace(() -> String.format("GET: authTag[%s]", authTag));
    NodeContext context = authTags.get(authTag);
    return context == null ? Optional.empty() : Optional.of(context);
  }

  public Optional<BytesValue> getTag(NodeContext context) {
    logger.trace(() -> String.format("GET: context %s", context));
    BytesValue authTag = contexts.get(context);
    return authTag == null ? Optional.empty() : Optional.of(authTag);
  }

  public synchronized void expire(NodeContext context) {
    logger.trace(() -> String.format("REMOVE: context %s", context));
    BytesValue authTag = contexts.remove(context);
    logger.trace(
        () ->
            authTag == null
                ? "Context %s not found, was not removed"
                : String.format("Context %s removed with authTag[%s]", context, authTag));
    if (authTag != null) {
      authTags.remove(authTag);
    }
  }
}
