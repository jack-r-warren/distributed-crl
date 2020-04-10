import com.google.protobuf.ByteString
import java.io.File
import java.security.SecureRandom
import kotlin.random.Random

open class ParticipantRoleServer(
  otherServers: MutableMap<NetworkIdentity, SocketTuple>,
  trustStore: File,
  selfCertificateFile: File,
  selfPrivateKeyFile: File
) :
  ParticipantJavaAbstract(otherServers, trustStore, selfCertificateFile, selfPrivateKeyFile) {

  // This block gets called during construction. [otherServers] and the superclass helper [sendMessageToIdentity] will
  // both already be good to go here, so we'll want to use this place to send out our Announce messages to the other
  // servers.
  override fun callbackUponConfigured() {
    println("Started")
    Dcrl.DCRLMessage.newBuilder().apply {
      signedMessageBuilder.apply {
        certificate = selfCertificate
        Dcrl.Announce.newBuilder().apply {
          nonce = Random.nextLong()
        }.build().let {
          println("Announce made")
          announce = it
          signature = ByteString.copyFrom(Util.sign(it, selfPrivateKey))
        }
      }
    }.build().let { message ->
      println("About to send")
      otherParticipantsAndAuthorities.forEach {
        sendMessageToIdentity(it, message)
        println("Sent announce to $it")
      }
    }
    println("Started")
  }

  override fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.BlockMessage,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage? =
    if (message.certificate.verify(
        trustStore::get,
        currentRevokedList::containsKey,
        Dcrl.CertificateUsage.PARTICIPATION
      )
    )
      ProtocolServerUtil.buildErrorMessage("Invalid certificate", selfCertificate, selfPrivateKey)
    else
      super.handleMessage(identity, message, from)

  override fun handleMessage(
    identity: NetworkIdentity,
    message: Dcrl.Announce,
    from: Dcrl.Certificate
  ): Dcrl.DCRLMessage? {
    otherParticipantsAndAuthorities.add(identity)
    println("Hello new participant or authority at $identity")
    return null
  }
}
