import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

fun main(args: Array<String>) = ClientMain.main(args)

object ClientMain : CliktCommand() {
  private val discovery: NetworkIdentity by option().convert { NetworkIdentity.from(it) }.required()

  override fun run() = runProtocolServer(
    discoveryServer = discovery,
    becomeDiscoverable = false,
    // Method reference syntax, used here to reference a constructor
    protocolServerFactory = ::AuthorityRoleServer
  )
}