package qa.integration_tests

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.ethereum.beacon.test.type.model.BeaconStateData

import org.ethereum.beacon.test.type.model.BlockData

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = SlotEvent::class, name = "slot"),
    JsonSubTypes.Type(value = BlockEvent::class, name = "block"),
    JsonSubTypes.Type(value = AttestationEvent::class, name = "attestation"),
    JsonSubTypes.Type(value = CheckEvent::class, name = "check"))
abstract class TestEvent()
data class SlotEvent(val slot: Int): TestEvent() {
  constructor(): this(0)
}
data class BlockEvent(val block: BlockData): TestEvent() {
  constructor(): this(BlockData())
}
data class AttestationEvent(val attestation: BeaconStateData.AttestationData): TestEvent() {
  constructor(): this(BeaconStateData.AttestationData())
}
data class CheckEvent(val checks: Map<String, Any>): TestEvent() {
  constructor(): this(mapOf())
}

data class TestScenario(
    /*val specConstants: SpecConstants?, */
    /*val genesisState: BeaconStateData?, */
    var validators: Int,
    var steps: List<TestEvent>) {
  constructor() : this(0, listOf()) {
  }
}
