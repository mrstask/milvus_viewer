# Security Policy

## 🔒 Supported Versions

We take security seriously and provide security updates for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | ✅ Yes            |

## 🚨 Reporting a Vulnerability

If you discover a security vulnerability in the Milvus Vector Database Connector plugin, please follow these steps:

### 🔐 Private Disclosure

**DO NOT** create a public GitHub issue for security vulnerabilities. Instead, please:

1. **Email us directly**: Send an email to `security@potik.dev`
2. **Include details**: Provide a detailed description of the vulnerability
3. **Provide reproduction steps**: Include steps to reproduce the issue
4. **Wait for acknowledgment**: We will acknowledge receipt within 48 hours

### 📧 What to Include

When reporting a security vulnerability, please include:

- **Description**: Clear description of the vulnerability
- **Impact**: What an attacker could achieve
- **Reproduction**: Step-by-step instructions to reproduce
- **Environment**: IDE version, plugin version, OS, etc.
- **Proof of Concept**: Code or screenshots if applicable

### ⏱️ Response Timeline

- **Acknowledgment**: Within 48 hours
- **Initial Assessment**: Within 1 week
- **Status Updates**: Weekly until resolved
- **Resolution**: Varies based on complexity

### 🎯 Scope

Security issues we're particularly interested in:

- **Authentication bypass**: Circumventing Milvus authentication
- **Data exposure**: Unauthorized access to collection data
- **Code injection**: Injection attacks through UI inputs
- **Credential leakage**: Exposure of stored credentials
- **Privilege escalation**: Gaining elevated permissions
- **Cross-site scripting**: XSS in UI components

### 🚫 Out of Scope

The following are generally not considered security vulnerabilities:

- Issues requiring physical access to the machine
- Self-XSS that cannot be used to attack other users
- Issues requiring social engineering
- Vulnerabilities in dependencies (please report to the relevant project)

## 🛡️ Security Measures

The plugin implements several security measures:

### 🔑 Credential Security
- Passwords are stored using JetBrains' secure credential store
- No credentials are stored in plain text
- Credentials are cleared from memory after use

### 🔐 Connection Security
- TLS/HTTPS support for encrypted connections
- Certificate validation for secure connections
- Connection timeout protection

### 🔍 Input Validation
- SQL injection prevention
- Input sanitization for UI components
- Proper error handling without information leakage

### 🚨 Vulnerability Response
- Security patches are prioritized
- Vulnerabilities are fixed in the latest supported version
- Security advisories are published for confirmed issues

## 📋 Security Best Practices

When using the plugin:

1. **Use TLS**: Always enable TLS for production connections
2. **Strong Authentication**: Use strong passwords for Milvus authentication
3. **Network Security**: Use firewalls and network segmentation
4. **Keep Updated**: Always use the latest plugin version
5. **Monitor Access**: Monitor Milvus access logs regularly

## 🔄 Security Updates

Security updates are:

- Released as soon as possible after verification
- Announced through GitHub security advisories
- Tagged with security labels in release notes
- Backported to supported versions when possible

## 📞 Contact

For security-related questions or concerns:

- **Security Team**: security@potik.dev
- **General Support**: support@potik.dev
- **GitHub Issues**: For non-security bugs only

## 🙏 Acknowledgments

We appreciate security researchers who help keep the plugin secure:

- Responsible disclosure researchers will be credited (with permission)
- Severe vulnerabilities may be eligible for recognition
- We maintain a hall of fame for security contributors

---

**Thank you for helping keep the Milvus Vector Database Connector secure!** 🛡️