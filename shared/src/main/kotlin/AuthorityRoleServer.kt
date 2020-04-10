import com.google.protobuf.ByteString
import org.apache.commons.codec.binary.Base64
import java.io.File

class AuthorityRoleServer(
  otherServers: MutableMap<NetworkIdentity, SocketTuple>,
  trustStore: File,
  selfCertificate: File,
  selfPrivateKey: File
) :
  ParticipantRoleServer(otherServers, trustStore, selfCertificate, selfPrivateKey) {

  fun revokeCertificate(encodedCert: String): RevocationResponse {
    try {
      Base64.decodeBase64(encodedCert)
        .let { certBytes ->
          try {
            Dcrl.Certificate.parseFrom(certBytes)
          } catch (e: Exception) {
            println("Couldn't parse certificate")
            throw e
          }
        }.let { cert ->
          println("Making revocation message")
          Dcrl.DCRLMessage.newBuilder().apply {
            signedMessageBuilder.apply {
              certificate = selfCertificate
              Dcrl.CertificateRevocation.newBuilder().apply {
                certificate = cert
              }.build().let {
                certificateRevocation = it
                signature = Util.sign(it, selfPrivateKey)
              }
              println("Finished with revocation message")
            }
          }
        }.build().let { wrappedMsg ->
          sendMessageToIdentity(otherParticipantsAndAuthorities.random().also { println("Started revocation with $it") }, wrappedMsg)
        }
    } catch (e: Throwable) {
      println("$e")
      return RevocationResponse.REVOCATION_REJECTED
    }
    return RevocationResponse.REVOCATION_STARTED
  }

  enum class RevocationResponse {
    REVOCATION_STARTED, REVOCATION_REJECTED
  }
}