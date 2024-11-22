package it.baldarn.sipario

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import it.baldarn.sipario.databinding.FragmentOwnerDashboardBinding
import it.baldarn.sipario.databinding.FragmentSignUpBinding
import java.nio.charset.Charset
import java.util.UUID

class OwnerDashboard : Fragment() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    private var _binding: FragmentOwnerDashboardBinding? = null
    private var currentRingerMode: Int? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val bluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        _binding = FragmentOwnerDashboardBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.logOut.setOnClickListener { _ ->
            SharedPrefsHelper.saveOwnerJwtToken(requireActivity().applicationContext, null)
            findNavController().navigate(R.id.action_OwnerDashboard_to_SignIn)
        }

        binding.scanQr.setOnClickListener { _ ->
            findNavController().navigate(R.id.action_OwnerDashboard_to_ScanQr)
        }

        startScanning()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord
            val serviceUuids = scanRecord?.serviceUuids

            serviceUuids?.forEach { uuid ->
                Log.d("BLE", "Service UUID trovato: ${uuid.uuid}")
            }

            if (ActivityCompat.checkSelfPermission(
                    requireActivity().applicationContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            val deviceId = result.device.name

            val requestBody = "{\"certified_presence\":{\"device_identifier\":\"${deviceId}\"}}"
            val bearerToken =
                SharedPrefsHelper.getOwnerJwtToken(requireActivity().applicationContext)

            if (bearerToken != null) {
                Fuel.post("${BuildConfig.BACKEND_URL}/certified_presences.json")
                    .header("Content-Type" to "application/json")
                    .header("Authorization", bearerToken)
                    .body(requestBody, Charset.forName("UTF-8"))
                    .response { _, response, result ->
                        when (result) {
                            is Result.Success -> {
                            }

                            is Result.Failure -> {
                            }
                        }
                    }

            }
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

        val siparioUUID = ParcelUuid(UUID.fromString("c675512d-0c57-48bc-8886-a5039d513087"))

        val filter = ScanFilter.Builder()
            .setServiceUuid(siparioUUID)
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothLeScanner.startScan(listOf(filter), settings, leScanCallback)
    }
}
