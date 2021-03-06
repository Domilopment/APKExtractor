package domilopment.apkextractor.utils

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import domilopment.apkextractor.autoBackup.AutoBackupService
import domilopment.apkextractor.data.Application
import domilopment.apkextractor.data.ListOfAPKs
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Comparator
import kotlin.jvm.Throws

class SettingsManager(context: Context) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val packageManager = context.packageManager

    companion object {
        // Sort types for App List
        const val SORT_BY_NAME = 0
        const val SORT_BY_PACKAGE = 1
        const val SORT_BY_INSTALL_TIME = 2
        const val SORT_BY_UPDATE_TIME = 3
    }

    /**
     * Creates a List containing of all Types the User Selected in Settings
     * @return List of Selected App Types
     */
    fun selectedAppTypes(): List<Application> {
        val mData: MutableList<Application> = mutableListOf()
        if (sharedPreferences.getBoolean("updated_system_apps", false)) {
            mData.addAll(ListOfAPKs(packageManager).updatedSystemApps)
            if (sharedPreferences.getBoolean("system_apps", false))
                mData.addAll(ListOfAPKs(packageManager).systemApps)
        }
        if (sharedPreferences.getBoolean("user_apps", true))
            mData.addAll(ListOfAPKs(packageManager).userApps)
        return sortData(mData)
    }


    /**
     * Gives back in SharedPreferences Saved Directory Path
     * @return Saved Directory Path
     */
    fun saveDir(): Uri? = sharedPreferences.getString("dir", null)?.let { Uri.parse(it) }


    /**
     * Sorts Data by user selected Order
     * @param data Unsorted List of APKs
     * @return Sorted List of APKs
     * @throws Exception if given sort type doesn't exist
     */
    @Throws(Exception::class)
    fun sortData(data: List<Application>, sortMode: Int = sharedPreferences.getInt("app_sort", SORT_BY_NAME)): List<Application> {
        return when (sortMode) {
            SORT_BY_NAME -> data.sortedWith(Comparator.comparing(Application::appName))
            SORT_BY_PACKAGE -> data.sortedWith(Comparator.comparing(Application::appPackageName))
            SORT_BY_INSTALL_TIME -> data.sortedWith(Comparator.comparing(Application::appInstallTime).reversed())
            SORT_BY_UPDATE_TIME -> data.sortedWith(Comparator.comparing(Application::appUpdateTime).reversed())
            else -> throw Exception("No such sort type")
        }
    }

    /**
     * Switch ui mode (System, Light, Dark) either with given Parameter or with saved Preference
     * @param newValue Int castable String value to switch ui mode
     */
    fun changeUIMode(
        newValue: String = sharedPreferences.getString(
            "list_preference_ui_mode",
            "0"
        )!!
    ) {
        when (newValue.toInt()) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    /**
     * Gets an app and Creates a Name from its Data
     * @param app the resource App
     * @return String of the name after the APK should be named
     */
    fun appName(app: Application): String {
        val sb = StringBuilder()
        sharedPreferences.getStringSet("app_save_name", setOf())?.also { prefs ->
            if (prefs.contains("name")) sb.append(app.appName)
            if (prefs.contains("package")) sb.append(" ${app.appPackageName}")
            if (prefs.contains("version_name")) sb.append(" ${app.appVersionCode}")
            if (prefs.contains("version_number")) sb.append(" v${app.appVersionName}")
            if (prefs.contains("datetime")) sb.append(" ${SimpleDateFormat.getDateTimeInstance().format(Date())}")
        }
        if (sb.isEmpty()) sb.append(app.appName)
        return sb.append(FileHelper.PREFIX).toString()
    }

    /**
     * Get set of Packages that should be looked for to Auto Backup
     * @return A Set of Package Names
     */
    fun listOfAutoBackupApps(): Set<String>? {
        return sharedPreferences.getStringSet("app_list_auto_backup", setOf())
    }

    /**
     * Tells an Activity if AutoBackupService should be started
     * @return true if Service isn't running and should be started
     */
    fun shouldStartService(): Boolean {
        val pref = sharedPreferences.getBoolean("auto_backup", false)
        val service = AutoBackupService.isRunning
        return pref and !service
    }
}