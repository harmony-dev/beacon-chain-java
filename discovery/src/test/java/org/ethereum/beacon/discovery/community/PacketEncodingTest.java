package org.ethereum.beacon.discovery.community;

import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.packet.RandomPacket;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import static org.junit.Assert.assertEquals;

public class PacketEncodingTest {
  @Test
  public void encodeRandomPacketTest() {
    RandomPacket randomPacket =
        RandomPacket.create(
            Bytes32.fromHexString(
                "0x0101010101010101010101010101010101010101010101010101010101010101"),
            BytesValue.fromHexString("0x020202020202020202020202"),
            BytesValue.fromHexString(
                "0x0404040404040404040404040404040404040404040404040404040404040404040404040404040404040404"));
    assertEquals(
        BytesValue.fromHexString(
            "0x01010101010101010101010101010101010101010101010101010101010101018c0202020202020202020202020404040404040404040404040404040404040404040404040404040404040404040404040404040404040404"),
        randomPacket.getBytes());
  }

  @Test
  public void encodeWhoAreYouTest() {
    WhoAreYouPacket whoAreYouPacket =
        WhoAreYouPacket.create(
            BytesValue.fromHexString(
                "0x0101010101010101010101010101010101010101010101010101010101010101"),
            BytesValue.fromHexString("0x020202020202020202020202"),
            Bytes32.fromHexString(
                "0x0303030303030303030303030303030303030303030303030303030303030303"),
            UInt64.valueOf(1));
    assertEquals(
        BytesValue.fromHexString(
            "0101010101010101010101010101010101010101010101010101010101010101ef8c020202020202020202020202a0030303030303030303030303030303030303030303030303030303030303030301"),
        whoAreYouPacket.getBytes());
  }

  @Test
  public void encodeAuthPacketTest() {
    Bytes32 tag =
        Bytes32.fromHexString("0x93a7400fa0d6a694ebc24d5cf570f65d04215b6ac00757875e3f3a5f42107903");
    BytesValue authTag = BytesValue.fromHexString("0x27b5af763c446acd2749fe8e");
    BytesValue idNonce =
        BytesValue.fromHexString(
            "0xe551b1c44264ab92bc0b3c9b26293e1ba4fed9128f3c3645301e8e119f179c65");
    BytesValue ephemeralPubkey =
        BytesValue.fromHexString(
            "0xb35608c01ee67edff2cffa424b219940a81cf2fb9b66068b1cf96862a17d353e22524fbdcdebc609f85cbd58ebe7a872b01e24a3829b97dd5875e8ffbc4eea81");
    BytesValue authRespCiphertext =
        BytesValue.fromHexString(
            "0x570fbf23885c674867ab00320294a41732891457969a0f14d11c995668858b2ad731aa7836888020e2ccc6e0e5776d0d4bc4439161798565a4159aa8620992fb51dcb275c4f755c8b8030c82918898f1ac387f606852");
    BytesValue messageCiphertext =
        BytesValue.fromHexString("0xa5d12a2d94b8ccb3ba55558229867dc13bfa3648");
    BytesValue authHeader =
        AuthHeaderMessagePacket.encodeAuthHeaderRlp(
            authTag, idNonce, ephemeralPubkey, authRespCiphertext);
    AuthHeaderMessagePacket authHeaderMessagePacket =
        AuthHeaderMessagePacket.create(tag, authHeader, messageCiphertext);

    assertEquals(
        BytesValue.fromHexString(
            "0x93a7400fa0d6a694ebc24d5cf570f65d04215b6ac00757875e3f3a5f42107903f8cc8c27b5af763c446acd2749fe8ea0e551b1c44264ab92bc0b3c9b26293e1ba4fed9128f3c3645301e8e119f179c658367636db840b35608c01ee67edff2cffa424b219940a81cf2fb9b66068b1cf96862a17d353e22524fbdcdebc609f85cbd58ebe7a872b01e24a3829b97dd5875e8ffbc4eea81b856570fbf23885c674867ab00320294a41732891457969a0f14d11c995668858b2ad731aa7836888020e2ccc6e0e5776d0d4bc4439161798565a4159aa8620992fb51dcb275c4f755c8b8030c82918898f1ac387f606852a5d12a2d94b8ccb3ba55558229867dc13bfa3648"),
        authHeaderMessagePacket.getBytes());
  }

  @Test
  public void encodeMessagePacketTest() {
    MessagePacket messagePacket =
        MessagePacket.create(
            Bytes32.fromHexString(
                "0x93a7400fa0d6a694ebc24d5cf570f65d04215b6ac00757875e3f3a5f42107903"),
            BytesValue.fromHexString("0x27b5af763c446acd2749fe8e"),
            BytesValue.fromHexString("0xa5d12a2d94b8ccb3ba55558229867dc13bfa3648"));

    assertEquals(
        BytesValue.fromHexString(
            "0x93a7400fa0d6a694ebc24d5cf570f65d04215b6ac00757875e3f3a5f421079038c27b5af763c446acd2749fe8ea5d12a2d94b8ccb3ba55558229867dc13bfa3648"),
        messagePacket.getBytes());
  }
}
