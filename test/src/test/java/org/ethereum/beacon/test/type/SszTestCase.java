package org.ethereum.beacon.test.type;

import java.util.List;

/**
 * SSZ test case <a
 * href="https://github.com/ethereum/eth2.0-tests/tree/master/ssz">https://github.com/ethereum/eth2.0-tests/tree/master/ssz</a>
 */
public class SszTestCase implements TestCase {
  private String type;
  private String valid;
  private String ssz;
  private List<String> tags;
  private String value;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getValid() {
    return valid;
  }

  public void setValid(String valid) {
    this.valid = valid;
  }

  public String getSsz() {
    return ssz;
  }

  public void setSsz(String ssz) {
    this.ssz = ssz;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
