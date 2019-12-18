package org.ethereum.beacon.validator.proposer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Attestation;
import tech.pegasys.artemis.util.collections.Bitlist;

public class SequentialAggregator implements Function<Collection<Attestation>, List<Attestation>> {

  private final Function<List<Attestation>, Attestation> aggregateOperator;

  public SequentialAggregator(BeaconChainSpec spec) {
    this.aggregateOperator = new AggregateOperator(spec);
  }

  @Override
  public List<Attestation> apply(Collection<Attestation> attestations) {
    return GroupByAttestationData.apply(attestations).stream()
        .peek(group -> group.sort(HeaviestAggregationBitfieldFirst))
        .map(GroupByDisjointAggregationBitsSequentially)
        .flatMap(groups -> groups.stream().map(aggregateOperator))
        .collect(Collectors.toList());
  }

  private static final Function<List<Attestation>, List<List<Attestation>>>
      GroupByDisjointAggregationBitsSequentially =
          attestations -> {
            List<List<Attestation>> groups = new ArrayList<>();
            for (int i = 0; i < attestations.size(); i++) {
              Attestation attestation = attestations.remove(i);
              List<Attestation> group = new ArrayList<>();
              group.add(attestation);
              groups.add(group);

              Bitlist bits = attestation.getAggregationBits();
              for (int j = 0; j < attestations.size(); j++) {
                Attestation candidate = attestations.get(j);
                if (bits.and(candidate.getAggregationBits()).isZero()) {
                  group.add(attestations.remove(i));
                }
              }
            }

            return groups;
          };

  private static final Function<Collection<Attestation>, Collection<List<Attestation>>>
      GroupByAttestationData =
          attestations ->
              attestations.stream().collect(Collectors.groupingBy(Attestation::getData)).values();

  private static final Comparator<Attestation> HeaviestAggregationBitfieldFirst =
      Comparator.<Attestation>comparingInt(attestation -> attestation.getAggregationBits().size())
          .reversed();
}
