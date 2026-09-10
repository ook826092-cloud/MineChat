package cn.mine.minestars.data.sync

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cn.mine.minestars.data.db.AppDatabase
import cn.mine.minestars.data.datastore.Settings
import cn.mine.minestars.data.datastore.SettingsStore
import cn.mine.minestars.utils.JsonInstant
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LocalBackupSync(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val appDatabase: AppDatabase,
) {
    companion object {
        private const val APP_DB_NAME = "minechat"
        private const val SETTINGS_FILE = "settings.json"
    }

    suspend fun prepareBackupFile(): File = withContext(Dispatchers.IO) {
        // 先 checkpoint，确保 .db 文件自一致（不再需要 -wal/-shm）
        runCatching {
            appDatabase.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
        }

        val cacheDir = File(context.cacheDir, "backup")
        cacheDir.mkdirs()
        val backupFile = File(cacheDir, "minechat_backup_${System.currentTimeMillis()}.zip")

        ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
            // settings.json
            val settings = settingsStore.settingsFlow.value
            val settingsJson = JsonInstant.encodeToString(settings)
            zos.putNextEntry(ZipEntry(SETTINGS_FILE))
            zos.write(settingsJson.toByteArray())
            zos.closeEntry()

            // AppDatabase — minechat.db
            writeDbToZip(zos, APP_DB_NAME)

            // files 目录
            val filesDir = context.filesDir
            if (filesDir.exists()) {
                filesDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val relativePath = file.relativeTo(filesDir).path
                        zos.putNextEntry(ZipEntry("files/$relativePath"))
                        file.inputStream().use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }

        backupFile
    }

    suspend fun restoreFromLocalFile(file: File) = withContext(Dispatchers.IO) {
        // checkpoint 当前数据库，防止 WAL 残留被覆盖后丢失
        runCatching {
            appDatabase.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
        }

        // 关闭数据库，避免文件覆盖冲突
        appDatabase.close()

        val cacheDir = File(context.cacheDir, "restore")
        cacheDir.mkdirs()
        cacheDir.listFiles()?.forEach { it.delete() }

        ZipInputStream(FileInputStream(file)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val targetFile = File(cacheDir, entry.name)
                targetFile.parentFile?.mkdirs()
                if (!entry.isDirectory) {
                    FileOutputStream(targetFile).use { output -> zis.copyTo(output) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // settings.json
        val settingsFile = File(cacheDir, SETTINGS_FILE)
        if (settingsFile.exists()) {
            val settings = JsonInstant.decodeFromString<Settings>(settingsFile.readText())
            settingsStore.update(settings)
        }

        // AppDatabase — minechat.db（兼容旧备份：存在 -wal/-shm 也一并恢复）
        restoreDbFromCache(cacheDir, APP_DB_NAME)

        // files 目录
        val filesDir = context.filesDir
        val restoredFilesDir = File(cacheDir, "files")
        if (restoredFilesDir.exists()) {
            restoredFilesDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(restoredFilesDir).path
                    File(filesDir, relativePath).also { target ->
                        target.parentFile?.mkdirs()
                        file.copyTo(target, overwrite = true)
                    }
                }
            }
        }

        // 清理临时文件
        cacheDir.listFiles()?.forEach { it.delete() }

    }

    private fun writeDbToZip(zos: ZipOutputStream, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) {
            zos.putNextEntry(ZipEntry("$dbName.db"))
            dbFile.inputStream().use { input -> input.copyTo(zos) }
            zos.closeEntry()
        }
    }

    private fun restoreDbFromCache(cacheDir: File, dbName: String) {
        val dbFile = context.getDatabasePath(dbName)
        val restoredDb = File(cacheDir, "$dbName.db")
        if (!restoredDb.exists()) return

        dbFile.parentFile?.let { parent ->
            restoredDb.copyTo(dbFile, overwrite = true)
            // 兼容旧备份（包含 -wal/-shm）
            File(cacheDir, "$dbName.db-wal").takeIf { it.exists() }
                ?.copyTo(File(parent, "$dbName-wal"), overwrite = true)
            File(cacheDir, "$dbName.db-shm").takeIf { it.exists() }
                ?.copyTo(File(parent, "$dbName-shm"), overwrite = true)
        }
    }

}
