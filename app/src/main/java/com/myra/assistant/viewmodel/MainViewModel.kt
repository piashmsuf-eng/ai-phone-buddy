package com.myra.assistant.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.myra.assistant.model.AppCommand
import com.myra.assistant.service.AccessibilityHelperService
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val commandResult = MutableLiveData<String?>()

    fun executeCommand(cmd: AppCommand) {
        viewModelScope.launch(Dispatchers.IO) {
            if (cmd.type != "prime_call" && cmd.type != "prime_msg" && !AccessibilityHelperService.isEnabled(getApplication())) {
                withContext(Dispatchers.Main) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    getApplication<Application>().startActivity(intent)
                }
                return@launch
            }
            try {
                when (cmd.type) {
                    "open_app" -> openApp(cmd.params["package"] ?: "", cmd.params["name"] ?: "")
                    "close_app" -> AccessibilityHelperService.instance?.closeCurrentApp()
                    "call" -> makeCall(cmd.params["name"] ?: "")
                    "prime_call" -> primeCall(cmd.params["index"]?.toIntOrNull() ?: 0)
                    "prime_msg" -> primeMsg(cmd.params["index"]?.toIntOrNull() ?: 0, cmd.params["message"] ?: "")
                    "volume_up" -> adjustVolume(AudioManager.ADJUST_RAISE)
                    "volume_down" -> adjustVolume(AudioManager.ADJUST_LOWER)
                    "flashlight_on" -> toggleFlashlight(true)
                    "flashlight_off" -> toggleFlashlight(false)
                    "wifi_on" -> toggleWifi(true)
                    "wifi_off" -> toggleWifi(false)
                    "bluetooth_on" -> toggleBluetooth(true)
                    "bluetooth_off" -> toggleBluetooth(false)
                }
            } catch (e: Exception) {
                Log.e("MYRA_VM", "Command error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openApp(pkg: String, name: String) {
        val ctx = getApplication<Application>()
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } else {
            val allApps = ctx.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            val found = allApps.find { ctx.packageManager.getApplicationLabel(it).toString().lowercase() == name.lowercase() }
            if (found != null) {
                val i = ctx.packageManager.getLaunchIntentForPackage(found.packageName)
                i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i?.let { ctx.startActivity(it) }
            }
        }
    }

    private fun makeCall(name: String) {
        val ctx = getApplication<Application>()
        val number = resolveContact(name)
        if (number != null) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        }
    }

    private fun primeCall(index: Int) {
        val contacts = getPrimeContacts()
        if (index < contacts.size) {
            val number = contacts[index].optString("number")
            if (number.isNotBlank()) {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(intent)
            }
        }
    }

    private fun primeMsg(index: Int, message: String) {
        val contacts = getPrimeContacts()
        if (index < contacts.size) {
            val number = contacts[index].optString("number")
            if (number.isNotBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=${Uri.encode(message)}"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(intent)
            }
        }
    }

    private fun getPrimeContacts(): List<JSONObject> {
        val prefs = getApplication<Application>().getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("prime_contacts_json", null)
        if (json == null) {
            val legacyName = prefs.getString("prime_name", null)
            val legacyNumber = prefs.getString("prime_number", null)
            if (legacyName != null && legacyNumber != null) {
                return listOf(JSONObject().apply { put("name", legacyName); put("number", legacyNumber) })
            }
            return emptyList()
        }
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun resolveContact(name: String): String? {
        val ctx = getApplication<Application>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        ctx.contentResolver.query(uri, projection, selection, arrayOf("%$name%"), null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return null
    }

    private fun adjustVolume(direction: Int) {
        val am = getApplication<Application>().getSystemService(AudioManager::class.java) as AudioManager
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
    }

    private fun toggleFlashlight(on: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cm = getApplication<Application>().getSystemService(CameraManager::class.java) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull() ?: return
            cm.setTorchMode(cameraId, on)
        }
    }

    private fun toggleWifi(on: Boolean) {
        val wm = getApplication<Application>().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wm.isWifiEnabled = on
    }

    private fun toggleBluetooth(on: Boolean) {
        val bm = getApplication<Application>().getSystemService(BluetoothManager::class.java) as BluetoothManager
        val adapter = bm.adapter ?: return
        if (on) adapter.enable() else adapter.disable()
    }
}
