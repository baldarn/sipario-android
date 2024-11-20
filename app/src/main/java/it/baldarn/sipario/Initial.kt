package it.baldarn.sipario

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import it.baldarn.sipario.databinding.FragmentInitialBinding
import java.lang.Exception

class Initial : Fragment() {

    private var _binding: FragmentInitialBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val waitHandler = Handler(Looper.myLooper()!!)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentInitialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        waitHandler.postDelayed(Runnable {
            val jwtToken = SharedPrefsHelper.getJwtToken(requireActivity())
            val ownerJwtToken = SharedPrefsHelper.getOwnerJwtToken(requireActivity())
            try {
                if (ownerJwtToken != null) {
                    findNavController().navigate(R.id.action_Initial_to_OwnerDashboard)
                }
                else if (jwtToken != null) {
                    findNavController().navigate(R.id.action_Initial_to_SiparioToggle)
                    return@Runnable
                }
                findNavController().navigate(R.id.action_Initial_to_SignIn)
            } catch (e: Exception) {
                // silent catch bubbu
            }
        }, 1500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
