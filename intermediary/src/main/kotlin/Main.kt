import io.ktor.network.sockets.ServerSocket

fun main(args: Array<String>) = ServerRunner { socket: ServerSocket, otherServers: Collection<NetworkLocation> ->
  // Just instantiate a Runnable (probably an implementation of the Participant interface) here,
  // and the server runner will handle the rest
  TODO()
}.main(args)