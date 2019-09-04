package org.ethereum.beacon.node.metrics;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.util.collections.Bitlist;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements (some) metrics from Beacon chain metrics specs.
 * @See <a href="https://github.com/ethereum/eth2.0-metrics/blob/master/metrics.md"/>
 *
 */
public class Metrics {
  static Gauge PEERS =
      Gauge.build().name("beaconchain_peers").help("Tracks number of peers").register();
  static Gauge CURRENT_SLOT =
      Gauge.build()
          .name("beaconchain_current_slot")
          .help("Latest slot recorded by the beacon chain")
          .register();
  // extra metric, not required by spec
  static Gauge CURRENT_EPOCH =
      Gauge.build()
          .name("beaconchain_current_epoch")
          .help("Latest epoch recorded by the beacon chain")
          .register();
  static Gauge CURRENT_JUSTIFIED_EPOCH =
      Gauge.build()
          .name("beaconchain_current_justified_epoch")
          .help("Current justified epoch")
          .register();
  static Gauge CURRENT_FINALIZED_EPOCH =
      Gauge.build()
          .name("beaconchain_current_finalized_epoch")
          .help("Current finalized epoch")
          .register();
  static Gauge CURRENT_PREV_JUSTIFIED_EPOCH =
      Gauge.build()
          .name("beaconchain_current_prev_justified_epoch")
          .help("Current previously justified epoch")
          .register();
  // the metric implementation is probably partially correct, since it accounts only validators,
  // which have been included in the chain
  static Gauge CURRENT_EPOCH_LIVE_VALIDATORS =
      Gauge.build()
          .name("beaconchain_current_epoch_live_validators")
          .help("Number of active validators who reported for the current epoch")
          .register();
  // the metric implementation is probably partially correct, since it accounts only validators,
  // which have been included in the chain
  static Gauge PREVIOUS_EPOCH_LIVE_VALIDATORS =
      Gauge.build()
          .name("beaconchain_previous_epoch_live_validators")
          .help("Number of active validators who reported for the previous epoch")
          .register();
  // not yet implemented
  static Counter REORG_EVENTS_TOTAL =
      Counter.build()
          .name("beaconchain_reorg_events_total")
          .help("Occurrence of a reorganization of the chain")
          .register();
  // not yet supported by beacon chain yet
  static Gauge PENDING_DEPOSITS =
      Gauge.build()
          .name("beaconchain_pending_deposits")
          .help("Number of pending deposits")
          .register();
  // not yet supported by beacon chain yet
  static Gauge PENDING_EXITS =
      Gauge.build().name("beaconchain_pending_exits").help("Number of pending exits").register();
  static Gauge TOTAL_DEPOSITS =
      Gauge.build().name("beaconchain_total_deposits").help("Number of total deposits").register();
  // not yet implemented
  static Gauge PREVIOUS_EPOCH_STALE_BLOCKS =
      Gauge.build()
          .name("beaconchain_previous_epoch_stale_blocks")
          .help("Number of blocks not included into canonical chain in the previous epoch")
          .register();
  // simple implementation currently - counts attestations arrived from wire during a slot
  static Gauge PROPAGATED_ATTESTATIONS =
      Gauge.build()
          .name("beaconchain_propagated_attestations")
          .help("Number of distinct attestations to a slot received from the wire")
          .register();
  private static HTTPServer metricsServer;

  private static final Object attestation_lock = new Object();
  private static SlotNumber currentSlot = null;
  /**
   * Accumulates atetstations received during a slot. Cleared on a new slot.
   */
  private static final Map<AttestationData, Bitlist> currentSlotAttestations = new HashMap<>();

  // setting initial values explicitly (zeros by default)
  static {
    PEERS.set(0);
    CURRENT_SLOT.set(Double.NaN);
    CURRENT_EPOCH.set(Double.NaN);
    CURRENT_JUSTIFIED_EPOCH.set(Double.NaN);
    CURRENT_FINALIZED_EPOCH.set(Double.NaN);
    CURRENT_PREV_JUSTIFIED_EPOCH.set(Double.NaN);
    CURRENT_EPOCH_LIVE_VALIDATORS.set(Double.NaN);
    PREVIOUS_EPOCH_LIVE_VALIDATORS.set(Double.NaN);
    PENDING_DEPOSITS.set(0);
    PENDING_EXITS.set(0);
    TOTAL_DEPOSITS.set(Double.NaN);
    PREVIOUS_EPOCH_STALE_BLOCKS.set(Double.NaN);
    PROPAGATED_ATTESTATIONS.set(Double.NaN);
  }

  public static void startMetricsServer(String host, int port) {
    if (metricsServer != null) {
      throw new IllegalStateException("Metrics server already started");
    }
    try {
      metricsServer = new HTTPServer(host, port);
    } catch (IOException e) {
      new RuntimeException("Cannot start metrics server", e);
    }
  }

  public static void stopMetricsServer() {
    if (metricsServer == null) {
      throw new IllegalStateException("Metrics server has not been started");
    } else {
      metricsServer.stop();
      metricsServer = null;
    }
  }

  public static void peerAdded() {
    PEERS.inc();
  }

  public static void peerRemoved() {
    PEERS.dec();
  }

  public static void onNewState(BeaconChainSpec spec, ObservableBeaconState obs) {
    BeaconStateEx state = obs.getLatestSlotState();

    synchronized (attestation_lock) {
      if (currentSlot == null) {
        currentSlot = state.getSlot();
      } else if (currentSlot.less(state.getSlot())) {
        currentSlot = state.getSlot();
        currentSlotAttestations.clear();
      }
    }

    CURRENT_SLOT.set(state.getSlot().doubleValue());
    CURRENT_EPOCH.set(spec.get_current_epoch(state).doubleValue());
    CURRENT_JUSTIFIED_EPOCH.set(state.getCurrentJustifiedCheckpoint().getEpoch().doubleValue());
    CURRENT_FINALIZED_EPOCH.set(state.getFinalizedCheckpoint().getEpoch().doubleValue());
    CURRENT_PREV_JUSTIFIED_EPOCH.set(
        state.getPreviousJustifiedCheckpoint().getEpoch().doubleValue());

    CURRENT_EPOCH_LIVE_VALIDATORS.set(
        calculateEpochValidators(state.getCurrentEpochAttestations().listCopy()));
    PREVIOUS_EPOCH_LIVE_VALIDATORS.set(
        calculateEpochValidators(state.getPreviousEpochAttestations().listCopy()));

    TOTAL_DEPOSITS.set(state.getEth1Data().getDepositCount().doubleValue());
  }

  private static int calculateEpochValidators(List<PendingAttestation> epochAttestations) {
    return epochAttestations.stream()
        .mapToInt(pa -> pa.getAggregationBits().getBits().size())
        .sum();
  }

  public static void attestationPropagated(Attestation attestation) {
    Bitlist aggregationBits = attestation.getAggregationBits();
    AttestationData attestationData = attestation.getData();

    int attestationCount;
    synchronized (attestation_lock) {
      Bitlist attestations;
      if (!currentSlotAttestations.containsKey(attestationData)) {
        attestations = aggregationBits.cappedCopy(aggregationBits.maxSize());
      } else {
        attestations = currentSlotAttestations.get(attestationData).or(aggregationBits);
      }
      currentSlotAttestations.put(attestationData, attestations);
      attestationCount =
          currentSlotAttestations.values().stream().mapToInt(bl -> bl.getBits().size()).sum();
    }

    PROPAGATED_ATTESTATIONS.set(attestationCount);
  }
}
