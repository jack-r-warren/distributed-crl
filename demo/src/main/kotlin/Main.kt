import Dcrl.Certificate
import Util.hash
import Util.signCert
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.google.protobuf.ByteString
import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import org.apache.commons.codec.binary.Base64
import java.io.File
import java.time.Instant


const val CERT_FILE_EXT = ".cert"
const val SECRET_KEY_FILE_EXT = ".priv"
const val PUB_KEY_FILE_EXT = ".pub"

object Demo : CliktCommand() {
  override fun run() {}
}

object Query : CliktCommand() {
  private val serverAddress by argument(help = "The IP address of the server to communicate to")
  private val certPath by argument(help = "The path to the cert to check")
  override fun run() {
    val cert = Dcrl.Certificate.parseFrom(readFile(certPath))
    val hashHexString = Base64.encodeBase64URLSafeString(Util.hashCert(cert).toByteArray())
    runBlocking {
      echo(HttpClient().get<String>("http://$serverAddress/check/$hashHexString"))
    }
  }

  /**
   * Wrapper helper function to print out nicer error messages.
   * Read from a file if it exists, and return it as a ByteArray.
   */
  private fun readFile(filename: String): ByteArray {
    val file = File(filename)
    if (!file.exists()) {
      error("Failed to find file at ${file.absolutePath}")
    }
    return file.readBytes()
  }
}

object Generate : CliktCommand() {
  private val filename by argument(help = "The subject the certificate is for")
  private val overwrite by option("-f", help = "Overwrite an existing file, if it exists").flag()
  private val valid_from by option("-t", help = "Epoch timestamp (seconds) of the starting valid time")
    .long()
    .default(Instant.now().epochSecond)
  private val valid_length by option("-l", help = "How long, in seconds, should the certificate be valid for")
    .int()
    .default(300)
  private val usages by option("-u", help = "What usages should the generated certs have")
    .choice("authority", "participation")
    .multiple()
    .unique()
  private val keypair_path by option(
    "-p",
    help = "A path to the filenames of a public/private keypair to use. If not provided, one is generated"
  )
  private val issuer_cert by option(
    "-i",
    help = "A path to a protobuf certificate file to use as the issuer. A $SECRET_KEY_FILE_EXT file must exist with the same name. If not provided, then the cert will be self-signed."
  )

  override fun run() {
    val certBuilder =
      Certificate.newBuilder().setSubject(filename).setValidFrom(valid_from).setValidLength(valid_length)

    // Key signing field generation
    if (keypair_path != null) {
      certBuilder.signingPublicKey = ByteString.copyFrom(readFile("$keypair_path$PUB_KEY_FILE_EXT"))
      val secretKey = File("$keypair_path$SECRET_KEY_FILE_EXT")
      if (!secretKey.exists()) {
        echo("Warning: private key at ${secretKey.absolutePath} does not exist!", err = true)
      }
    } else {
      val sodium = LazySodiumJava(SodiumJava())
      val keypair = sodium.cryptoSignKeypair()
      certBuilder.signingPublicKey = ByteString.copyFrom(keypair.publicKey.asBytes)
      writeSafely("$filename$SECRET_KEY_FILE_EXT", keypair.secretKey.asBytes)
      writeSafely("$filename$PUB_KEY_FILE_EXT", keypair.publicKey.asBytes)
    }

    certBuilder.addAllUsages(usages.map {
      when (it) {
        "authority" -> Dcrl.CertificateUsage.AUTHORITY
        "participation" -> Dcrl.CertificateUsage.PARTICIPATION
        else -> error("Unknown usage $it") // Should never reach here
      }
    })

    if (issuer_cert != null) {
      val certFile = readFile("$issuer_cert$CERT_FILE_EXT")
      val secretKey = readFile("$issuer_cert$SECRET_KEY_FILE_EXT")
      certBuilder.issuerCertificateHash = hash(Certificate.parseFrom(certFile))
      certBuilder.issuerSignature = signCert(certBuilder, secretKey)
    } else {
      val secretKey = readFile("$filename$SECRET_KEY_FILE_EXT")
      certBuilder.issuerSignature = signCert(certBuilder, secretKey)
    }

    val cert = certBuilder.build()
    writeSafely("$filename.cert", cert.toByteArray())
  }

  /**
   * Wrapper helper function to print out nicer error messages.
   * Writes to a file if and only if it doesn't already exist, or only if the overwrite flag is set.
   */
  private fun writeSafely(filename: String, data: ByteArray) {
    val file = File(filename)
    if (file.exists() && !overwrite) {
      error("File exists at ${file.absolutePath} but overwrite flag not set. quitting!")
    }
    file.writeBytes(data)
  }

  /**
   * Wrapper helper function to print out nicer error messages.
   * Read from a file if it exists, and return it as a ByteArray.
   */
  private fun readFile(filename: String): ByteArray {
    val file = File(filename)
    if (!file.exists()) {
      error("Failed to find file at ${file.absolutePath}")
    }
    return file.readBytes()
  }
}

object Revoke : CliktCommand() {
  private val serverAddress by argument(help = "The IP address of the server to communicate to")
  private val certPath by argument(help = "The path to the cert to revoke")
  override fun run() {
    val fileHashString = Base64.encodeBase64URLSafeString(readFile(certPath))
    runBlocking {
      echo(HttpClient().get<String>("http://$serverAddress/revoke/$fileHashString"))
    }
  }

  /**
   * Wrapper helper function to print out nicer error messages.
   * Read from a file if it exists, and return it as a ByteArray.
   */
  private fun readFile(filename: String): ByteArray {
    val file = File(filename)
    if (!file.exists()) {
      error("Failed to find file at ${file.absolutePath}")
    }
    return file.readBytes()
  }
}

fun main(args: Array<String>) = Demo.subcommands(Query, Generate, Revoke).main(args)