import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * This DiscoveryServer is an object, a Kotlin way of saying "a class that only ever has one instance so don't worry
 * about constructors." It extends [CliktCommand], which gives us a few things:
 * - Automatic help text
 * - The ability to delegate properties to a command line option with the "by option" syntax
 *      (This uses Kotlin's delegated properties, see https://kotlinlang.org/docs/reference/delegated-properties.html
 *      if you're interested)
 * - The ability to use [echo] (pretty much just [println] with some cross-platform compatibility thrown in)
 */
object DiscoveryServer : CliktCommand(help = "Run a discovery server to facilitate a proof-of-concept") {

  private val port by option(help = "The port to run the discovery server on").int().default(22301)

  private val serverSet: MutableSet<Discovery.Server> = ConcurrentHashMap.newKeySet()

  /**
   * This method is called by Clikt immediately upon all command line input being parsed. This is essentially the
   * "main" method for this server.
   */
  override fun run() = runBlocking {
    aSocket(ActorSelectorManager(Dispatchers.IO))
      .tcp()
      .bind(InetSocketAddress(port))
      .use { serverSocket -> // `use` will always close the socket down correctly

        echo("Discovery server online at ${serverSocket.localAddress} (from local observation)")

        while (true) {
          val socket = serverSocket.accept()

          launch {
            socket.use { socket ->
              val input = socket.openReadChannel().toInputStream()
              val output = socket.openWriteChannel(autoFlush = true).toOutputStream()
              val remoteIP = (socket.remoteAddress as InetSocketAddress).hostString

              // parseDelimitedFrom is like what we do for NSTP manually with writing an int of the size
              // before every message, except it is more efficient and it is built-in to Java's protobuf
              // code
              val message = Discovery.FromClientMessage.parseDelimitedFrom(input)

              when (message.messageCase) {
                // Upon HELLO, respond with any existing servers and add the new one to the set
                Discovery.FromClientMessage.MessageCase.HELLO -> {
                  Discovery.Response.newBuilder().apply {
                    addAllServers(serverSet)
                  }.build().writeDelimitedTo(output)

                  // All protobuf fields optional and will be null if omitted, I'm using this here to let clients
                  // request servers without adding themselves to the list
                  @Suppress("UNNECESSARY_SAFE_CALL")
                  message.hello.port?.let {
                    serverSet.add(makeServer(remoteIP, it))
                  }
                }
                // Upon GOODBYE, remove it from the set
                Discovery.FromClientMessage.MessageCase.GOODBYE ->
                  // All protobuf fields optional and will be null if omitted, I'm using this here to let clients
                  // request servers without adding themselves to the list
                  @Suppress("UNNECESSARY_SAFE_CALL")
                  message.goodbye.port?.let {
                    serverSet.remove(makeServer(remoteIP, it))
                  }
                // If we can't parse, just print something
                Discovery.FromClientMessage.MessageCase.MESSAGE_NOT_SET, null ->
                  echo("Message from ${socket.remoteAddress} couldn't be parsed!")
              }
            }
          }
        }
      }
  }

  /**
   * Make a new Discovery.Server object for the given [ip] and [port]
   */
  private fun makeServer(ip: String, port: Int): Discovery.Server {
    return Discovery.Server.newBuilder().apply {
      this.ipAddress = ip
      this.portNumber = port
    }.build()
  }

}

/**
 * Essentially a dummy function, this redirects to the DiscoveryServer object that makes use of the Clikt
 * command line parser
 */
fun main(args: Array<String>) = DiscoveryServer.main(args)