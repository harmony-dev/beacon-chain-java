package org.ethereum.beacon.emulator.config.main;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ValidatorKeys.Generate.class, name = "generate"),
    @JsonSubTypes.Type(value = ValidatorKeys.Private.class, name = "private"),
    @JsonSubTypes.Type(value = ValidatorKeys.Public.class, name = "public"),
    @JsonSubTypes.Type(value = ValidatorKeys.InteropKeys.class, name = "interop"),
})
public abstract class ValidatorKeys {

  public static class Generate extends ValidatorKeys {
    private int count;
    private int seed = 0;
    private int startIndex = 0;

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public int getSeed() {
      return seed;
    }

    public void setSeed(int seed) {
      this.seed = seed;
    }

    public int getStartIndex() {
      return startIndex;
    }

    public void setStartIndex(int startIndex) {
      this.startIndex = startIndex;
    }
  }

  public static class InteropKeys extends ValidatorKeys {
    private int count;

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }
  }

  public static class ExplicitKeys extends ValidatorKeys {
    private List<String> keys;

    public List<String> getKeys() {
      return keys;
    }

    public void setKeys(List<String> keys) {
      this.keys = keys;
    }
  }

  public static class Private extends ExplicitKeys {

    public Private() {
    }

    public Private(List<String> keys) {
      setKeys(keys);
    }
  }

  public static class Public extends ExplicitKeys {}
}
