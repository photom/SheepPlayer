# Contributing to SheepPlayer 🤝

Thank you for your interest in contributing to SheepPlayer! This document provides guidelines and
information for contributors.

## 🎯 How to Contribute

### Ways to Contribute

- 🐛 **Bug Reports**: Report issues or unexpected behavior
- 💡 **Feature Requests**: Suggest new features or improvements
- 🔧 **Code Contributions**: Submit bug fixes or new features
- 📚 **Documentation**: Improve or add documentation
- 🧪 **Testing**: Add or improve test coverage
- 🎨 **UI/UX**: Enhance user interface and experience

## 🚀 Getting Started

### 1. Fork and Clone

```bash
# Fork the repository on GitHub
# Clone your fork
git clone https://github.com/yourusername/SheepPlayer.git
cd SheepPlayer

# Add upstream remote
git remote add upstream https://github.com/original/SheepPlayer.git
```

### 2. Set Up Development Environment

Follow the [Build & Setup Guide](docs/BUILD_SETUP.md) for detailed instructions.

Quick setup:

- Install Android Studio Arctic Fox or later
- JDK 11 or higher
- Android SDK API Level 33+
- Kotlin 2.0.21+

### 3. Create a Branch

```bash
# Create and switch to a new branch
git checkout -b feature/your-feature-name
# or
git checkout -b fix/issue-description
```

## 📝 Development Guidelines

### Code Style and Standards

#### Kotlin Conventions

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Prefer `val` over `var` when possible
- Use null safety features (`?.`, `!!`, etc.)

#### Code Structure

```kotlin
// Class structure example
class ExampleClass(
    private val dependency: Dependency
) {
    
    // Properties first
    private var _state = MutableLiveData<State>()
    val state: LiveData<State> = _state
    
    // Public methods
    fun publicMethod() {
        // Implementation
    }
    
    // Private methods last
    private fun privateHelper() {
        // Implementation
    }
    
    // Companion object at the end
    companion object {
        private const val TAG = "ExampleClass"
    }
}
```

#### Documentation

```kotlin
/**
 * Brief description of the class or method
 * 
 * @param parameter Description of parameter
 * @return Description of return value
 * @throws ExceptionType When this exception is thrown
 */
fun exampleFunction(parameter: String): Boolean {
    // Implementation
}
```

### Android-Specific Guidelines

#### Resource Naming

```xml
<!-- Layouts: activity_name.xml, fragment_name.xml, item_type.xml -->
activity_main.xml
fragment_playing.xml
item_track.xml

<!-- IDs: componentType_description -->
<TextView android:id="@+id/text_track_title" />
<Button android:id="@+id/button_play_pause" />

<!-- Strings: screen_purpose or common_action -->
<string name="playing_title">Now Playing</string>
<string name="common_play">Play</string>
```

#### Dependency Injection

- Use constructor injection when possible
- Keep dependencies minimal and focused
- Mock dependencies in tests

### Security Guidelines

#### File Handling

```kotlin
// ✅ Good: Validate file paths
fun loadFile(filePath: String): Boolean {
    if (!isValidPath(filePath)) return false
    // ... rest of implementation
}

// ❌ Bad: Direct file access without validation
fun loadFile(filePath: String) {
    File(filePath).readText() // Security risk!
}
```

#### Input Validation

```kotlin
// ✅ Good: Validate and sanitize inputs
fun processUserInput(input: String?): String {
    return input?.trim()?.takeIf { it.isNotBlank() } ?: "Default"
}

// ❌ Bad: Trust user input
fun processUserInput(input: String) {
    // Direct use without validation
}
```

## 🧪 Testing Requirements

### Test Coverage

- **Unit Tests**: Required for all new utility classes and business logic
- **Integration Tests**: Required for complex interactions
- **UI Tests**: Required for new user-facing features

### Test Structure

```kotlin
class ExampleTest {
    
    @Before
    fun setUp() {
        // Test setup
    }
    
    @Test
    fun `should return expected result when given valid input`() {
        // Arrange
        val input = "test"
        
        // Act
        val result = functionUnderTest(input)
        
        // Assert
        assertEquals("expected", result)
    }
    
    @After
    fun tearDown() {
        // Cleanup
    }
}
```

### Running Tests

```bash
# Run all tests before submitting PR
./gradlew test
./gradlew connectedAndroidTest

# Check test coverage
./gradlew jacocoTestReport
```

## 📋 Commit Guidelines

### Commit Message Format

```
type(scope): description

[optional body]

[optional footer]
```

### Types

- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, etc.)
- **refactor**: Code refactoring
- **test**: Adding or modifying tests
- **chore**: Maintenance tasks

### Examples

```bash
feat(player): add shuffle functionality

- Implement shuffle algorithm
- Add shuffle button to UI
- Update player state management

Closes #123

fix(security): prevent path traversal in file loading

- Add path validation in MusicRepository
- Implement isValidAudioFile method
- Add unit tests for security validation

style(ui): improve track list item spacing

refactor(repository): extract MediaStore query logic

test(utils): add tests for TimeUtils formatting

docs(readme): update installation instructions
```

## 🔄 Pull Request Process

### Before Submitting

1. **Update your branch**:

```bash
git fetch upstream
git rebase upstream/main
```

2. **Run all checks**:

```bash
./gradlew clean
./gradlew build
./gradlew test
./gradlew lint
```

3. **Test manually**:
    - Verify your changes work as expected
    - Test on different devices/screen sizes
    - Check for performance impact

### PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature  
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] Tested on device/emulator

## Screenshots (if applicable)
Add screenshots for UI changes

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Tests pass locally
- [ ] Documentation updated
```

### Review Process

1. **Automated checks**: All CI checks must pass
2. **Code review**: At least one maintainer review required
3. **Testing**: Changes tested on multiple devices
4. **Documentation**: Relevant docs updated

## 🐛 Bug Reports

### Before Reporting

- Check existing issues to avoid duplicates
- Test on latest version
- Gather relevant information

### Bug Report Template

```markdown
**Describe the Bug**
Clear description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

**Expected Behavior**
What you expected to happen.

**Screenshots**
If applicable, add screenshots.

**Device Information:**
- Device: [e.g. Pixel 4]
- OS: [e.g. Android 12]
- App Version: [e.g. 1.0.0]

**Additional Context**
Any other context about the problem.
```

## 💡 Feature Requests

### Feature Request Template

```markdown
**Is your feature request related to a problem?**
Clear description of the problem.

**Describe the solution you'd like**
Clear description of what you want to happen.

**Describe alternatives you've considered**
Other solutions you've considered.

**Additional context**
Any other context, screenshots, or mockups.
```

## 🏗️ Architecture Decisions

### When Adding New Features

1. **Follow existing patterns**: Maintain consistency with current architecture
2. **Security first**: All inputs must be validated and sanitized
3. **Test coverage**: Include comprehensive tests
4. **Documentation**: Update relevant documentation
5. **Performance**: Consider impact on app performance

### Major Changes

For significant architectural changes:

1. Open an issue for discussion
2. Provide detailed proposal
3. Get maintainer approval before implementation
4. Update architecture documentation

## 📚 Resources

### Documentation

- [Project Structure](docs/PROJECT_STRUCTURE.md)
- [API Reference](docs/API_REFERENCE.md)
- [Build Setup](docs/BUILD_SETUP.md)
- [Testing Guide](docs/TESTING_GUIDE.md)

### External Resources

- [Android Developer Guides](https://developer.android.com/guide)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Material Design Guidelines](https://material.io/design)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)

## 🎉 Recognition

Contributors will be recognized in:

- README.md contributors section
- Release notes for significant contributions
- GitHub repository insights

## 📞 Getting Help

- **Questions**: Open a GitHub discussion
- **Issues**: Create a GitHub issue
- **Security**: Email maintainers privately for security issues

## 📄 License

By contributing to SheepPlayer, you agree that your contributions will be licensed under the same
license as the project.

---

Thank you for contributing to SheepPlayer! Your contributions help make this project better for
everyone. 🎵