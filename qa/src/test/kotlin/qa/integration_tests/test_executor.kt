package qa.integration_tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.kotlintest.TestCase
import io.kotlintest.TestType
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec
import org.ethereum.beacon.core.types.SlotNumber
import org.ethereum.beacon.test.StateTestUtils
import qa.TestChain
import qa.Tester
import qa.Tester.Companion.createContract
import tech.pegasys.artemis.ethereum.core.Hash32
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.reflect.full.callSuspend

class TestExecutor() : AnnotationSpec(), TestBase {
  private fun getTestFilePaths(): List<Path> =
      Files.list(Paths.get(".")).filter {
        it.toString().endsWith(".yaml") || it.toString().endsWith(".yml")
      }.collect(Collectors.toList())

  override fun testCases(): List<TestCase> {
    val filePaths = getTestFilePaths()
    return filePaths.map {
      val name = it.fileName.toString()
      createTestCase(name.slice(0 until  (name.length-5)), { ::exec.callSuspend(it) }, defaultTestCaseConfig, TestType.Test)
    }
  }

  private fun readTestScenario(testScenarioFile: Path): TestScenario {
    return ObjectMapper(YAMLFactory()).readValue<TestScenario>(testScenarioFile.toFile(), TestScenario::class.java)
  }

  fun exec(testScenarioFile: Path) {
    val testScenario = readTestScenario(testScenarioFile)

    val tester = Tester(createContract(genesisTime, testScenario.validators), recordEvents = false)
    val testChain = TestChain(tester)
    for(step in testScenario.steps) {
      when(step) {
        is SlotEvent -> tester.currentSlot = SlotNumber(step.slot)
        is BlockEvent -> testChain.sendBlock(StateTestUtils.parseSignedBlockData(step.block, tester.spec.constants))
        is AttestationEvent -> testChain.sendAttestation(StateTestUtils.parseAttestation(step.attestation, tester.spec.constants))
        is CheckEvent -> {
          for(check in step.checks.entries) {
            when {
              check.key == "head" -> {
                val expectedHead = Hash32.fromHexString(check.value as String)
                tester.root(tester.testLauncher.lastObservableState.head) shouldBe expectedHead
              }
              check.key == "block_in_store" -> {
                val root = Hash32.fromHexString(check.value as String)
                tester.blockStorage[root].isPresent shouldBe true
              }
              check.key == "!block_in_store" -> {
                val root = Hash32.fromHexString(check.value as String)
                tester.blockStorage[root].isPresent shouldBe false
              }
              check.key == "justified_checkpoint.epoch" -> {
                tester.chainStorage.justifiedStorage.get().get().epoch.intValue shouldBe (check.value as Int)
              }
              else -> {
                TODO("${check.key} is not supported")
              }
            }
          }
        }
      }
    }
  }
}