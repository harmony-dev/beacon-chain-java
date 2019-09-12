package org.ethereum.beacon.node.metrics;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import org.ethereum.beacon.chain.BeaconChainHead;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
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
  /* Interop Metrics */
  static Gauge PEERS =
      Gauge.build().name("libp2p_peers").help("Tracks number of libp2p peers").register();
  static Gauge CURRENT_SLOT =
      Gauge.build().name("beacon_slot").help("Latest slot of the beacon chain state").register();
  // extra metric, not required by spec
  static Gauge CURRENT_EPOCH =
      Gauge.build().name("beacon_epoch").help("Latest epoch of the beacon chain state").register();
  static Gauge HEAD_SLOT =
      Gauge.build()
          .name("beacon_head_slot")
          .help("Slot of the head block of the beacon chain")
          .register();
  static Gauge HEAD_ROOT =
      Gauge.build()
          .name("beacon_head_root")
          .help("Root of the head block of the beacon chain")
          .register();
  // extra metric, not required by spec
  static Gauge CURRENT_JUSTIFIED_EPOCH =
      Gauge.build()
          .name("beacon_current_justified_epoch")
          .help("Current justified epoch")
          .register();
  static Gauge CURRENT_JUSTIFIED_ROOT =
      Gauge.build()
          .name("beacon_current_justified_root")
          .help("Current justified root")
          .register();
  static Gauge CURRENT_FINALIZED_EPOCH =
      Gauge.build().name("beacon_finalized_epoch").help("Current finalized epoch").register();
  static Gauge CURRENT_FINALIZED_ROOT =
      Gauge.build().name("beacon_finalized_root").help("Current finalized root").register();
  static Gauge CURRENT_PREV_JUSTIFIED_EPOCH =
      Gauge.build()
          .name("beacon_previous_justified_epoch")
          .help("Current previously justified epoch")
          .register();
  static Gauge CURRENT_PREV_JUSTIFIED_ROOT =
      Gauge.build()
          .name("beacon_previous_justified_root")
          .help("Current previously justified root")
          .register();

  /* Additional Metrics */
  static Gauge CURRENT_EPOCH_VALIDATORS =
      Gauge.build()
          .name("beacon_current_validators")
          .help(
              "Number of status=\"pending|active|exited|withdrawable\" validators in current epoch")
          .create();
  static Gauge PREVIOUS_EPOCH_VALIDATORS =
      Gauge.build()
          .name("beacon_previous_validators")
          .help(
              "Number of status=\"pending|active|exited|withdrawable\" validators in previous epoch")
          .create();
  static Gauge CURRENT_EPOCH_LIVE_VALIDATORS =
      Gauge.build()
          .name("beacon_current_live_validators")
          .help(
              "Number of active validators that successfully included attestation on chain for current epoch")
          .register();
  static Gauge PREVIOUS_EPOCH_LIVE_VALIDATORS =
      Gauge.build()
          .name("beacon_previous_live_validators")
          .help(
              "Number of active validators that successfully included attestation on chain for previous epoch")
          .register();
  // not yet implemented
  static Counter REORG_EVENTS_TOTAL =
      Counter.build()
          .name("beacon_reorgs_total")
          .help("Total occurrences of reorganizations of the chain")
          .create();
  // not yet supported by beacon chain yet
  static Gauge PENDING_DEPOSITS =
      Gauge.build()
          .name("beacon_pending_deposits")
          .help("Number of pending deposits")
          .register();
  // not yet supported by beacon chain yet
  static Gauge PENDING_EXITS =
      Gauge.build()
          .name("beacon_pending_exits")
          .help("Number of pending voluntary exits in local operation pool")
          .create();
  static Gauge TOTAL_DEPOSITS =
      Gauge.build()
          .name("beacon_processed_deposits_total")
          .help("Number of total deposits included on chain")
          .register();
  // not yet implemented
  static Gauge PREVIOUS_EPOCH_ORPHANED_BLOCKS =
      Gauge.build()
          .name("beacon_previous_epoch_orphaned_blocks")
          .help("Number of blocks orphaned in the previous epoch")
          .create();
  // simple implementation currently - counts attestations arrived from wire during a slot
  static Gauge PROPAGATED_ATTESTATIONS =
      Gauge.build()
          .name("beacon_propagated_attestations")
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
    HEAD_SLOT.set(Double.NaN);
    HEAD_ROOT.set(Double.NaN);
    CURRENT_JUSTIFIED_EPOCH.set(Double.NaN);
    CURRENT_JUSTIFIED_ROOT.set(Double.NaN);
    CURRENT_FINALIZED_EPOCH.set(Double.NaN);
    CURRENT_FINALIZED_ROOT.set(Double.NaN);
    CURRENT_PREV_JUSTIFIED_EPOCH.set(Double.NaN);
    CURRENT_PREV_JUSTIFIED_ROOT.set(Double.NaN);
    CURRENT_EPOCH_LIVE_VALIDATORS.set(Double.NaN);
    PREVIOUS_EPOCH_LIVE_VALIDATORS.set(Double.NaN);
    PENDING_DEPOSITS.set(0);
    TOTAL_DEPOSITS.set(Double.NaN);
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
        PROPAGATED_ATTESTATIONS.set(0);
      }
    }

    CURRENT_SLOT.set(state.getSlot().doubleValue());
    CURRENT_EPOCH.set(spec.get_current_epoch(state).doubleValue());
    CURRENT_JUSTIFIED_EPOCH.set(state.getCurrentJustifiedCheckpoint().getEpoch().doubleValue());
    setRoot(CURRENT_JUSTIFIED_ROOT, state.getCurrentJustifiedCheckpoint().getRoot());
    CURRENT_FINALIZED_EPOCH.set(state.getFinalizedCheckpoint().getEpoch().doubleValue());
    setRoot(CURRENT_FINALIZED_ROOT, state.getFinalizedCheckpoint().getRoot());
    CURRENT_PREV_JUSTIFIED_EPOCH.set(
        state.getPreviousJustifiedCheckpoint().getEpoch().doubleValue());
    setRoot(CURRENT_PREV_JUSTIFIED_ROOT, state.getPreviousJustifiedCheckpoint().getRoot());

    CURRENT_EPOCH_LIVE_VALIDATORS.set(
        calculateEpochValidators(state.getCurrentEpochAttestations().listCopy()));
    PREVIOUS_EPOCH_LIVE_VALIDATORS.set(
        calculateEpochValidators(state.getPreviousEpochAttestations().listCopy()));

    TOTAL_DEPOSITS.set(state.getEth1Data().getDepositCount().doubleValue());
    PENDING_DEPOSITS.set(
        state.getEth1Data().getDepositCount().minus(state.getEth1DepositIndex()).doubleValue());
  }

  public static void onHeadChanged(BeaconChainSpec spec, BeaconChainHead head) {
    HEAD_SLOT.set(head.getBlock().getSlot().doubleValue());

    EpochNumber currentEpoch = spec.get_current_epoch(head.getState());
    SlotNumber epochBoundarySlot = spec.compute_start_slot_of_epoch(currentEpoch);
    Hash32 block_root = epochBoundarySlot.equals(head.getState().getSlot())
        ? spec.signing_root(head.getBlock())
        : spec.get_block_root_at_slot(head.getState(), epochBoundarySlot);
    setRoot(
        HEAD_ROOT, block_root);
  }


  private static void setRoot(Gauge metric, Hash32 root) {
    byte[] a = root.extractArray();
    long res = convertHash32ToLong(a);
    metric.set(res);
  }

  /**
   * Convert hash32 bytes to long, according to metrics spec.
   * int.from_bytes(root[24:32], byteorder='little', signed=True)
   */
  private static long convertHash32ToLong(byte[] a) {
    return (((long) a[31]) << 56)
        | (((long) a[30] & 0xff) << 48)
        | (((long) a[29] & 0xff) << 40)
        | (((long) a[28] & 0xff) << 32)
        | (((long) a[27] & 0xff) << 24)
        | (((long) a[26] & 0xff) << 16)
        | (((long) a[25] & 0xff) << 8)
        | ((long) a[24] & 0xff);
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
