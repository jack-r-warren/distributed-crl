import com.google.protobuf.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

abstract public class ParticipantJavaAbstract extends ObserverRoleServer {
  protected final Dcrl.Certificate selfCertificate;
  protected final byte[] selfPrivateKey;
  protected final List<NetworkIdentity> otherParticipantsAndAuthorities;

  protected List<Dcrl.CertificateRevocation> revocationsToProcess;
  // inherits this.blockchain
  protected ByteString lastValidatedHash;
  protected int lastValidatedHeight;

  // for block creation
  protected int revocationsPerBlock;

  public ParticipantJavaAbstract(@NotNull Map<NetworkIdentity, SocketTuple> otherServers,
                                 @NotNull File trustStore,
                                 @NotNull File selfCertificate,
                                 @NotNull File selfPrivateKey) throws IOException {
    super(otherServers, trustStore);
    this.selfCertificate = Dcrl.Certificate.parseFrom(Files.readAllBytes(selfCertificate.toPath()));
    this.selfPrivateKey = Files.readAllBytes(selfPrivateKey.toPath());

    this.revocationsToProcess = new ArrayList<Dcrl.CertificateRevocation>();
    this.blockchain = new ArrayList<Dcrl.BlockMessage>();
    this.blockchain.add(Constants.GENESIS_BLOCK);
    this.lastValidatedHash = Constants.GENESIS_BLOCK_HASH;
    this.lastValidatedHeight = Constants.GENESIS_BLOCK_HEIGHT;

    this.revocationsPerBlock = 1;

    this.otherParticipantsAndAuthorities = new ArrayList<>(otherServers.keySet());
  }


  /**
   * need to make a block?
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.CertificateRevocation message,
                                        @NotNull Dcrl.Certificate from) {

    StringBuilder errorCollector = new StringBuilder();
    Consumer<String> errorConsumer = errorCollector::append;

    if (!CryptoKt.verifyVerbose(from,
        errorConsumer,
        ((ByteString bytes) -> getTrustStore().get(bytes)),
        ((ByteString bytes) -> getCurrentRevokedList().containsKey(bytes)),
        Dcrl.CertificateUsage.AUTHORITY
    )) {
      if (errorCollector.toString().contains("trusted"))
        getTrustStore().forEach((hash, cert) -> System.out.println(hash));
      return ProtocolServerUtil.buildErrorMessage("Bad revocation for certificate for " + from.getSubject() + "! " + errorCollector.toString(), selfCertificate, selfPrivateKey);
    }

    if (message.getCertificate().getIssuerCertificateHash() == Util.hash(from)) {
      return ProtocolServerUtil.buildErrorMessage("Not from the right person!", selfCertificate, selfPrivateKey);
    }

    this.revocationsToProcess.add(message);
    System.out.println("Added revocation, now storing " + this.revocationsToProcess.size());

    if (this.revocationsToProcess.size() == this.revocationsPerBlock) {

      Dcrl.BlockMessage blockMessage = Dcrl.BlockMessage.newBuilder()
          .setCertificate(this.selfCertificate)
          .setHeight((int) this.lastValidatedHeight + 1)
          .setPreviousBlock(this.lastValidatedHash)
          .setTimestamp(new Date().getTime())
          .setMerkleRoot(ByteString.copyFrom(Util.merkleRoot(this.revocationsToProcess)))
          .addAllCertificateRevocations(this.revocationsToProcess)
          .build();

      this.blockchain.add(blockMessage);
      for (Dcrl.CertificateRevocation revocation :
          this.revocationsToProcess) {
        this.getCurrentRevokedList().put(Util.hashCert(revocation.getCertificate()), revocation.getCertificate());
      }

      Dcrl.DCRLMessage messageToSend = Dcrl.DCRLMessage.newBuilder()
          .setSignedMessage(
              Dcrl.SignedMessage.newBuilder()
                  .setCertificate(this.selfCertificate)
                  .setSignature(Util.sign(blockMessage, this.selfPrivateKey))
                  .setBlockMessage(blockMessage)
          )
          .build();

      this.lastValidatedHeight += 1;
      this.lastValidatedHash = Util.hash(blockMessage);

      // need to flood messageToSend
      for (NetworkIdentity server : this.otherParticipantsAndAuthorities) {
        sendMessageToIdentity(server, messageToSend);
      }
      this.revocationsToProcess.clear();
    }
    return null;
  }

  /**
   * Receive a new block from a peer.
   * The incoming message has had its signature verified.
   * 1. Validate the Certificate (same certificate as the one in the SignedMessage)
   * 2. Check the block's height. If this Block's height is greater than 1 plus the height of the
   * Participant's last validated Block, then the Participant will need to perform Fork Resolution.
   * 3. Compare the previous block's hash with the prev hash reported in this block.
   * 4. Verify the signature of all CertificateRevocations in the block.
   * 5. accept it if we get to this step.
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.BlockMessage message,
                                        @NotNull Dcrl.Certificate from) {

    // validate the block's cert
    Dcrl.Certificate blockCertificate = message.getCertificate();
    if (!blockCertificate.equals(from)) {
      // certs do not match, send signed error
      return ProtocolServerUtil.buildErrorMessage(
          "SignedMessage's Certificate does not match BlockMessage's Certificate.",
          this.selfCertificate,
          this.selfPrivateKey);
    }

    System.out.println("Heights:");
    System.out.println(message.getHeight());
    System.out.println(this.lastValidatedHeight);

    // check height
    if (message.getHeight() <= this.lastValidatedHeight) {
      // old block or block at same height, drop, send signed error
      return ProtocolServerUtil.buildErrorMessage(
          "You sent a stale block.",
          this.selfCertificate,
          this.selfPrivateKey);
    } else if (message.getHeight() > this.lastValidatedHeight + 1) {
      // missing blocks. need to get and verify new blockchain (ez fork resolution)
      return this.requestNewBlockchain(identity);
    }

    // now we have message.getHeight() == this.lastValidatedHeight + 1

    if (this.lastValidatedHash != message.getPreviousBlock()) {
      // our prev block is the wrong block. need to request new blockchain
      return this.requestNewBlockchain(identity);
    }

    // we can't validate the signatures of the CertificateRevocations because there are no signatures :)

    this.lastValidatedHeight = (int) message.getHeight();
    this.lastValidatedHash = Util.hash(message);
    this.blockchain.add(message);

    return null;
  }

  /**
   * drop message bc we don't use it.
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.BlockResponse message,
                                        @NotNull Dcrl.Certificate from) {
    return null;
  }

  /**
   * Reply with the block they requested
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity, @NotNull Dcrl.BlockRequest message) {
    Dcrl.BlockResponse blockResponse = Dcrl.BlockResponse.newBuilder().setBlock(
        this.blockchain.get((int) message.getHeight()))
        .build();

    return Dcrl.DCRLMessage.newBuilder()
        .setSignedMessage(
            Dcrl.SignedMessage.newBuilder()
                .setCertificate(this.selfCertificate)
                .setSignature(Util.sign(blockResponse, this.selfPrivateKey))
                .setBlockResponse(blockResponse))
        .build();
  }

  /**
   * reply with blockchain
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity, @NotNull Dcrl.BlockchainRequest message) {
    Dcrl.BlockchainResponse blockchainResponse = Dcrl.BlockchainResponse.newBuilder().addAllBlocks(this.blockchain).build();

    return Dcrl.DCRLMessage.newBuilder()
        .setSignedMessage(
            Dcrl.SignedMessage.newBuilder()
                .setCertificate(this.selfCertificate)
                .setSignature(Util.sign(blockchainResponse, this.selfPrivateKey))
                .setBlockchainResponse(blockchainResponse))
        .build();
  }

  /**
   * set blockchain if it validates
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.BlockchainResponse message,
                                        @NotNull Dcrl.Certificate from) {
    // validate blockchain
    if (!this.validateBlockchain(message.getBlocksList())) {
      return ProtocolServerUtil.buildErrorMessage(
          "Invalid blockchain.",
          this.selfCertificate,
          this.selfPrivateKey
      );
    }

    this.blockchain = message.getBlocksList();

    System.out.println(this.blockchain.size());
    this.getCurrentRevokedList().clear();
    for (Dcrl.BlockMessage block :
        this.blockchain) {
      for (Dcrl.CertificateRevocation revocation :
          block.getCertificateRevocationsList()) {
        this.getCurrentRevokedList().put(Util.hashCert(revocation.getCertificate()), revocation.getCertificate());
      }
    }
    return null;
  }

  /**
   * print error
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.ErrorMessage message,
                                        @Nullable Dcrl.Certificate from) {
    System.out.println("got error" + message.getError());
    System.out.println(message.getError());
    return null;
  }

  @NotNull
  private Dcrl.DCRLMessage requestNewBlockchain(NetworkIdentity identity) {
    return Dcrl.DCRLMessage.newBuilder()
        .setUnsignedMessage(
            Dcrl.UnsignedMessage.newBuilder()
                .setBlockchainRequest(Dcrl.BlockchainRequest.getDefaultInstance()))
        .build();
  }

  /**
   * checks if the given blockchain is valid
   * <p>
   * - blocks must have a valid and trusted certificate
   * - blocks must be hashed in order
   * - blocks must be in increasing order
   */
  private boolean validateBlockchain(List<Dcrl.BlockMessage> chain) {
    if (chain.size() == 0) {
      return false;
    }

    ByteString prevBlockPrevHash = Constants.GENESIS_BLOCK_HASH;
    int prevBlockHeight = Constants.GENESIS_BLOCK_HEIGHT;

    for (int b = 1; b < chain.size(); b++) {
      Dcrl.BlockMessage block = chain.get(b);
      Dcrl.Certificate blockCert = block.getCertificate();
      ByteString blockPrevHash = block.getPreviousBlock();
      int blockHeight = (int) block.getHeight();


      Consumer<String> errorPrinter = System.err::println;
      if (!CryptoKt.verifyVerbose(blockCert,
          errorPrinter,
          ((ByteString bytes) -> getTrustStore().get(bytes)),
          ((ByteString bytes) -> getCurrentRevokedList().containsKey(bytes)),
          Dcrl.CertificateUsage.PARTICIPATION
      )) {
        System.out.println(errorPrinter.toString());
        return false;
      }

      if (!prevBlockPrevHash.equals(blockPrevHash)) {
        System.out.println("Hashes didn't match: previous block hash should have been ");
        System.out.println(prevBlockPrevHash);
        System.out.println("but instead was");
        System.out.println(blockPrevHash);
        return false;
      }

      if (prevBlockHeight + 1 != blockHeight) {
        System.out.println("Height didn't match");
        return false;
      }

      prevBlockPrevHash = Util.hash(block);
      prevBlockHeight++;
    }
    return true;
  }
}