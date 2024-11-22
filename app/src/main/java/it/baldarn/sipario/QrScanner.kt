package it.baldarn.sipario

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import it.baldarn.sipario.databinding.FragmentQrScannerBinding
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult

class QRScannerFragment : Fragment() {

    private var _binding: FragmentQrScannerBinding? = null
    private val binding get() = _binding!!

    private val barcodeCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            if (result != null) {
                Log.d("QRScanner", "Scanned QR code: ${result.text}")
                // Mostra il risultato o esegui un'azione
                binding.barcodeScanner.pause()
                handleQRCode(result.text)

                findNavController().navigate(R.id.action_ScanQr_to_OwnerDashboard)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQrScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verifica permessi
        checkCameraPermission()

        binding.barcodeScanner.decodeContinuous(barcodeCallback)
        binding.barcodeScanner.resume()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Avvia scanner
                binding.barcodeScanner.resume()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Mostra una spiegazione del perché servono i permessi
            }
            else -> {
                // Richiedi i permessi
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            binding.barcodeScanner.resume()
        } else {
            // Permesso negato, mostra un messaggio
        }
    }

    private fun handleQRCode(qrCode: String) {
        // Esegui un'azione con il testo del QR code
        Log.d("QRScanner", "QR Code Content: $qrCode")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
