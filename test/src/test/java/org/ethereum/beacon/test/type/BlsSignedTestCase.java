package org.ethereum.beacon.test.type;

/** For tests containing `bls_setting` flag. See description below */
public interface BlsSignedTestCase extends TestCase {

  /**
   * `bls_setting`: int -- optional, can have 3 different values:
   *
   * <p>0: (default, applies if key-value pair is absent). Free to choose either BLS ON or OFF.
   * Tests are generated with valid BLS data in this case, but there is no change of outcome when
   * running the test if BLS is ON or OFF.
   *
   * <p>1: known as "BLS required" - if the test validity is strictly dependent on BLS being ON
   *
   * <p>2: known as "BLS ignored" - if the test validity is strictly dependent on BLS being OFF
   *
   * @return `bls_setting` or null if not set
   */
  Integer getBlsSetting();
}
