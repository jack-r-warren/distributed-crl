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

fun Dcrl.SignedMessage.verifySignature(): Boolean = let { message ->
  when (message.messageCase) {
    Dcrl.SignedMessage.MessageCase.CERTIFICATE_REVOCATION -> message.certificateRevocation
    Dcrl.SignedMessage.MessageCase.BLOCK_MESSAGE -> message.blockMessage
    Dcrl.SignedMessage.MessageCase.BLOCKCHAIN_RESPONSE -> message.blockchainResponse
    Dcrl.SignedMessage.MessageCase.BLOCK_RESPONSE -> message.blockResponse
    Dcrl.SignedMessage.MessageCase.ERROR_MESSAGE -> message.errorMessage
    Dcrl.SignedMessage.MessageCase.ANNOUNCE -> message.announce
    else -> null
  }?.toByteArray()?.verifySign(message.signature.toByteArray(), message.certificate.signingPublicKey.toByteArray())
    ?: false
}
