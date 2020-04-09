import com.google.protobuf.Message
import io.ktor.network.sockets.isClosed
import java.io.File
import java.nio.file.InvalidPathException

abstract class ProtocolServer(val otherServers: MutableMap<NetworkIdentity, SocketTuple>) {

  companion object {
    fun readTrustStore(path: String): List<Dcrl.Certificate> {
      val dir = File(path)
      if (!dir.isDirectory) {
        throw InvalidPathException(path, "Not a folder");
      }

      return dir.walk().map {
        var cert: Dcrl.Certificate? = null;
        if (it.isFile) {
          try {
            cert = Dcrl.Certificate.parseFrom(it.readBytes())
          } catch (e: Exception) {
            System.err.println("Failed to parse ${it.absolutePath}, ignoring")
          }
        } else {
          System.err.println("Ignoring ${it.absolutePath} because it is not a file.")
        }
        cert
      }.filterNotNull().toList()
    }
  }

  /*
  Socket stuff
   */

  // Helper function to send some message to some identity
  fun sendMessageToIdentity(identity: NetworkIdentity, message: Dcrl.DCRLMessage): Unit {
    otherServers[identity]?.let { socket ->
      message.writeTo(socket.outputStream)
    } ?: println("Was asked to send a message to $identity but it didn't exist in the otherServers map!")
  }

  // This function is called by [runProtocolServer] to have a coroutine sit around and babysit a socket
  fun babysitSocket(identity: NetworkIdentity, socket: SocketTuple) {
    try {
      // Receive messages and send any non-null outputs of the handleMessage function
      while (true) {
        handleMessage(identity, Dcrl.DCRLMessage.parseDelimitedFrom(socket.inputStream))?.writeTo(socket.outputStream)
      }
    } catch (e: Throwable) {
      println("Error bubbled up to socket handling, so the socket ($identity) was closed.")
      println(e)
    } finally {
      kotlin.runCatching { if (!socket.isClosed) socket.close() }
      otherServers.remove(identity)
    }
  }

  /*
  Public-facing interface stuff
   */

  // I don't know how we want the hash to be encoded as a string, but it needs to go in a URL.
  fun checkCertificate(hash: String): CheckResponse {
    return CheckResponse.NOT_REVOKED
  }

  enum class CheckResponse {
    NOT_REVOKED, REVOKED
  }

  /*
  Message handling stuff
   */

  private fun failOnNotSet(message: Message): Nothing =
    throw IllegalArgumentException("Message case in $message was not set")

  private fun failOnNull(message: Message): Nothing =
    throw IllegalArgumentException("Message case in $message was null")

  fun handleMessage(identity: NetworkIdentity, message: Dcrl.DCRLMessage): Dcrl.DCRLMessage? =
    when (message.messageCase) {
      Dcrl.DCRLMessage.MessageCase.UNSIGNED_MESSAGE -> handleMessage(identity, message.unsignedMessage)
      Dcrl.DCRLMessage.MessageCase.SIGNED_MESSAGE -> handleMessage(identity, message.signedMessage)
      Dcrl.DCRLMessage.MessageCase.MESSAGE_NOT_SET -> failOnNotSet(message)
      null -> failOnNull(message)
    }

  fun handleMessage(identity: NetworkIdentity, message: Dcrl.UnsignedMessage): Dcrl.DCRLMessage? =
    when (message.messageCase) {
      Dcrl.UnsignedMessage.MessageCase.BLOCKCHAIN_REQUEST -> handleMessage(identity, message.blockchainRequest)
      Dcrl.UnsignedMessage.MessageCase.BLOCK_REQUEST -> handleMessage(identity, message.blockRequest)
      Dcrl.UnsignedMessage.MessageCase.ERROR_MESSAGE -> handleMessage(identity, message.errorMessage)
      Dcrl.UnsignedMessage.MessageCase.MESSAGE_NOT_SET -> failOnNotSet(message)
      null -> failOnNull(message)
    }

  abstract fun handleMessage(identity: NetworkIdentity, message: Dcrl.BlockchainRequest): Dcrl.DCRLMessage?
  abstract fun handleMessage(identity: NetworkIdentity, message: Dcrl.BlockRequest): Dcrl.DCRLMessage?
  abstract fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.ErrorMessage,
    from: Dcrl.Certificate? = null
  ): Dcrl.DCRLMessage?

  fun handleMessage(identity: NetworkIdentity, message: Dcrl.SignedMessage): Dcrl.DCRLMessage? =
    when (message.messageCase) {
      Dcrl.SignedMessage.MessageCase.CERTIFICATE_REVOCATION -> handleMessage(
        identity,
        message.certificateRevocation,
        message.certificate
      )
      Dcrl.SignedMessage.MessageCase.BLOCK_MESSAGE -> handleMessage(identity, message.blockMessage, message.certificate)
      Dcrl.SignedMessage.MessageCase.BLOCKCHAIN_RESPONSE -> handleMessage(
        identity,
        message.blockchainResponse,
        message.certificate
      )
      Dcrl.SignedMessage.MessageCase.BLOCK_RESPONSE -> handleMessage(
        identity,
        message.blockResponse,
        message.certificate
      )
      Dcrl.SignedMessage.MessageCase.ERROR_MESSAGE -> handleMessage(identity, message.errorMessage, message.certificate)
      Dcrl.SignedMessage.MessageCase.ANNOUNCE -> handleMessage(identity, message.announce, message.certificate)
      Dcrl.SignedMessage.MessageCase.MESSAGE_NOT_SET -> failOnNotSet(message)
      null -> failOnNull(message)
    }

  abstract fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.CertificateRevocation,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage?

  abstract fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.BlockMessage,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage?

  abstract fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.BlockchainResponse,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage?

  abstract fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.BlockResponse,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage?

  abstract fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.Announce,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage?
}