package org.ethereum.beacon.consensus;

public enum TransitionType {
  INITIAL,
  SLOT,
  BLOCK,
  EPOCH,
  UNKNOWN;

  public boolean canBeAppliedAfter(TransitionType beforeTransition) {
    switch (beforeTransition) {
      case UNKNOWN: return true;
      case INITIAL:
      case EPOCH:
        switch (this) {
          case INITIAL: return false;
          case SLOT: return true;
          case BLOCK: return false;
          case EPOCH: return false;
        }
      case SLOT:
        switch (this) {
          case INITIAL: return false;
          case SLOT: return true;
          case BLOCK: return true;
          case EPOCH: return true;
        }
      case BLOCK:
        switch (this) {
          case INITIAL: return false;
          case SLOT: return true;
          case BLOCK: return false;
          case EPOCH: return true;
        }
    }
    throw new RuntimeException("Impossible");
  }

  public void checkCanBeAppliedAfter(TransitionType beforeTransition) throws RuntimeException {
    if (!canBeAppliedAfter(beforeTransition)) {
      throw new RuntimeException(
          this + " transition can't be applied after " + beforeTransition + " transition");
    }
  }
}
