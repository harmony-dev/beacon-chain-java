package org.ethereum.beacon.pow;

import java.util.Scanner;

public class ContractAbi {

  private ContractAbi() {}

  public static String getContractAbi() {
    // https://community.oracle.com/blogs/pat/2004/10/23/stupid-scanner-tricks
    return new Scanner(
                ContractAbi.class.getResourceAsStream("ContractAbi.json"), "UTF-8")
            .useDelimiter("\\A")
            .next();
  }
}
