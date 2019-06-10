package org.ethereum.beacon.validator.crypto;

import org.apache.milagro.amcl.BLS381.ECP2;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import tech.pegasys.artemis.util.bytes.Bytes32;

/** A pair of {@link BLS381MessageSigner} instance and a corresponding pubkey. */
public class BLS381Credentials {
  private final BLSPubkey pubkey;
  private final MessageSigner<BLSSignature> signer;

  public BLS381Credentials(BLSPubkey pubkey, MessageSigner<BLSSignature> signer) {
    this.pubkey = pubkey;
    this.signer = signer;
  }

  public BLSPubkey getPubkey() {
    return pubkey;
  }

  public MessageSigner<BLSSignature> getSigner() {
    return signer;
  }

  public static BLS381Credentials createWithDummySigner(BLS381.KeyPair keyPair) {
    BLSPubkey pubkey = BLSPubkey.wrap(keyPair.getPublic().getEncodedBytes());
    BLS381MessageSigner signer =
        (messageHash, domain) ->
            BLSSignature.wrap(BLS381.Signature.create(new ECP2()).getEncoded());
    return new BLS381Credentials(pubkey, signer);
  }

  public static BLS381Credentials createWithInsecureSigner(BLS381.KeyPair keyPair) {
    BLSPubkey pubkey = BLSPubkey.wrap(keyPair.getPublic().getEncodedBytes());
    BLS381MessageSigner signer =
        new InsecureBLS381MessageSigner(keyPair.getPrivate().getEncodedBytes());
    return new BLS381Credentials(pubkey, signer);
  }

  public static BLS381Credentials createWithInsecureSigner(Bytes32 privateKeyBytes) {
    BLSPubkey pubkey =
        BLSPubkey.wrap(
            BLS381.KeyPair.create(PrivateKey.create(privateKeyBytes))
                .getPublic()
                .getEncodedBytes());
    BLS381MessageSigner signer = new InsecureBLS381MessageSigner(privateKeyBytes);
    return new BLS381Credentials(pubkey, signer);
  }
}
