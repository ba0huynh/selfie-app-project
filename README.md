# Nhật ký Selfie - Selfie Diary App

An Android application for taking, storing, and managing private selfies daily. The app serves as an image diary that helps users track their personal changes through smart reminder features and a friendly interface.

## Features Implemented

### ✅ Core Features

1. **Private Photo Storage**
   - Photos are stored in application's private directory
   - `.nomedia` file prevents photos from appearing in Gallery
   - Images are not accessible by other apps

2. **Smart Reminder System**
   - Customizable reminder time (hour and minute)
   - Uses AlarmManager for daily reminders
   - Schedules automatically after device reboot
   - Only sends notification if no photo was taken that day

3. **Camera Integration**
   - Full CameraX integration for front-facing camera
   - Real-time preview
   - Permission handling with user-friendly UI
   - Capture and save photos

4. **Photo Grid View**
   - Displays all photos in a grid layout
   - Grouped by date with clear date headers
   - "Hôm nay" (Today) label for today's photos
   - Empty state with prompt to take first photo

5. **Photo Viewer**
   - Full-screen photo viewing
   - Pinch-to-zoom functionality (up to 3x)
   - Double-tap to toggle zoom
   - Delete photo with confirmation dialog
   - Navigate back easily

6. **Multi-Select Mode**
   - Long-press any photo to enter multi-select mode
   - Visual indicators for selected photos
   - Contextual action bar shows count of selected items
   - Bulk delete with confirmation

7. **Settings Screen**
   - Enable/disable reminders
   - Configure reminder time
   - Placeholder sections for:
     - Backup & Sync (under development)
     - Security settings (under development)
     - Time-lapse video creation (under development)

### 📦 Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM-like with Repository pattern
- **Camera**: CameraX (androidx.camera:camera-core, camera-camera2, camera-lifecycle, camera-view)
- **Image Loading**: Coil
- **Navigation**: Navigation Compose
- **Work Management**: WorkManager for background tasks
- **Permissions**: Accompanist Permissions
- **System UI**: Accompanist System UI Controller

### 🔧 Build & Run

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on device or emulator (requires camera hardware)

### 📱 Minimum Requirements

- Android 7.0 (API 24) or higher
- Front-facing camera (required)
- Storage permission
- Camera permission
- Notification permission (Android 13+)

### 🚀 Permissions

The app requests the following permissions:
- `CAMERA` - For taking photos
- `POST_NOTIFICATIONS` - For reminder notifications (Android 13+)
- `SCHEDULE_EXACT_ALARM` - For exact daily reminders
- `USE_EXACT_ALARM` - For exact daily reminders

### 📋 Features Under Development

1. **Photo Filters**
   - Apply color filters (black & white, sepia, vintage)
   - Adjust brightness, contrast, saturation

2. **Photo Editing**
   - Crop photos
   - Rotate photos
   - Flip photos horizontally/vertically

3. **Notes & Emojis**
   - Add text notes to each photo
   - Add emoji to represent daily mood
   - Display emoji on thumbnails in grid view

4. **"On This Day" Feature**
   - Display photos from same date in previous years
   - Nostalgic reminders of past moments

5. **Security**
   - PIN lock
   - Fingerprint authentication
   - Hide sensitive photos

6. **Backup & Sync**
   - Google Drive integration
   - Dropbox integration
   - Automatic backup

7. **Time-lapse Videos**
   - Create videos from photo collections
   - Select date range
   - Export and share

### 🏗️ Project Structure

```
app/src/main/java/com/example/selfie/
├── data/
│   ├── PhotoMetadata.kt          # Data class for photo metadata
│   ├── PhotoRepository.kt        # Repository for photo management
│   ├── PreferencesManager.kt     # SharedPreferences manager
│   └── ReminderSettings.kt       # Data class for reminder settings
├── ui/
│   ├── camera/
│   │   └── CameraScreen.kt       # Camera interface
│   ├── photoGrid/
│   │   └── PhotoGridScreen.kt   # Main photo grid view
│   ├── photoViewer/
│   │   └── PhotoViewerScreen.kt # Full-screen photo viewer
│   └── settings/
│       └── SettingsScreen.kt    # Settings interface
├── work/
│   ├── ReminderReceiver.kt      # Broadcast receiver for reminders
│   ├── ReminderScheduler.kt    # Alarm scheduling
│   └── ReminderWorker.kt        # WorkManager worker
├── util/
│   ├── Extensions.kt            # Helper extension functions
│   ├── NoMediaUtils.kt         # .nomedia file utilities
│   └── Route.kt                # Navigation helpers
├── theme/                       # Material 3 theme
└── MainActivity.kt             # Main activity with navigation

```

### 🎨 UI/UX Highlights

- Clean Material Design 3 interface
- Vietnamese language support
- Dark mode support (via system theme)
- Smooth animations and transitions
- Intuitive gesture controls

### 🔒 Privacy

- All photos stored in app's private directory
- Not accessible by file managers (unless rooted)
- Does not appear in Gallery app
- No cloud storage by default
- No analytics or tracking

### 📝 License

This project is for educational purposes.

---

**Note**: This is a work-in-progress application. Some features mentioned in requirements are still under development or require additional implementation.

