package org.ethereum.beacon.test.type.state.field;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.test.type.model.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.ethereum.beacon.test.type.state.field.DepositField.parseDepositData;

public interface DepositsField extends DataMapperAccessor {
  default List<Deposit> getDeposits() {
    final Function<Integer, String> depositKey =
        useSszWhenPossible()
            ? (n) -> String.format("deposits_%d.ssz", n)
            : (n) -> String.format("deposits_%d.yaml", n);
    final String metaKey = "meta.yaml";
    try {
      if (getFiles().containsKey(metaKey)) {
        List<Deposit> deposits = new ArrayList<>();
        Integer depositsCount =
            getMapper()
                .readValue(getFiles().get(metaKey).extractArray(), MetaClass.class)
                .getDepositsCount();
        for (int i = 0; i < depositsCount; ++i) {
          // SSZ
          if (useSszWhenPossible()) {
            deposits.add(
                getSszSerializer().decode(getFiles().get(depositKey.apply(i)), Deposit.class));
          } else { // YAML
            BlockData.BlockBodyData.DepositData depositData =
                getMapper()
                    .readValue(
                        getFiles().get(depositKey.apply(i)).extractArray(),
                        BlockData.BlockBodyData.DepositData.class);
            deposits.add(parseDepositData(depositData));
          }
        }
        return deposits;
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    throw new RuntimeException("`deposits` not defined");
  }

  class MetaClass {
    @JsonProperty("deposits_count")
    private Integer depositsCount;

    public Integer getDepositsCount() {
      return depositsCount;
    }

    public void setDepositsCount(Integer depositsCount) {
      this.depositsCount = depositsCount;
    }
  }
}
