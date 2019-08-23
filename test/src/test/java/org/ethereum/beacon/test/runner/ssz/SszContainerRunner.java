package org.ethereum.beacon.test.runner.ssz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Objects;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.ssz.SSZBuilder;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.ssz.access.container.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.test.runner.Runner;
import org.ethereum.beacon.test.type.TestCase;
import org.ethereum.beacon.test.type.ssz.SszGenericCase;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.collections.Bitvector;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt16;
import tech.pegasys.artemis.util.uint.UInt32;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt8;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.ethereum.beacon.test.SilentAsserts.assertEquals;

/**
 * TestRunner for predefined container types {@link SszGenericCase}
 *
 * <p>Test format description: <a
 * href="https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic">https://github.com/ethereum/eth2.0-specs/tree/dev/specs/test_formats/ssz_generic</a>
 */
public class SszContainerRunner implements Runner {
  private SszGenericCase testCase;
  private BeaconChainSpec spec;
  private ObjectMapper yamlMapper = new YAMLMapper();
  private SSZSchemeBuilder.SSZScheme currentScheme;
  private SSZSerializer sszSerializer;

  public SszContainerRunner(TestCase testCase, BeaconChainSpec spec) {
    if (!(testCase instanceof SszGenericCase)) {
      throw new RuntimeException("TestCase runner accepts only SszGenericCase.class as input!");
    }
    if (!((SszGenericCase) testCase).getTypeName().equals("containers")) {
      throw new RuntimeException(
          "Type " + ((SszGenericCase) testCase).getSubTypeName() + " is not supported");
    }
    this.testCase = (SszGenericCase) testCase;
    this.spec = spec;
    this.sszSerializer = new SSZBuilder().withExplicitAnnotations(false).buildSerializer();
  }

  public Optional<String> run() {
    String testStructure =
        testCase.getSubTypeName().substring(0, testCase.getSubTypeName().indexOf('_'));
    switch (testStructure) {
      case "SingleFieldTestStruct":
        {
          return runWithImplementation(SingleFieldTestStruct.class, SingleFieldTestStructDO.class);
        }
      case "SmallTestStruct":
        {
          return runWithImplementation(SmallTestStruct.class, SmallTestStructDO.class);
        }
      case "FixedTestStruct":
        {
          return runWithImplementation(FixedTestStruct.class, FixedTestStructDO.class);
        }
      case "VarTestStruct":
        {
          return runWithImplementation(VarTestStruct.class, VarTestStructDO.class);
        }
      case "ComplexTestStruct":
        {
          return runWithImplementation(ComplexTestStruct.class, ComplexTestStructDO.class);
        }
      case "BitsStruct":
        {
          return runWithImplementation(BitsStruct.class, BitsStructDO.class);
        }
      default:
        {
          throw new RuntimeException(
              String.format("Subtype %s handler not implemented", testCase.getSubTypeName()));
        }
    }
  }

  private <V> Optional<String> runWithImplementation(
      Class<? extends V> clazz, Class<? extends DOCreator<V>> clazzDO) {
    if (testCase.isValid()) {
      DOCreator<V> expected = null;
      try {
        expected = yamlMapper.readValue(testCase.getValue(), clazzDO);
      } catch (IOException e) {
        throw new RuntimeException("Unable to read expected value from file", e);
      }
      V actual = sszSerializer.decode(testCase.getSerialized(), clazz);
      return assertEquals(expected.create(), actual);
    } else {
      try {
        V actual = sszSerializer.decode(testCase.getSerialized(), clazz);
      } catch (Exception ex) {
        return Optional.empty();
      }
      return Optional.of(
          "SSZ encoded data ["
              + testCase.getSerialized()
              + "] is not valid but was successfully decoded.");
    }
  }

  public interface DOCreator<V> {
    V create();
  }

  @SSZSerializable
  public static class SingleFieldTestStruct {
    private Bytes1 A;

    public Bytes1 getA() {
      return A;
    }

    public void setA(Bytes1 a) {
      A = a;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SingleFieldTestStruct that = (SingleFieldTestStruct) o;
      return Objects.equal(A, that.A);
    }
  }

  public static class SingleFieldTestStructDO implements DOCreator<SingleFieldTestStruct> {
    @JsonProperty("A")
    private Integer A;

    public SingleFieldTestStruct create() {
      SingleFieldTestStruct singleFieldTestStruct = new SingleFieldTestStruct();
      singleFieldTestStruct.setA(Bytes1.wrap(A.byteValue()));
      return singleFieldTestStruct;
    }

    public Integer getA() {
      return A;
    }

    public void setA(Integer a) {
      A = a;
    }
  }

  @SSZSerializable
  public static class SmallTestStruct {
    private UInt16 A;
    private UInt16 B;

    public UInt16 getA() {
      return A;
    }

    public void setA(UInt16 a) {
      A = a;
    }

    public UInt16 getB() {
      return B;
    }

    public void setB(UInt16 b) {
      B = b;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SmallTestStruct that = (SmallTestStruct) o;
      return Objects.equal(A, that.A) && Objects.equal(B, that.B);
    }
  }

  public static class SmallTestStructDO implements DOCreator<SmallTestStruct> {
    @JsonProperty("A")
    private Long A;

    @JsonProperty("B")
    private Long B;

    public SmallTestStruct create() {
      SmallTestStruct smallTestStruct = new SmallTestStruct();
      smallTestStruct.setA(UInt16.valueOf(Math.toIntExact(A)));
      smallTestStruct.setB(UInt16.valueOf(Math.toIntExact(B)));
      return smallTestStruct;
    }

    public Long getA() {
      return A;
    }

    public void setA(Long a) {
      A = a;
    }

    public Long getB() {
      return B;
    }

    public void setB(Long b) {
      B = b;
    }
  }

  @SSZSerializable
  public static class FixedTestStruct {
    private UInt8 A;
    private UInt64 B;
    private UInt32 C;

    public UInt8 getA() {
      return A;
    }

    public void setA(UInt8 a) {
      A = a;
    }

    public UInt64 getB() {
      return B;
    }

    public void setB(UInt64 b) {
      B = b;
    }

    public UInt32 getC() {
      return C;
    }

    public void setC(UInt32 c) {
      C = c;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FixedTestStruct that = (FixedTestStruct) o;
      return Objects.equal(A, that.A) && Objects.equal(B, that.B) && Objects.equal(C, that.C);
    }
  }

  public static class FixedTestStructDO implements DOCreator<FixedTestStruct> {
    @JsonProperty("A")
    private Long A;

    @JsonProperty("B")
    private BigInteger B;

    @JsonProperty("C")
    private Long C;

    public FixedTestStruct create() {
      FixedTestStruct fixedTestStruct = new FixedTestStruct();
      fixedTestStruct.setA(UInt8.valueOf(Math.toIntExact(A)));
      fixedTestStruct.setB(UInt64.valueOf(B.toString()));
      fixedTestStruct.setC(UInt32.valueOf(C));
      return fixedTestStruct;
    }

    public Long getA() {
      return A;
    }

    public void setA(Long a) {
      A = a;
    }

    public BigInteger getB() {
      return B;
    }

    public void setB(BigInteger b) {
      B = b;
    }

    public Long getC() {
      return C;
    }

    public void setC(Long c) {
      C = c;
    }
  }

  @SSZSerializable
  public static class VarTestStruct {
    private UInt16 A;

    @SSZ(maxSize = 1024)
    private ReadList<Integer, UInt16> B;

    private UInt8 C;

    public UInt16 getA() {
      return A;
    }

    public void setA(UInt16 a) {
      A = a;
    }

    public ReadList<Integer, UInt16> getB() {
      return B;
    }

    public void setB(ReadList<Integer, UInt16> b) {
      B = b;
    }

    public UInt8 getC() {
      return C;
    }

    public void setC(UInt8 c) {
      C = c;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      VarTestStruct that = (VarTestStruct) o;
      return Objects.equal(A, that.A) && Objects.equal(B, that.B) && Objects.equal(C, that.C);
    }
  }

  public static class VarTestStructDO implements DOCreator<VarTestStruct> {
    @JsonProperty("A")
    private Integer A;

    @JsonProperty("B")
    private List<Integer> B;

    @JsonProperty("C")
    private Integer C;

    public VarTestStruct create() {
      VarTestStruct varTestStruct = new VarTestStruct();
      varTestStruct.setA(UInt16.valueOf(A));
      varTestStruct.setB(
          ReadList.wrap(
              B.stream().map(UInt16::valueOf).collect(Collectors.toList()),
              Function.identity(),
              1024));
      varTestStruct.setC(UInt8.valueOf(C));
      return varTestStruct;
    }

    public Integer getA() {
      return A;
    }

    public void setA(Integer a) {
      A = a;
    }

    public List<Integer> getB() {
      return B;
    }

    public void setB(List<Integer> b) {
      B = b;
    }

    public Integer getC() {
      return C;
    }

    public void setC(Integer c) {
      C = c;
    }
  }

  @SSZSerializable
  public static class ComplexTestStruct {
    private UInt16 A;

    @SSZ(maxSize = 128)
    private ReadList<Integer, UInt16> B;

    private UInt8 C;

    @SSZ(maxSize = 256)
    private ReadList<Integer, Bytes1> D;

    private VarTestStruct E;

    @SSZ(vectorLength = 4)
    private ReadVector<Integer, FixedTestStruct> F;

    @SSZ(vectorLength = 2)
    private ReadVector<Integer, VarTestStruct> G;

    public UInt16 getA() {
      return A;
    }

    public void setA(UInt16 a) {
      A = a;
    }

    public ReadList<Integer, UInt16> getB() {
      return B;
    }

    public void setB(ReadList<Integer, UInt16> b) {
      B = b;
    }

    public UInt8 getC() {
      return C;
    }

    public void setC(UInt8 c) {
      C = c;
    }

    public ReadList<Integer, Bytes1> getD() {
      return D;
    }

    public void setD(ReadList<Integer, Bytes1> d) {
      D = d;
    }

    public VarTestStruct getE() {
      return E;
    }

    public void setE(VarTestStruct e) {
      E = e;
    }

    public ReadVector<Integer, FixedTestStruct> getF() {
      return F;
    }

    public void setF(ReadVector<Integer, FixedTestStruct> f) {
      F = f;
    }

    public ReadVector<Integer, VarTestStruct> getG() {
      return G;
    }

    public void setG(ReadVector<Integer, VarTestStruct> g) {
      G = g;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ComplexTestStruct that = (ComplexTestStruct) o;
      return Objects.equal(A, that.A)
          && Objects.equal(B, that.B)
          && Objects.equal(C, that.C)
          && Objects.equal(D, that.D)
          && Objects.equal(E, that.E)
          && Objects.equal(F, that.F)
          && Objects.equal(G, that.G);
    }
  }

  public static class ComplexTestStructDO implements DOCreator<ComplexTestStruct> {
    @JsonProperty("A")
    private Integer A;

    @JsonProperty("B")
    private List<Integer> B;

    @JsonProperty("C")
    private Integer C;

    @JsonProperty("D")
    private String D;

    @JsonProperty("E")
    private VarTestStructDO E;

    @JsonProperty("F")
    private List<FixedTestStructDO> F;

    @JsonProperty("G")
    private List<VarTestStructDO> G;

    public ComplexTestStruct create() {
      ComplexTestStruct complexTestStruct = new ComplexTestStruct();
      complexTestStruct.setA(UInt16.valueOf(A));
      complexTestStruct.setB(
          ReadList.wrap(
              B.stream().map(UInt16::valueOf).collect(Collectors.toList()),
              Function.identity(),
              128));
      complexTestStruct.setC(UInt8.valueOf(C));
      List<Bytes1> dValue = new ArrayList<>();
      byte[] data = BytesValue.fromHexString(D).extractArray();
      for (int i = 0; i < data.length; ++i) {
        dValue.add(Bytes1.wrap(data[i]));
      }
      complexTestStruct.setD(ReadList.wrap(dValue, Function.identity(), 256));
      complexTestStruct.setE(E.create());
      complexTestStruct.setF(
          ReadVector.wrap(
              F.stream().map(FixedTestStructDO::create).collect(Collectors.toList()),
              Function.identity(),
              4));
      complexTestStruct.setG(
          ReadVector.wrap(
              G.stream().map(VarTestStructDO::create).collect(Collectors.toList()),
              Function.identity(),
              2));
      return complexTestStruct;
    }

    public Integer getA() {
      return A;
    }

    public void setA(Integer a) {
      A = a;
    }

    public List<Integer> getB() {
      return B;
    }

    public void setB(List<Integer> b) {
      B = b;
    }

    public Integer getC() {
      return C;
    }

    public void setC(Integer c) {
      C = c;
    }

    public String getD() {
      return D;
    }

    public void setD(String d) {
      D = d;
    }

    public VarTestStructDO getE() {
      return E;
    }

    public void setE(VarTestStructDO e) {
      E = e;
    }

    public List<FixedTestStructDO> getF() {
      return F;
    }

    public void setF(List<FixedTestStructDO> f) {
      F = f;
    }

    public List<VarTestStructDO> getG() {
      return G;
    }

    public void setG(List<VarTestStructDO> g) {
      G = g;
    }
  }

  @SSZSerializable
  public static class BitsStruct {
    @SSZ(maxSize = 5)
    private Bitlist A;

    @SSZ(vectorLength = 2)
    private Bitvector B;

    @SSZ(vectorLength = 1)
    private Bitvector C;

    @SSZ(maxSize = 6)
    private Bitlist D;

    @SSZ(vectorLength = 8)
    private Bitvector E;

    public Bitlist getA() {
      return A;
    }

    public void setA(Bitlist a) {
      A = a;
    }

    public Bitvector getB() {
      return B;
    }

    public void setB(Bitvector b) {
      B = b;
    }

    public Bitvector getC() {
      return C;
    }

    public void setC(Bitvector c) {
      C = c;
    }

    public Bitlist getD() {
      return D;
    }

    public void setD(Bitlist d) {
      D = d;
    }

    public Bitvector getE() {
      return E;
    }

    public void setE(Bitvector e) {
      E = e;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      BitsStruct that = (BitsStruct) o;
      return Objects.equal(A, that.A)
          && Objects.equal(B, that.B)
          && Objects.equal(C, that.C)
          && Objects.equal(D, that.D)
          && Objects.equal(E, that.E);
    }
  }

  public static class BitsStructDO implements DOCreator<BitsStruct> {
    @JsonProperty("A")
    private String A;

    @JsonProperty("B")
    private String B;

    @JsonProperty("C")
    private String C;

    @JsonProperty("D")
    private String D;

    @JsonProperty("E")
    private String E;

    public BitsStruct create() {
      BitsStruct bitsStruct = new BitsStruct();
      bitsStruct.setA(Bitlist.of(BytesValue.fromHexString(A), 5));
      bitsStruct.setB(Bitvector.of(2, BytesValue.fromHexString(B)));
      bitsStruct.setC(Bitvector.of(1, BytesValue.fromHexString(C)));
      bitsStruct.setD(Bitlist.of(BytesValue.fromHexString(D), 6));
      bitsStruct.setE(Bitvector.of(8, BytesValue.fromHexString(E)));
      return bitsStruct;
    }

    public String getA() {
      return A;
    }

    public void setA(String a) {
      A = a;
    }

    public String getB() {
      return B;
    }

    public void setB(String b) {
      B = b;
    }

    public String getC() {
      return C;
    }

    public void setC(String c) {
      C = c;
    }

    public String getD() {
      return D;
    }

    public void setD(String d) {
      D = d;
    }

    public String getE() {
      return E;
    }

    public void setE(String e) {
      E = e;
    }
  }
}
