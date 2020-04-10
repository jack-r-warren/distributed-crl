import com.google.protobuf.ByteString
import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.interfaces.Sign.BYTES
import com.sun.jna.Pointer
import java.time.Instant
import java.util.function.Consumer

fun ByteArray.sign(secretKey: ByteArray) = let { input ->
  (LazySodiumJava(SodiumJava()) as Sign.Native).run {
    Sign.StateCryptoSign().let { state ->
      cryptoSignInit(state)
      cryptoSignUpdate(state, input, input.size.toLong())
      ByteArray(BYTES).also {
        cryptoSignFinalCreate(state, it, Pointer.NULL, secretKey)
      }
    }
  }
}

fun ByteArray.verifySign(sign: ByteArray, publicKey: ByteArray) = let { input ->
  (LazySodiumJava(SodiumJava()) as Sign.Native).run {
    Sign.StateCryptoSign().let { state ->
      cryptoSignInit(state)
      cryptoSignUpdate(state, input, input.size.toLong())
      cryptoSignFinalVerify(state, sign, publicKey)
    }
  }
}

fun Dcrl.Certificate.verifyVerbose(
  errorPrinter: Consumer<String>,
  getFromTrustStore: (ByteString) -> Dcrl.Certificate?,
  isRevoked: (ByteString) -> Boolean,
  vararg usage: Dcrl.CertificateUsage
): Boolean = verifyVerbose(
  { errorPrinter.accept(it) }, getFromTrustStore, isRevoked, *usage
)

fun Dcrl.Certificate.verifyVerbose(
  errorPrinter: (String) -> Any?,
  getFromTrustStore: (ByteString) -> Dcrl.Certificate?,
  isRevoked: (ByteString) -> Boolean,
  vararg usage: Dcrl.CertificateUsage
): Boolean {
  when {
    Instant.now().epochSecond < validFrom -> errorPrinter("It is before the certificate was valid: ${Instant.now().epochSecond} < $validFrom")
    Instant.now().epochSecond > validFrom + validLength ->
      errorPrinter("It is after the certificate was valid: ${Instant.now().epochSecond} > $validFrom + $validLength")
    usage.any { it !in usagesList } -> errorPrinter("Required usages $usage not found in $usagesList")
    isRevoked(Util.hashCert(this)) -> errorPrinter("isRevoked said this cert was revoked")
    subject.isNullOrEmpty() -> errorPrinter("Subject was null")
    getFromTrustStore(issuerCertificateHash) != null -> if (Util.digestForSignature(this).verifySign(
        issuerSignature.toByteArray(),
        getFromTrustStore(issuerCertificateHash)!!.signingPublicKey.toByteArray()
      )
    ) return true else errorPrinter("Had issuer but verification of their signature failed")
    getFromTrustStore(Util.hashCert(this)) != null -> if (Util.digestForSignature(this).verifySign(
        issuerSignature.toByteArray(),
        signingPublicKey.toByteArray()
      )
    ) return true else errorPrinter("Cert was self signed but the verification of the signature failed")
    else -> errorPrinter(
      "The cert ${Util.hashCert(this)
        .joinToString()} wasn't trusted and neither was its issuer ${issuerCertificateHash.toByteArray()
        .joinToString()}"
    )
  }
  return false
}
