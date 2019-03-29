package org.ethereum.beacon.consensus;

public enum TransitionType {
  INITIAL,
  CACHING,
  EPOCH,
  SLOT,
  BLOCK,
  UNKNOWN;

  public boolean canBeAppliedAfter(TransitionType previousTransition) {
    switch (previousTransition) {
      case UNKNOWN: return true;
      case INITIAL:
        switch (this) {
          case INITIAL: return false;
          case CACHING: return true;
          case EPOCH: return false;
          case SLOT: return false;
          case BLOCK: return false;
        }
      case CACHING:
        switch (this) {
          case INITIAL: return false;
          case CACHING: return false;
          case EPOCH: return true;
          case SLOT: return true;
          case BLOCK: return false;
        }
      case EPOCH:
        switch (this) {
          case INITIAL: return false;
          case CACHING: return false;
          case EPOCH: return false;
          case SLOT: return true;
          case BLOCK: return false;
        }
      case SLOT:
        switch (this) {
          case INITIAL: return false;
          case CACHING: return true;
          case EPOCH: return false;
          case SLOT: return false;
          case BLOCK: return true;
        }
      case BLOCK:
        switch (this) {
          case INITIAL: return false;
          case CACHING: return true;
          case EPOCH: return false;
          case SLOT: return false;
          case BLOCK: return false;
        }
    }
    throw new RuntimeException("Impossible");
  }

  public void checkCanBeAppliedAfter(TransitionType previousTransition) throws RuntimeException {
    if (!canBeAppliedAfter(previousTransition)) {
      throw new RuntimeException(
          this + " transition can't be applied after " + previousTransition + " transition");
    }
  }
}
