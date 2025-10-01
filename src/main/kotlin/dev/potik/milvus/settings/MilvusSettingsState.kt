package dev.potik.milvus.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import dev.potik.milvus.core.MilvusConnectionService

@Service(Service.Level.APP)
@State(name = "MilvusConnectionSettings", storages = [Storage("milvus-viewer.xml")])
class MilvusSettingsState : PersistentStateComponent<MilvusSettingsState.State> {

    data class State(
        var host: String = "localhost",
        var port: Int = 19530,
        var username: String = "",
        var secure: Boolean = false,
        var defaultCollection: String? = null,
        var previewLimit: Int = MilvusConnectionService.DEFAULT_PREVIEW_LIMIT
    )

    private var state: State = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun updateFrom(config: MilvusConnectionService.ConnectionConfig) {
        state = state.copy(
            host = config.host,
            port = config.port,
            username = config.username.orEmpty(),
            secure = config.secure,
            defaultCollection = config.defaultCollection,
            previewLimit = config.previewLimit
        )
    }

    fun updatePreviewLimit(limit: Int) {
        state = state.copy(previewLimit = limit)
    }

    fun toConnectionConfig(): MilvusConnectionService.ConnectionConfig =
        MilvusConnectionService.ConnectionConfig(
            host = state.host,
            port = state.port,
            username = state.username.ifBlank { null },
            secure = state.secure,
            defaultCollection = state.defaultCollection,
            previewLimit = state.previewLimit
        )

    companion object {
        fun getInstance(): MilvusSettingsState =
            ApplicationManager.getApplication().getService(MilvusSettingsState::class.java)
    }
}
