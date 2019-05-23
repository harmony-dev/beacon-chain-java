package org.ethereum.beacon.wire;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Wrapper for some asynchronous result for which the result consumer may leave a feedback.
 *
 * E.g. blocks downloaded from a remote peer are then later process and verified. The PeerManager
 * may want to know if a peer sends invalid blocks and thus ban it.
 *
 * @param <TResult>
 */
public interface Feedback<TResult> {

  static <T> Feedback<T> of(T result) {
    return new Impl<>(result);
  }

  /**
   * Return the wrapped value
   */
  TResult get();

  /**
   * Report the value is OK
   */
  void feedbackSuccess();

  /**
   * Report the value is erroneous
   */
  void feedbackError(Throwable e);

  /**
   * Creates a CompletableFuture which is done when feedback left
   */
  CompletableFuture<Void> getFeedback();

  /**
   * Creates another Feedback with other value which forwards feedback to this instance
   * @see #map(Function)
   */
  <TOtherResult> Feedback<TOtherResult> delegate(TOtherResult otherResult);

  /**
   * Convenient shortcut for {@link #delegate(Object)} method.
   * Converts wrapped value to another Feedback wrapped value
   */
  default <TOtherResult> Feedback<TOtherResult> map(Function<TResult, TOtherResult> mapper) {
    return delegate(mapper.apply(get()));
  }

  class Impl<TResult> implements Feedback<TResult> {
    private final TResult result;
    private final CompletableFuture<Void> feedback = new CompletableFuture<>();

    private Impl(TResult result) {
      this.result = result;
    }

    @Override
    public TResult get() {
      return result;
    }

    @Override
    public void feedbackSuccess() {
      feedback.complete(null);
    }

    @Override
    public void feedbackError(Throwable e) {
      feedback.completeExceptionally(e);
    }

    public CompletableFuture<Void> getFeedback() {
      return feedback;
    }

    @Override
    public <TOtherResult> Feedback<TOtherResult> delegate(TOtherResult otherResult) {
      Impl<TOtherResult> ret = new Impl<>(otherResult);
      ret.getFeedback().whenComplete((v, t) -> {
        if (t == null) {
          feedbackSuccess();
        } else {
          feedbackError(t);
        }
      });
      return ret;
    }
  }
}
