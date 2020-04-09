import java.io.File

class AuthorityRoleServer(otherServers: MutableMap<NetworkIdentity, SocketTuple>, trustStore: File) :
  ParticipantRoleServer(otherServers, trustStore) {
}