package org.ethereum.beacon.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Random;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.MessageParameters.Impl;
import org.ethereum.beacon.crypto.bls.codec.PointData;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;

public class BLS381Test {

  @Test
  public void checkSignAndVerifyFlow() {
    KeyPair keyPair = BLS381.KeyPair.generate();
    BytesValue message = randomMessage();
    Bytes8 domain = randomDomain();

    MessageParameters params = new Impl(Hashes.keccak256(message), domain);
    Signature signature = BLS381.sign(params, keyPair);

    PublicKey decodedPublicKey = PublicKey.create(keyPair.getPublic().getEncodedBytes());
    Signature decodedSignature = Signature.create(signature.getEncoded());

    boolean verified = BLS381.verify(params, decodedSignature, decodedPublicKey);

    assertThat(verified).isTrue();
  }

  @Test
  public void failToVerifyIfMessageIsWrong() {
    KeyPair keyPair = BLS381.KeyPair.generate();

    MessageParameters rightMessage = new Impl(Hashes.keccak256(randomMessage()), randomDomain());
    MessageParameters wrongMessage = new Impl(Hashes.keccak256(randomMessage()), randomDomain());

    Signature signature = BLS381.sign(rightMessage, keyPair);
    boolean verified = BLS381.verify(wrongMessage, signature, keyPair.getPublic());

    assertThat(verified).isFalse();
  }

  @Test
  public void failToVerifyIfSignatureIsWrong() {
    KeyPair keyPair = BLS381.KeyPair.generate();

    MessageParameters rightMessage = new Impl(Hashes.keccak256(randomMessage()), randomDomain());
    MessageParameters wrongMessage = new Impl(Hashes.keccak256(randomMessage()), randomDomain());

    Signature wrongSignature = BLS381.sign(wrongMessage, keyPair);
    boolean verified = BLS381.verify(rightMessage, wrongSignature, keyPair.getPublic());

    assertThat(verified).isFalse();
  }

  @Test
  public void failToVerifyIfPubKeyIsWrong() {
    KeyPair rightKeyPair = BLS381.KeyPair.generate();
    KeyPair wrongKeyPair = BLS381.KeyPair.generate();

    MessageParameters message = new Impl(Hashes.keccak256(randomMessage()), randomDomain());
    Signature signature = BLS381.sign(message, rightKeyPair);
    boolean verified = BLS381.verify(message, signature, wrongKeyPair.getPublic());

    assertThat(verified).isFalse();
  }

  @Test
  public void verifyAggregatedSignature() {
    KeyPair bob = BLS381.KeyPair.generate();
    KeyPair alice = BLS381.KeyPair.generate();

    MessageParameters message = new Impl(Hashes.keccak256(randomMessage()), randomDomain());

    Signature bobSignature = BLS381.sign(message, bob);
    Signature aliceSignature = BLS381.sign(message, alice);

    Signature aggregatedSignature =
        BLS381.Signature.aggregate(Arrays.asList(bobSignature, aliceSignature));
    PublicKey aggregatedKeys =
        BLS381.PublicKey.aggregate(Arrays.asList(bob.getPublic(), alice.getPublic()));
    boolean verified = BLS381.verify(message, aggregatedSignature, aggregatedKeys);

    assertThat(verified).isTrue();
  }

  @Test
  public void failToVerifyIfWrongMessagesAggregated() {
    KeyPair bob = BLS381.KeyPair.generate();
    KeyPair alice = BLS381.KeyPair.generate();

    MessageParameters message = new Impl(Hashes.keccak256(randomMessage()), randomDomain());
    MessageParameters wrongMessage = new Impl(Hashes.keccak256(randomMessage()), randomDomain());

    Signature bobSignature = BLS381.sign(message, bob);
    Signature wrongSignature = BLS381.sign(wrongMessage, alice);

    Signature aggregatedSignature =
        BLS381.Signature.aggregate(Arrays.asList(bobSignature, wrongSignature));
    PublicKey aggregatedKeys =
        BLS381.PublicKey.aggregate(Arrays.asList(bob.getPublic(), alice.getPublic()));
    boolean verified = BLS381.verify(message, aggregatedSignature, aggregatedKeys);

    assertThat(verified).isFalse();
  }

  @Test
  public void failToVerifyIfWrongSignaturesAggregated() {
    KeyPair bob = BLS381.KeyPair.generate();
    KeyPair alice = BLS381.KeyPair.generate();
    KeyPair wrongKeyPair = BLS381.KeyPair.generate();

    MessageParameters message = new Impl(Hashes.keccak256(randomMessage()), randomDomain());

    Signature bobSignature = BLS381.sign(message, bob);
    Signature wrongSignature = BLS381.sign(message, wrongKeyPair);

    Signature aggregatedSignature =
        BLS381.Signature.aggregate(Arrays.asList(bobSignature, wrongSignature));
    PublicKey aggregatedKeys =
        BLS381.PublicKey.aggregate(Arrays.asList(bob.getPublic(), alice.getPublic()));
    boolean verified = BLS381.verify(message, aggregatedSignature, aggregatedKeys);

    assertThat(verified).isFalse();
  }

  @Test
  public void failToVerifyIfWrongPubKeysAggregated() {
    KeyPair bob = BLS381.KeyPair.generate();
    KeyPair alice = BLS381.KeyPair.generate();
    KeyPair wrongKeyPair = BLS381.KeyPair.generate();

    MessageParameters message = new Impl(Hashes.keccak256(randomMessage()), randomDomain());

    Signature bobSignature = BLS381.sign(message, bob);
    Signature aliceSignature = BLS381.sign(message, alice);

    Signature aggregatedSignature =
        BLS381.Signature.aggregate(Arrays.asList(bobSignature, aliceSignature));
    PublicKey aggregatedKeys =
        BLS381.PublicKey.aggregate(Arrays.asList(bob.getPublic(), wrongKeyPair.getPublic()));
    boolean verified = BLS381.verify(message, aggregatedSignature, aggregatedKeys);

    assertThat(verified).isFalse();
  }

  @Test
  public void throwIfSignatureIsInvalidPoint() {
    byte[] x1 =
        BytesValue.fromHexString(
                "0x074752311471f52ffd86405410eb65ab9cf09cc67496e11b6706e1e25c35898c6021fdb9dffa908b5981d21211a9350a")
            .getArrayUnsafe();
    byte[] x2 =
        BytesValue.fromHexString(
                "0x04b93f0c13abd2a249b267ca11fd3b207eb2de5189f6015f0a9b915fcf1e047057f3ba85216850e1255598657502411f")
            .getArrayUnsafe();

    BytesValue notInG2Point = PointData.G2.create(x1, x2, false, 0).encode();
    MutableBytesValue wrongEncoding = MutableBytesValue.wrap(notInG2Point.extractArray());
    wrongEncoding.set(0, (byte) (wrongEncoding.get(0) & 0x7F));

    assertThatThrownBy(() -> Signature.create(wrongEncoding))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Failed to instantiate signature, invalid c_flag, should always be 1 but got 0");

    assertThatThrownBy(() -> Signature.create(notInG2Point))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Failed to instantiate signature, given point is not a G2 member");
  }

  @Test
  public void throwIfPublicKeyIsInvalidPoint() {
    byte[] x =
        BytesValue.fromHexString(
                "0x074752311471f52ffd86405410eb65ab9cf09cc67496e11b6706e1e25c35898c6021fdb9dffa908b5981d21211a9350a")
            .getArrayUnsafe();

    BytesValue notInG1Point = PointData.G1.create(x, false, 0).encode();
    MutableBytesValue wrongEncoding = MutableBytesValue.wrap(notInG1Point.extractArray());
    wrongEncoding.set(0, (byte) (wrongEncoding.get(0) & 0x7F));

    assertThatThrownBy(() -> PublicKey.create(wrongEncoding))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "Failed to instantiate public key, invalid c_flag, should always be 1 but got 0");

    assertThatThrownBy(() -> PublicKey.create(notInG1Point))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Failed to instantiate public key, given point is not a G1 member");
  }

  @Test
  public void checkSignAndVerifyMultipleFlow() {
    KeyPair keyPair1 = BLS381.KeyPair.generate();
    KeyPair keyPair2 = BLS381.KeyPair.generate();
    BytesValue message1 = randomMessage();
    BytesValue message2 = randomMessage();
    Bytes8 domain = randomDomain();

    MessageParameters params1 = new MessageParameters.Impl(Hashes.keccak256(message1), domain);
    MessageParameters params2 = new MessageParameters.Impl(Hashes.keccak256(message2), domain);
    Signature signature1 = BLS381.sign(params1, keyPair1);
    Signature signature2 = BLS381.sign(params2, keyPair2);

    PublicKey decodedPublicKey1 = PublicKey.create(keyPair1.getPublic().getEncodedBytes());
    PublicKey decodedPublicKey2 = PublicKey.create(keyPair2.getPublic().getEncodedBytes());
    Signature aggregatedSignature = Signature.aggregate(Arrays.asList(signature1, signature2));
    Signature decodedSignature = Signature.create(aggregatedSignature.getEncoded());

    boolean verified =
        BLS381.verifyMultiple(
            Arrays.asList(params1, params2),
            decodedSignature,
            Arrays.asList(decodedPublicKey1, decodedPublicKey2));

    assertThat(verified).isTrue();
  }

  @Test
  public void failVerifyMultipleIfPublicKeysAreMixed() {
    KeyPair keyPair1 = BLS381.KeyPair.generate();
    KeyPair keyPair2 = BLS381.KeyPair.generate();
    BytesValue message1 = randomMessage();
    BytesValue message2 = randomMessage();
    Bytes8 domain = randomDomain();

    MessageParameters params1 = new MessageParameters.Impl(Hashes.keccak256(message1), domain);
    MessageParameters params2 = new MessageParameters.Impl(Hashes.keccak256(message2), domain);
    Signature signature1 = BLS381.sign(params1, keyPair1);
    Signature signature2 = BLS381.sign(params2, keyPair2);

    PublicKey decodedPublicKey1 = PublicKey.create(keyPair1.getPublic().getEncodedBytes());
    PublicKey decodedPublicKey2 = PublicKey.create(keyPair2.getPublic().getEncodedBytes());
    Signature aggregatedSignature = Signature.aggregate(Arrays.asList(signature1, signature2));
    Signature decodedSignature = Signature.create(aggregatedSignature.getEncoded());

    boolean verified =
        BLS381.verifyMultiple(
            Arrays.asList(params1, params2),
            decodedSignature,
            Arrays.asList(decodedPublicKey2, decodedPublicKey1));

    assertThat(verified).isFalse();
  }

  @Test
  public void checkVerifyMultipleIfPublicKeysAreSame() {
    KeyPair keyPair = BLS381.KeyPair.generate();
    BytesValue message1 = randomMessage();
    BytesValue message2 = randomMessage();
    Bytes8 domain = randomDomain();

    MessageParameters params1 = new MessageParameters.Impl(Hashes.keccak256(message1), domain);
    MessageParameters params2 = new MessageParameters.Impl(Hashes.keccak256(message2), domain);
    Signature signature1 = BLS381.sign(params1, keyPair);
    Signature signature2 = BLS381.sign(params2, keyPair);

    PublicKey decodedPublicKey = PublicKey.create(keyPair.getPublic().getEncodedBytes());
    Signature aggregatedSignature = Signature.aggregate(Arrays.asList(signature1, signature2));
    Signature decodedSignature = Signature.create(aggregatedSignature.getEncoded());

    boolean verified =
        BLS381.verifyMultiple(
            Arrays.asList(params1, params2),
            decodedSignature,
            Arrays.asList(decodedPublicKey, decodedPublicKey));

    assertThat(verified).isTrue();
  }

  @Test
  public void throwIfMessagesAndPublicKeysDoNotMatch() {
    KeyPair keyPair = BLS381.KeyPair.generate();
    BytesValue message = randomMessage();
    Bytes8 domain = randomDomain();

    MessageParameters params = new MessageParameters.Impl(Hashes.keccak256(message), domain);
    Signature signature = BLS381.sign(params, keyPair);

    assertThatThrownBy(
            () ->
                BLS381.verifyMultiple(
                    Arrays.asList(params),
                    signature,
                    Arrays.asList(keyPair.getPublic(), keyPair.getPublic())))
        .isInstanceOf(AssertionError.class);
  }

  BytesValue randomMessage() {
    Random random = new Random();
    byte[] message = new byte[Math.abs(random.nextInt()) % 32 + 32];
    random.nextBytes(message);
    return BytesValue.wrap(message);
  }

  Bytes8 randomDomain() {
    Random random = new Random();
    byte[] domain = new byte[8];
    random.nextBytes(domain);
    return Bytes8.wrap(domain);
  }
}
