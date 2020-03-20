import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress

class ServerRunner(private val runnable: suspend (ServerSocket, Collection<NetworkLocation>) -> Runnable) : CliktCommand() {
  private val discovery by option(
    help = "The address of the discovery server, like 0.0.0.0:12345"
  ).convert { NetworkLocation.from(it) }.required()

  override fun run(): Unit = runBlocking {
    // Make the server socket
    val serverSocket = aSocket(ActorSelectorManager(Dispatchers.IO))
      .tcp()
      .bind()

    // Compute the server's port as late as possible (but keep track of it in case something happens
    val serverSocketPort by lazy { (serverSocket.localAddress as InetSocketAddress).port }

    // Inform the discover server of us and get any other connected servers
    val otherServers: Collection<NetworkLocation> = aSocket(ActorSelectorManager(Dispatchers.IO))
      .tcp()
      .connect(discovery).use { discoverySocket ->
        Discovery.Hello.newBuilder().apply {
          this.port = serverSocketPort
        }.build().writeTo(discoverySocket.openWriteChannel(true).toOutputStream())

        Discovery.Response.parseDelimitedFrom(discoverySocket.openReadChannel().toInputStream())
          .serversList
          .map { NetworkLocation(it) }
      }

    try {
      // The meat of everything; actually call whatever needs the socket and the other servers
      runnable.invoke(serverSocket, otherServers)
    } finally {

      // Tell the discovery server that we are/have shut down
      aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .connect(discovery).use { discoverySocket ->
          Discovery.Goodbye.newBuilder().apply {
            this.port = serverSocketPort
          }.build().writeTo(discoverySocket.openWriteChannel(true).toOutputStream())
        }

      // Try to close the server socket and swallow any related errors
      kotlin.runCatching { serverSocket.close() }
    }
  }
}