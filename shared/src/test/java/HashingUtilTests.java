import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HashingUtilTests {

  public static void main(String[] args) throws IOException {
    Hashable rev0 = new Example(0);
    Hashable rev1 = new Example(1);
    Hashable rev2 = new Example(2);
    Hashable rev3 = new Example(3);
    Hashable rev4 = new Example(4);
    Hashable rev5 = new Example(5);
    Hashable rev6 = new Example(6);

    List<Hashable> test1 = new ArrayList<Hashable>();
    test1.add(rev0);
    test1.add(rev1);
    test1.add(rev2);
    test1.add(rev3);
    test1.add(rev4);
    test1.add(rev5);
    test1.add(rev6);
    System.out.print("Test1: ");
    System.out.print(merkleRoot(test1));
    System.out.println();

    List<Hashable> test2 = new ArrayList<Hashable>();
    test2.add(rev0);
    test2.add(rev1);
    test2.add(rev2);
    test2.add(rev3);
    test2.add(rev4);
    test2.add(rev5);
    System.out.print("Test2: ");
    System.out.print(merkleRoot(test2));
    System.out.println();

    List<Hashable> test3 = new ArrayList<Hashable>();
    test3.add(rev0);
    test3.add(rev1);
    test3.add(rev2);
    test3.add(rev3);
    test3.add(rev4);
    System.out.print("Test3: ");
    System.out.print(merkleRoot(test3));
    System.out.println();

    List<Hashable> test4 = new ArrayList<Hashable>();
    test4.add(rev0);
    test4.add(rev1);
    test4.add(rev2);
    test4.add(rev3);
    test4.add(rev4);
    System.out.print("Test4: ");
    System.out.print(merkleRoot(test4));
    System.out.println();

    List<Hashable> test5 = new ArrayList<Hashable>();
    test5.add(rev0);
    test5.add(rev1);
    test5.add(rev2);
    test5.add(rev3);
    System.out.print("Test5: ");
    System.out.print(merkleRoot(test5));
    System.out.println();

    List<Hashable> test6 = new ArrayList<Hashable>();
    test6.add(rev0);
    test6.add(rev1);
    test6.add(rev2);
    System.out.print("Test6: ");
    System.out.print(merkleRoot(test6));
    System.out.println();

    List<Hashable> test7 = new ArrayList<Hashable>();
    test7.add(rev0);
    test7.add(rev1);
    System.out.print("Test7: ");
    System.out.print(merkleRoot(test7));
    System.out.println();

    List<Hashable> test8 = new ArrayList<Hashable>();
    test8.add(rev0);
    System.out.print("Test8: ");
    System.out.print(merkleRoot(test8));
    System.out.println();

    List<Hashable> test9 = new ArrayList<Hashable>();
    System.out.print("Test9: ");
    System.out.print(merkleRoot(test9));
    System.out.println();

  }

  // wanted to test Merkle root function without generating a bunch of real Certificate Revocations
  public static byte[] merkleRoot(List<Hashable> revocations) {
    if (revocations.size() == 0) {
      return new byte[0];

    } else if (revocations.size() == 1) {
      // repeat the element
      // no mutating :)
      List<Hashable> dup = new ArrayList<Hashable>();
      dup.add(revocations.get(0));
      dup.add(revocations.get(0));

      return merkleRoot(dup);

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

      List<Hashable> left = revocations.subList(0, divide);
      List<Hashable> right = revocations.subList(divide, revocations.size());

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
}

interface Hashable {
  byte[] hash();
}

class Example implements Hashable {

  private int i;

  public Example(int i) {
    this.i = i;
  }

  public byte[] hash() {
    HashFunction hf = Hashing.sha256();
    return hf.newHasher().putInt(this.i).hash().asBytes();
  }
}
