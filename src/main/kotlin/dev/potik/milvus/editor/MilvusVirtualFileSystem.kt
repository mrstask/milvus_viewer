package dev.potik.milvus.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.VirtualFileSystem

class MilvusVirtualFileSystem : VirtualFileSystem() {

    companion object {
        private const val PROTOCOL = "milvus"
        private var instance: MilvusVirtualFileSystem? = null
        
        fun getInstance(): MilvusVirtualFileSystem {
            val vfs = VirtualFileManager.getInstance().getFileSystem(PROTOCOL)
            if (vfs is MilvusVirtualFileSystem) {
                return vfs
            }
            
            // If not found or wrong type, create and cache instance
            return instance ?: run {
                val newInstance = MilvusVirtualFileSystem()
                instance = newInstance
                newInstance
            }
        }
    }

    override fun getProtocol(): String = PROTOCOL

    override fun findFileByPath(path: String): VirtualFile? = null

    override fun refresh(asynchronous: Boolean) {}

    override fun refreshAndFindFileByPath(path: String): VirtualFile? = null

    override fun addVirtualFileListener(listener: VirtualFileListener) {}

    override fun removeVirtualFileListener(listener: VirtualFileListener) {}

    override fun deleteFile(requestor: Any?, vFile: VirtualFile) {
        throw UnsupportedOperationException("Cannot delete Milvus collections")
    }

    override fun moveFile(requestor: Any?, vFile: VirtualFile, newParent: VirtualFile) {
        throw UnsupportedOperationException("Cannot move Milvus collections")
    }

    override fun renameFile(requestor: Any?, vFile: VirtualFile, newName: String) {
        throw UnsupportedOperationException("Cannot rename Milvus collections")
    }

    override fun createChildFile(requestor: Any?, vDir: VirtualFile, fileName: String): VirtualFile {
        throw UnsupportedOperationException("Cannot create files in Milvus file system")
    }

    override fun createChildDirectory(requestor: Any?, vDir: VirtualFile, dirName: String): VirtualFile {
        throw UnsupportedOperationException("Cannot create directories in Milvus file system")
    }

    override fun copyFile(requestor: Any?, virtualFile: VirtualFile, newParent: VirtualFile, copyName: String): VirtualFile {
        throw UnsupportedOperationException("Cannot copy Milvus collections")
    }

    override fun isReadOnly(): Boolean = true
}