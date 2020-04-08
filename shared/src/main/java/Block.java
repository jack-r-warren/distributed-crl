import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.List;

public class Block {

  /*
  Class representing a Block
   */

  public final Certificate CREATOR;
  public final int HEIGHT;
  public final byte[] PREVIOUS_BLOCK_HASH;
  public final long TIMESTAMP;
  public final byte[] MERKLE_ROOT;
  public final List<CertificateRevocation> REVOCATIONS;

  public Block(Certificate creator, int height, byte[] previous_block_hash, long timestamp,
               List<CertificateRevocation> revocations) {
    CREATOR = creator;
    HEIGHT = height;
    PREVIOUS_BLOCK_HASH = previous_block_hash;
    TIMESTAMP = timestamp;
    REVOCATIONS = revocations;
    MERKLE_ROOT = this.merkle_root(); //TODO is this OK? method call using this.REVOCATIONS in constructor?

  }

  /*
  hashes this Block over its header (creator, height, previous_block_hash, timestamp, and merkle_root)
   */
  public byte[] hash() {
    HashFunction hf = Hashing.sha256();

    byte[] hash = hf.newHasher()
        .putBytes(this.CREATOR.hash())
        .putInt(this.HEIGHT)
        .putBytes(this.PREVIOUS_BLOCK_HASH)
        .putLong(this.TIMESTAMP)
        .putBytes(this.MERKLE_ROOT)
        .hash()
        .asBytes();

    return hash;
  }

  /*
  Calculates the Merkle root of the Block's list of revocations.
  If the list is empty, then the hash is empty
   */
  public byte[] merkle_root() {
    if (this.REVOCATIONS.size() == 0) {
      return new byte[0];
    } else {
      return merkle_root_helper(this.REVOCATIONS);
    }
  }

  /*
  recursive helper for merkle_root

  Uses generative recursion.
  - size will always be non-zero
  - Base case size == 1: repeat the revocation and recur (so I don't copy code)
  - Base case size == 2: hash each revocation, concatenate, and hash again
  - Recursive case:
    - divide the list so the left is smaller than the input and a power of 2.
    - the right is the rest of the list.
    - recur on each side
    - concatenate the hashes and return the hash
   */
  private byte[] merkle_root_helper(List<CertificateRevocation> revocations) {
    if (revocations.size() == 1) {
      // repeat the element
      revocations.add(revocations.get(0));
      return merkle_root_helper(revocations);

    } else if (revocations.size() == 2) {

      // hash each element, concatenate, and hash again
      HashFunction sha256 = Hashing.sha256();

      byte[] rev0_hash = sha256.newHasher()
          .putBytes(revocations.get(0).hash())
          .hash()
          .asBytes();
      byte[] rev1_hash = sha256.newHasher()
          .putBytes(revocations.get(1).hash())
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

      List<CertificateRevocation> left = revocations.subList(0, divide);
      List<CertificateRevocation> right = revocations.subList(divide, revocations.size());

      // hash each side, concatenate, and hash again
      byte[] left_hash = merkle_root_helper(left);
      byte[] right_hash = merkle_root_helper(right);

      HashFunction sha256 = Hashing.sha256();

      byte[] root_hash = sha256.newHasher()
          .putBytes(left_hash)
          .putBytes(right_hash)
          .hash()
          .asBytes();

      return root_hash;
    }
  }
}
