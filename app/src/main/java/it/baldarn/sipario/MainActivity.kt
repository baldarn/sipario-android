package it.baldarn.sipario

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import it.baldarn.sipario.databinding.ActivityMainBinding
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    private var REQUEST_CODE_PERMISSIONS = 123
    private val screenEventReceiver = ScreenEventReceiver()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        requestPermissions()

        val channelId = "my_channel_id"
        val channelName = "My Channel Name"
        val importance = NotificationManager.IMPORTANCE_LOW // Or other importance levels
        val channel = NotificationChannel(channelId, channelName, importance)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenEventReceiver, filter)
    }

    inner class ScreenEventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> Log.d("ScreenEventActivity", "Schermo acceso")
                Intent.ACTION_SCREEN_OFF -> Log.d("ScreenEventActivity", "Schermo spento")
                Intent.ACTION_USER_PRESENT -> Log.d("ScreenEventActivity", "Dispositivo sbloccato")
                Intent.ACTION_USER_UNLOCKED -> Log.d("ScreenEventActivity", "Sloccato")
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, permissions, 123)
            } else {
                // Permission already granted, proceed with creating the notification
            }
        } else {
            // For Android versions below Tiramisu, notification permission is granted by default
        }

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } else {
            // DND access granted, proceed with modifying audio settings
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun sendDeviceToken() {
        val bearerToken = SharedPrefsHelper.getJwtToken(applicationContext)
        val notificationToken = SharedPrefsHelper.getNotificationToken(applicationContext)
        if (bearerToken != null && notificationToken != null) {
            Fuel.post("${BuildConfig.BACKEND_URL}/devices.json")
                .header("Authorization", bearerToken)
                .header("Content-Type" to "application/json")
                .body(
                    "{\"token\":\"$notificationToken\",\"platform\":\"android\"}",
                    Charset.forName("UTF-8")
                )
                .response { _, _, res ->
                    when (res) {
                        is Result.Success -> {
                        }

                        is Result.Failure -> {
                        }
                    }
                }
        }
    }
}
