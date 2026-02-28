# LogcatToolkit

### About this `logcatviewer` fork
This fork is named **LogcatToolkit** to distinguish it from the original `logcatviewer`.

It is a customizable Logcat utility for Android (SDK 19+), featuring
background logcat dumping and native binary format parsing.

#### Additional Features
What makes this library different from the original `logcatviewer`:
- **SDK 19 Support:** Specifically aimed to work with Android SDK 19 (KitKat) and above.
- **Improved Parsing:** Uses `LogcatBinaryParser.kt`
  (from `de.brudaswen.android.logcat.core.parser`) for better handling of system logs.
- **Highly Customizable:** Allows customized output formats and global
  library settings (see Example 1 below).
- **LogcatDumper:** Safely dump a specific time period of logs leading up to a crash.
  It runs in the background and is manually triggered by the user (see Example 2 below).

#### Technical Improvements (Internal)
- **Modern Stack:** Fully migrated to **Kotlin Flow** and **Coroutines**.
- **Refactored Core:** Extracted common logic into `LogcatReader` for better
  maintainability.
- **Cleanup:** Fixed various typos and improved overall code structure.

##### code example (1) global library settings
Define a method to change the default config to your preference

```kotlin
fun setupCustomLogcatSettings() {
    val customLogfileFormat = object : LogFileFormat {
        override suspend fun writeLogs(
            logFileName: String,
            logs: Array<LogItem>,
            writer: BufferedWriter
        ) {
            if (logs.isNotEmpty()) {
                writer.write("Logcat: $logFileName\n")
                writer.write("------------------\n")
                for (log in logs) {
                    writer.write(log.origin + "\n")
                }
            }
        }
    }

    val customLogFileName: LogFileName = object : LogFileName {
        override fun getLogFileName(): String {
            val dateFormat =
                SimpleDateFormat("'${BuildConfig.FLAVOR}_'yyyy-MM-dd_HH:mm:ss.SSS'.log'", Locale.ROOT)
            return dateFormat.format(Date())
        }
    }

    // overwrite parts of the LogFileShareDefault implementation
    val customLogFileShare = object : LogFileShareDefault() {
        override fun launchIntent(context: Context, shareIntent: Intent): Boolean {
            shareIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            YourShareUtils.openIntent(context, shareIntent)
            return true // we want to handle error messaging ourselves in YourShareUtils.openIntent()
        }
    }

    var customCleanupStrategy: CleanupConfig = CleanupConfig(
        object : LogFileDeleteStrategy {
            override fun apply(logDir: File, threshold: Int) {
                // implement your own strategy. Eg. keep all files with a special keyword.
                // Or you could also override an existing implementation
                // from settings/LogFileDeleteStrategy.kt
            }

        }, 20)

    val customLogStorageLocation: ExportLogFileUtils.StorageLocation =
        ExportLogFileUtils.StorageLocation.CACHE_EXTERNAL

    Settings.Default.update { current ->
        current.copy(
            logfileFormat = customLogfileFormat,
            logFileName = customLogFileName,
            logFileShare = customLogFileShare,
            logCleanupStrategy = customCleanupStrategy,
            logStorageLocation = customLogStorageLocation
        )
    }
}
```
##### code example (2) use LogcatDumper
If you just want to have the e.g. last 60 seconds before a crash happend you can
use LogcatDumper like this:
```kotlin
class CustomLogcatDumper {

    /**
     * we want the timestamp to set the filename.
     */
    class CustomLogFileNameFromTimestamp : LogcatDumper.LogFileNameFromTimestamp {

        private var timestamp: Long = 0

        override fun getLogFileName(): String {
            return "custom_${timestamp}.log"
        }

        override fun setTimestamp(timestamp: Long) {
            this.timestamp = timestamp
        }
    }

    class CustomLogFileFormat : LogFileFormat {
        override suspend fun writeLogs(
            logFileName: String,
            logs: Array<LogItem>,
            writer: BufferedWriter
        ) {
            if (logs.isNotEmpty()) {
                writer.write("Logcat: $logFileName\n")
                writer.write("------------------\n")
                for (log in logs) {
                    writer.write(log.origin + "\n")
                }
            }
        }
    }

    companion object {
        private val logcatDump = LogcatDumper(
            App.getApplicationContext(), // get the application context from somewhere
            CustomLogFileNameFromTimestamp(),
            CustomLogFileFormat(),
            ExportLogFileUtils.StorageLocation.CACHE_INTERNAL
        )
        private const val CAPTURE_PERIOD_BEFORE_TIMESTAMP: Long = 60 * 1000

        fun triggerLogCapture() {
            val time = System.currentTimeMillis()
            logcatDump.dump(time, CAPTURE_PERIOD_BEFORE_TIMESTAMP)
        }
    }
}

```
Now you could just call `CustomLogFileFormat.triggerLogCapture()` to create logfile
called `${timestamp}.log` containing only the last
`CAPTURE_PERIOD_BEFORE_TIMESTAMP` logcat entries.

#### Repository
Releases are distributed via jitpack. Make sure you include jitpack in
your `build.gradle` or `settings.gradle.kts`:

```gradle
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url = uri("https://jitpack.io") }
	}
}
```

```gradle
dependencies {
        implementation("com.github.evermind-zz:logcat-toolkit:Tag")
}
```

---
original README:
# LogcatViewer library
### Feature:

- Priority filter
- Clear logcat
- Export as file 
- Floating window

### Integrate guide

1. Clone this library as a project module, add module dependence.

2. Add launch code in your code:

    - Start logcat viewer
    ```kotlin
    LogcatActivity.start(getContext())
    ```
   
   - Start logcat viewer with log exclude rule
   ```kotlin
   val logcatExcludeRules = listOf(
       Pattern.compile(".*]: processMotionEvent MotionEvent \\{ action=ACTION_.*"),
       Pattern.compile(".*]: dispatchPointerEvent handled=true, event=MotionEvent \\{ action=ACTION_.*")
   )
   LogcatActivity.start(getContext(), logcatExcludeRules)
   ```

### Screenshot

<img src="https://raw.githubusercontent.com/kyze8439690/logcatviewer/master/screenshot/1.jpg" width="360">
<img src="https://raw.githubusercontent.com/kyze8439690/logcatviewer/master/screenshot/2.jpg" width="360">
<img src="https://raw.githubusercontent.com/kyze8439690/logcatviewer/master/screenshot/3.jpg" width="360">
<img src="https://raw.githubusercontent.com/kyze8439690/logcatviewer/master/screenshot/4.jpg" width="360">
