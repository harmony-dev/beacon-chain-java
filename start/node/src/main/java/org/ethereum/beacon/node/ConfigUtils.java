package org.ethereum.beacon.node;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.emulator.config.main.Signer;
import org.ethereum.beacon.emulator.config.main.Signer.Insecure;
import org.ethereum.beacon.emulator.config.main.ValidatorKeys;
import org.ethereum.beacon.emulator.config.main.ValidatorKeys.Generate;
import org.ethereum.beacon.emulator.config.main.ValidatorKeys.InteropKeys;
import org.ethereum.beacon.emulator.config.main.ValidatorKeys.Private;
import org.ethereum.beacon.emulator.config.main.ValidatorKeys.Public;
import org.ethereum.beacon.emulator.config.main.conract.Contract;
import org.ethereum.beacon.emulator.config.main.conract.EmulatorContract;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.start.common.util.SimpleDepositContract;
import org.ethereum.beacon.start.common.util.SimulateUtils;
import org.ethereum.beacon.start.common.util.SimulationKeyPairGenerator;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

public class ConfigUtils {

  public static List<BLS381Credentials> createCredentials(Signer config, boolean isBlsSign) {
    if (config == null) {
      return null;
    }
    if (config instanceof Signer.Insecure) {
      return createKeyPairs(((Insecure) config).getKeys()).stream()
          .map(
              key ->
                  isBlsSign
                      ? BLS381Credentials.createWithInsecureSigner(key)
                      : BLS381Credentials.createWithDummySigner(key))
          .collect(Collectors.toList());
    } else {
      throw new IllegalArgumentException(
          "This config class is not yet supported: " + config.getClass());
    }
  }

  public static List<KeyPair> createKeyPairs(List<ValidatorKeys> keys) {
    return keys.stream().flatMap(k -> createKeyPairs(k).stream()).collect(Collectors.toList());
  }

  public static List<KeyPair> createKeyPairs(ValidatorKeys keys) {
    if (keys instanceof Public) {
      throw new IllegalArgumentException("Can't generate key pairs from public keys: " + keys);
    } else if (keys instanceof Private) {
      return ((Private) keys)
          .getKeys().stream()
              .map(Bytes32::fromHexString)
              .map(PrivateKey::create)
              .map(KeyPair::create)
              .collect(Collectors.toList());
    } else if (keys instanceof Generate) {
      Generate genKeys = (Generate) keys;
      return SimulationKeyPairGenerator.generateRandomKeys(
          genKeys.getSeed(), genKeys.getStartIndex(), genKeys.getCount());
    } else if (keys instanceof InteropKeys) {
      InteropKeys interopKeys = (InteropKeys) keys;
      return SimulationKeyPairGenerator.generateInteropKeys(
          interopKeys.getStartIndex(), interopKeys.getCount());
    } else {
      throw new IllegalArgumentException("Unknown ValidatorKeys subclass: " + keys.getClass());
    }
  }

  public static ChainStart createChainStart(
      Contract config, BeaconChainSpec spec, boolean verifyProof) {
    if (config instanceof EmulatorContract) {
      EmulatorContract eConfig = (EmulatorContract) config;
      List<KeyPair> keyPairs = createKeyPairs(eConfig.getKeys());
      Random random = new Random(1);
      Gwei amount =
          eConfig.getBalance() != null
              ? Gwei.ofEthers(eConfig.getBalance())
              : spec.getConstants().getMaxEffectiveBalance();

      List<Deposit> deposits;
      if (eConfig.isInteropCredentials()) {
        // depostis with mocked start credentials for interop
        List<Hash32> withdrawalCredentials =
            SimulateUtils.generateInteropCredentials(
                spec.getConstants().getBlsWithdrawalPrefix(), keyPairs);
        // keep random proofs for now
        List<List<Hash32>> depositProofs =
            SimulateUtils.generateRandomDepositProofs(random, keyPairs);
        deposits =
            SimulateUtils.getDepositsForKeyPairs(
                keyPairs, withdrawalCredentials, amount, depositProofs, spec, verifyProof);
      } else {
        deposits =
            SimulateUtils.getDepositsForKeyPairs(
                UInt64.ZERO, random, keyPairs, spec, amount, verifyProof);
      }

      ReadList<Integer, DepositData> depositDataList =
          ReadList.wrap(
              deposits.stream().map(Deposit::getData).collect(Collectors.toList()),
              Integer::new,
              1L << spec.getConstants().getDepositContractTreeDepth().getIntValue());
      Hash32 blockHash =
          eConfig.getEth1BlockHash() == null || eConfig.getEth1BlockHash().length() == 0
              ? Hash32.random(random)
              : Hash32.wrap(Bytes32.fromHexString(eConfig.getEth1BlockHash()));
      Eth1Data eth1Data =
          new Eth1Data(
              spec.hash_tree_root(depositDataList), UInt64.valueOf(deposits.size()), blockHash);
      return new ChainStart(Time.of(eConfig.getGenesisTime().getTime() / 1000), eth1Data, deposits);
    } else {
      throw new IllegalArgumentException(
          "This config class is not yet supported: " + config.getClass());
    }
  }
}
