package com.myai.assistant.inference

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadProgress {
    data class InProgress(val bytesRead: Long, val totalBytes: Long) : DownloadProgress()
    data class Done(val file: File) : DownloadProgress()
    data class Failed(val error: Throwable) : DownloadProgress()
}

class ModelDownloader(private val context: Context) {

    fun modelsDir(): File = File(context.filesDir, "models").apply { mkdirs() }

    fun localFileFor(model: ModelInfo): File = File(modelsDir(), "${model.id}.gguf")

    fun isDownloaded(model: ModelInfo): Boolean = localFileFor(model).exists()

    fun download(model: ModelInfo): Flow<DownloadProgress> = flow {
        val target = localFileFor(model)
        val tmp = File(target.parentFile, "${target.name}.part")
        try {
            val connection = URL(model.downloadUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connect()
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalRead += read
                        emit(DownloadProgress.InProgress(totalRead, total))
                    }
                }
            }
            tmp.renameTo(target)
            emit(DownloadProgress.Done(target))
        } catch (t: Throwable) {
            tmp.delete()
            emit(DownloadProgress.Failed(t))
        }
    }.flowOn(Dispatchers.IO)

    fun importLocalFile(sourcePath: String, modelId: String): File {
        val source = File(sourcePath)
        val target = File(modelsDir(), "$modelId.gguf")
        source.copyTo(target, overwrite = true)
        return target
    }

    fun delete(model: ModelInfo) {
        localFileFor(model).delete()
    }
}
