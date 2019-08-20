package org.ethereum.beacon.test.type.state.field;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.test.type.model.BlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface DepositsField extends DataMapperAccessor {
  default List<Deposit> getDeposits() {
    final Function<Integer, String> depositKey = (n) -> String.format("deposits_%d.yaml", n);
    final String metaKey = "meta.yaml";
    try {
      if (getFiles().containsKey(metaKey)) {
        List<BlockData.BlockBodyData.DepositData> deposits = new ArrayList<>();
        Integer depositsCount =
            getMapper().readValue(getFiles().get(metaKey), MetaClass.class).getDepositsCount();
        for (int i = 0; i < depositsCount; ++i) {
          deposits.add(
              getMapper()
                  .readValue(
                      getFiles().get(depositKey.apply(i)),
                      BlockData.BlockBodyData.DepositData.class));
        }
        return deposits.stream().map(DepositField::fromDepositData).collect(Collectors.toList());
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
