import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import com.goterl.lazycode.lazysodium.interfaces.Sign
import com.goterl.lazycode.lazysodium.interfaces.Sign.BYTES
import com.sun.jna.Pointer

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

// We pass this a lambda. In Kotlin, call this like
// `myCertificate.verifySignature { issuerHash -> TODO(return the issuer or null if the hash doesn't match something) }`
fun Dcrl.Certificate.verifySignature(getIssuer: (ByteArray) -> Dcrl.Certificate?): Boolean = let { certificate ->
  certificate.issuerCertificateHash.let { if (it == null || it.isEmpty) certificate else getIssuer(it.toByteArray()) }
    ?.let { issuer ->
      Util.digestForSignature(certificate)
        .verifySign(certificate.issuerSignature.toByteArray(), issuer.signingPublicKey.toByteArray())
    } ?: false
}
