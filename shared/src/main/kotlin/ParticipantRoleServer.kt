import java.io.File

open class ParticipantRoleServer(
  otherServers: MutableMap<NetworkIdentity, SocketTuple>,
  trustStore: File,
  selfCertificate: File,
  selfPrivateKey: File
) :
  ParticipantJavaAbstract(otherServers, trustStore, selfCertificate, selfPrivateKey) {

  // This block gets called during construction. [otherServers] and the superclass helper [sendMessageToIdentity] will
  // both already be good to go here, so we'll want to use this place to send out our Announce messages to the other
  // servers.
  init {
  }

}
