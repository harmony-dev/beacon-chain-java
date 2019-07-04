package org.ethereum.beacon.time.provider;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.stream.SimpleProcessor;
import org.reactivestreams.Publisher;

import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/** Time provider using NTP servers to correct time every defined period */
public class NetworkTime implements TimeProvider {
  private static final int MILLIS_IN_SEC = 1000;
  private static final int DOUBLE_CHECK_DIFF = 5 * MILLIS_IN_SEC;
  private final SimpleProcessor<Time> timeProcessor;

  private final List<String> ntpServers;
  private final SecureRandom random = new SecureRandom();
  private final AtomicLong latest = new AtomicLong(-1);
  private final AtomicLong offset = new AtomicLong(0);
  private String latestServer = "";

  public NetworkTime(
      Scheduler events,
      Scheduler worker,
      Scheduler networkWorker,
      List<String> ntpServers,
      long correctionPeriodMs) {
    this.ntpServers = ntpServers;
    this.timeProcessor = new SimpleProcessor<Time>(events, "TimeProvider.system");
    worker.executeR(
        () -> {
          while (!Thread.interrupted()) {
            emitTime();
          }
        });
    networkWorker.executeAtFixedRate(
        Duration.ZERO,
        Duration.ofMillis(correctionPeriodMs),
        () -> {
          try {
            String server = getNextServer(latestServer);
            long newOffset = pullOffset(server);
            // If we get big difference - recheck
            if (Math.abs(newOffset - offset.get()) > DOUBLE_CHECK_DIFF) {
              server = getNextServer(server);
              newOffset = pullOffset(server);
            }
            this.latestServer = server;

            // Reset latest if offset has changed dramatically to reset emitting
            if (Math.abs(newOffset - offset.get()) > MILLIS_IN_SEC) {
              latest.set(-1);
            }
            offset.set(newOffset);
          } catch (Exception e) {
            e.printStackTrace();
            // TODO: log me
          }
        });
  }

  /** Clock offset in ms needed to adjust local clock to match remote clock */
  long pullOffset(String serverHost) {
    try {
      NTPUDPClient timeClient = new NTPUDPClient();
      InetAddress inetAddress = InetAddress.getByName(serverHost);
      TimeInfo timeInfo = timeClient.getTime(inetAddress);
      timeInfo.computeDetails();
      return timeInfo.getOffset();
    } catch (Exception ex) {
      throw new RuntimeException(
          String.format("Failed to get time from server %s", serverHost), ex);
    }
  }

  private String getNextServer(String filter) {
    List<String> remaining =
        ntpServers.stream().filter(s -> !s.equals(filter)).collect(Collectors.toList());
    if (remaining.isEmpty()) {
      remaining = ntpServers;
    }

    return remaining.get(random.nextInt(remaining.size()));
  }

  private void emitTime() {
    Instant nowCorrected = Instant.now().plusMillis(offset.get());
    long currentSec = nowCorrected.getEpochSecond();
    long currentMs = nowCorrected.getLong(ChronoField.MILLI_OF_SECOND);
    if (currentSec > latest.get()) {
      timeProcessor.onNext(Time.of(currentSec));
      latest.set(currentSec);
    }
    try {
      Thread.sleep(MILLIS_IN_SEC - currentMs);
    } catch (InterruptedException e) {
      throw new RuntimeException("NetworkTime stream thread interrupted!", e);
    }
  }

  @Override
  public Publisher<Time> getTimeStream() {
    return timeProcessor;
  }
}
