package org.ethereum.beacon.pow;

import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.util.ConsumerList;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.ReadVector;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Progressive Merkle Adoption of <a
 * href="https://github.com/ethereum/research/blob/master/beacon_chain_impl/progressive_merkle_tree.py">https://github.com/ethereum/research/blob/master/beacon_chain_impl/progressive_merkle_tree.py</a>
 */
public class DepositIncrementalMerkle extends DepositDataMerkle {

  private final int treeDepth;
  private final List<Hash32> lastElements;
  private int branchDepositCount = 0;
  private List<BytesValue> branch;

  /**
   * Incremental Merkle using
   *
   * @param hashFunction hash function
   * @param treeDepth tree with depth of
   * @param bufferDeposits number of not committed deposits, we could easily roll to any state
   *     backwards if it doesn't involve skipping more than this number of deposits
   */
  public DepositIncrementalMerkle(
      Function<BytesValue, Hash32> hashFunction, int treeDepth, int bufferDeposits) {
    super(hashFunction, treeDepth);
    this.treeDepth = treeDepth;
    Consumer<Hash32> consumer =
        depositData -> {
          branchDepositCount++;
          add_value(branch, branchDepositCount - 1, depositData.slice(0));
        };
    this.lastElements = ConsumerList.create(bufferDeposits, consumer);
    this.branch =
        IntStream.range(0, treeDepth)
            .mapToObj(this::getZeroHash)
            .map(h -> h.slice(0))
            .collect(Collectors.toList());
  }

  // # Add a value to a Merkle tree by using the algo
  // # that stores a branch of sub-roots
  // def add_value(branch, index, value):
  //    i = 0
  //    while (index+1) % 2**(i+1) == 0:
  //        i += 1
  //    for j in range(0, i):
  //        value = hash(branch[j] + value)
  //        # branch[j] = zerohashes[j]
  //    branch[i] = value
  void add_value(List<BytesValue> branch, int index, BytesValue value) {
    int i = 0;
    while ((index + 1) % (1 << (i + 1)) == 0) {
      ++i;
    }

    BytesValue valueCopy = value;
    for (int j = 0; j < i; ++j) {
      valueCopy = getHashFunction().apply(branch.get(j).concat(valueCopy));
    }
    branch.set(i, valueCopy);
  }

  // def get_root_from_branch(branch, size):
  //    r = b'\x00' * 32
  //    for h in range(32):
  //        if (size >> h) % 2 == 1:
  //            r = hash(branch[h] + r)
  //        else:
  //            r = hash(r + zerohashes[h])
  // return r
  BytesValue get_root_from_branch(List<BytesValue> branch, int size) {
    BytesValue r = Bytes32.ZERO.slice(0);
    for (int h = 0; h < treeDepth; ++h) {
      if ((size >> h) % 2 == 1) {
        r = getHashFunction().apply(branch.get(h).concat(r));
      } else {
        r = getHashFunction().apply(r.concat(getZeroHash(h)));
      }
    }

    return r;
  }

  @Override
  public ReadVector<Integer, Hash32> getProof(int index, int size) {
    verifyIndexNotTooBig(index);
    verifyIndexNotTooOld(index);
    if (size > (getLastIndex() + 1)) {
      throw new RuntimeException(
          String.format("Max size is %s, asked for size %s!", getLastIndex() + 1, size));
    }
    List<BytesValue> branchCopy = new ArrayList<>(branch);
    for (int i = branchDepositCount; i < index + 1; ++i) {
      add_value(branchCopy, i, lastElements.get(i - branchDepositCount));
    }

    return ReadVector.wrap(
        branchCopy.stream().map(e -> Hash32.wrap(Bytes32.leftPad(e))).collect(Collectors.toList()),
        Integer::new);
  }

  @Override
  public Hash32 getDepositRoot(UInt64 index) {
    verifyIndexNotTooBig(index.intValue());
    verifyIndexNotTooOld(index.intValue());

    List<BytesValue> branchCopy = new ArrayList<>(branch);
    // index of 1st == 0
    for (int i = branchDepositCount; i < index.intValue() + 1; ++i) {
      add_value(branchCopy, i, lastElements.get(i - branchDepositCount));
    }
    BytesValue root = get_root_from_branch(branchCopy, index.intValue() + 1);
    return Hash32.wrap(Bytes32.leftPad(root));
  }

  private void verifyIndexNotTooOld(int index) {
    if (index < branchDepositCount) {
      throw new RuntimeException(
          String.format("Too old element index queried, %s, minimum: %s!", index, branchDepositCount));
    }
  }

  private void verifyIndexNotTooBig(int index) {
    if (index > getLastIndex()) {
      throw new RuntimeException(
          String.format("Max element index is %s, asked for %s!", getLastIndex(), index));
    }
  }

  @Override
  public void insertValue(DepositData value) {
    lastElements.add(createDepositDataValue(value, getHashFunction()));
  }

  @Override
  public int getLastIndex() {
    return branchDepositCount + lastElements.size() - 1;
  }

  public Stream<Hash32> getLastElementsStream() {
    return lastElements.stream();
  }

  public int getBranchDepositCount() {
    return branchDepositCount;
  }

  public Stream<BytesValue> getBranchStream() {
    return branch.stream();
  }
}
