import io.ktor.network.sockets.*
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.utils.io.jvm.javaio.toOutputStream

// A network socket. Wraps a socket to only expose what we need: the outputStream and inputStream. This wrapping
// helps prevent us from accidentally opening multiple streams on the same socket, which would cause errors.
class SocketTuple(private val socket: Socket) : ASocket by socket, AConnectedSocket by socket {
  // "by lazy {...}" just means "execute {...} the first time I'm called and store the result"
  val outputStream by lazy { socket.openWriteChannel(true).toOutputStream() }
  val inputStream by lazy { socket.openReadChannel().toInputStream() }
}