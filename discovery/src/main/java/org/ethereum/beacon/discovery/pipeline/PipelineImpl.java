package org.ethereum.beacon.discovery.pipeline;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.ethereum.beacon.discovery.pipeline.Field.INCOMING;

public class PipelineImpl implements Pipeline {
  private final List<EnvelopeHandler> envelopeHandlers = new ArrayList<>();
  private final AtomicBoolean started = new AtomicBoolean(false);
  private Flux<Envelope> pipeline = ReplayProcessor.cacheLast();
  private final FluxSink<Envelope> pipelineSink = ((ReplayProcessor<Envelope>) pipeline).sink();
  private Disposable subscription;

  @Override
  public synchronized Pipeline build() {
    started.set(true);
    for (EnvelopeHandler handler : envelopeHandlers) {
      pipeline = pipeline.doOnNext(handler::handle);
    }
    this.subscription = Flux.from(pipeline).subscribe();
    return this;
  }

  @Override
  public void push(Object object) {
    if (!started.get()) {
      throw new RuntimeException("You should build pipeline first");
    }
    if (!(object instanceof Envelope)) {
      Envelope envelope = new Envelope();
      envelope.put(INCOMING, object);
      pipelineSink.next(envelope);
    } else {
      pipelineSink.next((Envelope) object);
    }
  }

  @Override
  public Pipeline addHandler(EnvelopeHandler envelopeHandler) {
    if (started.get()) {
      throw new RuntimeException("Pipeline already started, couldn't add any handlers");
    }
    envelopeHandlers.add(envelopeHandler);
    return this;
  }

  @Override
  public Publisher<Envelope> getOutgoingEnvelopes() {
    return pipeline;
  }
}
