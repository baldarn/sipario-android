package it.baldarn.sipario

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioManager
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.result.Result
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.annotations.SerializedName
import it.baldarn.sipario.databinding.FragmentSiparioToggleBinding
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.util.UUID


class SiparioToggle : Fragment() {

    private var PERSISTENT_SIPARIO_MODE_NOTIFICATION_ID = 1

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var siparioSessionId: String
    private lateinit var providers: Array<Provider>


    private var _binding: FragmentSiparioToggleBinding? = null
    private var currentRingerMode: Int? = null

    private var siparioModeOn = false
    private lateinit var siparioSessionStartedAt: LocalDateTime
    private lateinit var providerForSession: Provider

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        notificationBuilder =
            NotificationCompat.Builder(requireActivity().applicationContext, "my_channel_id")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Sipario Attivo")
                .setContentText("Goditi lo spettacolo")
                .setOngoing(true) // Make it persistent
                .setPriority(NotificationCompat.PRIORITY_LOW) // Or other priority levels

        val bluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

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

        }

        bluetoothAdapter.setName("ShortName")
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity().applicationContext)
        getLastLocation()

        _binding = FragmentSiparioToggleBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.siparioToggle.visibility = View.INVISIBLE

        binding.siparioToggle.setOnClickListener {
            siparioModeOn = !siparioModeOn
            if (siparioModeOn) {
                siparioModeOn()
            } else {
                siparioModeOff()
            }
        }

        binding.resetPositionButton.setOnClickListener {
            getLastLocation()
        }

        binding.showPoints.setOnClickListener { _ ->
            val bundle =
                Bundle().apply { putString("path", "${BuildConfig.BACKEND_URL}/point_events") }
            findNavController().navigate(R.id.action_SiparioToggle_to_WebView, bundle)
        }

        binding.logOut.setOnClickListener { _ ->
            SharedPrefsHelper.saveJwtToken(requireActivity().applicationContext, null)
            findNavController().navigate(R.id.action_SiparioToggle_to_SignIn)
        }
    }

    override fun onDestroyView() {
        siparioModeOff()
        super.onDestroyView()
        _binding = null
    }

    private fun siparioModeOn() {
        val isDndEnabled =
            requireActivity().getSystemService(NotificationManager::class.java).currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        if (!isDndEnabled) {
            val audioManager =
                requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            currentRingerMode = audioManager.ringerMode
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        } else {
            // Handle DND scenario (e.g., show a message to the user)
        }

        setSiparioToggleLabel(getString(R.string.sipario_off))

        requireActivity().getSystemService(NotificationManager::class.java)
            .notify(PERSISTENT_SIPARIO_MODE_NOTIFICATION_ID, notificationBuilder.build())

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

        providerForSession = providers.first()

        binding.resetPositionButton.visibility = View.INVISIBLE
        binding.showPoints.visibility = View.INVISIBLE
        binding.logOut.visibility = View.INVISIBLE

        siparioSessionStartedAt = LocalDateTime.now()

        Toast.makeText(
            requireActivity(),
            "You will need to keep the sipario session on for ${providerForSession.minutesForPoints} minutes!",
            Toast.LENGTH_LONG
        ).show()

        startAdvertising()
        startScanning()
    }

    private fun siparioModeOff() {
        val isDndEnabled =
            requireActivity().getSystemService(NotificationManager::class.java).currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        if (!isDndEnabled) {
            if (currentRingerMode != null) {
                val audioManager =
                    requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.ringerMode = currentRingerMode as Int
            }
        } else {
            // Handle DND scenario (e.g., show a message to the user)
        }
        setSiparioToggleLabel(getString(R.string.sipario_on))

        requireActivity().getSystemService(NotificationManager::class.java)
            .cancel(PERSISTENT_SIPARIO_MODE_NOTIFICATION_ID)

        if (ActivityCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        binding.resetPositionButton.visibility = View.VISIBLE
        binding.showPoints.visibility = View.VISIBLE
        binding.logOut.visibility = View.VISIBLE

        bluetoothLeScanner.stopScan(leScanCallback)
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)

        var timeNow = LocalDateTime.now()

        if (siparioSessionStartedAt.isAfter(timeNow.plusMinutes(providerForSession.minutesForPoints))) {
            sendSiparioSession()
        } else {
            Toast.makeText(
                requireActivity(),
                "You did not wait for ${providerForSession.minutesForPoints} minutes!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord
            val serviceUuids = scanRecord?.serviceUuids

            serviceUuids?.forEach { uuid ->
                Log.d("BLE", "Service UUID trovato: ${uuid.uuid}")
            }
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

        val siparioUUID = ParcelUuid(UUID.fromString("c675512d-0c57-48bc-8886-a5039d513087"))

        val filter = ScanFilter.Builder()
            .setServiceUuid(siparioUUID)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothLeScanner.startScan(listOf(filter), settings, leScanCallback)
    }

    private fun startAdvertising() {
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val siparioUUID = ParcelUuid(UUID.fromString("c675512d-0c57-48bc-8886-a5039d513087"))
        val data = AdvertiseData.Builder()
            .addServiceUuid(siparioUUID)
            .setIncludeDeviceName(true)
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
        siparioSessionId = generateRandomString(11)
        bluetoothAdapter.setName(siparioSessionId)
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

    fun generateRandomString(length: Int = 8): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    val bearerToken =
                        SharedPrefsHelper.getJwtToken(requireActivity().applicationContext)
                    if (bearerToken != null) {
                        Fuel.get("${BuildConfig.BACKEND_URL}/providers/search.json?lat=${latitude}&lon=${longitude}")
                            .header("Authorization", bearerToken)
                            .header("Content-Type" to "application/json")
                            .responseObject<Array<Provider>> { _, _, result ->
                                result.fold(
                                    success = { providers ->
                                        this.providers = providers

                                        if (providers.isNotEmpty()) {
                                            setSiparioToggleLabel(getString(R.string.sipario_on))
                                            binding.siparioToggle.visibility = View.VISIBLE
                                        } else {
                                            binding.siparioToggle.visibility = View.INVISIBLE
                                        }
                                    },
                                    failure = { error ->
                                    }
                                )
                            }
                    }
                }
            }
    }


    data class Provider(
        @SerializedName("id") val id: Long,
        @SerializedName("name") val name: String,
        @SerializedName("minutes_for_points") val minutesForPoints: Long
    )

    private fun setSiparioToggleLabel(label: String) {
        binding.siparioToggle.text = label.plus(" ").plus(providers.first().name)
    }

    private fun sendSiparioSession() {
        val bearerToken = SharedPrefsHelper.getJwtToken(requireActivity().applicationContext)
        if (bearerToken != null) {
            Fuel.post("${BuildConfig.BACKEND_URL}/sipario_sessions.json")
                .header("Authorization", bearerToken)
                .header("Content-Type" to "application/json")
                .body(
                    "{\"provider_id\":\"${providerForSession.id}\", \"device_identifier\":\"$siparioSessionId\"}",
                    Charset.forName("UTF-8")
                ).response { _, _, res ->
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
