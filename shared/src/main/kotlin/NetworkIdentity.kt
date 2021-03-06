import io.ktor.network.sockets.AConnectedSocket
import java.net.InetSocketAddress

// A network identity is some ip:port combination. An identity is not the same as an active socket, see [SocketTuple]
// for that.
data class NetworkIdentity(val ipAddress: String, val portNumber: Int) : InetSocketAddress(ipAddress, portNumber) {
  companion object {
    fun from(address: InetSocketAddress) = NetworkIdentity(address.hostString, address.port)
    fun from(socket: AConnectedSocket) = from(socket.remoteAddress as InetSocketAddress)

    // Creates a [NetworkIdentity] from a string like "0.0.0.0:1234"
    fun from(string: String) = string.split(':').let { NetworkIdentity(it[0], it[1].toInt()) }
  }
}