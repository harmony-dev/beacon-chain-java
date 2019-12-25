package qa.integration_tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.kotlintest.Description
import io.kotlintest.TestCase
import io.kotlintest.specs.AnnotationSpec
import org.ethereum.beacon.core.operations.attestation.AttestationData
import org.ethereum.beacon.core.state.Checkpoint
import org.ethereum.beacon.core.types.CommitteeIndex
import org.ethereum.beacon.core.types.EpochNumber
import org.ethereum.beacon.core.types.SlotNumber
import org.ethereum.beacon.start.common.Launcher
import qa.ObservableStates
import qa.Tester
import tech.pegasys.artemis.ethereum.core.Hash32
import java.io.File
import java.nio.file.Paths
import java.util.*

interface TestBase {
  val genesisTime get() = Date(2019, 8, 24, 0, 0, 0).time
  val Launcher.lastObservableState get() = ObservableStates.data[this]!!
}

open class IntegrationSpec: AnnotationSpec(), TestBase {
    override fun testCases(): List<TestCase> {
        return super.testCases().map {
            if (it.name.startsWith("f_")) {
                val name = "f:" + it.name.subSequence(2, it.name.length)
                TestCase(Description(it.description.parents, name), it.spec, it.test, it.source, it.type, it.config)
            } else {
                it
            }
        }
    }

    fun AttestationData.withBeaconBlockRoot(root: Hash32) =
            AttestationData(slot, index, root, source, target)

  fun AttestationData.withSlot(slot: SlotNumber) =
      AttestationData(slot, index, beaconBlockRoot, source, target)

  fun AttestationData.withIndex(index: CommitteeIndex) =
      AttestationData(slot, index, beaconBlockRoot, source, target)

  fun AttestationData.withSource(source: Checkpoint) =
      AttestationData(slot, index, beaconBlockRoot, source, target)

  fun AttestationData.withTarget(target: Checkpoint) =
      AttestationData(slot, index, beaconBlockRoot, source, target)

  fun Checkpoint.withEpoch(epoch: EpochNumber) = Checkpoint(epoch, root)
  fun Checkpoint.withRoot(root: Hash32) = Checkpoint(epoch, root)

  private var _tester: Tester? = null
  var tester: Tester
    get() = _tester!!
    set(value) {
      _tester = value
    }

  private var testName: String? = null
  override fun beforeTest(testCase: TestCase) {
    testName = testCase.name
    super.beforeTest(testCase)
  }

  @AfterEach
  fun dumpEvents() {
    if (tester.recordEvents) {
      val mapper = ObjectMapper(YAMLFactory())

      val ts = TestScenario(/*tester.spec.constants, */
          tester.genesisState.validators.listCopy().size, tester.events
      )

      println(Paths.get(".").toAbsolutePath())
      File("$testName.yaml").writeText(mapper.writeValueAsString(ts))
    }
  }
}
