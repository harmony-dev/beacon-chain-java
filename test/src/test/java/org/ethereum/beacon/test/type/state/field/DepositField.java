package org.ethereum.beacon.test.type.state.field;

import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.test.type.DataMapperAccessor;
import org.ethereum.beacon.test.type.model.BlockData;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.stream.Collectors;

public interface DepositField extends DataMapperAccessor {
  static Deposit parseDepositData(BlockData.BlockBodyData.DepositData depositData) {
    Deposit deposit =
        Deposit.create(
            depositData.getProof().stream().map(Hash32::fromHexString).collect(Collectors.toList()),
            new DepositData(
                BLSPubkey.fromHexString(depositData.getData().getPubkey()),
                Hash32.fromHexString(depositData.getData().getWithdrawalCredentials()),
                Gwei.castFrom(UInt64.valueOf(depositData.getData().getAmount())),
                BLSSignature.wrap(Bytes96.fromHexString(depositData.getData().getSignature()))));

    return deposit;
  }

  default Deposit getDeposit() {
    final String key = useSszWhenPossible() ? "deposit.ssz" : "deposit.yaml";
    if (!getFiles().containsKey(key)) {
      throw new RuntimeException("`deposit` not defined");
    }

    // SSZ
    if (useSszWhenPossible()) {
      return getSszSerializer().decode(getFiles().get(key), Deposit.class);
    }

    // YAML
    try {
      BlockData.BlockBodyData.DepositData depositData =
          getMapper().readValue(getFiles().get(key).extractArray(), BlockData.BlockBodyData.DepositData.class);
      return parseDepositData(depositData);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
