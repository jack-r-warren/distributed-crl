import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;

import java.util.ArrayList;

public class Constants {

  public static final Dcrl.Certificate GENESIS_BLOCK_CERTIFICATE = Dcrl.Certificate.getDefaultInstance();
  public static final int GENESIS_BLOCK_HEIGHT = 0;
  public static final byte[] GENESIS_BLOCK_PREV = new byte[32];
  public static final long GENESIS_BLOCK_TIMESTAMP = 1586466355631L;
  public static final byte[] GENESIS_BLOCK_MERKLE_ROOT = Util.merkleRoot(new ArrayList<Dcrl.CertificateRevocation>());

  public static final Dcrl.BlockMessage GENESIS_BLOCK = Dcrl.BlockMessage.newBuilder()
      .setCertificate(GENESIS_BLOCK_CERTIFICATE)
      .setHeight(GENESIS_BLOCK_HEIGHT)
      .setPreviousBlock(ByteString.copyFrom(GENESIS_BLOCK_PREV)) // array of all 0s same size as sha256 hash
      .setTimestamp(GENESIS_BLOCK_TIMESTAMP)
      .setMerkleRoot(ByteString.copyFrom(GENESIS_BLOCK_MERKLE_ROOT))
      .build();

  public static final byte[] GENESIS_BLOCK_HASH = Util.hash(GENESIS_BLOCK);

}
