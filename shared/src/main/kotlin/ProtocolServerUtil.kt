import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.InetSocketAddress

abstract class CommandLineBase : CliktCommand() {
  protected val discoveryNetworkIdentity: NetworkIdentity by option("--discovery", "-d")
    .convert { NetworkIdentity.from(it) }
    .required()
  protected val trustStoreDirectory: File by option("--trust", "-t")
    .file(fileOkay = false, folderOkay = true, exists = true, readable = true)
    .required()
}

// An plain function that will run a given protocol server. Command line needs to be parsed or whatever before
// calling this function
fun <T : ProtocolServer> runProtocolServer(
  discoveryServer: NetworkIdentity,
  becomeDiscoverable: Boolean = true,
  trustStoreDirectory: File,
  protocolServerFactory: (MutableMap<NetworkIdentity, SocketTuple>, File) -> T,
  callbackWithConfiguredServer: ((T) -> Unit)? = null
): Unit {
  runBlocking {
    // Make the server socket
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind()

    // Compute the server's port as late as possible (but keep track of it in case something happens)
    val serverSocketPort by lazy { (serverSocket.localAddress as InetSocketAddress).port }

    // Inform the discovery server of us and initiate a client connection to everyone the
    // discovery server is keeping track of
    val otherServers = aSocket(ActorSelectorManager(Dispatchers.IO))
      .tcp().connect(discoveryServer).use { discoverySocket ->
        // Tell the server about us (if we should)
        when (becomeDiscoverable) {
          true -> Discovery.Hello.newBuilder().apply { this.port = serverSocketPort }.build()
          false -> Discovery.Hello.getDefaultInstance()
        }.writeTo(discoverySocket.openWriteChannel(true).toOutputStream())

        // Parse the server's response
        Discovery.Response.parseDelimitedFrom(discoverySocket.openReadChannel().toInputStream()).serversList.map {
          NetworkIdentity(it.ipAddress, it.portNumber)
        }.map {
          Pair<NetworkIdentity, SocketTuple>(
            it, SocketTuple(aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(it))
          )
        }.toMap(mutableMapOf())
      }

    try {
      protocolServerFactory(otherServers, trustStoreDirectory).let { protocolServer: T ->
        // For every server we know about, initiate a socket
        for ((identity, socket) in protocolServer.otherServers)
          launch { protocolServer.babysitSocket(identity, socket) }

        callbackWithConfiguredServer?.let { launch { it.invoke(protocolServer) } }

        // Listen for any future connections, accept each as a socket
        while (true) SocketTuple(serverSocket.accept()).let { socket ->
          NetworkIdentity.from(socket).let { identity ->
            protocolServer.otherServers[identity] = socket
            launch { protocolServer.babysitSocket(identity, socket) }
          }
        }
      }
    } finally {
      // Clean up all the sockets
      for (socket in otherServers.values)
        kotlin.runCatching { if (!socket.isClosed) socket.close() }

      if (becomeDiscoverable) {
        // Tell the discovery server that we are shutting down
        aSocket(ActorSelectorManager(Dispatchers.IO))
          .tcp().connect(discoveryServer).use { discoverySocket ->
            Discovery.Goodbye.newBuilder().apply {
              this.port = serverSocketPort
            }.build().writeTo(discoverySocket.openWriteChannel(true).toOutputStream())
          }
      }

      // Try to close the server socket and swallow any related errors
      kotlin.runCatching { if (!serverSocket.isClosed) serverSocket.close() }
    }
  }
}

