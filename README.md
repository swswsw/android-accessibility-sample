# Accessibility Screenshot and Click Sample App

This sample application demonstrates how to use an Android `AccessibilityService` to capture a screenshot and programmatically perform a click gesture.

## Features

- **Accessibility Service**: Implements a custom `AccessibilityService` to interact with the device.
- **Screenshot Capture**: Uses the `takeScreenshot` API (available in Android 11+) to capture the screen.
- **Gesture Dispatching**: Uses `dispatchGesture` to simulate a click at a specific coordinate.
- **Jetpack Compose UI**: A simple interface to guide the user through enabling the service and triggering actions.

## Getting Started

### Prerequisites

- Android Studio Koala or newer.
- An Android device or emulator running Android 11 (API level 30) or higher.

### Installation

1. Clone or open the project in Android Studio.
2. Build and run the app on your device or emulator.

### How to Use

1. **Launch the App**: Open "My Application" on your device.
2. **Enable Accessibility Service**:
   - Click the **"Enable Accessibility Service"** button. This will open the system's Accessibility settings.
   - Locate **"My Application"** (or the app name) in the list of downloaded/installed services.
   - Toggle the switch to **ON**. Confirm any system dialogs that appear.
3. **Trigger Action**:
   - Return to the app.
   - Click the **"Take Screenshot and Click at (500, 500)"** button.
   - The app will request a screenshot via the service. Once successful, it will simulate a click at the coordinates (500, 500) on your screen.
   - **Note**: After clicking the button, you can see the action logs (screenshot success and click completion) in the **Logcat** window in Android Studio.

## Project Structure

- `MyAccessibilityService.kt`: Contains the service logic for screenshots and clicks.
- `MainActivity.kt`: The main UI of the application.
- `AndroidManifest.xml`: Configures the service and required permissions.
- `accessibility_service_config.xml`: Defines the capabilities of the accessibility service.

## Verification

You can verify the actions by checking the **Logcat** in Android Studio. Look for tags like `MyAccessibilityService` to see logs for:
- "Screenshot taken successfully"
- "Click completed at (500, 500)"

## Permissions

The app uses the following specialized permission:
- `android.permission.BIND_ACCESSIBILITY_SERVICE`: Allows the app to function as an accessibility service.
