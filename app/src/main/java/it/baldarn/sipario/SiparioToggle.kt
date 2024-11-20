package it.baldarn.sipario

import android.Manifest
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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import it.baldarn.sipario.databinding.FragmentSiparioToggleBinding
import java.nio.charset.Charset
import java.util.UUID

class SiparioToggle : Fragment() {

    private var PERSISTENT_SIPARIO_MODE_NOTIFICATION_ID = 1

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser

    private lateinit var notificationBuilder: NotificationCompat.Builder

    private var _binding: FragmentSiparioToggleBinding? = null
    private var currentRingerMode: Int? = null

    private var siparioModeOn = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        notificationBuilder = NotificationCompat.Builder(requireActivity().applicationContext, "my_channel_id")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Sipario Attivo")
            .setContentText("Goditi lo spettacolo")
            .setOngoing(true) // Make it persistent
            .setPriority(NotificationCompat.PRIORITY_LOW) // Or other priority levels

        val bluetoothManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        _binding = FragmentSiparioToggleBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.siparioToggle.setOnClickListener {
            siparioModeOn = !siparioModeOn
            if (siparioModeOn) {
                siparioModeOn()
            } else {
                siparioModeOff()
            }
        }

        binding.showPoints.setOnClickListener { _ ->
            val bundle = Bundle().apply { putString("path", "${BuildConfig.BACKEND_URL}/point_events") }
            findNavController().navigate(R.id.action_SiparioToggle_to_WebView, bundle)
        }

        binding.logOut.setOnClickListener { _ ->
            SharedPrefsHelper.saveJwtToken(requireActivity().applicationContext,null)
            findNavController().navigate(R.id.action_SiparioToggle_to_SignIn)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun siparioModeOn() {
        val isDndEnabled = requireActivity().getSystemService(NotificationManager::class.java).currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        if (!isDndEnabled) {
            val audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            currentRingerMode = audioManager.ringerMode
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        } else {
            // Handle DND scenario (e.g., show a message to the user)
        }

        binding.siparioToggle.setText(R.string.sipario_off)
        requireActivity().getSystemService(NotificationManager::class.java).notify(PERSISTENT_SIPARIO_MODE_NOTIFICATION_ID, notificationBuilder.build())

        // Abilita il Bluetooth se non è già attivo
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            if (ActivityCompat.checkSelfPermission(
                    requireActivity().applicationContext,
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
        val isDndEnabled = requireActivity().getSystemService(NotificationManager::class.java).currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        if (!isDndEnabled) {
            if (currentRingerMode != null) {
                val audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.ringerMode = currentRingerMode as Int
            }
        } else {
            // Handle DND scenario (e.g., show a message to the user)
        }
        binding.siparioToggle.setText(R.string.sipario_on)

        requireActivity().getSystemService(NotificationManager::class.java).cancel(PERSISTENT_SIPARIO_MODE_NOTIFICATION_ID)

        if (ActivityCompat.checkSelfPermission(
                requireActivity().applicationContext,
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
                requireActivity().applicationContext,
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
                requireActivity().applicationContext,
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
