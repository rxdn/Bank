package com.perkelle.dev.bank.config

import com.google.common.io.ByteStreams
import com.perkelle.dev.bank.Bank
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.net.URL

abstract class YMLConfig(val folder: File? = JavaPlugin.getPlugin(Bank::class.java).dataFolder) {

    private lateinit var file: File
    lateinit var config: YamlConfiguration

    @Throws(IOException::class)
    fun load() {
        folder?.let {
            if(!it.exists()) it.mkdir()
        }

        val fileName = javaClass.getAnnotation(FileName::class.java).fileName
        file = if(folder == null) File(fileName)
        else File(folder, fileName)

        if(!file.exists()) file createFromResource fileName

        config = YamlConfiguration.loadConfiguration(file)

        file.save()
    }

    @Throws(IOException::class)
    fun File.save() {
        config.save(this)
    }

    @Throws(IOException::class)
    fun save() {
        file.save()
    }

    @Throws(IOException::class)
    private infix fun File.createFromResource(resourceName: String) {
        createNewFile()
        val inStream = Bank::class.java.classLoader.getResourceAsStream(resourceName)
        val outStream = outputStream()

        ByteStreams.copy(inStream, outStream)

        inStream.close()
        outStream.close()
    }

    @Suppress("UNCHECKED_CAST")
    fun<T> getConfigValue(key: String, default: T): T = config.get(key, default) as? T ?: default

    @Suppress("UNCHECKED_CAST")
    fun<T> getConfigValue(key: String): T? = getConfigValue<T?>(key, null)
}

annotation class FileName(val fileName: String = "config.yml")