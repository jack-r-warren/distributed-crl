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
