package dev.potik.milvus.editor

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import dev.potik.milvus.core.MilvusConnectionService
import java.io.InputStream
import java.io.OutputStream

class MilvusCollectionVirtualFile(
    private val collectionName: String,
    private val connectionConfig: MilvusConnectionService.ConnectionConfig
) : VirtualFile() {

    override fun getName(): String = collectionName

    override fun getFileSystem(): VirtualFileSystem = MilvusVirtualFileSystem.getInstance()

    override fun getPath(): String = "milvus://${connectionConfig.host}:${connectionConfig.port}/${connectionConfig.databaseName}/$collectionName"

    override fun isWritable(): Boolean = false

    override fun isDirectory(): Boolean = false

    override fun isValid(): Boolean = true

    override fun getParent(): VirtualFile? = null

    override fun getChildren(): Array<VirtualFile> = emptyArray()

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        throw UnsupportedOperationException("Milvus collections are read-only")
    }

    override fun contentsToByteArray(): ByteArray = "Milvus Collection: $collectionName".toByteArray()

    override fun getTimeStamp(): Long = 0

    override fun getLength(): Long = 0

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {}

    override fun getInputStream(): InputStream = contentsToByteArray().inputStream()

    override fun getModificationStamp(): Long = 0

    fun getCollectionName(): String = collectionName
    
    fun getConnectionConfig(): MilvusConnectionService.ConnectionConfig = connectionConfig

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MilvusCollectionVirtualFile) return false
        return collectionName == other.collectionName && connectionConfig == other.connectionConfig
    }

    override fun hashCode(): Int {
        return collectionName.hashCode() * 31 + connectionConfig.hashCode()
    }
}