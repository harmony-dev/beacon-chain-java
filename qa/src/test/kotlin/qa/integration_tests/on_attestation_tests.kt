package qa.integration_tests

import io.kotlintest.shouldBe
import org.ethereum.beacon.chain.BeaconTuple
import org.ethereum.beacon.core.operations.Attestation
import org.ethereum.beacon.core.types.BLSSignature
import org.ethereum.beacon.core.types.CommitteeIndex
import org.ethereum.beacon.core.types.EpochNumber
import org.ethereum.beacon.core.types.SlotNumber
import qa.TestChain
import qa.Tester
import tech.pegasys.artemis.util.collections.Bitlist

open class InvalidAttestationSpec: IntegrationSpec() {
  var genesis: BeaconTuple? = null
  var block0: BeaconTuple? = null
  var block1: BeaconTuple? = null
  var block2: BeaconTuple? = null

  var _testChain: TestChain? = null
  val testChain get() = _testChain ?: throw IllegalStateException("Not initialized")

  val tester get() = testChain.tester

  fun setUp(startEpoch: Int? = null) {
    _testChain = TestChain(Tester(createContract(genesisTime, 16)))
    genesis = testChain.head

    val startEpochNumber = startEpoch ?: 0
    val startSlot = tester.spec.compute_start_slot_of_epoch(EpochNumber.of(startEpochNumber)).intValue

    block0 = if (startEpochNumber == 0) {
      genesis
    } else {
      tester.currentSlot = SlotNumber(startSlot)
      testChain.proposeBlock(startSlot, genesis)
    }

    tester.currentSlot = SlotNumber(startSlot+1)
    block1 = testChain.proposeBlock(startSlot+1, block0)
    //testChain.sendAttestations(testChain.mkAttestations(block1).subList(0,2))

    tester.currentSlot = SlotNumber(startSlot+2)
    tester.testLauncher.lastObservableState.head shouldBe block1!!.block
    block2 = testChain.proposeBlock(startSlot+2, block0)

    tester.testLauncher.lastObservableState.head shouldBe block1!!.block
  }

  fun allInvalidAttestationsTest(postProcess: ((Attestation) -> Attestation)? = null,
                                 postSign: ((Attestation) -> Attestation)? = null,
                                 startEpoch: Int? = null) {
    setUp(startEpoch)
    testChain.sendAttestations(testChain.mkAttestations(block2,
        postProcess = postProcess,
        postSign = postSign))
    tester.mdcControlledSchedulers.addTime(1000)

    tester.testLauncher.lastObservableState.head shouldBe block1!!.block
  }

  fun firstInvalidAttestationTest(postProcess: ((Attestation) -> Attestation)? = null,
                                  postSign: ((Attestation) -> Attestation)? = null,
                                  startEpoch: Int? = null) {
    setUp(startEpoch)
    val attesters = testChain.getAttesters(block2)
    testChain.sendAttestations(testChain.mkAttestations(block2,
        validators = attesters.subList(0, 1),
        postProcess = postProcess,
        postSign = postSign))
    testChain.sendAttestations(testChain.mkAttestations(block2,
        validators = attesters.subList(1, attesters.size)))
    tester.mdcControlledSchedulers.addTime(1000)

    tester.testLauncher.lastObservableState.head shouldBe block2!!.block
  }

  fun lastInvalidAttestationTest(postProcess: ((Attestation) -> Attestation)? = null,
                                 postSign: ((Attestation) -> Attestation)? = null,
                                 startEpoch: Int? = null) {
    setUp(startEpoch)
    val attesters = testChain.getAttesters(block2)
    testChain.sendAttestations(testChain.mkAttestations(block2,
        validators = attesters.subList(0, attesters.size - 1)))
    testChain.sendAttestations(testChain.mkAttestations(block2,
        validators = attesters.subList(attesters.size - 1, attesters.size),
        postProcess = postProcess,
        postSign = postSign))
    tester.mdcControlledSchedulers.addTime(1000)

    tester.testLauncher.lastObservableState.head shouldBe block2!!.block
  }
}

class BadShardTests : InvalidAttestationSpec() {

  fun updateWithInvalidShard(a: Attestation, offset: Int) =
      a.withData(a.data.withIndex(CommitteeIndex(a.data.index.value + offset.toLong())))

  fun makeAttestationWithInvalidShard(a: Attestation) = updateWithInvalidShard(a, 4)

  @Test
  fun attestation_bad_shard_all() {
    allInvalidAttestationsTest(::makeAttestationWithInvalidShard)
  }

  @Test
  fun attestation_bad_shard_first() {
    firstInvalidAttestationTest(::makeAttestationWithInvalidShard)
  }

  @Test
  fun attestation_bad_shard_last() {
    lastInvalidAttestationTest(::makeAttestationWithInvalidShard)
  }

  fun mkNonExistingTargetRoot(a: Attestation) =
      a.withData(a.data.withTarget(a.data.target.withRoot(tester.spec.hash_tree_root(0))))

  @Test
  fun non_existing_target_root_all() {
    allInvalidAttestationsTest(::mkNonExistingTargetRoot)
  }

  @Test
  fun non_existing_target_root_first() {
    firstInvalidAttestationTest(::mkNonExistingTargetRoot)
  }

  @Test
  fun non_existing_target_root_last() {
    lastInvalidAttestationTest(::mkNonExistingTargetRoot)
  }

  fun mkInvalidTargetEpoch(a: Attestation) =
      a.withData(a.data.withTarget(a.data.target.withEpoch(a.data.target.epoch.decrement())))

  @Test
  fun invalid_target_epoch_all() {
    allInvalidAttestationsTest(::mkInvalidTargetEpoch)
  }

  @Test
  fun invalid_target_epoch_first() {
    firstInvalidAttestationTest(::mkInvalidTargetEpoch)
  }

  @Test
  fun invalid_target_epoch_last() {
    lastInvalidAttestationTest(::mkInvalidTargetEpoch)
  }
}

class InvalidIndexedAttestation: InvalidAttestationSpec() {

  /*fun setWrongCustodyBit1(a: Attestation): Attestation {
    val size = a.custodyBits.size()
    val maxSize = a.custodyBits.maxSize()
    val badCistodyBits = arrayListOf<Int>()
    for(i in 0 until size) {
      if (!a.aggregationBits.getBit(i)) {
        badCistodyBits.add(i)
        break
      }
    }
    return a.withCustodyBits(Bitlist.of(size, badCistodyBits, maxSize), tester.spec.constants)
  }*/

  /*fun setWrongCustodyBit2(a: Attestation): Attestation {
    val size = a.custodyBits.size()
    val maxSize = a.custodyBits.maxSize()
    val badCistodyBits = arrayListOf<Int>()
    for(i in 0 until size) {
      if (a.aggregationBits.getBit(i)) {
        badCistodyBits.add(i)
        break
      }
    }
    return a.withCustodyBits(Bitlist.of(size, badCistodyBits, maxSize), tester.spec.constants)
  }*/

  /*@Test
  fun all_invalid_indexed_attestation_wrong_custodybit() {
    allInvalidAttestationsTest(::setWrongCustodyBit1)
  }

  @Test
  fun first_invalid_indexed_attestation_wrong_custodybit() {
    firstInvalidAttestationTest(::setWrongCustodyBit1)
  }

  @Test
  fun last_invalid_indexed_attestation_wrong_custodybit() {
    lastInvalidAttestationTest(::setWrongCustodyBit1)
  }

  @Test
  fun all_invalid_indexed_attestation_wrong_custodybit2() {
    allInvalidAttestationsTest(::setWrongCustodyBit2)
  }

  @Test
  fun first_invalid_indexed_attestation_wrong_custodybit2() {
    firstInvalidAttestationTest(::setWrongCustodyBit2)
  }

  @Test
  fun last_invalid_indexed_attestation_wrong_custodybit2() {
    lastInvalidAttestationTest(::setWrongCustodyBit2)
  }*/

  fun makeInvalidSignature(a:Attestation) = a.withSignature(BLSSignature.ZERO)
  @Test
  fun all_invalid_indexed_attestation_wrong_signature() {
    allInvalidAttestationsTest(postSign = ::makeInvalidSignature)
  }

  @Test
  fun first_invalid_indexed_attestation_wrong_signature() {
    firstInvalidAttestationTest(postSign = ::makeInvalidSignature)
  }

  @Test
  fun last_invalid_indexed_attestation_wrong_signature() {
    lastInvalidAttestationTest(postSign = ::makeInvalidSignature)
  }

  @Test
  fun `!test`() {
    testChain.attestBlock(block2)
    tester.mdcControlledSchedulers.addTime(1000)
    tester.testLauncher.lastObservableState.head shouldBe block2!!.block

    for(slot in 4..7) {
      tester.currentSlot = SlotNumber(slot)
      testChain.attestBlock(block1)
      tester.mdcControlledSchedulers.addTime(1000)
      tester.testLauncher.lastObservableState.head shouldBe block1!!.block
    }

    for(slot in 8..11) {
      tester.currentSlot = SlotNumber(slot)
      testChain.attestBlock(block2)
      tester.mdcControlledSchedulers.addTime(1000)
    }

    tester.testLauncher.lastObservableState.head shouldBe block2!!.block
  }

}

class OnAttestationTests : IntegrationSpec() {

  @Test
  fun testAttestation_late_parent() {
    val tester = Tester(createContract(genesisTime, 16))
    val testChain = TestChain(tester)
    val genesis = testChain.head

    tester.currentSlot = SlotNumber(1)
    val block1 = testChain.proposeBlock(1, genesis)

    testChain.sendAttestations(testChain.mkAttestations(block1).subList(0, 2))

    tester.currentSlot = SlotNumber(2)
    tester.testLauncher.lastObservableState.head shouldBe block1.block
    val block2 = testChain.mkBlock(2, genesis)

    testChain.sendAttestations(testChain.mkAttestations(block2))
    tester.mdcControlledSchedulers.addTime(1000);
    tester.testLauncher.lastObservableState.head shouldBe block1.block

    testChain.sendBlock(block2.block)
    tester.testLauncher.lastObservableState.head shouldBe block2.block
  }

  @Test
  fun testAttestation() {
    val tester = Tester(createContract(genesisTime, 16))
    val testChain = TestChain(tester)
    val genesis = testChain.head

    tester.currentSlot = SlotNumber(1)
    testChain.proposeBlock(1, genesis)

    tester.currentSlot = SlotNumber(2)
    val block2 = testChain.proposeBlock(2, genesis)

    tester.currentSlot = SlotNumber(4)
    testChain.attestBlock(block2)

    tester.currentSlot = SlotNumber(5)
    val block3 = testChain.proposeBlock(5)

    tester.spec.signing_root(tester.testLauncher.lastObservableState.head) shouldBe tester.spec.signing_root(block3.block)
  }

  @Test
  fun testAttestation1_wire_attestations() {
    val tester = Tester(createContract(genesisTime, 16))
    val testChain = TestChain(tester)
    val genesis = testChain.head

    tester.currentSlot = SlotNumber(1)
    testChain.proposeBlock(1, genesis)

    tester.currentSlot = SlotNumber(2)
    val block2 = testChain.proposeBlock(2, genesis)

    tester.currentSlot = SlotNumber(4)
    testChain.attestBlock(block2)

    tester.currentSlot = SlotNumber(5)

    tester.spec.signing_root(tester.testLauncher.lastObservableState.head) shouldBe tester.spec.signing_root(block2.block)
  }

  @Test
  fun testAttestation1_block_attestations() {
    val tester = Tester(createContract(genesisTime, 16))
    val testChain = TestChain(tester)
    val genesis = testChain.head

    tester.currentSlot = SlotNumber(1)
    val block1 = testChain.proposeBlock(1, genesis)

    tester.currentSlot = SlotNumber(2)
    val block2 = testChain.proposeBlock(2, genesis)

    tester.currentSlot = SlotNumber(4)
    testChain.gatherAttestations(block2)

    tester.currentSlot = SlotNumber(5)
    val block3 = testChain.proposeBlock(5, parent = block1)

    tester.spec.signing_root(tester.testLauncher.lastObservableState.head) shouldBe tester.spec.signing_root(block2.block)
  }

  @Test
  fun testAttestation1_block_attestations2() {
    val tester = Tester(createContract(genesisTime, 16))
    val testChain = TestChain(tester)
    val genesis = testChain.head

    tester.currentSlot = SlotNumber(1)
    val block1 = testChain.proposeBlock(1, genesis)

    tester.currentSlot = SlotNumber(2)
    val block2 = testChain.proposeBlock(2, genesis)

    tester.currentSlot = SlotNumber(4)
    testChain.gatherAttestations(block2)

    tester.currentSlot = SlotNumber(5)
    val block3 = testChain.proposeBlock(5)

    tester.spec.signing_root(tester.testLauncher.lastObservableState.head) shouldBe tester.spec.signing_root(block3.block)
  }

  @Test
  fun testAttestation2() {
    val tester = Tester(createContract(genesisTime, 16))
    val testChain = TestChain(tester)
    val genesis = testChain.head

    tester.currentSlot = SlotNumber(1)
    val block1 = testChain.proposeBlock(1, genesis)

    tester.currentSlot = SlotNumber(2)
    val block2 = testChain.proposeBlock(2, genesis)

    tester.currentSlot = SlotNumber(4)
    testChain.attestBlock(block2)

    tester.currentSlot = SlotNumber(5)
    val block3 = testChain.proposeBlock(5, parent = block1)

    tester.spec.signing_root(tester.testLauncher.lastObservableState.head) shouldBe tester.spec.signing_root(block2.block)
  }

  @Test
  fun testAttestation1_block_wire_attestations() {
    val tester = Tester(createContract(genesisTime, 16))
    val testChain = TestChain(tester)
    val genesis = testChain.head

    tester.currentSlot = SlotNumber(1)
    val block1 = testChain.proposeBlock(1, genesis)

    tester.currentSlot = SlotNumber(2)
    val block2 = testChain.proposeBlock(2, genesis)

    tester.currentSlot = SlotNumber(4)
    testChain.gatherAttestations(block2)
    testChain.sendAttestations(testChain.attestationCache)

    tester.currentSlot = SlotNumber(5)
    val block3 = testChain.proposeBlock(5, parent = block1)

    tester.spec.signing_root(tester.testLauncher.lastObservableState.head) shouldBe tester.spec.signing_root(block2.block)
  }

  @Test
  fun testAttestation_early() {
    val tester = Tester(createContract(genesisTime, 16))
    val testChain = TestChain(tester)
    val genesis = testChain.head

    tester.currentSlot = SlotNumber(1)
    val block1 = testChain.proposeBlock(1, genesis)

    tester.currentSlot = SlotNumber(2)
    val block2 = testChain.proposeBlock(2, genesis)

    val prevHead = tester.testLauncher.lastObservableState.head
    val nonHeadTuple = if (prevHead == block1.block) block2 else block1

    tester.currentSlot = SlotNumber(4)
    testChain.gatherAttestations(nonHeadTuple)
    testChain.sendAttestations(testChain.mkAttestations())

    tester.spec.signing_root(tester.testLauncher.lastObservableState.head) shouldBe tester.spec.signing_root(prevHead)
    tester.testLauncher.lastObservableState.head shouldBe prevHead
  }

  @Test
  fun testAttestation_late() {
    val tester = Tester(createContract(genesisTime, 16))
    val testChain = TestChain(tester)
    val genesis = testChain.head

    tester.currentSlot = SlotNumber(1)
    val block1 = testChain.proposeBlock(1, genesis)

    tester.currentSlot = SlotNumber(2)
    val block2 = testChain.proposeBlock(2, genesis)

    val prevHeadBlock = tester.testLauncher.lastObservableState.head
    val prevHeadTuple = if (prevHeadBlock == block1.block) block1 else block2
    val nonHeadTuple = if (prevHeadBlock == block1.block) block2 else block1

    tester.currentSlot = SlotNumber(4)
    val atts = testChain.mkAttestations(nonHeadTuple)

    tester.currentSlot = SlotNumber(5)
    val block3 = testChain.proposeBlock(5, parent = prevHeadTuple)

    tester.testLauncher.lastObservableState.head shouldBe block3.block
    tester.spec.signing_root(tester.testLauncher.lastObservableState.head) shouldBe tester.spec.signing_root(block3.block)

    testChain.sendAttestations(atts)

    tester.mdcControlledSchedulers.addTime(1000)

    tester.testLauncher.lastObservableState.head shouldBe nonHeadTuple.block
    tester.spec.signing_root(tester.testLauncher.lastObservableState.head) shouldBe tester.spec.signing_root(nonHeadTuple.block)

    tester.currentSlot = SlotNumber(6)

    tester.testLauncher.lastObservableState.head shouldBe nonHeadTuple.block
    tester.spec.signing_root(tester.testLauncher.lastObservableState.head) shouldBe tester.spec.signing_root(nonHeadTuple.block)
  }

  @Test
  fun testAttestation_early_before_block() {
    val tester = Tester(createContract(genesisTime, 16))
    val testChain = TestChain(tester)
    val genesis = testChain.head

    tester.currentSlot = SlotNumber(1)
    val block1 = testChain.proposeBlock(1, genesis)

    tester.currentSlot = SlotNumber(2)
    val block2 = testChain.proposeBlock(2, genesis)

    val prevHeadBlock = tester.testLauncher.lastObservableState.head
    val prevHeadTuple = if (prevHeadBlock == block1.block) block1 else block2
    val nonHeadTuple = if (prevHeadBlock == block1.block) block2 else block1

    tester.currentSlot = SlotNumber(4)
    tester.testLauncher.lastObservableState.head shouldBe prevHeadTuple.block

    val atts = testChain.mkAttestations(nonHeadTuple)
    testChain.sendAttestations(atts)
    tester.mdcControlledSchedulers.addTime(1000)

    tester.testLauncher.lastObservableState.head shouldBe nonHeadTuple.block

    val block3 = testChain.proposeBlock(4, parent = prevHeadTuple)

    tester.testLauncher.lastObservableState.head shouldBe nonHeadTuple.block
  }

}