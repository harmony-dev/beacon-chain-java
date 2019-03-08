package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.emulator.config.Config;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Spec implements Config {
  private SpecConstantsData specConstants;
  private SpecHelpersOptions specHelpersOptions = new SpecHelpersOptions();

  public SpecConstants buildSpecConstants() {
    return specConstants.buildSpecConstants();
  }

  public SpecHelpers buildSpecHelpers(boolean blsVerifyEnabled) {
    SpecHelpers defaultSpecHelpers =
        SpecHelpers.createWithSSZHasher(buildSpecConstants());
    if (blsVerifyEnabled) {
      return defaultSpecHelpers;
    } else {
      return new SpecHelpers(
          defaultSpecHelpers.getConstants(),
          defaultSpecHelpers.getHashFunction(),
          defaultSpecHelpers.getObjectHasher()) {

        @Override
        public boolean bls_verify(PublicKey blsPublicKey, Hash32 message, BLSSignature signature,
            Bytes8 domain) {
          return true;
        }

        @Override
        public boolean bls_verify_multiple(List<PublicKey> publicKeys, List<Hash32> messages,
            BLSSignature signature, Bytes8 domain) {
          return true;
        }
      };
    }
  }

  public SpecConstantsData getSpecConstants() {
    return specConstants;
  }

  public void setSpecConstants(
      SpecConstantsData specConstants) {
    this.specConstants = specConstants;
  }

  public SpecHelpersOptions getSpecHelpersOptions() {
    return specHelpersOptions;
  }

  public void setSpecHelpersOptions(
      SpecHelpersOptions specHelpersOptions) {
    this.specHelpersOptions = specHelpersOptions;
  }
}
