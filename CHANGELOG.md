# Changelog

All notable changes to the Milvus Vector Database Connector plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-10-01

### Added
- Initial release of Milvus Vector Database Connector
- Direct database connection with authentication support
- TLS/HTTPS connection support for secure connections
- Interactive collection browser with real-time statistics
- Paginated data viewer with 100 records per page navigation
- Smart data expansion for vector embeddings and JSON objects
- Double-click functionality to view complex data in readable format
- Secure credential management using JetBrains credential store
- Real-time column counts and record statistics for collections
- Support for all Milvus data types including arrays and nested structures
- Compatible with all JetBrains IDEs (IntelliJ IDEA, PyCharm, WebStorm, etc.)
- Support for custom databases and connection configurations
- Professional UI with loading indicators and responsive design

### Features
- **Connection Management**: Full support for Milvus 2.0+ connections
- **Data Browsing**: Efficient pagination and data exploration
- **Data Visualization**: Expandable views for complex data types
- **Security**: Encrypted password storage and TLS support
- **Compatibility**: Works with PyCharm 2024.2 and later versions

### Technical Details
- Built with Kotlin and IntelliJ Platform SDK
- Uses Milvus Java SDK 2.4.8
- Implements custom virtual file system for collection viewing
- Background threading for non-blocking UI operations
- Comprehensive error handling and user feedback

[1.0.0]: https://github.com/potik-dev/milvus-connector/releases/tag/v1.0.0