package org.ethereum.beacon.consensus.util;

import org.ethereum.beacon.consensus.spec.BLSFunctions;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.Hashes;
import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.Arrays;
import java.util.Random;

public class PseudoBLSFunctionsTest {

  @Test
  public void bls_verify_multiple() {
    BLSFunctions blsFunctions = new PseudoBLSFunctions();

    Random rnd = new Random(1);
    BLSPubkey pk0 = BLSPubkey.wrap(Bytes48.random(rnd));
    BLSPubkey pk1 = BLSPubkey.wrap(Bytes48.random(rnd));
    BLSPubkey pk2 = BLSPubkey.wrap(Bytes48.random(rnd));
    BLSPubkey pk3 = BLSPubkey.wrap(Bytes48.ZERO);

    UInt64 domain = UInt64.valueOf(123);
    Hash32 msg0 = Hashes.sha256(UInt64.random(rnd).toBytes8());
    Hash32 msg1 = Hashes.sha256(UInt64.random(rnd).toBytes8());
    Hash32 msg2 = Hashes.sha256(UInt64.random(rnd).toBytes8());
    Hash32 msg3 = Hashes.sha256(UInt64.random(rnd).toBytes8());
    BLSSignature sig0 = BLSSignature.wrap(PseudoBLSFunctions.pseudoSign(pk0, msg0, domain));
    Assert.assertTrue(blsFunctions.bls_verify(BLSPubkey.wrap(pk0), msg0, sig0, domain));
    BLSSignature sig1 = BLSSignature.wrap(PseudoBLSFunctions.pseudoSign(pk1, msg1, domain));
    Assert.assertTrue(blsFunctions.bls_verify(BLSPubkey.wrap(pk1), msg1, sig1, domain));
    BLSSignature sig2 = BLSSignature.wrap(PseudoBLSFunctions.pseudoSign(pk2, msg2, domain));
    Assert.assertTrue(blsFunctions.bls_verify(BLSPubkey.wrap(pk2), msg2, sig2, domain));
    BLSSignature sig3 = BLSSignature.wrap(PseudoBLSFunctions.pseudoSign(pk3, msg3, domain));
    Assert.assertTrue(blsFunctions.bls_verify(BLSPubkey.wrap(pk3), msg3, sig3, domain));

    BLSSignature aggregated =
        PseudoBLSFunctions.mergeSignatures(Arrays.asList(sig0, sig1, sig2, sig3));

    BLS381.PublicKey ppk0 = BLS381.PublicKey.createWithoutValidation(pk0);
    BLS381.PublicKey ppk1 = BLS381.PublicKey.createWithoutValidation(pk1);
    BLS381.PublicKey ppk2 = BLS381.PublicKey.createWithoutValidation(pk2);
    BLS381.PublicKey ppk3 = BLS381.PublicKey.createWithoutValidation(pk3);

    Assert.assertTrue(
        blsFunctions.bls_verify_multiple(
            Arrays.asList(ppk0, ppk1, ppk2, ppk3),
            Arrays.asList(msg0, msg1, msg2, msg3),
            aggregated,
            domain));
  }
}
