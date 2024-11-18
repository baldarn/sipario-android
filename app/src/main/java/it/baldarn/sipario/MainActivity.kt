package it.baldarn.sipario

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import java.util.UUID


class MainActivity : ComponentActivity() {

    private var REQUEST_CODE_PERMISSIONS = 123
    private var PERSISTENT_SIPARIO_MODE_NOTIFICATION_ID = 1
    private var siparioModeOn = false
    private val screenEventReceiver = ScreenEventReceiver()
    private var currentRingerMode: Int? = null

    private lateinit var siparioToggleButton: Button
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser

    val notificationBuilder = NotificationCompat.Builder(this, "my_channel_id")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Sipario Attivo")
        .setContentText("Goditi lo spettacolo")
        .setOngoing(true) // Make it persistent
        .setPriority(NotificationCompat.PRIORITY_LOW) // Or other priority levels


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPermissions()

        // Ottieni l'adattatore Bluetooth
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

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

        setContentView(R.layout.activity_main)

        siparioToggleButton = findViewById<Button>(R.id.sipario_toggle)

        siparioToggleButton.setOnClickListener {
            siparioModeOn = !siparioModeOn
            if (siparioModeOn) {
                siparioModeOn()
            } else {
                siparioModeOff()
            }
        }
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

    private fun requestPermissions(){
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 123)
            } else {
                // Permission already granted, proceed with creating the notification
            }
        } else {
            // For Android versions below Tiramisu, notification permission is granted by default
        }

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            } else {
                // DND access granted, proceed with modifying audio settings
            }
        }
    }

    private fun siparioModeOn() {
        val isDndEnabled = getSystemService(NotificationManager::class.java).currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        if (!isDndEnabled) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            currentRingerMode = audioManager.ringerMode
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        } else {
            // Handle DND scenario (e.g., show a message to the user)
        }

        siparioToggleButton.setText(R.string.sipario_off)
        getSystemService(NotificationManager::class.java).notify(PERSISTENT_SIPARIO_MODE_NOTIFICATION_ID, notificationBuilder.build())

        // Abilita il Bluetooth se non è già attivo
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            startActivity(enableBtIntent)
        }

        startAdvertising()
        startScanning()
    }

    private fun siparioModeOff() {
        val isDndEnabled = getSystemService(NotificationManager::class.java).currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        if (!isDndEnabled) {
            if (currentRingerMode != null) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.ringerMode = currentRingerMode as Int
            }
        } else {
            // Handle DND scenario (e.g., show a message to the user)
        }
        siparioToggleButton.setText(R.string.sipario_on)
        getSystemService(NotificationManager::class.java).cancel(PERSISTENT_SIPARIO_MODE_NOTIFICATION_ID)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        bluetoothLeScanner.stopScan(leScanCallback)
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device: BluetoothDevice = result.device
            // Ottieni l'ID del dispositivo remoto (ad esempio, l'indirizzo MAC)
            val deviceId = device.address
            // Gestisci l'ID come necessario
            Log.d("ScanResult", "Dispositivo trovato: $deviceId")
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                // Gestisci i risultati della scansione in batch
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // Gestisci l'errore di scansione
            Log.e("ScanFailed", "Errore di scansione: $errorCode")
        }
    }

    private fun startScanning() {
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        bluetoothLeScanner.startScan(leScanCallback)
    }

    private fun startAdvertising() {
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")))
            .setIncludeDeviceName(false)
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            // Pubblicità iniziata con successo
            Log.d("Advertise", "Advertising iniziato con successo")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            // Gestisci l'errore
            Log.e("Advertise", "Errore nell'advertising: $errorCode")
        }
    }
}
