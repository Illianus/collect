package org.odk.collect.android.utilities

import android.annotation.TargetApi
import android.os.Build
import java.io.*
import java.nio.channels.Channel
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object IOUtils {

    fun closeQuietly(stream: OutputStream) {
        closeQuietly(stream as Closeable)
    }

    fun closeQuietly(writer: OutputStreamWriter) {
        closeQuietly(writer as Closeable)
    }

    @Throws(IOException::class)
    fun forceDelete(file: File) {
        if (file.isDirectory) {
            deleteDirectory(file)
        } else {
            val filePresent = file.exists()
            if (!file.delete()) {
                if (!filePresent) {
                    throw FileNotFoundException("File does not exist: $file")
                }
                val message = "Unable to delete file: $file"
                throw IOException(message)
            }
        }
    }

    @Throws(IOException::class)
    private fun deleteDirectory(file: File) {
        for (item in file.listFiles()) {
            if (item.isDirectory) {
                deleteDirectory(item)
                val filePresent = item.exists()
                if (!item.delete()) {
                    if (!filePresent) {
                        throw FileNotFoundException()
                    }
                    val message = "Unable to delete file: $file"
                    throw IOException(message)
                }
            } else
                forceDelete(item)
        }
    }

    fun closeQuietly(stream: InputStream) {
        closeQuietly(stream as Closeable)
    }

    private fun closeQuietly(stream: Closeable) {
        try {
            stream.close()
        } catch (e: IOException) {
            //ignore
        }

    }

    fun closeQuietly(channel: Channel) {
        closeQuietly(channel as Closeable)
    }

    @Throws(IOException::class)
    fun moveFileToDirectory(mediaFile: File, formMediaPath: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            moveFileToDirectoryO(mediaFile, formMediaPath)
        } else {
            if (!formMediaPath.exists()) formMediaPath.mkdirs()
            if (!mediaFile.exists()) throw FileNotFoundException("File not found!")
            val destination = File(formMediaPath, mediaFile.name)
            if (destination.exists()) destination.delete()
            FileInputStream(mediaFile).use { reader ->
                FileOutputStream(destination).use { writer ->
                    val buffer = ByteArray(1024 * 1024)
                    var read: Int = reader.read(buffer, 0, 1024 * 1024)
                    while (read > 0) {
                        writer.write(buffer, 0, read)
                        read = reader.read(buffer, 0, 1024 * 1024)
                    }
                    reader.close()
                    forceDelete(mediaFile)
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun moveFileToDirectoryO(mediaFile: File, formMediaPath: File) {
        try {
            Files.move(mediaFile.toPath(), formMediaPath.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        } catch (e: IOException) {
            //ignore
        }

    }

    fun copy(stream: InputStream, outputStream: OutputStream) {
        try {
            val buffer = ByteArray(1024 * 1024)
            var read: Int = stream.read(buffer, 0, 1024 * 1024)
            while (read > 0) {
                outputStream.write(buffer, 0, read)
                read = stream.read(buffer, 0, 1024 * 1024)
            }
            stream.close()
            outputStream.close()
        } catch (e: IOException) {
            //ignore
        }

    }
}
