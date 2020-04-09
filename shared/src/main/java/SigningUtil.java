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

  public static boolean verify(Dcrl.SignedMessage signedMessage) {
    if (signedMessage == null) {
      return false;
    }

    // get inner message from signedMessage
    GeneratedMessageV3 message;
    switch (signedMessage.getMessageCase()) {
      case CERTIFICATE_REVOCATION:
        message = signedMessage.getCertificateRevocation();
        break;
      case BLOCK_MESSAGE:
        message = signedMessage.getBlockMessage();
        break;
      case BLOCKCHAIN_RESPONSE:
        message = signedMessage.getBlockchainResponse();
        break;
      case BLOCK_RESPONSE:
        message = signedMessage.getBlockResponse();
        break;
      case ERROR_MESSAGE:
        message = signedMessage.getErrorMessage();
        break;
      case ANNOUNCE:
        message = signedMessage.getAnnounce();
        break;
      default: // handles MESSAGE_NOT_SET case
        return false;
    }

    // get signature from signedMessage
    byte[] signature = signedMessage.getSignature().toByteArray();
    // error checking
    if (signature.length == 0) {
      return false;
    }

    // get public key from cert
    Dcrl.Certificate cert = signedMessage.getCertificate();
    // error checking
    if (cert.equals(Dcrl.Certificate.getDefaultInstance())) {
      return false;
    }
    byte[] public_key = cert.getSigningPublicKey().toByteArray();
    if (public_key.length == 0) {
      return false;
    }

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