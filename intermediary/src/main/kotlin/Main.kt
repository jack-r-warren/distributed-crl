fun main(args: Array<String>) = ClientMain.main(args)

object ClientMain : CommandLineBase() {

  override fun run() = runProtocolServer(
    discoveryServer = discoveryNetworkIdentity,
    becomeDiscoverable = false,
    trustStoreDirectory = trustStoreDirectory,
    // Method reference syntax, used here to reference a constructor
    protocolServerFactory = ::ParticipantRoleServer
  )
}