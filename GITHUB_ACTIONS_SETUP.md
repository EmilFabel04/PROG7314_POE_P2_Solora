# GitHub Actions Setup Guide for Solora App

This guide will help you set up automated testing and building for your Solora Android app using GitHub Actions.

## Prerequisites

- GitHub repository with your Solora app code
- Firebase project with `google-services.json` file
- GitHub account with repository access

## Step-by-Step Setup

### Step 1: Add Workflow File

The workflow file `.github/workflows/android-ci.yml` has already been added to your repository. This file defines the CI/CD pipeline.

### Step 2: Set Up GitHub Secrets

You need to add your Firebase configuration as a secret:

#### 2.1 Encode google-services.json

Open terminal in your project directory and run:

```bash
cat app/google-services.json | base64
```

Or on Windows:
```powershell
[Convert]::ToBase64String([System.IO.File]::ReadAllBytes("app\google-services.json"))
```

Copy the entire output (it will be a long string).

#### 2.2 Add Secret to GitHub

1. Go to your GitHub repository
2. Click on **Settings** tab
3. In the left sidebar, click **Secrets and variables** → **Actions**
4. Click **New repository secret** button
5. Set the following:
   - **Name**: `GOOGLE_SERVICES_JSON`
   - **Value**: Paste the base64-encoded string from step 2.1
6. Click **Add secret**

### Step 3: Commit and Push

```bash
git add .
git commit -m "Add GitHub Actions CI/CD workflow and unit tests"
git push origin main
```

### Step 4: Verify Workflow

1. Go to your GitHub repository
2. Click on the **Actions** tab
3. You should see the "Android CI" workflow running
4. Click on the workflow to see detailed logs

## What the Workflow Does

### On Every Push/PR to main or develop:

1. **Build Job**:
   - ✅ Sets up Java 17 environment
   - ✅ Caches Gradle dependencies for faster builds
   - ✅ Decodes and places Firebase configuration
   - ✅ Builds the entire project
   - ✅ Runs all unit tests
   - ✅ Runs lint checks for code quality
   - ✅ Builds debug APK
   - ✅ Uploads build artifacts (APK, test results, reports)
   - ✅ Generates code coverage reports

2. **Instrumented Test Job**:
   - ✅ Runs tests on Android emulator
   - ✅ Tests UI functionality
   - ✅ Uploads test results

## Viewing Results

### Build Status

After pushing code, check the Actions tab to see:
- ✅ Green checkmark = All tests passed
- ❌ Red X = Tests failed or build error
- 🟡 Yellow dot = Build in progress

### Artifacts

After each build, you can download:
- **debug-apk**: The compiled Android application
- **test-results**: Unit test results
- **lint-reports**: Code quality reports
- **coverage-reports**: Test coverage statistics

To download artifacts:
1. Go to Actions tab
2. Click on a completed workflow run
3. Scroll to "Artifacts" section at the bottom
4. Click to download

### Test Reports

View test results directly in the Actions logs:
1. Click on a workflow run
2. Click on "build" or "instrumented-test" job
3. Expand steps to see detailed output
4. Failed tests will show error messages and stack traces

## Adding Status Badge to README

Add this badge to your README.md to show build status:

```markdown
## Build Status

![Android CI](https://github.com/YOUR_USERNAME/YOUR_REPO_NAME/workflows/Android%20CI/badge.svg)
```

Replace `YOUR_USERNAME` and `YOUR_REPO_NAME` with your actual GitHub username and repository name.

## Running Tests Locally

Before pushing, run tests locally:

```bash
# Run unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Build project
./gradlew build

# Run instrumented tests (requires emulator or device)
./gradlew connectedDebugAndroidTest
```

## Troubleshooting

### Issue: Workflow fails with "google-services.json not found"

**Solution**: Ensure you've correctly set up the `GOOGLE_SERVICES_JSON` secret:
1. Verify the secret exists in Settings → Secrets and variables → Actions
2. Check that the base64 encoding was done correctly
3. Make sure the secret name is exactly `GOOGLE_SERVICES_JSON`

### Issue: Build fails with Gradle errors

**Solution**: 
1. Check that your local build works: `./gradlew clean build`
2. Commit any missing Gradle wrapper files
3. Check Java version compatibility (workflow uses Java 17)

### Issue: Tests fail on CI but pass locally

**Solution**:
1. Check environment differences (time zones, locales)
2. Review test logs in Actions tab for specific errors
3. Ensure tests don't depend on local files or configurations
4. Check Firebase configuration is correct

### Issue: Instrumented tests timeout

**Solution**:
1. Increase timeout in workflow file
2. Simplify tests or split into smaller test suites
3. Use faster emulator configuration

### Issue: Artifact uploads fail

**Solution**:
1. Check that paths in workflow match actual build output locations
2. Ensure build completes successfully before artifact upload
3. Verify artifacts exist: `ls -la app/build/outputs/apk/debug/`

## Customization

### Change Trigger Branches

Edit `.github/workflows/android-ci.yml`:

```yaml
on:
  push:
    branches: [ main, develop, feature/* ]  # Add more branches
  pull_request:
    branches: [ main ]
```

### Change Emulator Configuration

Edit the instrumented test job:

```yaml
- name: Run instrumented tests
  uses: reactivecircus/android-emulator-runner@v2
  with:
    api-level: 30  # Change Android version
    target: google_apis  # Use Google APIs
    arch: x86_64
    profile: pixel_5  # Change device profile
```

### Add Release Build

Add this step after the debug build:

```yaml
- name: Build release APK
  run: ./gradlew assembleRelease --stacktrace
  
- name: Upload release APK
  uses: actions/upload-artifact@v4
  with:
    name: release-apk
    path: app/build/outputs/apk/release/app-release-unsigned.apk
```

## Best Practices

1. **Run Tests Locally First**: Always run `./gradlew test` before pushing
2. **Small Commits**: Make small, focused commits for easier debugging
3. **Descriptive Messages**: Use clear commit messages
4. **Check Actions**: Monitor the Actions tab after pushing
5. **Fix Failures Quickly**: Don't let failing builds accumulate
6. **Review Artifacts**: Regularly check generated reports
7. **Update Dependencies**: Keep testing libraries up to date

## Advanced Features

### Auto-Deploy to Play Store (Future)

Add this step to deploy successful builds:

```yaml
- name: Deploy to Play Store
  uses: r0adkll/upload-google-play@v1
  if: github.ref == 'refs/heads/main'
  with:
    serviceAccountJson: ${{ secrets.PLAY_STORE_JSON }}
    packageName: dev.solora
    releaseFiles: app/build/outputs/apk/release/app-release.apk
    track: internal
```

### Slack Notifications

Add notification step:

```yaml
- name: Notify Slack
  if: always()
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Code Coverage Reporting

Add coverage upload:

```yaml
- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v3
  with:
    files: app/build/reports/jacoco/test/jacocoTestReport.xml
    token: ${{ secrets.CODECOV_TOKEN }}
```

## Support Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android CI/CD Best Practices](https://developer.android.com/studio/projects/continuous-integration)
- [Gradle Documentation](https://docs.gradle.org)
- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)

## Next Steps

1. ✅ Push workflow files to GitHub
2. ✅ Set up secrets
3. ✅ Verify first build succeeds
4. ⏳ Add more unit tests
5. ⏳ Add instrumented UI tests
6. ⏳ Set up automated releases
7. ⏳ Add deployment to Play Store

## Questions?

If you encounter issues:
1. Check Actions logs for detailed error messages
2. Review this guide's troubleshooting section
3. Ensure all secrets are properly configured
4. Test the build locally first
5. Check that all dependencies are available

