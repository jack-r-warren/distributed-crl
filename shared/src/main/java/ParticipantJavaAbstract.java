import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

abstract public class ParticipantJavaAbstract extends ObserverRoleServer {
  protected final Dcrl.Certificate selfCertificate;
  protected final byte[] selfPrivateKey;

  public ParticipantJavaAbstract(@NotNull Map<NetworkIdentity, SocketTuple> otherServers,
                                 @NotNull File trustStore,
                                 @NotNull File selfCertificate,
                                 @NotNull File selfPrivateKey) throws IOException {
    super(otherServers, trustStore);
    this.selfCertificate = Dcrl.Certificate.parseFrom(Files.readAllBytes(selfCertificate.toPath()));
    this.selfPrivateKey = Files.readAllBytes(selfPrivateKey.toPath());
  }
}
