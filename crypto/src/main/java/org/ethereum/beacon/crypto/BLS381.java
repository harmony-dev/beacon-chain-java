package org.ethereum.beacon.crypto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.ethereum.beacon.crypto.bls.milagro.MilagroCodecs.G1;
import static org.ethereum.beacon.crypto.bls.milagro.MilagroCodecs.G2;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.List;
import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ECP;
import org.apache.milagro.amcl.BLS381.ECP2;
import org.apache.milagro.amcl.BLS381.FP12;
import org.apache.milagro.amcl.BLS381.FP2;
import org.apache.milagro.amcl.BLS381.PAIR;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.ethereum.beacon.crypto.bls.bc.BCParameters;
import org.ethereum.beacon.crypto.bls.codec.Validator;
import org.ethereum.beacon.crypto.bls.milagro.BIGs;
import org.ethereum.beacon.crypto.bls.milagro.MilagroParameters;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.bytes.BytesValues;

public class BLS381 {

  private static final String ALGORITHM = "BLS";

  private static final String KEY_GENERATOR_ALGORITHM = "ECDSA";
  private static final String KEY_GENERATOR_PROVIDER = "BC";

  private static final BytesValue BYTES_ONE = BytesValues.ofUnsignedByte(1);
  private static final BytesValue BYTES_TWO = BytesValues.ofUnsignedByte(2);

  private static final KeyPairGenerator KEY_PAIR_GENERATOR;

  static {
    Security.addProvider(new BouncyCastleProvider());
    try {
      KEY_PAIR_GENERATOR =
          KeyPairGenerator.getInstance(KEY_GENERATOR_ALGORITHM, KEY_GENERATOR_PROVIDER);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      ECParameterSpec spec =
          new ECParameterSpec(
              BCParameters.G1.CURVE,
              BCParameters.G1.G,
              BCParameters.G1.CURVE.getOrder(),
              BCParameters.G1.CURVE.getCofactor());

      KEY_PAIR_GENERATOR.initialize(spec, new SecureRandom());
    } catch (InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }
  }

  public static Signature sign(MessageSpec spec, KeyPair keyPair) {
    ECP2 messagePoint = mapMessageSpecToG2(spec);
    ECP2 product = messagePoint.mul(keyPair.privateKey.asFieldElement());
    return Signature.create(product);
  }

  public static boolean verify(MessageSpec spec, Signature signature, PublicKey publicKey) {
    ECP2 messagePoint = mapMessageSpecToG2(spec);
    FP12 leftProduct = pairingProduct(publicKey.asEcPoint(), messagePoint);
    FP12 rightProduct = pairingProduct(ECP.generator(), signature.asEcPoint());

    return leftProduct.equals(rightProduct);
  }

  private static FP12 pairingProduct(ECP g1, ECP2 g2) {
    FP12 ateProduct = PAIR.ate(g2, g1);
    return PAIR.fexp(ateProduct);
  }

  private static ECP2 mapMessageSpecToG2(MessageSpec spec) {
    BytesValue reBytes = spec.getDomain().concat(BYTES_ONE).concat(spec.getHash());
    BytesValue imBytes = spec.getDomain().concat(BYTES_TWO).concat(spec.getHash());

    BIG reX = BIGs.fromBytes(Hashes.keccack384(reBytes));
    BIG imX = BIGs.fromBytes(Hashes.keccack384(imBytes));

    FP2 x = new FP2(reX, imX);
    ECP2 point = new ECP2(x);
    while (point.is_infinity()) {
      x.add(MilagroParameters.FP2.ONE);
      point = new ECP2(x);
    }

    return point.mul(MilagroParameters.G2.COFACTOR);
  }

  public static class Signature {

    private final BytesValue encoded;

    private Signature(BytesValue encoded) {
      Validator.Result result = Validator.G2.validate(encoded);
      checkArgument(result.isValid(), "Signature is invalid %s", result.getMessage());
      this.encoded = encoded;
    }

    public static Signature create(ECP2 ecPoint) {
      return new Signature(G2.encode(ecPoint));
    }

    public static Signature aggregate(List<Signature> signatures) {
      ECP2 product = new ECP2();
      signatures.forEach(signature -> product.add(signature.asEcPoint()));
      return create(product);
    }

    public BytesValue getEncoded() {
      return encoded;
    }

    ECP2 asEcPoint() {
      return G2.decode(encoded);
    }
  }

  public static class PrivateKey implements java.security.PrivateKey {

    private final BytesValue encoded;

    private PrivateKey(BytesValue encoded) {
      this.encoded = encoded;
    }

    public static PrivateKey create(BigInteger value) {
      return new PrivateKey(BytesValue.wrap(value.toByteArray()));
    }

    @Override
    public String getAlgorithm() {
      return ALGORITHM;
    }

    @Override
    public String getFormat() {
      return null;
    }

    @Override
    public byte[] getEncoded() {
      return encoded.getArrayUnsafe();
    }

    public BytesValue getEncodedBytes() {
      return encoded;
    }

    BIG asFieldElement() {
      return BIGs.fromBytes(encoded);
    }
  }

  public static class PublicKey implements java.security.PublicKey {

    private final BytesValue encoded;

    private PublicKey(BytesValue encoded) {
      Validator.Result result = Validator.G1.validate(encoded);
      checkArgument(result.isValid(), "Public key is invalid %s", result.getMessage());
      this.encoded = encoded;
    }

    public static PublicKey create(PrivateKey privateKey) {
      ECP product = ECP.generator().mul(privateKey.asFieldElement());
      return create(product);
    }

    public static PublicKey create(ECP ecPoint) {
      return new PublicKey(G1.encode(ecPoint));
    }

    public static PublicKey create(ECPoint ecPoint) {
      BIG x = BIGs.fromBigInteger(ecPoint.getAffineXCoord().toBigInteger());
      BIG y = BIGs.fromBigInteger(ecPoint.getAffineYCoord().toBigInteger());

      return new PublicKey(G1.encode(new ECP(x, y)));
    }

    public static PublicKey create(BytesValue encoded) {
      return new PublicKey(encoded);
    }

    public static PublicKey aggregate(List<PublicKey> publicKeys) {
      ECP product = new ECP();
      publicKeys.forEach(publicKey -> product.add(publicKey.asEcPoint()));
      return create(product);
    }

    @Override
    public String getAlgorithm() {
      return ALGORITHM;
    }

    @Override
    public String getFormat() {
      return null;
    }

    @Override
    public byte[] getEncoded() {
      return encoded.getArrayUnsafe();
    }

    public BytesValue getEncodedBytes() {
      return encoded;
    }

    ECP asEcPoint() {
      return G1.decode(encoded);
    }
  }

  public static class KeyPair {

    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * Constructs a key pair from the given public key and private key.
     *
     * <p>Note that this constructor only stores references to the public and private key components
     * in the generated key pair. This is safe, because {@code Key} objects are immutable.
     *
     * @param publicKey the public key.
     * @param privateKey the private key.
     */
    public KeyPair(PublicKey publicKey, PrivateKey privateKey) {
      this.publicKey = publicKey;
      this.privateKey = privateKey;
    }

    public static KeyPair create(PrivateKey privateKey) {
      return new KeyPair(PublicKey.create(privateKey), privateKey);
    }

    public static KeyPair generate() {
      java.security.KeyPair keyPairRaw = KEY_PAIR_GENERATOR.generateKeyPair();
      BCECPrivateKey privateKeyRaw = (BCECPrivateKey) keyPairRaw.getPrivate();
      BCECPublicKey publicKeyRaw = (BCECPublicKey) keyPairRaw.getPublic();

      PrivateKey privateKey = PrivateKey.create(privateKeyRaw.getD());
      PublicKey publicKey = PublicKey.create(publicKeyRaw.getQ());

      return new KeyPair(publicKey, privateKey);
    }

    /**
     * Returns a reference to the public key component of this key pair.
     *
     * @return a reference to the public key.
     */
    public PublicKey getPublic() {
      return publicKey;
    }

    /**
     * Returns a reference to the private key component of this key pair.
     *
     * @return a reference to the private key.
     */
    public PrivateKey getPrivate() {
      return privateKey;
    }
  }
}
