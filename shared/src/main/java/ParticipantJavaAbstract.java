import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

abstract public class ParticipantJavaAbstract extends ObserverRoleServer {
  public ParticipantJavaAbstract(@NotNull Map<NetworkIdentity, SocketTuple> otherServers,
                                 @NotNull File trustStore) {
    super(otherServers, trustStore);
  }
}
