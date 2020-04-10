import com.google.protobuf.ByteString;
import org.jetbrains.annotations.NotNull;

public class ProtocolServerUtil {
  public static Dcrl.DCRLMessage buildErrorMessage(String errorString,
                                                   @NotNull Dcrl.Certificate certificate,
                                                   @NotNull byte[] private_key) {
    if (certificate.equals(Dcrl.Certificate.getDefaultInstance()) || private_key.length == 0) {
      return Dcrl.DCRLMessage.newBuilder()
          .setUnsignedMessage(
              Dcrl.UnsignedMessage.newBuilder()
                  .setErrorMessage(
                      Dcrl.ErrorMessage.newBuilder()
                          .setError(errorString)
                  )
          ).build();
    } else {
      Dcrl.ErrorMessage error = Dcrl.ErrorMessage.newBuilder().setError(errorString).build();
      return Dcrl.DCRLMessage.newBuilder()
          .setSignedMessage(
              Dcrl.SignedMessage.newBuilder()
                  .setCertificate(certificate)
                  .setSignature(ByteString.copyFrom(Util.sign(error, private_key)))
                  .setErrorMessage(error)
          ).build();
    }
  }

  public static Dcrl.DCRLMessage buildErrorMessage(String error) {
    return buildErrorMessage(error, Dcrl.Certificate.getDefaultInstance(), new byte[0]);
  }
}
