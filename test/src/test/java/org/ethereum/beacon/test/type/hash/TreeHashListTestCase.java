package org.ethereum.beacon.test.type.hash;

import org.ethereum.beacon.test.type.TestCase;

import java.util.List;

/** Tree hash list/vector test case */
public class TreeHashListTestCase implements TestCase {
  private List<String> type;
  private String root;
  private List<String> tags;
  private List<String> value;

  public String getRoot() {
    return root;
  }

  public void setRoot(String root) {
    this.root = root;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public List<String> getType() {
    return type;
  }

  public void setType(List<String> type) {
    this.type = type;
  }

  public List<String> getValue() {
    return value;
  }

  public void setValue(List<String> value) {
    this.value = value;
  }
}
