package org.ethereum.beacon.validator.crypto;

import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;

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

  public static BLS381Credentials createWithInsecureSigner(BLS381.KeyPair keyPair) {
    BLSPubkey pubkey = BLSPubkey.wrap(keyPair.getPublic().getEncodedBytes());
    BLS381MessageSigner signer =
        new InsecureBLS381MessageSigner(keyPair.getPrivate().getEncodedBytes());
    return new BLS381Credentials(pubkey, signer);
  }
}
