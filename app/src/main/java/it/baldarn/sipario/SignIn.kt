package it.baldarn.sipario

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import it.baldarn.sipario.databinding.FragmentSignInBinding
import java.nio.charset.Charset


class SignIn : Fragment() {

    private var _binding: FragmentSignInBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signInButton.setOnClickListener { button ->
            button.isEnabled = false
            // TODO: wait spinner?
            val email = binding.signInEmailEditText.text
            val password = binding.signInPasswordEditText.text

            val requestBody = "{\"user\":{\"email\":\"$email\",\"password\":\"$password\"}}"

            Fuel.post("${BuildConfig.BACKEND_URL}/users/sign_in.json")
                .header("Content-Type" to "application/json")
                .body(requestBody, Charset.forName("UTF-8"))
                .response { _, response, result ->
                    when (result) {
                        is Result.Success -> {
                            val bearerToken = response["Authorization"].first()
                            SharedPrefsHelper.saveJwtToken(
                                requireActivity().applicationContext,
                                bearerToken
                            )
                            (activity as MainActivity).sendDeviceToken()
                            findNavController().navigate(R.id.action_SignIn_to_SiparioToggle)
                        }

                        is Result.Failure -> {
                            Toast.makeText(
                                requireActivity(),
                                "Wrong username or password!",
                                Toast.LENGTH_LONG
                            ).show()

                        }
                    }
                    button.isEnabled = true
                }
        }

        binding.signInOwnerButton.setOnClickListener { button ->
            button.isEnabled = false
            // TODO: wait spinner?
            val email = binding.signInEmailEditText.text
            val password = binding.signInPasswordEditText.text

            val requestBody = "{\"owner\":{\"email\":\"$email\",\"password\":\"$password\"}}"

            Fuel.post("${BuildConfig.BACKEND_URL}/owners/sign_in.json")
                .header("Content-Type" to "application/json")
                .body(requestBody, Charset.forName("UTF-8"))
                .response { _, response, result ->
                    when (result) {
                        is Result.Success -> {
                            val bearerToken = response["Authorization"].first()
                            SharedPrefsHelper.saveOwnerJwtToken(
                                requireActivity().applicationContext,
                                bearerToken
                            )
                            (activity as MainActivity).sendDeviceToken()
                            findNavController().navigate(R.id.action_SignIn_to_OwnerDashboard)
                        }

                        is Result.Failure -> {
                            Toast.makeText(
                                requireActivity(),
                                "Wrong username or password!",
                                Toast.LENGTH_LONG
                            ).show()

                        }
                    }
                    button.isEnabled = true
                }
        }

        binding.goToSignUpButton.setOnClickListener { _ ->
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("${BuildConfig.BACKEND_URL}/owners/sign_up"))
                startActivity(browserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Impossibile aprire il browser", Toast.LENGTH_SHORT).show()
            }
        }

        binding.goToSignUpOwnerButton.setOnClickListener { _ ->
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("${BuildConfig.BACKEND_URL}/owners/sign_up"))
                startActivity(browserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Impossibile aprire il browser", Toast.LENGTH_SHORT).show()
            }
        }

        binding.goToResetPassword.setOnClickListener{ _ ->
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("${BuildConfig.BACKEND_URL}/users/password/new"))
                startActivity(browserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Impossibile aprire il browser", Toast.LENGTH_SHORT).show()
            }
        }

        binding.goToResetPasswordOwner.setOnClickListener{ _ ->
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("${BuildConfig.BACKEND_URL}/owners/password/new"))
                startActivity(browserIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Impossibile aprire il browser", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
