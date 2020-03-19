import java.net.InetSocketAddress

// Turns an [address] like what would be received from the discovery server into something that is still
// usable as a [Discovery.Server] but also a normal [InetSocketAddress]
class NetworkLocation(address: Discovery.Server) :
  // Extending InetSocketAddress
  InetSocketAddress(address.ipAddress, address.portNumber),
  // Implementing Discovery.ServerOrBuilder interface with Kotlin delegation shorthand
  Discovery.ServerOrBuilder by address