import com.google.protobuf.GeneratedMessageV3;

public class SigningUtil {

  public static byte[] cert_signing(GeneratedMessageV3 message,
                                    byte[] private_key) {
    return CryptoKt.sign(message.toByteArray(), private_key);
  }

  public static boolean cert_validation(GeneratedMessageV3 message,
                                       byte[] sign,
                                       byte[] public_key) {
    return CryptoKt.verifySign(message.toByteArray(), sign, public_key);
  }

}
