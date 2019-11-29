package org.ethereum.beacon.discovery;

import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.util.Utils;
import org.junit.Test;
import org.web3j.crypto.ECKeyPair;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.ethereum.beacon.discovery.Functions.PUBKEY_SIZE;
import static org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket.createIdNonceMessage;
import static org.junit.Assert.assertEquals;

public class FunctionsTest {
  private final BytesValue testKey1 =
      BytesValue.fromHexString("3332ca2b7003810449b6e596c3d284e914a1a51c9f76e4d9d7d43ef84adf6ed6");
  private final BytesValue testKey2 =
      BytesValue.fromHexString("66fb62bfbd66b9177a138c1e5cddbe4f7c30c343e94e68df8769459cb1cde628");
  private Bytes32 nodeId1;
  private Bytes32 nodeId2;

  public FunctionsTest() {
    byte[] homeNodeIdBytes = new byte[32];
    homeNodeIdBytes[0] = 0x01;
    byte[] destNodeIdBytes = new byte[32];
    destNodeIdBytes[0] = 0x02;
    this.nodeId1 = Bytes32.wrap(homeNodeIdBytes);
    this.nodeId2 = Bytes32.wrap(destNodeIdBytes);
  }

  @Test
  public void testLogDistance() {
    Bytes32 nodeId0 =
        Bytes32.fromHexString("0000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 nodeId1a =
        Bytes32.fromHexString("0000000000000000000000000000000000000000000000000000000000000001");
    Bytes32 nodeId1b =
        Bytes32.fromHexString("1000000000000000000000000000000000000000000000000000000000000000");
    Bytes32 nodeId1s =
        Bytes32.fromHexString("1111111111111111111111111111111111111111111111111111111111111111");
    Bytes32 nodeId9s =
        Bytes32.fromHexString("9999999999999999999999999999999999999999999999999999999999999999");
    Bytes32 nodeIdfs =
        Bytes32.fromHexString("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    assertEquals(0, Functions.logDistance(nodeId1a, nodeId1a));
    assertEquals(1, Functions.logDistance(nodeId0, nodeId1a));
    // So it's big endian
    assertEquals(253, Functions.logDistance(nodeId0, nodeId1b));
    assertEquals(253, Functions.logDistance(nodeId0, nodeId1s));
    assertEquals(256, Functions.logDistance(nodeId0, nodeId9s));
    // maximum distance
    assertEquals(256, Functions.logDistance(nodeId0, nodeIdfs));
    // logDistance is not an additive function
    assertEquals(255, Functions.logDistance(nodeId9s, nodeIdfs));
  }

  @Test
  public void hkdfExpandTest() {
    BytesValue idNonce =
        Bytes32.fromHexString("68b02a985ecb99cc2d10cf188879d93ae7684c4f4707770017b078c6497c5a5d");
    Functions.HKDFKeys keys1 =
        Functions.hkdf_expand(
            nodeId1,
            nodeId2,
            testKey1,
            BytesValue.wrap(ECKeyPair.create(testKey2.extractArray()).getPublicKey().toByteArray()),
            idNonce);
    Functions.HKDFKeys keys2 =
        Functions.hkdf_expand(
            nodeId1,
            nodeId2,
            testKey2,
            BytesValue.wrap(ECKeyPair.create(testKey1.extractArray()).getPublicKey().toByteArray()),
            idNonce);
    assertEquals(keys1, keys2);
  }

  @Test
  public void testGcmSimple() {
    BytesValue authResponseKey = BytesValue.fromHexString("0x60bfc5c924a8d640f47df8b781f5a0e5");
    BytesValue authResponsePt =
        BytesValue.fromHexString(
            "0xf8aa05b8404f5fa8309cab170dbeb049de504b519288777aae0c4b25686f82310206a4a1e264dc6e8bfaca9187e8b3dbb56f49c7aa3d22bff3a279bf38fb00cb158b7b8ca7b865f86380018269648276348375647082765f826970847f00000189736563703235366b31b84013d14211e0287b2361a1615890a9b5212080546d0a257ae4cff96cf534992cb97e6adeb003652e807c7f2fe843e0c48d02d4feb0272e2e01f6e27915a431e773");
    BytesValue zeroNonce = BytesValue.wrap(new byte[12]);
    BytesValue authResponse =
        Functions.aesgcm_encrypt(authResponseKey, zeroNonce, authResponsePt, BytesValue.EMPTY);
    BytesValue authResponsePtDecrypted =
        Functions.aesgcm_decrypt(authResponseKey, zeroNonce, authResponse, BytesValue.EMPTY);
    assertEquals(authResponsePt, authResponsePtDecrypted);
  }

  @Test
  public void testRecoverFromSignature() throws Exception {
    BytesValue idNonceSig =
        BytesValue.fromHexString(
            "0xcf2bf743fc2273709bbc5117fd72775b0661ce1b6e9dffa01f45e2307fb138b90da16364ee7ae1705b938f6648d7725d35fe7e3f200e0ea022c1360b9b2e7385");
    BytesValue ephemeralKey =
        BytesValue.fromHexString(
            "0x9961e4c2356d61bedb83052c115d311acb3a96f5777296dcf297351130266231503061ac4aaee666073d7e5bc2c80c3f5c5b500c1cb5fd0a76abbb6b675ad157");
    BytesValue nonce =
        BytesValue.fromHexString(
            "0x02a77e3aa0c144ae7c0a3af73692b7d6e5b7a2fdc0eda16e8d5e6cb0d08e88dd04");
    BytesValue privKey =
        BytesValue.fromHexString(
            "0xfb757dc581730490a1d7a00deea65e9b1936924caaea8f44d476014856b68736");
    BytesValue pubKeyUncompressed =
        BytesValue.wrap(
            Utils.extractBytesFromUnsignedBigInt(
                ECKeyPair.create(privKey.extractArray()).getPublicKey(), PUBKEY_SIZE));
    BytesValue pubKey =
        BytesValue.wrap(Functions.publicKeyToPoint(pubKeyUncompressed).getEncoded(true));

    BytesValue message =
        BytesValue.wrap("discovery-id-nonce".getBytes()).concat(nonce).concat(ephemeralKey);
    assert Functions.verifyECDSASignature(idNonceSig, Functions.hash(message), pubKey);
  }

  @Test
  public void testSignAndRecoverFromSignature() {
    BytesValue idNonce =
        BytesValue.fromHexString(
            "0xd550ca9d62930c947efff75b58a4ea1b44716d841cc0d690879d4f3cab5a4e84");
    BytesValue ephemeralPubkey =
        BytesValue.fromHexString(
            "0xd9bc9158f0a0c40e75490de66ef44f865588d1c7110b29d0c479db19f7644ddad2d8e948cb933bd767437b173888409d73644a36ae1d068997217357a22d674f");
    BytesValue privKey =
        BytesValue.fromHexString(
            "0xb5a8efa45da6906663cf7a158cd506da71ae7d732a68220e6644468526bb098e");
    BytesValue pubKey =
        BytesValue.wrap(
            Functions.publicKeyToPoint(
                    BytesValue.wrap(
                        Utils.extractBytesFromUnsignedBigInt(
                            ECKeyPair.create(privKey.extractArray()).getPublicKey(), 64)))
                .getEncoded(true));
    BytesValue idNonceSig = AuthHeaderMessagePacket.signIdNonce(idNonce, privKey, ephemeralPubkey);
    assert Functions.verifyECDSASignature(
        idNonceSig, Functions.hash(createIdNonceMessage(idNonce, ephemeralPubkey)), pubKey);
  }
}
