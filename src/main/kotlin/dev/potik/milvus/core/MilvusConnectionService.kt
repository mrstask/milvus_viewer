package dev.potik.milvus.core

import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import io.milvus.client.MilvusServiceClient
import io.milvus.param.ConnectParam
import io.milvus.grpc.DataType
import io.milvus.param.R
import io.milvus.param.collection.DescribeCollectionParam
import io.milvus.param.collection.LoadCollectionParam
import io.milvus.param.highlevel.collection.ListCollectionsParam
import io.milvus.param.dml.QueryParam
import io.milvus.response.QueryResultsWrapper
import java.util.LinkedHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.APP)
class MilvusConnectionService : Disposable {

    data class ConnectionConfig(
        val host: String,
        val port: Int,
        val username: String?,
        val secure: Boolean,
        val databaseName: String? = null,
        val defaultCollection: String? = null,
        val previewLimit: Int = DEFAULT_PREVIEW_LIMIT
    )

    data class CollectionSummary(
        val name: String,
        val description: String?,
        val fieldCount: Int,
        val loaded: Boolean
    )

    data class FieldInfo(
        val name: String,
        val dataType: DataType,
        val primaryKey: Boolean,
        val autoId: Boolean
    )

    data class CollectionSchema(
        val summary: CollectionSummary,
        val fields: List<FieldInfo>
    )

    data class CollectionPreview(
        val schema: CollectionSchema,
        val rows: List<Map<String, Any?>>
    )

    private val log = Logger.getInstance(MilvusConnectionService::class.java)
    private val executor: ExecutorService = AppExecutorUtil.getAppExecutorService()

    @Volatile
    private var client: MilvusServiceClient? = null
    private val activeConfig = AtomicReference<ConnectionConfig?>()

    @Suppress("DEPRECATION")
    fun connect(config: ConnectionConfig, password: CharArray?): CompletableFuture<Unit> {
        val passwordValue = password?.concatToString()?.takeIf { it.isNotBlank() }
        password?.fill('\u0000')

        return runAsync {
            val builder = ConnectParam.newBuilder()
                .withHost(config.host)
                .withPort(config.port)

            if (config.secure) {
                builder.secure(true)
            }

            if (config.username != null && passwordValue != null) {
                builder.withAuthorization(config.username, passwordValue)
            }

            if (config.databaseName != null) {
                builder.withDatabaseName(config.databaseName)
            }

            val newClient = MilvusServiceClient(builder.build())
            val version = newClient.getVersion()
            if (version.status != R.Status.Success.ordinal) {
                newClient.close()
                error("Failed to connect: ${version.message}")
            }

            synchronized(this) {
                client?.close()
                client = newClient
                activeConfig.set(config)
            }
            log.info("Connected to Milvus at ${config.host}:${config.port}")
        }
    }

    fun disconnect() {
        synchronized(this) {
            client?.close()
            client = null
            activeConfig.set(null)
        }
    }

    fun isConnected(): Boolean = client != null

    fun getActiveConfig(): ConnectionConfig? = activeConfig.get()

    fun listCollections(): CompletableFuture<List<CollectionSummary>> = runAsync {
        val milvus = client()
        val response = milvus.listCollections(ListCollectionsParam.newBuilder().build())
        if (response.status != R.Status.Success.ordinal) {
            error("Unable to list collections: ${response.message}")
        }

        response.data?.collectionNames?.sorted()?.map { name ->
            buildCollectionSummary(milvus, name)
        } ?: emptyList()
    }

    fun describeCollection(collectionName: String): CompletableFuture<CollectionSchema> = runAsync {
        buildCollectionSchema(client(), collectionName)
    }

    fun previewCollection(collectionName: String, limit: Int = DEFAULT_PREVIEW_LIMIT): CompletableFuture<CollectionPreview> = runAsync {
        val milvus = client()
        val schema = buildCollectionSchema(milvus, collectionName)

        if (!schema.summary.loaded) {
            val loadResult = milvus.loadCollection(
                LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build()
            )
            if (loadResult.status != R.Status.Success.ordinal) {
                log.warn("Failed to load collection $collectionName before preview: ${loadResult.message}")
            }
        }

        val fieldNames = schema.fields.map { it.name }
        val query = QueryParam.newBuilder()
            .withCollectionName(collectionName)
            .withOutFields(fieldNames)
            .withLimit(limit.toLong())
            .withOffset(0)
            .withExpr("")
            .build()

        val result = milvus.query(query)
        if (result.status != R.Status.Success.ordinal) {
            error("Query failure: ${result.message}")
        }

        val wrapper = QueryResultsWrapper(result.data)
        val rows = wrapper.rowRecords.map { record ->
            val row = LinkedHashMap<String, Any?>()
            fieldNames.forEach { field ->
                row[field] = record.get(field)
            }
            row
        }

        CollectionPreview(schema = schema, rows = rows)
    }

    fun previewCollectionPaginated(
        collectionName: String,
        offset: Int = 0,
        limit: Int = DEFAULT_PREVIEW_LIMIT
    ): CompletableFuture<CollectionPreview> = runAsync {
        val milvus = client()
        val schema = buildCollectionSchema(milvus, collectionName)

        if (!schema.summary.loaded) {
            val loadResult = milvus.loadCollection(
                LoadCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build()
            )
            if (loadResult.status != R.Status.Success.ordinal) {
                log.warn("Failed to load collection $collectionName before preview: ${loadResult.message}")
            }
        }

        val fieldNames = schema.fields.map { it.name }
        val query = QueryParam.newBuilder()
            .withCollectionName(collectionName)
            .withOutFields(fieldNames)
            .withLimit(limit.toLong())
            .withOffset(offset.toLong())
            .withExpr("")
            .build()

        val result = milvus.query(query)
        if (result.status != R.Status.Success.ordinal) {
            error("Query failure: ${result.message}")
        }

        val wrapper = QueryResultsWrapper(result.data)
        val rows = wrapper.rowRecords.map { record ->
            val row = LinkedHashMap<String, Any?>()
            fieldNames.forEach { field ->
                row[field] = record.get(field)
            }
            row
        }

        CollectionPreview(schema = schema, rows = rows)
    }

    fun getCollectionCount(collectionName: String): CompletableFuture<Long> = runAsync {
        val milvus = client()
        val countQuery = QueryParam.newBuilder()
            .withCollectionName(collectionName)
            .withOutFields(listOf("count(*)"))
            .withExpr("")
            .build()

        val countResult = milvus.query(countQuery)

        if (countResult.status != R.Status.Success.ordinal) {
            log.warn("Failed to get count for collection $collectionName: ${countResult.message}")
            0L
        } else {
            val wrapper = QueryResultsWrapper(countResult.data)
            try {
                val counts = wrapper.getFieldWrapper("count(*)").getFieldData()
                if (counts.isNotEmpty()) {
                    (counts.first() as? Number)?.toLong() ?: 0L
                } else {
                    0L
                }
            } catch (e: Exception) {
                log.warn("Failed to parse count result for collection $collectionName", e)
                0L
            }
        }
    }

    private fun buildCollectionSummary(client: MilvusServiceClient, name: String): CollectionSummary {
        val describe = client.describeCollection(
            DescribeCollectionParam.newBuilder()
                .withCollectionName(name)
                .build()
        )
        if (describe.status != R.Status.Success.ordinal) {
            error("Failed to describe collection $name: ${describe.message}")
        }
        return toCollectionSummary(name, describe.data)
    }

    private fun buildCollectionSchema(client: MilvusServiceClient, name: String): CollectionSchema {
        val describe = client.describeCollection(
            DescribeCollectionParam.newBuilder()
                .withCollectionName(name)
                .build()
        )
        if (describe.status != R.Status.Success.ordinal) {
            error("Failed to describe collection $name: ${describe.message}")
        }
        val summary = toCollectionSummary(name, describe.data)
        val fields = describe.data.schema.fieldsList.map { field ->
            FieldInfo(
                name = field.name,
                dataType = field.dataType,
                primaryKey = field.isPrimaryKey,
                autoId = field.autoID
            )
        }
        return CollectionSchema(summary, fields)
    }

    private fun toCollectionSummary(name: String, describe: Any): CollectionSummary {
        return CollectionSummary(
            name = name,
            description = null,
            fieldCount = 0,
            loaded = false
        )
    }

    private fun client(): MilvusServiceClient =
        client ?: error("Milvus connection has not been established")

    private fun <T> runAsync(task: () -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(task, executor)

    override fun dispose() {
        disconnect()
    }

    companion object {
        const val DEFAULT_PREVIEW_LIMIT = 100

        fun instance(): MilvusConnectionService =
            ApplicationManager.getApplication().getService(MilvusConnectionService::class.java)
    }
}


