import com.google.protobuf.ByteString;
import org.jetbrains.annotations.NotNull;

public class ProtocolServerUtil {
  public static Dcrl.DCRLMessage buildErrorMessage(String errorString,
                                                   @NotNull Dcrl.Certificate certificate,
                                                   @NotNull byte[] private_key) {

    Dcrl.DCRLMessage message;
    if (certificate.equals(Dcrl.Certificate.getDefaultInstance()) || private_key.length == 0) {
      message = Dcrl.DCRLMessage.newBuilder()
          .setUnsignedMessage(
              Dcrl.UnsignedMessage.newBuilder()
                  .setErrorMessage(
                      Dcrl.ErrorMessage.newBuilder()
                          .setError(errorString)
                  )
          )
          .build();
      return message;
    } else {
      Dcrl.ErrorMessage error = Dcrl.ErrorMessage.newBuilder().setError(errorString).build();

      message = Dcrl.DCRLMessage.newBuilder()
          .setSignedMessage(
              Dcrl.SignedMessage.newBuilder()
                .setCertificate(certificate)
                .setSignature(ByteString.copyFrom(new byte[0])) // TODO TODO TODO need to change this so it actually signs the message!!!!
                .setErrorMessage(error)
          )
          .build();
    }
    return message;
  }

  public static Dcrl.DCRLMessage buildErrorMessage(String error) {
    return buildErrorMessage(error, Dcrl.Certificate.getDefaultInstance(), new byte[0]);
  }
}
