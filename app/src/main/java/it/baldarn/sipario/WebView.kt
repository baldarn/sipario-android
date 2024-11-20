package it.baldarn.sipario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment

class WebView : Fragment() {

    private lateinit var webView: WebView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_web_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize WebView
        webView = view.findViewById(R.id.webView)

        // Configure WebView settings
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // Handle navigation within WebView
        webView.webViewClient = WebViewClient()

        // Load URL passed as an argument
        val url = arguments?.getString("path") ?: BuildConfig.BACKEND_URL
        webView.loadUrl(url)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webView.destroy() // Clean up WebView resources
    }
}
