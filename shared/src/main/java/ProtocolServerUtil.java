import javax.annotation.Nullable;

public class ProtocolServerUtil {
  public static Dcrl.DCRLMessage buildErrorMessage(String error, @Nullable Dcrl.Certificate certificate) {
    if (certificate == null) {
      Dcrl.DCRLMessage message = Dcrl.DCRLMessage.newBuilder()
          .setUnsignedMessage(
              Dcrl.UnsignedMessage.newBuilder()
                  .setErrorMessage(
                      Dcrl.ErrorMessage.newBuilder()
                          .setError(error)
                  )
          )
          .build();
      return message;
    } else {
      // TODO create and sign the error message.
      return null;
    }
  }

  public static Dcrl.DCRLMessage buildErrorMessage(String error) {
    return buildErrorMessage(error, null);
  }
}
