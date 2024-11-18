package it.baldarn.sipario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.result.Result
import it.baldarn.sipario.databinding.FragmentEditUserBinding
import it.baldarn.sipario.model.User
import java.nio.charset.Charset
import java.util.TimeZone


class EditUser : Fragment() {

    private var _binding: FragmentEditUserBinding? = null
    private lateinit var user: User

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        _binding = FragmentEditUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: maybe get user data?

        val spinner = binding.editUserLanguageSpinner
        ArrayAdapter.createFromResource(
            requireActivity(),
            R.array.languages,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        binding.editUserButton.setOnClickListener { button ->
            button.isEnabled = false
            // TODO: wait spinner?
            val email = binding.editUserEmailEditText.text.toString()
            val currentPassword = binding.editUserCurrentPasswordEditText
            val newPassword = binding.editUserNewPasswordEditText.text.toString()
            val passwordConfirmation = binding.editUserPasswordConfirmEditText.text.toString()
            val name = binding.editUserNameEditText.text.toString()
            val language = ""
            // TODO: parametrize for first elements
            if(!binding.editUserLanguageSpinner.selectedItem.equals("Select a Language")) {
//                language = binding.editUserLanguageSpinner.selectedItem
            }

            val tz = TimeZone.getDefault()
            val timeZone = tz.id

            if (newPassword.length < 8) {
                Toast.makeText(
                    requireActivity(),
                    "Write a minumum 8 character password!",
                    Toast.LENGTH_LONG
                ).show()
                button.isEnabled = true
                return@setOnClickListener
            }

            if (newPassword != passwordConfirmation) {
                Toast.makeText(
                    requireActivity(),
                    "The two passwords doesn't match!",
                    Toast.LENGTH_LONG
                ).show()
                button.isEnabled = true
                return@setOnClickListener
            }

            val requestBody = "{\"user\":" +
                    "{\"email\":\"$email\"," +
                    "\"currentPassword\":\"$currentPassword\"," +
                    "\"name\":\"$name\"," +
                    "\"language\":\"$language\"," +
                    "\"time_zone\":\"$timeZone\"}" +
                    "}"

            Fuel.put("${BuildConfig.BACKEND_URL}/users.json")
                .header("Content-Type" to "application/json")
                .body(requestBody, Charset.forName("UTF-8"))
                .response { _, _, result ->
                    when (result) {
                        is Result.Success -> {
                            Toast.makeText(
                                requireActivity(),
                                "User updated!",
                                Toast.LENGTH_LONG
                            ).show()

                            findNavController().navigate(R.id.go_to_SiparioToggle)
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