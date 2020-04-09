import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;

public class SigningUtil {

  /*
  Signs the message's toByteArray().
  If the message is a CertificateOrBuilder, then signs the byte digest
   */
  public static byte[] sign(GeneratedMessageV3 message,
                                    byte[] private_key) {
    if (message instanceof Dcrl.CertificateOrBuilder) {
      return signCert((Dcrl.CertificateOrBuilder) message, private_key);
    }

    return CryptoKt.sign(message.toByteArray(), private_key);
  }

  // TODO see my comments on Ivan's PR.
  public static boolean verify(GeneratedMessageV3 message,
                                       byte[] signature,
                                       byte[] public_key) {
    return CryptoKt.verifySign(message.toByteArray(), signature, public_key);
  }

  public static byte[] digestForSignature(Dcrl.CertificateOrBuilder cert) {

    ByteString digest = ByteString.EMPTY;
    digest = digest
        .concat(cert.getSubjectBytes())
        .concat(ByteString.copyFrom(Longs.toByteArray(cert.getValidFrom())))
        .concat(ByteString.copyFrom(Ints.toByteArray(cert.getValidLength())));
    for (Dcrl.CertificateUsage usage : cert.getUsagesList()) {
      digest = digest.concat(ByteString.copyFrom(Ints.toByteArray(usage.getNumber())));
    }
    digest = digest
        .concat(cert.getSigningPublicKey())
        .concat(cert.getIssuerCertificateHash());
    // issuer signature does not exist yet.

    return digest.toByteArray();
  }

  private static byte[] signCert(Dcrl.CertificateOrBuilder cert, byte[] private_key) {
    return CryptoKt.sign(digestForSignature(cert), private_key);
  }
}