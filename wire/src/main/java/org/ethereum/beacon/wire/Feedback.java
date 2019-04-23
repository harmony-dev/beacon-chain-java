package org.ethereum.beacon.wire;

import java.util.concurrent.CompletableFuture;

public interface Feedback<TResult> {

  static <T> Feedback<T> of(T result) {
    return new Impl<>(result);
  }

  TResult get();

  void feedbackSuccess();

  void feedbackError(Throwable e);

  CompletableFuture<Void> getFeedback();

  <TOtherResult> Feedback<TOtherResult> delegate(TOtherResult otherResult);

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
