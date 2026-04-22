package com.phpserver.android.file

import android.content.Context
import com.phpserver.android.server.ServerState
import java.io.File
import java.io.InputStream

object ProjectManager {

    fun getWwwDirectory(context: Context): File {
        val wwwDir = File(context.filesDir, "www")
        if (!wwwDir.exists()) {
            wwwDir.mkdirs()
            createDefaultFiles(wwwDir)
        }
        return wwwDir
    }

    private fun createDefaultFiles(wwwDir: File) {
        val d = "${'$'}"

        // Create index.php
        File(wwwDir, "index.php").writeText("""
            <?php echo phpversion(); ?>
        """.trimIndent())

        // Create phpinfo.php
        File(wwwDir, "phpinfo.php").writeText("""
            <?php phpinfo(); ?>
        """.trimIndent())

        // Create test.php
        File(wwwDir, "test.php").writeText("""
            <?php echo 'PHP Test Page'; ?>
        """.trimIndent())

        // Create info.php (API status page)
        File(wwwDir, "info.php").writeText("""
            <?php
            header('Content-Type: application/json');
            echo json_encode([
                'status' => 'ok',
                'php_version' => phpversion(),
            ]);
            ?>
        """.trimIndent())

        ServerState.logs.add("[ProjectManager] Created default PHP files in www directory")
    }

    fun listFiles(dir: File): List<FileInfo> {
        return dir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { FileInfo(it.name, it.length(), it.lastModified(), it.extension) }
            ?: emptyList()
    }

    fun listDirectories(dir: File): List<FileInfo> {
        return dir.listFiles()
            ?.filter { it.isDirectory }
            ?.sorted()
            ?.map { FileInfo(it.name, 0, it.lastModified(), "dir") }
            ?: emptyList()
    }

    fun createFile(dir: File, filename: String, content: String): Boolean {
        return try {
            File(dir, filename).writeText(content)
            ServerState.logs.add("[FileManager] Created: $filename")
            true
        } catch (e: Exception) {
            ServerState.logs.add("[FileManager] Error creating $filename: ${e.message}")
            false
        }
    }

    fun readFile(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            "// Error reading file: ${e.message}"
        }
    }

    fun deleteFile(file: File): Boolean {
        return try {
            file.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun createDirectory(parent: File, name: String): Boolean {
        return File(parent, name).mkdirs()
    }

    fun importFile(dir: File, filename: String, inputStream: InputStream): Boolean {
        return try {
            val file = File(dir, filename)
            file.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            ServerState.logs.add("[FileManager] Imported: $filename")
            true
        } catch (e: Exception) {
            false
        }
    }
}

data class FileInfo(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val extension: String
)
