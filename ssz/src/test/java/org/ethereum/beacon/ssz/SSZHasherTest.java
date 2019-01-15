package org.ethereum.beacon.ssz;

import net.consensys.cava.bytes.Bytes;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.fixtures.AttestationRecord;
import org.ethereum.beacon.ssz.fixtures.Bitfield;
import org.ethereum.beacon.ssz.fixtures.Sign;
import org.junit.Before;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.sun.org.apache.xerces.internal.impl.dv.util.HexBin.decode;
import static org.junit.Assert.assertEquals;

/**
 * Tests of {@link SSZHasher}
 */
public class SSZHasherTest {
  private SSZHasher sszHasher;

  @Before
  public void setup() {
    Function<Bytes, Bytes> hashingFunction = (data) -> Bytes.wrap(Hashes.keccak256(BytesValue.of(data.toArrayUnsafe())).getArrayUnsafe());
    sszHasher = new SSZHasher(SSZHasher.getDefaultBuilder(hashingFunction, false), hashingFunction);
  }

  private static byte[] DEFAULT_HASH = Hashes.keccak256(BytesValue.fromHexString("aa")).getArrayUnsafe();
  private static Sign.Signature DEFAULT_SIG = new Sign.Signature();
  static {
    DEFAULT_SIG.r = new BigInteger("23452342342342342342342315643768758756967967");
    DEFAULT_SIG.s = new BigInteger("8713785871");
  }

  @Test
  public void bitfieldTest() {
    Bitfield bitfield = new Bitfield(
        decode("abcd")
    );

    Bytes hash = sszHasher.calc(bitfield);
    assertEquals(Bytes.fromHexString("A0B1BE2F50398CA7FE11EA48E5AFE9F89F758EC815E5C12BE21315AF6D34FA1D"), hash);
  }

  @Test
  public void SignatureTest() {
    Bytes hash = sszHasher.calc(DEFAULT_SIG);
    assertEquals(Bytes.fromHexString("D75724A07F4EFB3B456408DD6C36C70A6DF189FAE6A09F7AD0C848F0D3341290"), hash);
  }

  @Test
  public void simpleTest() {
    AttestationRecord attestationRecord = new AttestationRecord(
        12412L,
        123,
        Collections.emptyList(),
        DEFAULT_HASH,
        new Bitfield(decode("abcdef45")),
        12400L,
        DEFAULT_HASH,
        DEFAULT_SIG
    );

    Bytes hash = sszHasher.calc(attestationRecord);
    assertEquals(Bytes.fromHexString("443F16942CE8F1EDC9E1BB6984B0E69FC40271D3E20E7103FFA026068A729379"), hash);
  }

  @Test
  public void list32Test() {
    List<byte[]> hashes = new ArrayList<>();
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("aa")).getArrayUnsafe());
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("bb")).getArrayUnsafe());
    hashes.add(Hashes.keccak256(BytesValue.fromHexString("cc")).getArrayUnsafe());
    AttestationRecord attestationRecord = new AttestationRecord(
        12412L,
        123,
        hashes,
        DEFAULT_HASH,
        new Bitfield(decode("abcdef45")),
        12400L,
        DEFAULT_HASH,
        DEFAULT_SIG
    );

    Bytes hash = sszHasher.calc(attestationRecord);
    assertEquals(Bytes.fromHexString("14190EBD6C1CFC3244E7BE9ECA0EC0BB52361822A923437A7C99E58A12260E42"), hash);
  }

  @Test
  public void list48Test() {
    List<byte[]> hashes = new ArrayList<>();
    hashes.add(Hashes.keccak384(BytesValue.fromHexString("aa")).getArrayUnsafe());
    hashes.add(Hashes.keccak384(BytesValue.fromHexString("bb")).getArrayUnsafe());
    hashes.add(Hashes.keccak384(BytesValue.fromHexString("cc")).getArrayUnsafe());
    AttestationRecord attestationRecord = new AttestationRecord(
        12412L,
        123,
        hashes,
        DEFAULT_HASH,
        new Bitfield(decode("abcdef45")),
        12400L,
        DEFAULT_HASH,
        DEFAULT_SIG
    );

    Bytes hash = sszHasher.calc(attestationRecord);
    assertEquals(Bytes.fromHexString("54BBAAED03788F6A7B5F16CB93CF2D07DEF7BE070ABC11C676086ABC645563F3"), hash);
  }

  @SSZSerializable
  public static class SomeObject {
    private List<Long> list;

    public SomeObject(List<Long> list) {
      this.list = list;
    }

    public List<Long> getList() {
      return list;
    }
  }

  @Test
  public void smallItemsListTest() {
    List<Long> list = new ArrayList<>();
    list.add(1L);
    list.add(2L);
    list.add(12345L);
    list.add(Long.MAX_VALUE);
    SomeObject someObject = new SomeObject(list);

    Bytes hash = sszHasher.calc(someObject);
    assertEquals(Bytes.fromHexString("BD4AB28F883B78BF4C5B3652AFCF272EAD9026C3361821A0420777A9B3C20425"), hash);
  }

  @SSZSerializable
  public static class AnotherObject {
    private int item;

    public AnotherObject(int item) {
      this.item = item;
    }

    public int getItem() {
      return item;
    }
  }

  @Test
  public void smallItemTest() {
    AnotherObject anotherObject1 = new AnotherObject(1);
    AnotherObject anotherObject2 = new AnotherObject(2);

    Bytes hash1 = sszHasher.calc(anotherObject1);
    Bytes hash2 = sszHasher.calc(anotherObject2);
    assertEquals(Bytes.fromHexString("FB5BAAECAB62C516763CEA2DFBA17FBBC24907E4E3B0BE426BDE71BE89AF495F"), hash1);
    assertEquals(Bytes.fromHexString("B7047395B0D5A9C70336FDE7E40DE2BB369FE67C8E762A35641E209B7338FDD9"), hash2);
  }
}
