import Util.sign
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
    Base64.decodeBase64(encodedCert)
      .let { certBytes ->
        try {
          Dcrl.Certificate.parseFrom(certBytes)
        } catch (e: Exception) {
          return RevocationResponse.REVOCATION_REJECTED
        }
      }.let { cert ->
        Dcrl.CertificateRevocation.newBuilder().setCertificate(cert).build()
      }.let { revokeMsg ->
        val builder =
          Dcrl.SignedMessage.newBuilder().setCertificate(selfCertificate).setCertificateRevocation(revokeMsg)
        builder.signature = ByteString.copyFrom(sign(builder.build(), selfPrivateKey))
        builder.build()
      }.let { signedMessage ->
        Dcrl.DCRLMessage.newBuilder().setSignedMessage(signedMessage).build()
      }.let { wrappedMsg ->
        sendMessageToIdentity(otherParticipantsAndAuthorities.random(), wrappedMsg)
      }

    return RevocationResponse.REVOCATION_STARTED
  }

  enum class RevocationResponse {
    REVOCATION_STARTED, REVOCATION_REJECTED
  }
}