package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static org.ethereum.beacon.discovery.pipeline.handler.WhoAreYouContextHandler.CONTEXT;

public class TaskHandler implements EnvelopeHandler {

  public static final String TASK = "task";
  public static final String PING = "PING";
  public static final String FUTURE = "future";

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(TASK)) {
      return;
    }
    if (!envelope.get(TASK).equals(PING)) {
      return; // TODO: implement other tasks
    }
    NodeContext context = (NodeContext) envelope.get(CONTEXT);
    CompletableFuture<Void> completableFuture = (CompletableFuture<Void>) envelope.get(FUTURE);
    context.initiate().whenComplete((aVoid, throwable) -> {
      if (throwable != null) {
        completableFuture.completeExceptionally(throwable);
      } else {
        completableFuture.complete(aVoid);
      }
    });

  }
}
