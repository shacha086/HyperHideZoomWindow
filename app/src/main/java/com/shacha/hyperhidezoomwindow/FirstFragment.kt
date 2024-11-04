package com.shacha.hyperhidezoomwindow

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.Context.MODE_WORLD_READABLE
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import com.shacha.hyperhidezoomwindow.databinding.FragmentFirstBinding
import de.robv.android.xposed.XSharedPreferences

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    @SuppressLint("WorldReadableFiles")
    @Suppress("DEPRECATION")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedPreferences = requireContext().getSharedPreferences("settings", MODE_WORLD_READABLE)
        binding.switchButton.isChecked = sharedPreferences.getBoolean("enabled", false)
        binding.switchButton.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit(commit = true) {
                putBoolean("enabled", isChecked)
            }
            requireContext().sendBroadcast(Intent(BROADCAST_ACTION).apply { putExtra("enabled", isChecked) })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}