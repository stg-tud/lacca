package kanban.persistence

import org.scalablytyped.runtime.StringDictionary
import typings.dexie.mod.Dexie

object DexieDB {
    val dexieDB: Dexie = new Dexie.^("kanban")
    dexieDB.version(1).stores(
        StringDictionary(
            ("projectcrdts", "id"),
            ("projects", "++id"),
            ("users", "++id")
        )
    )
}
