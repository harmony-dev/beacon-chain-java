package org.ethereum.beacon.test.runner.ssz;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.list.BitlistAccessor;
import org.ethereum.beacon.ssz.type.SSZBasicType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.list.SSZBitListType;
import org.ethereum.beacon.ssz.visitor.SSZReader;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.ssz.SszGenericCase;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Set;

import static org.ethereum.beacon.ssz.type.SSZType.VARIABLE_SIZE;
import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/**
 * TestRunner for Bitlists {@link SszGenericCase}
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic</a>
 */
public class SszBitlistRunner implements Runner {
  private SszGenericCase testCase;
  private BeaconChainSpec spec;
  private ObjectMapper yamlMapper = new YAMLMapper();
  private SSZType currentType;
  private SSZSerializer sszSerializer;

  public SszBitlistRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof SszGenericCase)) {
      throw new RuntimeException("TestCase runner accepts only SszGenericCase.class as input!");
    }
    this.testCase = (SszGenericCase) testCase;
    if (!((SszGenericCase) testCase).getTypeName().startsWith("bitlist")) {
      throw new RuntimeException(
          "Type " + ((SszGenericCase) testCase).getTypeName() + " is not supported");
    }
    this.spec = spec;
    SSZBuilder builder = new SSZBuilder();
    builder.withTypeResolver(descriptor -> currentType);
    this.sszSerializer = builder.buildSerializer();
  }

  private void activateTypeMock() {
    // Scheme mock doesn't work here because of variable size type.
    // So it couldn't be presented in container without additional data
    // Implementing type mock with defining only needed data
    this.currentType =
        new SSZBitListType(
            new SSZField(
                Bitlist.class,
                new SszBitvectorRunner.SSZListMock(VARIABLE_SIZE, getListMaxSize()),
                null,
                null,
                null,
                null),
            descriptor ->
                new SSZBasicType(
                    null,
                    // We are always inside bitlist here, query for its child elements
                    new SSZBasicAccessor() {
                      @Override
                      public Set<String> getSupportedSSZTypes() {
                        return null;
                      }

                      @Override
                      public Set<Class> getSupportedClasses() {
                        return null;
                      }

                      @Override
                      public int getSize(SSZField field) {
                        return 0;
                      }

                      @Override
                      public void encode(Object value, SSZField field, OutputStream result) {}

                      @Override
                      public Object decode(SSZField field, SSZReader reader) {
                        return reader.readUInt(8);
                      }
                    }) {
                  @Override
                  public int getSize() {
                    return 1;
                  }
                },
            new BitlistAccessor(),
            VARIABLE_SIZE,
            getListMaxSize());
  }

  private long getListMaxSize() {
    String type = testCase.getSubTypeName();
    int startSize = type.indexOf('_');
    int endSize = type.indexOf('_', startSize + 1);
    if (endSize == -1) {
      endSize = type.length();
    }
    String size = type.substring(startSize + 1, endSize);
    try {
      return Long.parseLong(size);
    } catch (NumberFormatException ex) {
      return Long.MAX_VALUE;
    }
  }

  public Optional<String> run() {
    activateTypeMock();
    if (testCase.isValid()) {
      Bitlist expected = null;
      try {
        String hexData = yamlMapper.readValue(testCase.getValue(), String.class);
        expected = Bitlist.of(BytesValue.fromHexString(hexData), getListMaxSize());
      } catch (IOException e) {
        throw new RuntimeException("Unable to read expected value from file", e);
      }
      Bitlist actual = sszSerializer.decode(testCase.getSerialized(), Bitlist.class);
      return assertEquals(expected, actual);
    } else {
      try {
        Bitlist actual = sszSerializer.decode(testCase.getSerialized(), Bitlist.class);
      } catch (Exception ex) {
        return Optional.empty();
      }
      return Optional.of(
          "SSZ encoded data ["
              + testCase.getSerialized()
              + "] is not valid but was successfully decoded.");
    }
  }
}
