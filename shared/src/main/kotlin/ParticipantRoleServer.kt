import com.google.protobuf.ByteString
import java.io.File
import java.security.SecureRandom

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
    init {
        Dcrl.DCRLMessage.newBuilder().apply {
            signedMessageBuilder.apply {
                certificate = selfCertificate
                Dcrl.Announce.newBuilder().apply {
                    nonce = SecureRandom.getInstanceStrong().nextLong()
                }.build().let {
                    announce = it
                    signature = ByteString.copyFrom(Util.sign(it, selfPrivateKey))
                }
            }
        }.build().let { message ->
            otherParticipantsAndAuthorities.forEach { sendMessageToIdentity(it, message) }
        }
    }

    override fun handleMessage(
        identity: NetworkIdentity,
        message: Dcrl.BlockMessage,
        from: Dcrl.Certificate
    ): Dcrl.DCRLMessage? =
        if (message.certificate.verifySignature(trustStore::get))
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
