import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class Util {

  /*
  Function for generating the Merkle root of a list of CertificateRevocations.

  Base Case: list is empty --> return byte[0]

  Uses Generative Recursion:
  - Base Case: size == 1 --> repeat the revocation and recur (immediately hitting Base Case 2) (so I don't copy code)
  - Base Case: size == 2 --> hash each revocation, concatenate, and hash again
  - Recursive Case:
    - Divide the list so the left is smaller than the input and a power of 2
    - The right is the rest of the list (right has a size of non-0)
    - Recur on each side
    - Concatenate the hashes and hash again
   */
  @NotNull
  public static byte[] merkleRoot(@NotNull List<Dcrl.CertificateRevocation> revocations) {
    if (revocations.size() == 0) {
      return new byte[0];

    } else if (revocations.size() == 1) {
      // repeat the element
      // no mutating :)
      List<Dcrl.CertificateRevocation> dup = new ArrayList<Dcrl.CertificateRevocation>();
      dup.add(revocations.get(0));
      dup.add(revocations.get(0));

      return merkleRoot(dup);

    } else if (revocations.size() == 2) {
      // hash each element, concatenate, and hash again
      HashFunction sha256 = Hashing.sha256();

      byte[] rev0_hash = sha256.newHasher()
          .putBytes(hash(revocations.get(0)))
          .hash()
          .asBytes();
      byte[] rev1_hash = sha256.newHasher()
          .putBytes(hash(revocations.get(1)))
          .hash()
          .asBytes();

      byte[] root_hash = sha256.newHasher()
          .putBytes(rev0_hash)
          .putBytes(rev1_hash)
          .hash()
          .asBytes();

      return root_hash;

    } else {
      // divide the list so the left is a power of two and the right is the rest
      double pow_2 = Math.ceil(Math.sqrt(revocations.size())); // want to round up
      int divide = (int) Math.pow(2, pow_2 - 1); // minus 1 so the left is always smaller than the input

      List<Dcrl.CertificateRevocation> left = revocations.subList(0, divide);
      List<Dcrl.CertificateRevocation> right = revocations.subList(divide, revocations.size());

      // hash each side, concatenate, and hash again
      byte[] left_hash = merkleRoot(left);
      byte[] right_hash = merkleRoot(right);

      HashFunction sha256 = Hashing.sha256();

      byte[] root_hash = sha256.newHasher()
          .putBytes(left_hash)
          .putBytes(right_hash)
          .hash()
          .asBytes();

      return root_hash;
    }
  }

  @NotNull
  public static byte[] hash(@NotNull GeneratedMessageV3 msg) {
    if (msg instanceof Dcrl.CertificateOrBuilder) {
      return hashCert((Dcrl.CertificateOrBuilder) msg);
    }

    if (msg instanceof Dcrl.BlockMessageOrBuilder) {
      return hashBlock((Dcrl.BlockMessageOrBuilder) msg);
    }

    HashFunction hf = Hashing.sha256();

    byte[] hash = hf.newHasher().putBytes(msg.toByteArray()).hash().asBytes();

    return hash;
  }

  @NotNull
  public static byte[] hashCert(@NotNull Dcrl.CertificateOrBuilder cert) {
    HashFunction hf = Hashing.sha256();

    Hasher hasher = hf.newHasher()
        .putBytes(cert.getSubjectBytes().toByteArray())
        .putLong(cert.getValidFrom())
        .putInt(cert.getValidLength());
    for (Dcrl.CertificateUsage usage : cert.getUsagesList()) {
      hasher.putInt(usage.getNumber());
    }
    hasher.putBytes(cert.getSigningPublicKey().toByteArray())
        .putBytes(cert.getIssuerCertificateHash().toByteArray())
        .putBytes(cert.getIssuerSignature().toByteArray());

    return hasher.hash().asBytes();
  }

  /*
  Function for hashing a BlockMessage
   */
  @NotNull
  public static byte[] hashBlock(@NotNull Dcrl.BlockMessageOrBuilder block) {
    HashFunction hf = Hashing.sha256();

    byte[] hash = hf.newHasher()
        .putBytes(digestForHash(block.getCertificate()))
        .putLong(block.getHeight())
        .putBytes(block.getPreviousBlock().toByteArray())
        .putLong(block.getTimestamp())
        .putBytes(block.getMerkleRoot().toByteArray())
        .hash()
        .asBytes();

    return hash;
  }

  @NotNull
  public static byte[] digestForHash(@NotNull Dcrl.CertificateOrBuilder cert) {

    ByteString digest = ByteString.EMPTY;
    digest = digest
        .concat(cert.getSubjectBytes())
        .concat(ByteString.copyFrom(Longs.toByteArray(cert.getValidFrom())))
        .concat(ByteString.copyFrom(Ints.toByteArray(cert.getValidLength())));
    for (Dcrl.CertificateUsage usage : cert.getUsagesList()) {
      digest = digest.concat(ByteString.copyFrom(Ints.toByteArray(usage.getNumber())));
    }
    digest = digest
        .concat(cert.getSigningPublicKey())
        .concat(cert.getIssuerCertificateHash())
        .concat(cert.getIssuerSignature());

    return digest.toByteArray();
  }

  /*
  Signs the message's toByteArray().
  If the message is a CertificateOrBuilder, then signs the byte digest
   */
  @NotNull
  public static byte[] sign(@NotNull GeneratedMessageV3 message,
                            byte[] private_key) {
    if (message instanceof Dcrl.CertificateOrBuilder) {
      return signCert((Dcrl.CertificateOrBuilder) message, private_key);
    }

    return CryptoKt.sign(message.toByteArray(), private_key);
  }

  @NotNull
  public static boolean verify(@NotNull Dcrl.SignedMessage signedMessage) {

    // get inner message from signedMessage
    GeneratedMessageV3 message;
    switch (signedMessage.getMessageCase()) {
      case CERTIFICATE_REVOCATION:
        message = signedMessage.getCertificateRevocation();
        break;
      case BLOCK_MESSAGE:
        message = signedMessage.getBlockMessage();
        break;
      case BLOCKCHAIN_RESPONSE:
        message = signedMessage.getBlockchainResponse();
        break;
      case BLOCK_RESPONSE:
        message = signedMessage.getBlockResponse();
        break;
      case ERROR_MESSAGE:
        message = signedMessage.getErrorMessage();
        break;
      case ANNOUNCE:
        message = signedMessage.getAnnounce();
        break;
      default: // handles MESSAGE_NOT_SET case
        return false;
    }

    // get signature from signedMessage
    byte[] signature = signedMessage.getSignature().toByteArray();
    // error checking
    if (signature.length == 0) {
      return false;
    }

    // get public key from cert
    Dcrl.Certificate cert = signedMessage.getCertificate();
    // error checking
    if (cert.equals(Dcrl.Certificate.getDefaultInstance())) {
      return false;
    }
    byte[] public_key = cert.getSigningPublicKey().toByteArray();
    if (public_key.length == 0) {
      return false;
    }

    return CryptoKt.verifySign(message.toByteArray(), signature, public_key);
  }

  @NotNull
  public static byte[] digestForSignature(@NotNull Dcrl.CertificateOrBuilder cert) {

    ByteString digest = ByteString.EMPTY;
    digest = digest
        .concat(cert.getSubjectBytes())
        .concat(ByteString.copyFrom(Longs.toByteArray(cert.getValidFrom())))
        .concat(ByteString.copyFrom(Ints.toByteArray(cert.getValidLength())));
    for (Dcrl.CertificateUsage usage : cert.getUsagesList()) {
      digest = digest.concat(ByteString.copyFrom(Ints.toByteArray(usage.getNumber())));
    }
    digest = digest
        .concat(cert.getSigningPublicKey())
        .concat(cert.getIssuerCertificateHash());
    // issuer signature does not exist yet.

    return digest.toByteArray();
  }

  @NotNull
  public static byte[] signCert(@NotNull Dcrl.CertificateOrBuilder cert, @NotNull byte[] private_key) {
    return CryptoKt.sign(digestForSignature(cert), private_key);
  }

}
