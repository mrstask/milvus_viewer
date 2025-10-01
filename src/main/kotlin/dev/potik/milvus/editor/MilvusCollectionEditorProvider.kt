package dev.potik.milvus.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class MilvusCollectionEditorProvider : FileEditorProvider, DumbAware {

    companion object {
        const val EDITOR_TYPE_ID = "milvus-collection-editor"
    }

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file is MilvusCollectionVirtualFile
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return MilvusCollectionEditor(project, file as MilvusCollectionVirtualFile)
    }

    override fun getEditorTypeId(): String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}