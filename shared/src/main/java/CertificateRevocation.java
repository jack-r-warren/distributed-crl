public class CertificateRevocation {
  public final Certificate CERTIFICATE;

  public CertificateRevocation(Certificate certificate) {
    CERTIFICATE = certificate;
  }

  public byte[] hash() {
    // TODO
    return null;
  }
}
