# Solora App - Automated Testing Documentation

This document outlines the automated testing setup for the Solora solar quotation application.

## Overview

The Solora app uses GitHub Actions for Continuous Integration (CI) to automatically build, test, and validate the application on every push and pull request to the main and develop branches.

## GitHub Actions Workflow

### Main CI Workflow (`.github/workflows/android-ci.yml`)

The workflow consists of two main jobs:

#### 1. Build Job
- **Triggers**: On push or pull request to `main` or `develop` branches
- **Runner**: Ubuntu latest
- **Steps**:
  1. Checkout code
  2. Set up JDK 17 (required for Android development)
  3. Cache Gradle dependencies for faster builds
  4. Decode Firebase configuration (google-services.json)
  5. Build the project
  6. Run unit tests
  7. Run lint checks
  8. Build debug APK
  9. Upload build artifacts (APK, test results, lint reports)
  10. Generate and upload code coverage reports

#### 2. Instrumented Test Job
- **Purpose**: Run Android instrumented tests on an emulator
- **Emulator Configuration**:
  - API Level: 29 (Android 10)
  - Target: Default
  - Architecture: x86_64
  - Profile: Nexus 6
- **Steps**:
  1. Set up Android environment
  2. Enable KVM for hardware acceleration
  3. Run instrumented tests on emulator
  4. Upload test results

## Test Structure

### Unit Tests

Located in `app/src/test/java/`

#### QuoteCalculatorTest
Tests the core quote calculation logic:
- ✅ Valid usage-based calculations
- ✅ Valid bill-based calculations
- ✅ System sizing accuracy
- ✅ Panel count calculations
- ✅ Inverter sizing (80% rule)
- ✅ Monthly savings calculations
- ✅ Payback period validation
- ✅ Minimum and maximum value handling
- ✅ Calculation consistency

#### FirebaseModelsTest
Tests data model integrity:
- ✅ FirebaseQuote creation and validation
- ✅ FirebaseLead creation and validation
- ✅ FirebaseUser creation and validation
- ✅ Data serialization
- ✅ Field validation (email, status, etc.)
- ✅ Optional field handling
- ✅ Default value assignments

### Instrumented Tests (Future)

Located in `app/src/androidTest/java/`

These tests will run on actual Android devices/emulators:
- UI navigation tests
- Database operations
- API integration tests
- End-to-end user flows

## Running Tests Locally

### Run Unit Tests
```bash
./gradlew test
```

### Run Unit Tests with Coverage
```bash
./gradlew jacocoTestReport
```

### Run Lint Checks
```bash
./gradlew lint
```

### Run Instrumented Tests
```bash
./gradlew connectedDebugAndroidTest
```

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Run All Checks
```bash
./gradlew build
```

## Test Coverage

The project uses JaCoCo for test coverage reporting. Coverage reports are automatically generated and uploaded as artifacts in the GitHub Actions workflow.

To view coverage locally:
1. Run `./gradlew jacocoTestReport`
2. Open `app/build/reports/jacoco/test/html/index.html` in a browser

## Setting Up GitHub Secrets

For the CI workflow to work, you need to set up the following secrets in your GitHub repository:

### GOOGLE_SERVICES_JSON

1. Go to your GitHub repository
2. Navigate to Settings → Secrets and variables → Actions
3. Click "New repository secret"
4. Name: `GOOGLE_SERVICES_JSON`
5. Value: Base64-encoded content of your `google-services.json` file

To encode your `google-services.json`:
```bash
cat app/google-services.json | base64
```

Copy the output and paste it as the secret value.

## CI/CD Status Badges

Add these badges to your README.md to show build status:

```markdown
![Android CI](https://github.com/YOUR_USERNAME/YOUR_REPO/workflows/Android%20CI/badge.svg)
```

## Test Categories

### 1. Quote Calculation Tests
- Solar system sizing
- Panel count calculations
- Inverter sizing
- Financial calculations
- Payback period
- Monthly savings

### 2. Data Model Tests
- FirebaseQuote validation
- FirebaseLead validation
- FirebaseUser validation
- Data integrity
- Field validation

### 3. Integration Tests (Future)
- Firebase API integration
- REST API calls
- Data synchronization
- Authentication flow

### 4. UI Tests (Future)
- Navigation flows
- Form validation
- User interactions
- Quote creation flow
- Lead management

## Best Practices

1. **Write Tests First**: Follow TDD (Test-Driven Development) when adding new features
2. **Keep Tests Isolated**: Each test should be independent
3. **Use Descriptive Names**: Test names should clearly describe what they test
4. **Test Edge Cases**: Include tests for minimum, maximum, and invalid values
5. **Maintain Coverage**: Aim for at least 80% code coverage
6. **Fast Tests**: Unit tests should run quickly (< 1 second each)
7. **Clean Up**: Use `@Before` and `@After` for setup and teardown

## Continuous Improvement

### Current Test Coverage
- ✅ Quote calculation logic
- ✅ Data model validation
- ⏳ API integration (in progress)
- ⏳ UI navigation (planned)
- ⏳ End-to-end flows (planned)

### Planned Enhancements
1. Add instrumented UI tests
2. Implement API mock testing
3. Add performance benchmarks
4. Set up automated release builds
5. Add security scanning
6. Implement snapshot testing for UI

## Troubleshooting

### Common Issues

**Issue**: Tests fail with Firebase initialization error
**Solution**: Ensure `google-services.json` is properly configured as a GitHub secret

**Issue**: Gradle build fails
**Solution**: Clear Gradle cache and rebuild:
```bash
./gradlew clean build --refresh-dependencies
```

**Issue**: Instrumented tests timeout
**Solution**: Increase timeout in workflow or use a faster emulator configuration

**Issue**: Lint errors block build
**Solution**: Fix lint errors or temporarily disable specific checks in `build.gradle.kts`

## Support

For issues with testing:
1. Check GitHub Actions logs for detailed error messages
2. Run tests locally to reproduce issues
3. Review test output and stack traces
4. Ensure all dependencies are up to date

## Resources

- [Android Testing Guide](https://developer.android.com/training/testing)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Gradle Test Documentation](https://docs.gradle.org/current/userguide/java_testing.html)

