package org.ethereum.beacon.pow;

import java.util.Scanner;

public class ContractSource {

  private ContractSource() {}

  public static String getContractAbi() {
    // https://community.oracle.com/blogs/pat/2004/10/23/stupid-scanner-tricks
    return new Scanner(ContractSource.class.getResourceAsStream("ContractAbi.json"), "UTF-8")
        .useDelimiter("\\A")
        .next();
  }

  public static String getContractBin() {
    // one-liner file
    return new Scanner(ContractSource.class.getResourceAsStream("ContractBin.bin"), "UTF-8").next();
  }
}
