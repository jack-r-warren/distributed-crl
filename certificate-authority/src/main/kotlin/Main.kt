import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import java.io.File

fun main(args: Array<String>) = ClientMain.main(args)

object ClientMain : SignerCommandLineBase() {
  private val webPort by option("--port", "-p").int().required()

  override fun run() = runProtocolServer(
    discoveryServer = discoveryNetworkIdentity,
    trustStoreDirectory = trustStoreDirectory,
    protocolServerFactory = { otherServers: MutableMap<NetworkIdentity, SocketTuple>, trustStore: File ->
      AuthorityRoleServer(otherServers, trustStore, selfCertificate, selfPrivateKey)
    },
    callbackWithConfiguredServer = ::runWebInterface
  )

  private fun runWebInterface(server: AuthorityRoleServer): Unit {
    println("Running web server")
    embeddedServer(CIO, webPort) {
      routing {
        get("/") {
          call.respondText(
            """
Go to /check/{hash} to check the validity of a certificate<br>
Go to /revoke/{cert} to revoke a certificate
          """.trimIndent(), ContentType.Text.Html
          )
        }
        get("/check/{hash}") {
          call.parameters["hash"].let {
            if (it != null && it.isNotEmpty())
              call.respondText(ContentType.Text.Html) { server.checkCertificate(it).name }
            else
              call.respondText("No certificate hash given as a parameter", ContentType.Text.Html)
          }
        }
        get("/revoke/{cert}") {
          call.parameters["cert"].let {
            if (it != null && it.isNotEmpty())
              call.respondText(ContentType.Text.Html) { server.revokeCertificate(it).name }
            else
              call.respondText("No certificate given as a parameter", ContentType.Text.Html)
          }
        }
      }
    }.start()
  }
}