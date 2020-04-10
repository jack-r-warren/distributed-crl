import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.interfaces.Sign.BYTES
import com.sun.jna.Pointer
import java.time.Instant

fun ByteArray.sign(secretKey: ByteArray) = let { input ->
  println("Started Signing")
  (LazySodiumJava(SodiumJava()) as Sign.Native).run {
    Sign.StateCryptoSign().let { state ->
      cryptoSignInit(state)
      cryptoSignUpdate(state, input, input.size.toLong())
      ByteArray(BYTES).also {
        cryptoSignFinalCreate(state, it, Pointer.NULL, secretKey)
      }
    }
  }
}.also { println("Done signing") }

fun ByteArray.verifySign(sign: ByteArray, publicKey: ByteArray) = let { input ->
  (LazySodiumJava(SodiumJava()) as Sign.Native).run {
    Sign.StateCryptoSign().let { state ->
      cryptoSignInit(state)
      cryptoSignUpdate(state, input, input.size.toLong())
      cryptoSignFinalVerify(state, sign, publicKey)
    }
  }
}

// We pass this a lambda. In Kotlin, call this like
// `myCertificate.verifySignature { issuerHash -> TODO(return the issuer or null if the hash doesn't match something) }`
fun Dcrl.Certificate.verify(
  getFromTrustStore: (ByteArray) -> Dcrl.Certificate?,
  isRevoked: (ByteArray) -> Boolean,
  vararg usage: Dcrl.CertificateUsage
): Boolean = Instant.now().epochSecond.let { validFrom < it && validFrom + validLength > it } &&
    usage.all { it in usagesList } &&
    !isRevoked(Util.hashCert(this)) &&
    !subject.isNullOrEmpty() && let { certificate ->
  certificate.issuerCertificateHash.let {
    if (it == null || it.isEmpty) getFromTrustStore(it.toByteArray())
    else getFromTrustStore(Util.hash(certificate))
  }?.let { issuer ->
    Util.digestForSignature(certificate)
      .verifySign(certificate.issuerSignature.toByteArray(), issuer.signingPublicKey.toByteArray())
  } ?: false
}
