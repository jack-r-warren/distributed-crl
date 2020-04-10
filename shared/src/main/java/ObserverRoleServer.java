import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ObserverRoleServer extends ProtocolServer {

  /*
  Observer has
    - a Map of the other servers
    - a list of preferences
    - the CRL
    - a timestamp of when the last CRL was received
   */

  List<NetworkIdentity> preferenceList;
  List<Dcrl.BlockMessage> blockchain;
  long timestamp;
  Runnable uponReceivingBlockchain = null;


  public ObserverRoleServer(@NotNull Map<NetworkIdentity, SocketTuple> otherServers,
                            @NotNull File trustStore,
                            @NotNull List<NetworkIdentity> preferenceList) {
    super(otherServers, trustStore);
    this.preferenceList = preferenceList;
    this.blockchain = new ArrayList<>();
    this.timestamp = 0;
  }

  public ObserverRoleServer(@NotNull Map<NetworkIdentity, SocketTuple> otherServers, @NotNull File trustStore) {
    this(otherServers, trustStore, new ArrayList<NetworkIdentity>());
  }


  public void setPreferenceList(@NotNull List<NetworkIdentity> preferenceList) {
    this.preferenceList = preferenceList;
  }

  @NotNull
  public List<NetworkIdentity> getPreferenceList() {
    return this.preferenceList;
  }

  /*
  Requests a copy of the blockchain from a specific server.
   */
  public void requestBlockchain(NetworkIdentity server) {
    // build the request
    System.out.println("Sending request to " + server.getIpAddress() + ":" + server.getPortNumber());
    Dcrl.DCRLMessage request = Dcrl.DCRLMessage.newBuilder()
        .setUnsignedMessage(
            Dcrl.UnsignedMessage.newBuilder()
                .setBlockchainRequest(
                    Dcrl.BlockchainRequest.getDefaultInstance()
                )
        )
        .build();

    sendMessageToIdentity(server, request);
  }

  /*
  Requests a copy of the Blockchain from the first NetworkIdentity in the preference list, if there is one.
  Defaults to a random server if there are no preferences.
  */
  public void requestBlockchain() {
    // send the request to the first preference, if there is one
    if (this.preferenceList.size() > 0) {
      requestBlockchain(this.preferenceList.get(0));
    } else {
      for (NetworkIdentity nt : this.getOtherServers().keySet()) {
        requestBlockchain(nt);
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
    List<Dcrl.BlockMessage> response = message.getBlocksList();

    System.out.println("got a message back");

    // error checking before updating this.blockchain
    if (response.isEmpty()) {
      return ProtocolServerUtil.buildErrorMessage("Empty blockchain.");
    } else {
      this.blockchain = response;
      processBlockchain();
      this.timestamp = (new Date()).getTime();
      if (uponReceivingBlockchain != null) uponReceivingBlockchain.run();
      uponReceivingBlockchain = null;
      return null;
    }
  }

  /**
   * Processes the blockchain to generate the current state of revoked CRLs. This function assumes that the blockchain
   * list is in order.
   */
  protected void processBlockchain() {

    this.getCurrentRevokedList().clear();

    for (Dcrl.BlockMessage block : this.blockchain) {
      for (Dcrl.CertificateRevocation revocation : block.getCertificateRevocationsList()) {
        this.getCurrentRevokedList().put(Util.hashCert(revocation.getCertificate()), revocation.getCertificate());
      }
    }
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
    System.out.println(message.getError());
    return null;
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
    // Don't think we do anything here... no need to reply with an ErrorMessage
    return null;
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity, @NotNull Dcrl.BlockchainRequest message) {
    System.out.println("wtf");
    return ProtocolServerUtil.buildErrorMessage(
        String.format("Message type %s not supported.", message.getClass().toString())
    );
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity, @NotNull Dcrl.BlockRequest message) {
    return ProtocolServerUtil.buildErrorMessage(
        String.format("Message type %s not supported.", message.getClass().toString())
    );
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.CertificateRevocation message,
                                        @NotNull Dcrl.Certificate from) {
    return ProtocolServerUtil.buildErrorMessage(
        String.format("Message type %s not supported.", message.getClass().toString())
    );
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.BlockMessage message,
                                        @NotNull Dcrl.Certificate from) {
    return ProtocolServerUtil.buildErrorMessage(
        String.format("Message type %s not supported.", message.getClass().toString())
    );
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.BlockResponse message,
                                        @NotNull Dcrl.Certificate from) {
    return ProtocolServerUtil.buildErrorMessage(
        String.format("Message type %s not supported.", message.getClass().toString())
    );
  }
}
