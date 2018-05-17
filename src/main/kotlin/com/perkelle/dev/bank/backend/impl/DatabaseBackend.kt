package com.perkelle.dev.bank.backend.impl

import com.perkelle.dev.bank.backend.StoreBackend
import com.perkelle.dev.bank.config.getConfig
import com.perkelle.dev.bank.utils.Callback
import com.perkelle.dev.bank.utils.onComplete
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
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
            config.addDataSourceProperty("maxIdle", 1)
            config.maximumPoolSize = 5

            ds = HikariDataSource(config)

            Database.connect(ds)
            SchemaUtils.create(BankTable, UUIDTable)
            true
        } catch(ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    override fun deposit(p: UUID, amount: Double) {
        getBalance(p) { current ->
            launch {
                BankTable.upsert(listOf(BankTable.balance)) {
                    it[uuid] = p.toString()
                    it[balance] = current.toBigDecimal() + amount.toBigDecimal()
                }
            }
        }
    }

    override fun withdraw(p: UUID, amount: Double) {
        getBalance(p) { current ->
            launch {
                BankTable.upsert(listOf(BankTable.balance)) {
                    it[uuid] = p.toString()
                    it[balance] = current.toBigDecimal() - amount.toBigDecimal()
                }
            }
        }
    }

    override fun getBalance(p: UUID, callback: Callback<Double>) {
        async {
            BankTable.select {
                BankTable.uuid eq p.toString()
            }.map { it[BankTable.balance] }.firstOrNull()
        }.onComplete {
            callback(it?.toDouble() ?: 0.0)
        }
    }

    override fun getUUID(name: String, callback: Callback<UUID?>) {
        async {
            UUIDTable.select {
                UUIDTable.name eq name
            }.map { it[UUIDTable.uuid] }.firstOrNull()
        }.onComplete {
            if(it == null) callback(null)
            else callback(UUID.fromString(it))
        }
    }

    override fun setUUID(name: String, uuid: UUID) {
        launch {
            UUIDTable.upsert(listOf(UUIDTable.uuid, UUIDTable.name)) {
                it[this.uuid] = uuid.toString()
                it[this.name] = name
            }
        }
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