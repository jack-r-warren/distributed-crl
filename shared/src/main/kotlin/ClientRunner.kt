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

class ClientRunner(private val runnable: suspend (Collection<NetworkLocation>) -> Runnable) : CliktCommand() {
  private val discovery by option(
    help = "The address of the discovery server, like 0.0.0.0:12345"
  ).convert { NetworkLocation.from(it) }.required()

  override fun run(): Unit {
    runBlocking {
      // Get any other connected servers
      val otherServers: Collection<NetworkLocation> = aSocket(ActorSelectorManager(Dispatchers.IO))
        .tcp()
        .connect(discovery).use { discoverySocket ->
          Discovery.Hello.getDefaultInstance().writeTo(discoverySocket.openWriteChannel(true).toOutputStream())

          Discovery.Response.parseDelimitedFrom(discoverySocket.openReadChannel().toInputStream())
            .serversList
            .map { NetworkLocation(it) }
        }

      // The meat of everything; actually call whatever needs the other servers
      runnable.invoke(otherServers)
    }
  }
}