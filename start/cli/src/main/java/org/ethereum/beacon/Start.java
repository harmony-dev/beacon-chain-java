package org.ethereum.beacon;

public class Start {
  public static void main(String[] args) {
    System.out.println("Starting Beacon chain java");
    for (int i = 0; i < args.length; ++i) {
      System.out.println("Arg #" + i + ": " + args[i]);
    }
  }
}
