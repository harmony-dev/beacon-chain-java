package org.ethereum.beacon.core;

import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.CustodyChallenge;
import org.ethereum.beacon.core.operations.CustodyReseed;
import org.ethereum.beacon.core.operations.CustodyResponse;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.core.operations.slashing.SlashableVoteData;
import org.ethereum.beacon.core.state.BeaconStateImpl;
import org.ethereum.beacon.core.state.CrosslinkRecord;
import org.ethereum.beacon.core.state.DepositRootVote;
import org.ethereum.beacon.core.state.ForkData;
import org.ethereum.beacon.core.state.PendingAttestationRecord;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.junit.Test;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Verifies {@link SSZSerializable} model test coverage
 * Check {@link #testAnnotatedClassesHaveTests()} JavaDoc for more info
 */
public class SSZSerializableAnnotationTest {

  /**
   * Just add your {@link SSZSerializable}
   * class to the list to stop this test fail.
   *
   * This test notifies user that every {@link SSZSerializable} model should be tested
   * like in {@link ModelsSerializeTest} to clarify in runtime correctness
   * of annotation scheme building for each case and test other routines
   */
  @Test
  public void testAnnotatedClassesHaveTests() throws Exception {
    Set<Class> testedClasses = new HashSet<>(Arrays.asList(
        Attestation.class,
        AttestationData.class,
        BeaconBlock.class,
        BeaconBlockBody.class,
        BeaconStateImpl.class,
        CasperSlashing.class,
        Deposit.class,
        DepositData.class,
        DepositInput.class,
        Exit.class,
        CustodyReseed.class,
        CustodyResponse.class,
        CustodyChallenge.class,
        ProposalSignedData.class,
        ProposerSlashing.class,
        SlashableVoteData.class,
        CrosslinkRecord.class,
        DepositRootVote.class,
        ForkData.class,
        PendingAttestationRecord.class,
        ValidatorRecord.class
    ));
    Class[] allClasses = getClasses("org.ethereum.beacon.core");

    for (Class clazz: allClasses) {
      if (testedClasses.contains(clazz)) continue;

      if (clazz.isAnnotationPresent(SSZSerializable.class)) {
        throw new RuntimeException(String.format("Class %s is marked with " +
            "@SSZSerializable annotation but not covered with tests!", clazz.getName()));
      }
    }
  }

  /**
   * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
   *
   * @param packageName The base package
   * @return The classes
   * @throws ClassNotFoundException
   * @throws IOException
   */
  private static Class[] getClasses(String packageName)
      throws ClassNotFoundException, IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = classLoader.getResources(path);
    List<File> dirs = new ArrayList<File>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      dirs.add(new File(resource.getFile()));
    }
    ArrayList<Class> classes = new ArrayList<Class>();
    for (File directory : dirs) {
      classes.addAll(findClasses(directory, packageName));
    }
    return classes.toArray(new Class[classes.size()]);
  }

  /**
   * Recursive method used to find all classes in a given directory and subdirs.
   *
   * @param directory   The base directory
   * @param packageName The package name for classes found inside the base directory
   * @return The classes
   * @throws ClassNotFoundException
   */
  private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
    List<Class> classes = new ArrayList<Class>();
    if (!directory.exists()) {
      return classes;
    }
    File[] files = directory.listFiles();
    for (File file : files) {
      if (file.isDirectory()) {
        assert !file.getName().contains(".");
        classes.addAll(findClasses(file, packageName + "." + file.getName()));
      } else if (file.getName().endsWith(".class")) {
        classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
      }
    }
    return classes;
  }
}
