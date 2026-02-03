# Milvus Vector Database Connector

[![JetBrains Plugin](https://img.shields.io/badge/JetBrains-Plugin-orange.svg)](https://plugins.jetbrains.com/)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)]()
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

A comprehensive IntelliJ Platform plugin that enables seamless integration with Milvus vector databases directly from your JetBrains IDE. Perfect for AI/ML developers working with vector embeddings, semantic search, and recommendation systems.

## Features

- **Connection Management**: Connect to Milvus instances with configurable host, port, authentication, and TLS settings
- **Collection Browser**: View and inspect Milvus collections with schema information
- **Vector Search**: Perform similarity searches with custom embeddings and configurable parameters
- **Settings Persistence**: Save and manage multiple connection configurations
- **Modern UI**: Integrated tool window with tabbed interface

## Installation

### Development Setup

1. Clone this repository
2. Open the project in IntelliJ IDEA or PyCharm
3. Run `./gradlew runIde` to launch PyCharm with the plugin loaded
4. The Milvus tool window will be available in the left sidebar

### Building the Plugin

```bash
./gradlew buildPlugin
```

The plugin will be built in `build/distributions/`.

## Usage

### Connecting to Milvus

1. Open the Milvus tool window (View → Tool Windows → Milvus)
2. In the Connection tab, enter your Milvus server details:
   - Host (default: localhost)
   - Port (default: 19530)
   - Username and password (if authentication is enabled)
   - TLS checkbox (if using secure connections)
3. Click "Connect"

### Browsing Collections

1. After connecting, switch to the "Collections" tab
2. Click "Refresh Collections" to load available collections
3. View collection schemas and metadata in the table

### Performing Vector Searches

1. Switch to the "Search" tab
2. Enter the collection name and vector field name
3. Configure search parameters:
   - Top K: Number of results to return
   - Metric: Similarity metric (IP, L2, COSINE)
   - Expression: Optional filter expression
4. Paste your embedding vector as a JSON array in the text area
5. Click "Search" to execute the query
6. View results in the table with IDs, distances, and scores

### Managing Saved Connections

1. Go to Settings → Tools → Milvus Connector
2. Add, edit, or remove saved connection configurations
3. Saved connections can be reused across sessions

## Configuration

The plugin supports the following Milvus connection parameters:

- **Host**: Milvus server hostname or IP
- **Port**: Milvus server port (default: 19530)
- **Authentication**: Username/password for authenticated connections
- **TLS**: Enable secure connections
- **Connection Timeout**: Configurable timeout settings

## Requirements

- PyCharm 2024.2 or later
- Java 11 or later
- Milvus 2.4+ server

## Dependencies

- Milvus Java SDK 2.4.6
- Jackson for JSON processing
- IntelliJ Platform SDK

## Development

### Project Structure

```
src/main/kotlin/dev/potik/milvus/
├── core/
│   └── MilvusConnectionService.kt    # Connection management
├── ui/
│   └── MilvusToolWindowFactory.kt    # Main UI components
├── settings/
│   ├── MilvusSettingsState.kt        # Settings persistence
│   └── MilvusSettingsConfigurable.kt # Settings UI
└── actions/
    └── ConnectAction.kt              # Plugin actions
```

### Key Components

- **MilvusConnectionService**: Manages connections, collections, and search operations
- **MilvusToolWindowFactory**: Creates the main tool window with connection, collections, and search tabs
- **MilvusSettingsState**: Persists connection configurations
- **MilvusSettingsConfigurable**: Provides settings UI for managing saved connections

## Troubleshooting

### Connection Issues

- Verify Milvus server is running and accessible
- Check firewall settings for the Milvus port
- Ensure authentication credentials are correct
- For TLS connections, verify certificate validity

### Search Issues

- Ensure the collection exists and is loaded
- Verify vector dimensions match the collection schema
- Check that the vector field name is correct
- Validate JSON format for embedding vectors

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and feature requests, please use the GitHub issue tracker.



