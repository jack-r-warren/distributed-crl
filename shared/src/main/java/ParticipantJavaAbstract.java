import org.jetbrains.annotations.NotNull;

import java.util.Map;

abstract public class ParticipantJavaAbstract extends ObserverRoleServer {
  public ParticipantJavaAbstract(@NotNull Map<NetworkIdentity, SocketTuple> otherServers) {
    super(otherServers);
  }
}
