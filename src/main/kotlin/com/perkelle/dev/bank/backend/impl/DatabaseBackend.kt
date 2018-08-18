package com.perkelle.dev.bank.backend.impl

import com.perkelle.dev.bank.backend.StoreBackend
import com.perkelle.dev.bank.config.getConfig
import com.perkelle.dev.bank.utils.Callback
import com.perkelle.dev.bank.utils.getBalance
import com.perkelle.dev.bank.utils.onComplete
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class DatabaseBackend: StoreBackend {

    private lateinit var ds: HikariDataSource

    private object BankTable: Table("${getConfig().getDatabaseDetails().tablePrefix}bankbalances") {
        val uuid = varchar("uuid", 36).uniqueIndex().primaryKey()
        val balance = decimal("balance", 10, 2)
    }

    private object UUIDTable: Table("${getConfig().getDatabaseDetails().tablePrefix}uuids") {
        val uuid = varchar("uuid", 36).uniqueIndex().primaryKey()
        val name = varchar("name", 16).uniqueIndex()
    }

    override fun setup(): Boolean {
        return try {
            val config = HikariConfig()
            config.jdbcUrl = "jdbc:mysql://${getConfig().getDatabaseDetails().host}:${getConfig().getDatabaseDetails().port}/${getConfig().getDatabaseDetails().database}"
            config.username = getConfig().getDatabaseDetails().username
            config.password = getConfig().getDatabaseDetails().password
            config.addDataSourceProperty("autoReconnect", true)
            config.addDataSourceProperty("useJDBCCompliantTimezoneShift", true)
            config.addDataSourceProperty("serverTimezone", "UTC")
            config.addDataSourceProperty("useLegacyDatetimeCode", false)
            config.addDataSourceProperty("cachePrepStmts", true)
            config.addDataSourceProperty("prepStmtCacheSize", 250)
            config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048)
            config.addDataSourceProperty("maxIdle", 4)
            config.maximumPoolSize = 5

            ds = HikariDataSource(config)

            Database.connect(ds)
            transaction {
                SchemaUtils.create(BankTable, UUIDTable)
            }
            true
        } catch(ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    override fun deposit(p: UUID, amount: Double) {
        getBalance(p) { current ->
            launch {
                transaction {
                    BankTable.upsert(listOf(BankTable.balance)) {
                        it[uuid] = p.toString()
                        it[balance] = current.toBigDecimal() + amount.toBigDecimal()
                    }
                }
            }
        }
    }

    override fun withdraw(p: UUID, amount: Double) {
        getBalance(p) { current ->
            launch {
                transaction {
                    BankTable.upsert(listOf(BankTable.balance)) {
                        it[uuid] = p.toString()
                        it[balance] = current.toBigDecimal() - amount.toBigDecimal()
                    }
                }
            }
        }
    }

    override fun setAmount(p: UUID, amount: Double) {
        launch {
            transaction {
                BankTable.upsert(listOf(BankTable.balance)) {
                    it[uuid] = p.toString()
                    it[balance] = amount.toBigDecimal()
                }
            }
        }
    }

    override fun getBalance(p: UUID, callback: Callback<Double>) {
        async {
            transaction {
                BankTable.select {
                    BankTable.uuid eq p.toString()
                }.map { it[BankTable.balance] }.firstOrNull()
            }
        }.onComplete {
            callback(it?.toDouble() ?: 0.0)
        }
    }

    override fun getUUID(name: String, callback: Callback<UUID?>) {
        async {
            transaction {
                UUIDTable.select {
                    UUIDTable.name eq name
                }.map { it[UUIDTable.uuid] }.firstOrNull()
            }
        }.onComplete {
            if(it == null) callback(null)
            else callback(UUID.fromString(it))
        }
    }

    override fun setUUID(name: String, uuid: UUID) {
        launch {
            transaction {
                UUIDTable.select {
                    UUIDTable.name eq name
                }.map { it[UUIDTable.name] to it[UUIDTable.uuid] }.filter { it.second == uuid.toString() }.filter { it.first != name }.forEach {
                    UUIDTable.deleteWhere {
                        UUIDTable.name eq it.first
                    }
                }

                UUIDTable.upsert(listOf(UUIDTable.uuid, UUIDTable.name)) {
                    it[this.uuid] = uuid.toString()
                    it[this.name] = name
                }
            }
        }
    }

    override fun getTop10(callback: Callback<List<Pair<String, Double>>>) {
        async {
            transaction {
                val uuids = UUIDTable.selectAll().map { it[UUIDTable.name] to it[UUIDTable.uuid] }

                val all = BankTable.selectAll().mapNotNull { row ->
                    val uuid = row[BankTable.uuid]
                    val bankBalance = row[BankTable.balance]

                    val p = Bukkit.getOfflinePlayer(UUID.fromString(uuid))
                    if(p == null || p.name == null) return@mapNotNull null

                    val ecoBalance = p.getBalance()

                    uuids.first { (_, puuid) -> puuid == uuid }.first to bankBalance.toDouble() + ecoBalance
                }.sortedByDescending { it.second }

                val top10 by lazy {
                    if(all.size > 10) all.subList(0, 10)
                    else all.subList(0, all.size)
                }

                top10
            }
        }.onComplete(callback)
    }

    override fun shutdown() {
        ds.close()
    }

    //Support upserts
    fun <T:Table> T.upsert(uniqueColumns: List<Column<*>>, body: T.(UpsertStatement<Number>) -> Unit): UpsertStatement<Number> = UpsertStatement<Number>(this, uniqueColumns).apply {
        body(this)
        execute(TransactionManager.current())
    }

    class UpsertStatement<Key: Any>(table: Table, val onDupUpdate: List<Column<*>>): InsertStatement<Key>(table, false) {
        override fun prepareSQL(transaction: Transaction): String {
            val onUpdateSQL = if(onDupUpdate.isNotEmpty()) {
                " ON DUPLICATE KEY UPDATE " + onDupUpdate.joinToString { "${transaction.identity(it)}=VALUES(${transaction.identity(it)})" }
            } else ""
            return super.prepareSQL(transaction) + onUpdateSQL
        }
    }
}