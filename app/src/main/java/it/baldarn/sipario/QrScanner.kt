package it.baldarn.sipario

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
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

                val url = result.text

                val bearerToken =
                    SharedPrefsHelper.getOwnerJwtToken(requireActivity().applicationContext)
                if (bearerToken != null) {
                    Fuel.get(url)
                        .header("Authorization", bearerToken)
                        .header("Content-Type" to "application/json")
                        .response { _, response, result ->
                            when (result) {
                                is Result.Success -> {
                                    Toast.makeText(
                                        requireActivity(),
                                        "CONSUMED",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    val providerId = url.split("/")[4]
                                    val awardId = url.split("/")[5]

                                    try {
                                        val browserIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("${BuildConfig.BACKEND_URL}/providers/${providerId}/awards/${awardId}")
                                        )
                                        startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(
                                            context,
                                            "Impossibile aprire il browser",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                is Result.Failure -> {
                                    Toast.makeText(
                                        requireActivity(),
                                        "PROBLEMS",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }

                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
