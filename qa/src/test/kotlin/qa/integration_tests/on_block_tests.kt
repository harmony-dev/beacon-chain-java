package qa.integration_tests

import io.kotlintest.matchers.collections.shouldHaveSingleElement
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.ethereum.beacon.chain.BeaconTuple
import org.ethereum.beacon.chain.MutableBeaconChain
import org.ethereum.beacon.core.BeaconBlock
import org.ethereum.beacon.core.types.BLSSignature
import org.ethereum.beacon.core.types.SlotNumber
import qa.Store
import qa.TestChain
import qa.Tester

class OnBlockTests: IntegrationSpec() {
    @Test
    fun testUpdateFinality() {
        val tester = Tester(createContract(genesisTime, 16))
        val testChain = TestChain(tester)
        val genesis = testChain.head

        testChain.gatherAttestations()

        for (i in 1..11) {
            tester.currentSlot = SlotNumber(i)
            testChain.proposeBlock(i)
            testChain.gatherAttestations()
        }

        val fork0 = testChain.head

        var fork2 = genesis
        testChain.attestationCache.clear()
        for (i in 12..16) {
            tester.currentSlot = SlotNumber(i)
            testChain.proposeBlock(i, parent = fork2)
            fork2 = testChain.head
            testChain.gatherAttestations(fork2)
        }
        val justifiedChkpt = tester.testLauncher.beaconChainStorage.justifiedStorage.get().get()


        testChain.proposeBlock(12, parent = fork0, attestations = emptyList())

        tester.testLauncher.beaconChainStorage.justifiedStorage.get().get().epoch.intValue shouldBe justifiedChkpt.epoch.intValue
    }

    @Test
    fun testFinalizedAncestor() {
        val tester = Tester(createContract(genesisTime, 16))
        val testChain = TestChain(tester)
        val genesis = testChain.head

        testChain.gatherAttestations() // attest genesis
        for(i in 1 .. 16) {
            tester.currentSlot = SlotNumber(i)
            testChain.proposeBlock(i)
            testChain.gatherAttestations()
        }
        tester.testLauncher.insertResults.clear()

        tester.currentSlot = SlotNumber(17)
        val pBlock = testChain.mkBlock(17, parent = genesis, attestations = emptyList())

        checkNoFinalizedAncestor(testChain, pBlock)

        testChain.sendBlock(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSize 1
        tester.testLauncher.insertResults[0] shouldNotBe MutableBeaconChain.ImportResult.OK
    }

    private fun checkNoFinalizedAncestor(testChain: TestChain, pBlock: BeaconTuple) {
        val storage = testChain.tester.testLauncher.beaconChainStorage
        val store = Store(storage)
        val finRoot = storage.finalizedStorage.get().get().root

        val block = pBlock.block
        val root = testChain.tester.spec.signing_root(block)
        store.put(root, block)

        val ancestorRoot = store.getAncestor(root, store.storage.blockStorage.get(finRoot).get().slot)
        // test validity precondition
        ancestorRoot shouldNotBe finRoot
    }

    @Test
    fun testFinalizedAncestor2() {
        val tester = Tester(createContract(genesisTime, 16))
        val testChain = TestChain(tester)

        val genesis = testChain.head

        testChain.gatherAttestations() // attest genesis
        for(i in 1 .. 12) {
            tester.currentSlot = SlotNumber(i)
            testChain.proposeBlock(i)
            testChain.gatherAttestations()
        }
        var head = testChain.head
        val blockNonFin = testChain.proposeBlock(12, parent = genesis, attestations = emptyList())
        for(i in 13 .. 16) {
            tester.currentSlot = SlotNumber(i)
            val pBlock = testChain.proposeBlock(i, parent = head)
            testChain.gatherAttestations(pBlock)
            head = tester.testLauncher.beaconChain.recentlyProcessed
        }
        tester.testLauncher.insertResults.clear()

        tester.currentSlot = SlotNumber(17)
        val pBlock = testChain.mkBlock(17, parent = blockNonFin, attestations = emptyList())

        checkNoFinalizedAncestor(testChain, pBlock)

        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSize 1
        tester.testLauncher.insertResults[0] shouldNotBe MutableBeaconChain.ImportResult.OK
    }

    @Test
    fun testFinalizedAncestor3() {
        val tester = Tester(createContract(genesisTime, 16))
        val testChain = TestChain(tester)
        val genesis = testChain.head

        testChain.gatherAttestations() // attest genesis
        for(i in 1 .. 14) {
            tester.currentSlot = SlotNumber(i)
            testChain.proposeBlock(i)
            testChain.gatherAttestations()
        }
        var head = tester.testLauncher.beaconChain.recentlyProcessed
        val blockNonFin = testChain.proposeBlock(14, parent = genesis, attestations = emptyList())
        for(i in 15 .. 16) {
            tester.currentSlot = SlotNumber(i)
            val pBlock = testChain.proposeBlock(i, parent = head)
            testChain.gatherAttestations(pBlock)
            head = tester.testLauncher.beaconChain.recentlyProcessed
        }
        tester.testLauncher.insertResults.clear()

        tester.currentSlot = SlotNumber(17)
        val pBlock = testChain.mkBlock(17, parent = blockNonFin, attestations = emptyList())

        checkNoFinalizedAncestor(testChain, pBlock)

        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSize 1
        tester.testLauncher.insertResults[0] shouldNotBe MutableBeaconChain.ImportResult.OK
    }

    @Test
    fun testValid1() {
        val tester = Tester(createContract(genesisTime, 16))

        val testChain = TestChain(tester)
        val pBlock = testChain.mkBlock(1)

        tester.currentSlot = SlotNumber(2)
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSingleElement MutableBeaconChain.ImportResult.OK
    }

    @Test
    fun testValid2() {
        val tester = Tester(createContract(genesisTime, 16))

        val testChain = TestChain(tester)
        val pBlock = testChain.mkBlock(2)

        tester.currentSlot = SlotNumber(2)
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSingleElement MutableBeaconChain.ImportResult.OK
    }

    @Test
    fun testExisting() {
        val tester = Tester(createContract(genesisTime, 16))

        val testChain = TestChain(tester)
        val pBlock = testChain.mkBlock(2)

        tester.currentSlot = SlotNumber(2)
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSingleElement MutableBeaconChain.ImportResult.OK
        tester.testLauncher.insertResults.clear()
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSingleElement MutableBeaconChain.ImportResult.ExistingBlock
    }

    @Test
    fun testExisting2() {
        val tester = Tester(createContract(genesisTime, 16))

        val testChain = TestChain(tester)
        val pBlock = testChain.mkBlock(2)

        tester.currentSlot = SlotNumber(2)
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSingleElement MutableBeaconChain.ImportResult.OK
        tester.currentSlot = SlotNumber(3)
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults.clear()
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSingleElement MutableBeaconChain.ImportResult.ExistingBlock
    }

    @Test
    fun testTimeReject1() {
        val tester = Tester(createContract(genesisTime, 16))

        val testChain = TestChain(tester)
        val pBlock = testChain.mkBlock(3)

        tester.currentSlot = SlotNumber(2)
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSize 1
        tester.testLauncher.insertResults[0] shouldBe MutableBeaconChain.ImportResult.ExpiredBlock
    }

    @Test
    fun testTimeReject2() {
        val tester = Tester(createContract(genesisTime, 16))

        val testChain = TestChain(tester)
        val pBlock = testChain.mkBlock(4)

        tester.currentSlot = SlotNumber(2)
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSingleElement MutableBeaconChain.ImportResult.ExpiredBlock
    }

    @Test
    fun testInvalidParent() {
        val tester = Tester(createContract(genesisTime, 16))

        val testChain = TestChain(tester)
        val pBlock = testChain.mkBlock(2, postProcess = {
            BeaconBlock.Builder.fromBlock(it).withParentRoot(tester.spec.hash_tree_root(0)).build()
        })

        tester.currentSlot = SlotNumber(2)
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSize 1
        tester.testLauncher.insertResults[0] shouldBe MutableBeaconChain.ImportResult.NoParent
    }

    @Test
    fun testInvalidStateRoot() {
        val tester = Tester(createContract(genesisTime, 16))

        val testChain = TestChain(tester)
        val pBlock = testChain.mkBlock(2, postProcess = {
            BeaconBlock.Builder.fromBlock(it).withStateRoot(tester.spec.hash_tree_root(0)).build()
        })

        tester.currentSlot = SlotNumber(2)
        tester.wireApi.blockProcSink.next(pBlock.block)
        tester.testLauncher.insertResults shouldHaveSize 1
        tester.testLauncher.insertResults[0] shouldBe MutableBeaconChain.ImportResult.StateMismatch
    }

    @Test
    fun testInvalidSignature() {
        val tester = Tester(createContract(genesisTime, 16))

        val testChain = TestChain(tester)
        val pBlock = testChain.mkBlock(2)

        val block = BeaconBlock.Builder.fromBlock(pBlock.block).withSignature(BLSSignature.ZERO).build()

        tester.currentSlot = SlotNumber(2)
        tester.wireApi.blockProcSink.next(block)
        tester.testLauncher.insertResults shouldHaveSize 1
        tester.testLauncher.insertResults[0] shouldBe MutableBeaconChain.ImportResult.InvalidBlock
    }

}