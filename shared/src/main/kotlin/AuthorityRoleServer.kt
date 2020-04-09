import java.io.File

class AuthorityRoleServer(
  otherServers: MutableMap<NetworkIdentity, SocketTuple>,
  trustStore: File,
  selfCertificate: File,
  selfPrivateKey: File
) :
  ParticipantRoleServer(otherServers, trustStore, selfCertificate, selfPrivateKey) {
}