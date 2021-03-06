/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package docs.persistence

//#plugin-imports

import akka.actor.ActorSystem
import akka.persistence._
import akka.persistence.journal._
import akka.persistence.snapshot._
import akka.testkit.TestKit
import com.typesafe.config._
import org.scalatest.WordSpec

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration._
//#plugin-imports

object PersistencePluginDocSpec {
  val config =
    """
      //#max-message-batch-size
      akka.persistence.journal.max-message-batch-size = 200
      //#max-message-batch-size
      //#journal-config
      akka.persistence.journal.leveldb.dir = "target/journal"
      //#journal-config
      //#snapshot-config
      akka.persistence.snapshot-store.local.dir = "target/snapshots"
      //#snapshot-config
      //#native-config
      akka.persistence.journal.leveldb.native = off
      //#native-config
    """
}

class PersistencePluginDocSpec extends WordSpec {
  new AnyRef {
    val providerConfig =
      """
        //#journal-plugin-config
        # Path to the journal plugin to be used
        akka.persistence.journal.plugin = "my-journal"

        # My custom journal plugin
        my-journal {
          # Class name of the plugin.
          class = "docs.persistence.MyJournal"
          # Dispatcher for the plugin actor.
          plugin-dispatcher = "akka.actor.default-dispatcher"
        }
        //#journal-plugin-config

        //#snapshot-store-plugin-config
        # Path to the snapshot store plugin to be used
        akka.persistence.snapshot-store.plugin = "my-snapshot-store"

        # My custom snapshot store plugin
        my-snapshot-store {
          # Class name of the plugin.
          class = "docs.persistence.MySnapshotStore"
          # Dispatcher for the plugin actor.
          plugin-dispatcher = "akka.persistence.dispatchers.default-plugin-dispatcher"
        }
        //#snapshot-store-plugin-config
      """

    val system = ActorSystem("PersistencePluginDocSpec", ConfigFactory.parseString(providerConfig).withFallback(ConfigFactory.parseString(PersistencePluginDocSpec.config)))
    try {
      Persistence(system)
    } finally {
      TestKit.shutdownActorSystem(system, 10.seconds, false)
    }
  }
}

object SharedLeveldbPluginDocSpec {
  import akka.actor._
  import akka.persistence.journal.leveldb.SharedLeveldbJournal

  val config =
    """
      //#shared-journal-config
      akka.persistence.journal.plugin = "akka.persistence.journal.leveldb-shared"
      //#shared-journal-config
      //#shared-store-native-config
      akka.persistence.journal.leveldb-shared.store.native = off
      //#shared-store-native-config
      //#shared-store-config
      akka.persistence.journal.leveldb-shared.store.dir = "target/shared"
      //#shared-store-config
    """

  //#shared-store-usage
  trait SharedStoreUsage extends Actor {
    override def preStart(): Unit = {
      context.actorSelection("akka.tcp://example@127.0.0.1:2552/user/store") ! Identify(1)
    }

    def receive = {
      case ActorIdentity(1, Some(store)) =>
        SharedLeveldbJournal.setStore(store, context.system)
    }
  }
  //#shared-store-usage
}

trait SharedLeveldbPluginDocSpec {
  val system: ActorSystem

  new AnyRef {
    import akka.actor._
    import akka.persistence.journal.leveldb.SharedLeveldbStore

    val store = system.actorOf(Props[SharedLeveldbStore], "store")
    //#shared-store-creation
  }
}

class MyJournal extends AsyncWriteJournal {
  def asyncWriteMessages(messages: Seq[PersistentRepr]): Future[Unit] = ???
  def asyncWriteConfirmations(confirmations: Seq[PersistentConfirmation]): Future[Unit] = ???
  def asyncDeleteMessages(messageIds: Seq[PersistentId], permanent: Boolean): Future[Unit] = ???
  def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long, permanent: Boolean): Future[Unit] = ???
  def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(replayCallback: (PersistentRepr) => Unit): Future[Unit] = ???
  def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = ???
}

class MySnapshotStore extends SnapshotStore {
  def loadAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Option[SelectedSnapshot]] = ???
  def saveAsync(metadata: SnapshotMetadata, snapshot: Any): Future[Unit] = ???
  def saved(metadata: SnapshotMetadata): Unit = ???
  def delete(metadata: SnapshotMetadata): Unit = ???
  def delete(persistenceId: String, criteria: SnapshotSelectionCriteria): Unit = ???
}

object PersistenceTCKDoc {
  new AnyRef {
    import akka.persistence.journal.JournalSpec

    //#journal-tck-scala
    class MyJournalSpec extends JournalSpec {
      override val config = ConfigFactory.parseString(
        """
          |akka.persistence.journal.plugin = "my.journal.plugin"
        """.stripMargin)
    }
    //#journal-tck-scala
  }
  new AnyRef {
    import akka.persistence.snapshot.SnapshotStoreSpec

    //#snapshot-store-tck-scala
    class MySnapshotStoreSpec extends SnapshotStoreSpec {
      override val config = ConfigFactory.parseString(
        """
          |akka.persistence.snapshot-store.plugin = "my.snapshot-store.plugin"
        """.stripMargin)
    }
    //#snapshot-store-tck-scala
  }
  new AnyRef {
    import java.io.File

    import akka.persistence.journal.JournalSpec
    import org.iq80.leveldb.util.FileUtils

    //#journal-tck-before-after-scala
    class MyJournalSpec extends JournalSpec {
      override val config = ConfigFactory.parseString(
        """
          |akka.persistence.journal.plugin = "my.journal.plugin"
        """.stripMargin)

      val storageLocations = List(
        new File(system.settings.config.getString("akka.persistence.journal.leveldb.dir")),
        new File(config.getString("akka.persistence.snapshot-store.local.dir")))

      override def beforeAll() {
        super.beforeAll()
        storageLocations foreach FileUtils.deleteRecursively
      }

      override def afterAll() {
        storageLocations foreach FileUtils.deleteRecursively
        super.afterAll()
      }

    }
    //#journal-tck-before-after-scala
  }
}