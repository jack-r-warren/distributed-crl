import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) = ClientMain.main(args)

object ClientMain : CommandLineBase() {
  private val webPort: Int by option("--port", "-p").int().required()

  override fun run() = runProtocolServer(
    discoveryServer = discoveryNetworkIdentity,
    becomeDiscoverable = false,
    trustStoreDirectory = trustStoreDirectory,
    // Method reference syntax, used here to reference a constructor...
    protocolServerFactory = ::ObserverRoleServer,
    // ...and used here to reference a method of this class
    callbackWithConfiguredServer = ::runWebInterface
  )

  private fun runWebInterface(server: ObserverRoleServer): Unit {
    embeddedServer(Netty, webPort) {
      routing {
        get("/") {
          call.respondText("Go to /check/{hash} to check the validity of a certificate", ContentType.Text.Html)
        }
        get("/check/{hash}") {
          call.parameters["hash"]?.let {
            call.respondText(ContentType.Text.Html) { server.checkCertificate(it).name }
          } ?: call.respondText("No certificate hash given as a parameter", ContentType.Text.Html)
        }
      }
    }.start(wait = true)
  }
}
