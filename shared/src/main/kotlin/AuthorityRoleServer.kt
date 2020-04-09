import org.apache.commons.codec.binary.Base64
import java.io.File

class AuthorityRoleServer(
  otherServers: MutableMap<NetworkIdentity, SocketTuple>,
  trustStore: File,
  selfCertificate: File,
  selfPrivateKey: File
) :
  ParticipantRoleServer(otherServers, trustStore, selfCertificate, selfPrivateKey) {

  fun revokeCertificate(hash: String): RevocationResponse {
    Base64.decodeBase64(hash).let { hashBytes ->
      return RevocationResponse.REVOCATION_REJECTED
      TODO("Actually do the revoking")
    }
  }

  enum class RevocationResponse {
    REVOCATION_STARTED, REVOCATION_REJECTED
  }
}