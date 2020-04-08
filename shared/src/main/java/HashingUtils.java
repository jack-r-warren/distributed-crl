import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.protobuf.GeneratedMessageV3;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class HashingUtils {

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
  public static byte[] merkleRoot(List<Dcrl.CertificateRevocation> revocations) {
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

  public static byte[] hash(GeneratedMessageV3 msg) {
    // TODO is this the right way of doing this?
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

  private static byte[] hashCert(Dcrl.CertificateOrBuilder cert) {
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
  private static byte[] hashBlock(Dcrl.BlockMessageOrBuilder block) {
    HashFunction hf = Hashing.sha256();

    byte[] hash = hf.newHasher()
        .putBytes(hash(block.getCertificate()))
        .putLong(block.getHeight())
        .putBytes(block.getPreviousBlock().toByteArray())
        .putLong(block.getTimestamp())
        // TODO do I need to handle case of no Merkle root and I need to generate it?
        .putBytes(block.getMerkleRoot().toByteArray())
        .hash()
        .asBytes();

    return hash;
  }

}
