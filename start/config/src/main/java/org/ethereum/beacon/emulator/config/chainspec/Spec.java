package org.ethereum.beacon.emulator.config.chainspec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.apache.milagro.amcl.BLS381.ECP;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.emulator.config.Config;
import org.ethereum.beacon.emulator.config.YamlPrinter;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Spec implements Config {
  private SpecConstantsData specConstants;
  private SpecHelpersOptions specHelpersOptions;

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

  @Override
  public String toString() {
    return new YamlPrinter(this).getString();
  }
}
