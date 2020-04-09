import com.google.common.primitives.Ints;
import com.google.protobuf.ByteString;

import java.util.ArrayList;

public class Constants {

  public static final Dcrl.BlockMessage GENESIS_BLOCK = Dcrl.BlockMessage.newBuilder()
      .setCertificate(Dcrl.Certificate.getDefaultInstance())
      .setHeight(0)
      .setPreviousBlock(ByteString.copyFrom(new byte[32])) // array of all 0s same size as sha256 hash
      .setTimestamp(1586466355631L)
      .setMerkleRoot(ByteString.copyFrom(Util.merkleRoot(new ArrayList<Dcrl.CertificateRevocation>())))
      .build();

  public static final byte[] GENESIS_BLOCK_HASH = Util.hash(GENESIS_BLOCK);
}
