package kanban.auth

import kanban.persistence.DexieDB.dexieDB
import typings.dexie.mod.Table
import ucan.{Ed25519KeyMaterial, KeyMaterial, Base32}
import com.raquo.airstream.state.Var

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scala.scalajs.js
import scala.concurrent.Future

object KeyPair {

  val publicKey: Var[Option[Array[Byte]]] = Var(None)
  val privateKey: Var[Option[Array[Byte]]] = Var(None)

  trait KeyPairTableEntry extends js.Object {
    val keyId: Int
    val publicKey: String
    val privateKey: String
  }

  private val keyPairTable: Table[KeyPairTableEntry, Int, KeyPairTableEntry] =
    dexieDB.table("keyPairs")

  private def getKeyPairFromDB: Future[Option[KeyPairTableEntry]] = {
    keyPairTable.get(0).toFuture.map(_.toOption)
  }

  // If a key pair exists, set the public and private keys
  getKeyPairFromDB.onComplete {
    case Success(Some(entry)) =>
      publicKey.set(Some(Base32.decode(entry.publicKey)))
      privateKey.set(Some(Base32.decode(entry.privateKey)))
      println(s"Found key pair in database: Public Key: ${publicKey.now().getOrElse("None")}, Private Key: ${privateKey.now().getOrElse("None")}")

    case Success(None) =>
      println("No key pair found in the database, generating a new one.")
      // If no key pair exists, generate a new one
      Ed25519KeyMaterial.create().onComplete {
        case Success(km) =>
          println(s"Generated new key pair: Public Key: ${
            Base32.encode(km.publicKey)
          }}, Private Key: ${
            km.privateKey.map(Base32.encode).
              getOrElse("None")
          }")

          // Set the public and private keys in Vars
          publicKey.set(Some(km.publicKey))
          privateKey.set(km.privateKey)


          /*Saving raw keys to the database isn't secure*/
          // Save the key pair to the database
          val entry: KeyPairTableEntry = new KeyPairTableEntry {
            val keyId: Int = 0
            val publicKey: String = Base32.encode(km.publicKey)
            val privateKey: String = km.privateKey match {
              case Some(pk) => Base32.encode(pk)
              case None => Base32.encode(Array.emptyByteArray)
            }
          }
          keyPairTable.add(entry).toFuture.onComplete {
            case Success(_) => println("New key pair added to database")
            case Failure(e) => println(s"Failed to add key pair to database: ${e.getMessage}")
          }


        case Failure(exception) =>
          println(s"Error generating key pair: ${exception.getMessage}")
      }

    case Failure(exception) =>
      println(s"Error retrieving key pair from database: ${exception.getMessage}")
  }
}
