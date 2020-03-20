import java.net.InetSocketAddress

// Turns an [address] like what would be received from the discovery server into something that is still
// usable as a [Discovery.Server] but also a normal [InetSocketAddress]
class NetworkLocation(address: Discovery.Server) :
  // Extending InetSocketAddress
  InetSocketAddress(address.ipAddress, address.portNumber),
  // Implementing Discovery.ServerOrBuilder interface with Kotlin delegation shorthand
  Discovery.ServerOrBuilder by address {

  // Companion objects hold all static stuff for a class
  companion object {

    // Creates a [NetworkLocation] from a string of the form "0.0.0.0:12345"
    fun from(s: String): NetworkLocation =
      NetworkLocation(Discovery.Server.newBuilder().apply {
        s.split(':').let {
          this.ipAddress = it[0]
          this.portNumber = it[1].toInt()
        }
      }.build())
  }
}