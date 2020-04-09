import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

public class ObserverRoleServer extends ProtocolServer{

  /*
  Observer has
    - a Map of the other servers
    - a list of preferences
    - the CRL
    - a timestamp of when the last CRL was received
   */

  private Map<NetworkIdentity, SocketTuple> otherServers;
  private List<NetworkIdentity> preferenceList;
  private Writer writer = new PrintWriter(System.out);
  private List<Dcrl.BlockMessage> blockchain = new ArrayList<Dcrl.BlockMessage>();
  private long timestamp = 0;

  public ObserverRoleServer(@NotNull Map<NetworkIdentity, SocketTuple> otherServers) {
    super(otherServers);
    this.otherServers = otherServers;
    this.preferenceList = new ArrayList<NetworkIdentity>();
  }

  public ObserverRoleServer(@NotNull Map<NetworkIdentity, SocketTuple> otherServers,
                            @NotNull List<NetworkIdentity> preferenceList) {
    super(otherServers);
    this.otherServers = otherServers;
    this.preferenceList = preferenceList;
  }

  public ObserverRoleServer(@NotNull Map<NetworkIdentity, SocketTuple> otherServers, @NotNull Writer writer) {
    super(otherServers);
    this.otherServers = otherServers;
    this.preferenceList = new ArrayList<NetworkIdentity>();
    this.writer = writer;
  }

  public ObserverRoleServer(@NotNull Map<NetworkIdentity, SocketTuple> otherServers,
                            @NotNull List<NetworkIdentity> preferenceList,
                            @NotNull Writer writer) {
    super(otherServers);
    this.otherServers = otherServers;
    this.preferenceList = preferenceList;
    this.writer = writer;
  }

  public void setPreferenceList(@NotNull List<NetworkIdentity> preferenceList) {
    this.preferenceList = preferenceList;
  }

  /*
  Requests a copy of the Blockchain from the first NetworkIdentity in the preference list, if there is one.

  This function does not wait for a response.
  */
  public void requestBlockchain() {
    // build the request
    Dcrl.DCRLMessage request = Dcrl.DCRLMessage.newBuilder()
        .setUnsignedMessage(
            Dcrl.UnsignedMessage.newBuilder()
              .setBlockchainRequest(
                  Dcrl.BlockchainRequest.getDefaultInstance()
              )
        )
        .build();

      // send the request to the first preference, if there is one
    if (this.preferenceList.size() > 0) {
      sendMessageToIdentity(this.preferenceList.get(0), request);
    } else {
      for (NetworkIdentity nt : this.otherServers.keySet()) {
        sendMessageToIdentity(nt, request);
        break;
      }
    }
  }

  /*
  Verifies the signature of the message
  Updates the blockchain and timestamp if the blockchain is not empty
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.BlockchainResponse message,
                                        @NotNull Dcrl.Certificate from) {
    // TODO verify signature

    List<Dcrl.BlockMessage> response = message.getBlocksList(); 

     // error checking before updating this.blockchain
    List<Dcrl.BlockMessage> empty = Collections.emptyList();
    if (response.equals(empty)) {
      return ProtocolServerUtil.buildErrorMessage("Empty blockchain.");
    } else {
      this.blockchain = response;
      this.timestamp = (new Date()).getTime();
      return null;
    }
  }

  /*
  Verifies the signature of the message
  Adds a new NetworkIdentity to the Map of other servers, unless there is an error.
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.Announce message,
                                        @NotNull Dcrl.Certificate from) {
    // TODO verify signature

    // TODO want to add the new NetworkIdentity to the Map, but I need a SocketTuple ...
    return null;
  }

  /*
  Verifies the signature of the message
  Prints the Error message
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.ErrorMessage message,
                                        @Nullable Dcrl.Certificate from) {
    // TODO verify signature (if there is one)

    try {
      this.writer.write(message.getError());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity, @NotNull Dcrl.BlockchainRequest message) {
    return ProtocolServerUtil.buildErrorMessage(
        String.format("Message type %s not supported.",message.getClass().toString())
    );
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity, @NotNull Dcrl.BlockRequest message) {
    return ProtocolServerUtil.buildErrorMessage(
        String.format("Message type %s not supported.",message.getClass().toString())
    );
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.CertificateRevocation message,
                                        @NotNull Dcrl.Certificate from) {
    return ProtocolServerUtil.buildErrorMessage(
        String.format("Message type %s not supported.",message.getClass().toString())
    );
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.BlockMessage message,
                                        @NotNull Dcrl.Certificate from) {
    return ProtocolServerUtil.buildErrorMessage(
        String.format("Message type %s not supported.",message.getClass().toString())
    );
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.BlockResponse message,
                                        @NotNull Dcrl.Certificate from) {
    return ProtocolServerUtil.buildErrorMessage(
        String.format("Message type %s not supported.",message.getClass().toString())
    );
  }
}
