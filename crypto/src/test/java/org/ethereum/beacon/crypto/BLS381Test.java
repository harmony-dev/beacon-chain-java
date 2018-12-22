package org.ethereum.beacon.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Random;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.MessageParameters.Impl;
import org.junit.Test;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public class BLS381Test {

  @Test
  public void checkSignAndVerifyFlow() {
    KeyPair keyPair = BLS381.KeyPair.generate();
    BytesValue message = randomMessage();
    BytesValue domain = randomDomain();

    MessageParameters spec = new Impl(Hashes.keccack256(message), domain);
    Signature signature = BLS381.sign(spec, keyPair);
    boolean verified = BLS381.verify(spec, signature, keyPair.getPublic());

    assertThat(verified).isTrue();
  }

  @Test
  public void failToVerifyIfMessageIsWrong() {
    KeyPair keyPair = BLS381.KeyPair.generate();

    MessageParameters rightMessage = new Impl(Hashes.keccack256(randomMessage()), randomDomain());
    MessageParameters wrongMessage = new Impl(Hashes.keccack256(randomMessage()), randomDomain());

    Signature signature = BLS381.sign(rightMessage, keyPair);
    boolean verified = BLS381.verify(wrongMessage, signature, keyPair.getPublic());

    assertThat(verified).isFalse();
  }

  @Test
  public void failToVerifyIfSignatureIsWrong() {
    KeyPair keyPair = BLS381.KeyPair.generate();

    MessageParameters rightMessage = new Impl(Hashes.keccack256(randomMessage()), randomDomain());
    MessageParameters wrongMessage = new Impl(Hashes.keccack256(randomMessage()), randomDomain());

    Signature wrongSignature = BLS381.sign(wrongMessage, keyPair);
    boolean verified = BLS381.verify(rightMessage, wrongSignature, keyPair.getPublic());

    assertThat(verified).isFalse();
  }

  @Test
  public void failToVerifyIfPubKeyIsWrong() {
    KeyPair rightKeyPair = BLS381.KeyPair.generate();
    KeyPair wrongKeyPair = BLS381.KeyPair.generate();

    MessageParameters message = new Impl(Hashes.keccack256(randomMessage()), randomDomain());
    Signature signature = BLS381.sign(message, rightKeyPair);
    boolean verified = BLS381.verify(message, signature, wrongKeyPair.getPublic());

    assertThat(verified).isFalse();
  }

  @Test
  public void verifyAggregatedSignature() {
    KeyPair bob = BLS381.KeyPair.generate();
    KeyPair alice = BLS381.KeyPair.generate();

    MessageParameters message = new Impl(Hashes.keccack256(randomMessage()), randomDomain());

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

    MessageParameters message = new Impl(Hashes.keccack256(randomMessage()), randomDomain());
    MessageParameters wrongMessage = new Impl(Hashes.keccack256(randomMessage()), randomDomain());

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

    MessageParameters message = new Impl(Hashes.keccack256(randomMessage()), randomDomain());

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

    MessageParameters message = new Impl(Hashes.keccack256(randomMessage()), randomDomain());

    Signature bobSignature = BLS381.sign(message, bob);
    Signature aliceSignature = BLS381.sign(message, alice);

    Signature aggregatedSignature =
        BLS381.Signature.aggregate(Arrays.asList(bobSignature, aliceSignature));
    PublicKey aggregatedKeys =
        BLS381.PublicKey.aggregate(Arrays.asList(bob.getPublic(), wrongKeyPair.getPublic()));
    boolean verified = BLS381.verify(message, aggregatedSignature, aggregatedKeys);

    assertThat(verified).isFalse();
  }

  BytesValue randomMessage() {
    Random random = new Random();
    byte[] message = new byte[Math.abs(random.nextInt()) % 32 + 32];
    random.nextBytes(message);
    return BytesValue.wrap(message);
  }

  BytesValue randomDomain() {
    Random random = new Random();
    byte[] domain = new byte[8];
    random.nextBytes(domain);
    return BytesValue.wrap(domain);
  }
}
