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

abstract public class ParticipantJavaAbstract extends ObserverRoleServer {
  protected final Dcrl.Certificate selfCertificate;
  protected final byte[] selfPrivateKey;

  protected List<Dcrl.CertificateRevocation> revocationsToProcess;
  // inherits this.blockchain
  protected byte[] lastValidatedHash;
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

    this.revocationsPerBlock = 4;
  }


  /**
   * need to make a block?
   *
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.CertificateRevocation message,
                                        @NotNull Dcrl.Certificate from) {

    this.revocationsToProcess.add(message);

    if (this.revocationsToProcess.size() == this.revocationsPerBlock) {

      Dcrl.BlockMessage blockMessage = Dcrl.BlockMessage.newBuilder()
          .setCertificate(this.selfCertificate)
          .setHeight((int) this.lastValidatedHeight + 1)
          .setPreviousBlock(ByteString.copyFrom(this.lastValidatedHash))
          .setTimestamp(new Date().getTime())
          .setMerkleRoot(ByteString.copyFrom(Util.merkleRoot(this.revocationsToProcess)))
          .addAllCertificateRevocations(this.revocationsToProcess)
          .build();

      Dcrl.DCRLMessage messageToSend = Dcrl.DCRLMessage.newBuilder()
          .setSignedMessage(
              Dcrl.SignedMessage.newBuilder()
                .setCertificate(this.selfCertificate)
                .setSignature(ByteString.copyFrom(Util.sign(blockMessage, this.selfPrivateKey)))
                .setBlockMessage(blockMessage)
          )
          .build();

      this.lastValidatedHeight += 1;
      this.lastValidatedHash = Util.sign(blockMessage, this.selfPrivateKey);

      return messageToSend;
    }
    return null;
  }

  /**
   * Receive a new block from a peer.
   * The incoming message has had its signature verified.
   * 1. Validate the Certificate (same certificate as the one in the SignedMessage)
   * 2. Check the block's height. If this Block's height is greater than 1 plus the height of the
   *    Participant's last validated Block, then the Participant will need to perform Fork Resolution.
   * 3. Compare the previous block's hash with the prev hash reported in this block.
   * 4. Verify the signature of all CertificateRevocations in the block.
   * 5. accept it if we get to this step.
   *
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.BlockMessage message,
                                        @NotNull Dcrl.Certificate from) {

    // validate the block's cert
    Dcrl.Certificate blockCertificate = message.getCertificate();
    // TODO verify the cert's signature
    // TODO check that cert is trusted
    if (! blockCertificate.equals(from)) {
      // certs do not match, send signed error
      return ProtocolServerUtil.buildErrorMessage(
          "SignedMessage's Certificate does not match BlockMessage's Certificate.",
          this.selfCertificate,
          this.selfPrivateKey);
    }

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

    if (this.lastValidatedHash != message.getPreviousBlock().toByteArray()) {
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
   *
   *
   * drop message bc we don't use it.
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.BlockResponse message,
                                        @NotNull Dcrl.Certificate from) {
    return null;
  }

  // TODO we never actually use this method... should we overwrite with return null?
  /**
   * Reply with the block they requested
   *
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity, @NotNull Dcrl.BlockRequest message) {
    Dcrl.BlockResponse blockResponse = Dcrl.BlockResponse.newBuilder().setBlock(
        this.blockchain.get((int) message.getHeight()))
        .build();

    Dcrl.DCRLMessage response = Dcrl.DCRLMessage.newBuilder()
        .setSignedMessage(
            Dcrl.SignedMessage.newBuilder()
                .setCertificate(this.selfCertificate)
                .setSignature(ByteString.copyFrom(Util.sign(blockResponse, this.selfPrivateKey)))
                .setBlockResponse(blockResponse))
        .build();

    return response;
  }

  /**
   * reply with blockchain
   *
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,@NotNull Dcrl.BlockchainRequest message) {
    Dcrl.BlockchainResponse blockchainResponse = Dcrl.BlockchainResponse.newBuilder().addAllBlocks(this.blockchain).build();

    Dcrl.DCRLMessage response = Dcrl.DCRLMessage.newBuilder()
        .setSignedMessage(
            Dcrl.SignedMessage.newBuilder()
            .setCertificate(this.selfCertificate)
            .setSignature(ByteString.copyFrom(Util.sign(blockchainResponse, this.selfPrivateKey)))
            .setBlockchainResponse(blockchainResponse))
        .build();

    return response;
  }

  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.Announce message,
                                        @NotNull Dcrl.Certificate from) {
    // TODO not sure what to do?
    return super.handleMessage(identity, message, from);
  }

  /**
   * set blockchain if it validates
   *
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
    return null;
  }

  /**
   * print error
   *
   */
  @Nullable
  @Override
  public Dcrl.DCRLMessage handleMessage(@NotNull NetworkIdentity identity,
                                        @NotNull Dcrl.ErrorMessage message,
                                        @Nullable Dcrl.Certificate from) {
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
   *
   * - blocks must have a valid and trusted certificate
   * - blocks must be hashed in order
   * - blocks must be in increasing order
   */
  @NotNull
  private boolean validateBlockchain(List<Dcrl.BlockMessage> chain) {
    if (chain.size() == 0) {
      return false;
    }

    byte[] prevBlockPrevHash = Constants.GENESIS_BLOCK_HASH;
    int prevBlockHeight = Constants.GENESIS_BLOCK_HEIGHT;

    for (int b = 1; b < chain.size(); b++) {
      Dcrl.BlockMessage block = chain.get(b);
      Dcrl.Certificate blockCert = block.getCertificate();
      byte[] blockPrevHash = block.getPreviousBlock().toByteArray();
      int blockHeight = (int) block.getHeight();

      // TODO validate cert
      // TODO check cert is trusted

      if (prevBlockPrevHash != blockPrevHash) {
        return false;
      }

      if (prevBlockHeight + 1 != blockHeight) {
        return false;
      }
    }
    return true;
  }
}