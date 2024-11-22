package it.baldarn.sipario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import it.baldarn.sipario.databinding.FragmentSignUpOwnerBinding
import java.nio.charset.Charset
import java.util.TimeZone

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SignUpOwner : Fragment() {

    private var _binding: FragmentSignUpOwnerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSignUpOwnerBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signUpButton.setOnClickListener { button ->
            button.isEnabled = false
            // TODO: wait spinner?
            val email = binding.signUpEmailEditText.text.toString()
            val password = binding.signUpPasswordEditText.text.toString()
            val passwordConfirmation = binding.signUpPasswordConfirmEditText.text.toString()

            if(password.length < 8) {
                Toast.makeText(
                    requireActivity(),
                    "Write a minumum 8 character password!",
                    Toast.LENGTH_LONG
                ).show()
                button.isEnabled = true
                return@setOnClickListener
            }

            if(password != passwordConfirmation) {
                Toast.makeText(
                    requireActivity(),
                    "The two passwords doesn't match!",
                    Toast.LENGTH_LONG
                ).show()
                button.isEnabled = true
                return@setOnClickListener
            }

            val requestBody = "{\"user\":{\"email\":\"$email\",\"password\":\"$password\"}}"

            Fuel.post("${BuildConfig.BACKEND_URL}/users.json")
                .header("Content-Type" to "application/json")
                .body(requestBody, Charset.forName("UTF-8"))
                .response { _, _, result ->
                    when (result) {
                        is Result.Success -> {
                            Toast.makeText(
                                requireActivity(),
                                "You must now confirm with your email!",
                                Toast.LENGTH_LONG
                            ).show()

                            findNavController().navigate(R.id.action_SignUp_to_SignIn)
                        }

                        is Result.Failure -> {
                            Toast.makeText(
                                requireActivity(),
                                "server error!",
                                Toast.LENGTH_LONG
                            ).show()
                            button.isEnabled = true
                        }
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
