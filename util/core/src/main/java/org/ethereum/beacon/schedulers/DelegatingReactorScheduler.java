package org.ethereum.beacon.schedulers;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

public class DelegatingReactorScheduler implements reactor.core.scheduler.Scheduler {

  protected final reactor.core.scheduler.Scheduler delegate;
  protected final Supplier<Long> timeSupplier;

  public DelegatingReactorScheduler(Scheduler delegate, Supplier<Long> timeSupplier) {
    this.delegate = delegate;
    this.timeSupplier = timeSupplier;
  }

  @Nonnull
  @Override
  public Disposable schedule(@Nonnull Runnable task) {
    return delegate.schedule(task);
  }

  @Nonnull
  @Override
  public Disposable schedule(Runnable task, long delay,
      TimeUnit unit) {
    return delegate.schedule(task, delay, unit);
  }

  @Nonnull
  @Override
  public Disposable schedulePeriodically(Runnable task, long initialDelay, long period,
      TimeUnit unit) {
    return delegate.schedulePeriodically(task, initialDelay, period, unit);
  }

  @Override
  public long now(TimeUnit unit) {
    return unit.convert(timeSupplier.get(), TimeUnit.MILLISECONDS);
  }

  @Nonnull
  @Override
  public Worker createWorker() {
    return delegate.createWorker();
  }

  @Override
  public void dispose() {
    delegate.dispose();
  }

  @Override
  public void start() {
    delegate.start();
  }

  @Override
  public boolean isDisposed() {
    return delegate.isDisposed();
  }

  public static class DelegateWorker implements Worker {
    protected final Worker delegate;

    public DelegateWorker(Worker delegate) {
      this.delegate = delegate;
    }

    @Nonnull
    @Override
    public Disposable schedule(@Nonnull Runnable task) {
      return delegate.schedule(task);
    }

    @Nonnull
    @Override
    public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
      return delegate.schedule(task, delay, unit);
    }

    @Nonnull
    @Override
    public Disposable schedulePeriodically(Runnable task, long initialDelay, long period,
        TimeUnit unit) {
      return delegate.schedulePeriodically(task, initialDelay, period, unit);
    }

    @Override
    public void dispose() {
      delegate.dispose();
    }

    @Override
    public boolean isDisposed() {
      return delegate.isDisposed();
    }
  }
}
