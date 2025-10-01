# Contributing to Milvus Vector Database Connector

Thank you for your interest in contributing to the Milvus Vector Database Connector plugin! We welcome contributions from the community and are pleased to have you join us.

## 🤝 How to Contribute

### Reporting Bugs

If you find a bug, please create an issue with:
- A clear description of the problem
- Steps to reproduce the issue
- Expected vs actual behavior
- Your environment details (OS, IDE version, Milvus version)
- Screenshots if applicable

### Suggesting Features

We welcome feature suggestions! Please create an issue with:
- A clear description of the proposed feature
- Use case and benefits
- Any implementation ideas you might have

### Code Contributions

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes**
4. **Test your changes thoroughly**
5. **Commit your changes**
   ```bash
   git commit -m "Add: your feature description"
   ```
6. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Create a Pull Request**

## 🛠️ Development Setup

### Prerequisites
- Java 21 or later
- IntelliJ IDEA or PyCharm
- Milvus 2.0+ server for testing

### Setup Steps
1. Clone the repository
   ```bash
   git clone https://github.com/potik-dev/milvus-connector.git
   cd milvus-connector
   ```

2. Open the project in your IDE

3. Run the plugin in development mode
   ```bash
   ./gradlew runIde
   ```

4. Build the plugin
   ```bash
   ./gradlew buildPlugin
   ```

## 📝 Coding Standards

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add documentation for public APIs
- Keep functions focused and small

### Testing
- Test your changes thoroughly
- Ensure existing functionality isn't broken
- Test with different Milvus configurations

### Commit Messages
Use clear and descriptive commit messages:
- `Add: new feature description`
- `Fix: bug description`
- `Update: improvement description`
- `Refactor: code changes`

## 🗂️ Project Structure

```
src/main/kotlin/dev/potik/milvus/
├── core/
│   └── MilvusConnectionService.kt    # Connection management
├── ui/
│   └── MilvusToolWindowFactory.kt    # Main UI components
├── editor/
│   ├── MilvusCollectionEditor.kt     # Collection data viewer
│   ├── MilvusCollectionVirtualFile.kt
│   └── MilvusVirtualFileSystem.kt
├── settings/
│   ├── MilvusSettingsState.kt        # Settings persistence
│   └── MilvusSettingsConfigurable.kt # Settings UI
└── actions/
    └── ConnectAction.kt              # Plugin actions
```

## 🧪 Testing Guidelines

### Manual Testing
1. Test database connections with various configurations
2. Verify collection browsing and data viewing
3. Test pagination and data expansion features
4. Check error handling and edge cases

### Areas to Test
- Connection establishment and authentication
- Collection listing and metadata display
- Data pagination and navigation
- Vector and JSON data expansion
- Settings persistence
- Error handling and user feedback

## 📋 Pull Request Checklist

Before submitting a pull request, ensure:
- [ ] Code follows project conventions
- [ ] Changes are thoroughly tested
- [ ] No debug code or console logs remain
- [ ] Documentation is updated if needed
- [ ] Commit messages are clear and descriptive
- [ ] Pull request description explains the changes

## 🚀 Release Process

1. Update version in `build.gradle.kts`
2. Update `CHANGELOG.md` with new changes
3. Update `plugin.xml` version and change notes
4. Create a release tag
5. Build and publish to marketplace

## 🐛 Debugging Tips

### IDE Development
- Use `./gradlew runIde` for development testing
- Check IDE logs for errors and warnings
- Use IntelliJ's built-in debugger

### Common Issues
- **Connection problems**: Verify Milvus server is running
- **UI issues**: Check Swing thread usage
- **Plugin loading**: Verify plugin.xml configuration

## 📞 Getting Help

- **Issues**: Create a GitHub issue for bugs or questions
- **Discussions**: Use GitHub Discussions for general questions
- **Email**: Contact support@potik.dev for sensitive issues

## 📄 License

By contributing to this project, you agree that your contributions will be licensed under the MIT License.

## 🙏 Recognition

All contributors will be recognized in our documentation and release notes. Thank you for helping make this plugin better!

---

**Happy Contributing!** 🎉