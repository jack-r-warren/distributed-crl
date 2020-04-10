import Util.hashCert
import com.google.protobuf.ByteString
import com.google.protobuf.Message
import io.ktor.network.sockets.isClosed
import kotlinx.coroutines.*
import org.apache.commons.codec.binary.Base64
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.seconds

abstract class ProtocolServer(val otherServers: MutableMap<NetworkIdentity, SocketTuple>, trustStorePath: File) {
  val trustStore: Map<ByteString, Dcrl.Certificate> = readTrustStore(trustStorePath).map {
    hashCert(it) to it
  }.toMap()

  val toSendTo = ConcurrentHashMap<NetworkIdentity, ConcurrentLinkedQueue<Dcrl.DCRLMessage>>()

  protected val currentRevokedList = HashMap<ByteString, Dcrl.Certificate>()

  companion object {
    fun readTrustStore(dir: File): List<Dcrl.Certificate> {
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

  open fun callbackUponConfigured(): Unit {
    return
  }

  // Helper function to send some message to some identity
  fun sendMessageToIdentity(identity: NetworkIdentity, message: Dcrl.DCRLMessage): Unit {
    otherServers[identity]?.let { socket ->
      message.writeDelimitedTo(socket.outputStream)
    } ?: println("Was asked to send a message to $identity but it didn't exist in the otherServers map!")
  }

  // This function is called by [runProtocolServer] to have a coroutine sit around and babysit a socket
  fun babysitSocket(identity: NetworkIdentity, socket: SocketTuple) {
    try {
      // Receive messages and send any non-null outputs of the handleMessage function
      while (true) {
        println("There's something in the socket!")
        handleMessage(
          identity,
          Dcrl.DCRLMessage.parseDelimitedFrom(socket.inputStream).also { println("Received $it from ${socket.remoteAddress}") }
        ).also { println("Sent $it to ${socket.remoteAddress}") }?.writeDelimitedTo(socket.outputStream)
      }
    } catch (e: Throwable) {
      println("Error bubbled up to socket handling, so the socket ($identity) was closed.")
      println(e)
    } finally {
      println("Removing $identity")
      kotlin.runCatching { if (!socket.isClosed) socket.close() }
      otherServers.remove(identity)
    }
  }

  /*
  Public-facing interface stuff
   */

  // Assumes the hash is a base64 encoded bytes
  fun checkCertificate(hash: String): CheckResponse {
    if (currentRevokedList.containsKey(ByteString.copyFrom(Base64.decodeBase64(hash)))) {
      return CheckResponse.REVOKED
    }
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

  fun handleMessage(identity: NetworkIdentity, message: Dcrl.DCRLMessage): Dcrl.DCRLMessage?  {
    println("got some message, trying to decrypt")
    return when (message.messageCase) {
      Dcrl.DCRLMessage.MessageCase.UNSIGNED_MESSAGE -> handleMessage(identity, message.unsignedMessage)
      Dcrl.DCRLMessage.MessageCase.SIGNED_MESSAGE -> {
        println("Called the handle for the signed")
        handleMessage(identity, message.signedMessage)
      }
      Dcrl.DCRLMessage.MessageCase.MESSAGE_NOT_SET -> failOnNotSet(message)
      null -> failOnNull(message)
    }
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
    if (!Util.verify(message)) {
      println("bad signature")
      ProtocolServerUtil.buildErrorMessage("Bad signing")
    }
    else {
      println("delegating message")
      when (message.messageCase) {
        Dcrl.SignedMessage.MessageCase.CERTIFICATE_REVOCATION -> {
          println("About to call for the revocation")
          handleMessage(
            identity,
            message.certificateRevocation,
            message.certificate
          )
        }
        Dcrl.SignedMessage.MessageCase.BLOCK_MESSAGE -> handleMessage(
          identity,
          message.blockMessage,
          message.certificate
        )
        Dcrl.SignedMessage.MessageCase.BLOCKCHAIN_RESPONSE -> {
          println("reached blockchain resp block")
          handleMessage(
            identity,
            message.blockchainResponse,
            message.certificate
          )
        }
        Dcrl.SignedMessage.MessageCase.BLOCK_RESPONSE -> handleMessage(
          identity,
          message.blockResponse,
          message.certificate
        )
        Dcrl.SignedMessage.MessageCase.ERROR_MESSAGE -> handleMessage(
          identity,
          message.errorMessage,
          message.certificate
        )
        Dcrl.SignedMessage.MessageCase.ANNOUNCE -> handleMessage(identity, message.announce, message.certificate)
        Dcrl.SignedMessage.MessageCase.MESSAGE_NOT_SET -> {
          println("msg case not set")
          failOnNotSet(message)
        }
        null -> {
          println("??? got null")
          failOnNull(message)
        }
      }
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