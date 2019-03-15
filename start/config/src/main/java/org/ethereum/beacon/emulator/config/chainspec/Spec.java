package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.emulator.config.Config;
import org.ethereum.beacon.emulator.config.YamlPrinter;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Spec implements Config {
  private SpecConstantsData specConstants;

  public SpecConstants buildSpecConstants() {
    return specConstants.buildSpecConstants();
  }

  public SpecHelpers buildSpecHelpers(boolean blsVerifyEnabled, boolean proofVerifyEnabled) {
    SpecHelpers defaultSpecHelpers =
        SpecHelpers.createWithSSZHasher(buildSpecConstants());
    if (blsVerifyEnabled && proofVerifyEnabled) {
      return defaultSpecHelpers;
    } else if (!blsVerifyEnabled) {
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

        @Override
        public void process_deposit(MutableBeaconState state, Deposit deposit) {
          super.process_deposit_inner(state, deposit, proofVerifyEnabled);
        }
      };
    } else {
      return new SpecHelpers(
          defaultSpecHelpers.getConstants(),
          defaultSpecHelpers.getHashFunction(),
          defaultSpecHelpers.getObjectHasher()) {

        @Override
        public void process_deposit(MutableBeaconState state, Deposit deposit) {
          super.process_deposit_inner(state, deposit, false);
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

  @Override
  public String toString() {
    return new YamlPrinter(this).getString();
  }
}
