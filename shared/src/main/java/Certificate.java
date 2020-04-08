import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Certificate {

  /*
  Class for Certificates
   */

  public final String SUBJECT;
  public final long VALID_FROM;
  public final int VALID_LENGTH;
  public final CertificateUsage[] USAGES;
  public final byte[] SIGNING_PUBLIC_KEY;
  public final byte[] ISSUER_CERTIFICATE_HASH;
  public final byte[] ISSUER_SIGNATURE;

  /*
  TODO:
  - use arrays or Collection<>s?
  - validate Certificate in construction? (I'm guessing no)
  - add a 2nd constructor to set ISSUER_CERTIFICATE_HASH to null if the Certificate is self-signed?
   */
  public Certificate(String subject, long valid_from, int valid_length, CertificateUsage[] usages,
                     byte[] signing_public_key, byte[] issuer_certificate_hash, byte[] issuer_signature) {
    SUBJECT = subject;
    VALID_FROM = valid_from;
    VALID_LENGTH = valid_length;
    USAGES = usages;
    SIGNING_PUBLIC_KEY = signing_public_key;
    ISSUER_CERTIFICATE_HASH = issuer_certificate_hash;
    ISSUER_SIGNATURE = issuer_signature;
  }

  /*
  generates the Sha 256 hash of the Certificate
   */
  public byte[] hash() {
    // TODO
    return null;
  }
}
