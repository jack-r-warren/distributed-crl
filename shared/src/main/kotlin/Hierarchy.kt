import com.google.protobuf.Message

interface Authority : Participant


interface Participant : Observer


interface Observer : Runnable {
  val servers: Collection<NetworkLocation>
}

abstract class MessageHandler : (Message) -> Unit {
  private fun failOnUnimplemented(message: Message): Nothing = TODO("Handling not implemented for $message")

  private fun failOnUnset(message: Message): Nothing = throw IllegalStateException("Enum unset in $message")

  override fun invoke(message: Message): Unit = when (message) {
    is Dcrl.DCRLMessage -> handle(message)
    else -> failOnUnimplemented(message)
  }

  fun handle(message: Dcrl.DCRLMessage): Unit = when (message.messageCase) {
    Dcrl.DCRLMessage.MessageCase.UNSIGNED_MESSAGE -> TODO()
    Dcrl.DCRLMessage.MessageCase.SIGNED_MESSAGE -> TODO()
    Dcrl.DCRLMessage.MessageCase.MESSAGE_NOT_SET -> failOnUnset(message)
    else -> failOnUnimplemented(message)
  }

  fun handle(message: Dcrl.UnsignedMessage): Unit = when (message.messageCase) {
    Dcrl.UnsignedMessage.MessageCase.BLOCKCHAIN_REQUEST -> handle(message.blockchainRequest)
    Dcrl.UnsignedMessage.MessageCase.BLOCK_REQUEST -> handle(message.blockRequest)
    Dcrl.UnsignedMessage.MessageCase.ERROR_MESSAGE -> handle(message.errorMessage)
    Dcrl.UnsignedMessage.MessageCase.MESSAGE_NOT_SET -> failOnUnset(message)
    else -> failOnUnimplemented(message)
  }

  abstract fun handle(message: Dcrl.BlockchainRequest)
  abstract fun handle(message: Dcrl.BlockRequest)
  abstract fun handle(message: Dcrl.ErrorMessage, from: Dcrl.Certificate? = null)

  fun handle(message: Dcrl.SignedMessage): Unit {
    TODO("implement hash checking here")
    when (message.messageCase) {
      Dcrl.SignedMessage.MessageCase.CERTIFICATE_REVOCATION -> handle(message.certificateRevocation, message.certificate)
      Dcrl.SignedMessage.MessageCase.BLOCK_MESSAGE -> handle(message.blockMessage, message.certificate)
      Dcrl.SignedMessage.MessageCase.BLOCKCHAIN_RESPONSE -> handle(message.blockchainResponse, message.certificate)
      Dcrl.SignedMessage.MessageCase.BLOCK_RESPONSE -> handle(message.blockResponse, message.certificate)
      Dcrl.SignedMessage.MessageCase.ERROR_MESSAGE -> handle(message.errorMessage, message.certificate)
      Dcrl.SignedMessage.MessageCase.ANNOUNCE -> handle(message.announce, message.certificate)
      Dcrl.SignedMessage.MessageCase.MESSAGE_NOT_SET -> failOnUnset(message)
      else -> failOnUnimplemented(message)
    }
  }

  abstract fun handle(message: Dcrl.CertificateRevocation, from: Dcrl.Certificate)
  abstract fun handle(message: Dcrl.BlockMessage, from: Dcrl.Certificate)
  abstract fun handle(message: Dcrl.BlockchainResponse, from: Dcrl.Certificate)
  abstract fun handle(message: Dcrl.BlockResponse, from: Dcrl.Certificate)
  abstract fun handle(message: Dcrl.Announce, from: Dcrl.Certificate)
}