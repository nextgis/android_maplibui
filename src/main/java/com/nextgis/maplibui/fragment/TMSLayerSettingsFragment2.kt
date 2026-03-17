package com.nextgis.maplibui.fragment

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2017 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * BUT WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.slider.RangeSlider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nextgis.maplib.api.ILayer
import com.nextgis.maplib.display.TMSRenderer
import com.nextgis.maplib.map.TMSLayer
import com.nextgis.maplib.util.Constants
import com.nextgis.maplib.util.Constants.FIELD_ALPHA
import com.nextgis.maplib.util.Constants.FIELD_BRIGHTNESS_MAX
import com.nextgis.maplib.util.Constants.FIELD_BRIGHTNESS_MIN
import com.nextgis.maplib.util.Constants.FIELD_CONTRAST
import com.nextgis.maplib.util.Constants.LAYER_ID_KEY
import com.nextgis.maplibui.R
import com.nextgis.maplibui.util.ConstantsUI
import com.nextgis.maplibui.util.ControlHelper

/**
 * Настройки слоя TMS (растровый, тайловый)
 */
class TMSLayerSettingsFragment2
//    : LayerSettingsFragment2()
{
//
//    private var rasterLayer: TMSLayer? = null
//    private var clearCacheFlag = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val layer = layer
//        if (layer != null && (layer.type == Constants.LAYERTYPE_REMOTE_TMS ||
//                    layer.type == Constants.LAYERTYPE_LOCAL_TMS ||
//                    layer.type == Constants.LAYERTYPE_NGW_RASTER ||
//                    layer.type == Constants.LAYERTYPE_NGW_WEBMAP)) {
//            rasterLayer = layer as TMSLayer
//            layerMinZoom = rasterLayer?.minZoom ?: 0f
//            layerMaxZoom = rasterLayer?.maxZoom ?: 0f
//        }
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//

//        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager2)
//        val tabLayout = view.findViewById<TabLayout>(R.id.tabs)
//
//        val adapter = TMSAdapter(this)
//        viewPager.adapter = adapter
//
//        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
//            tab.text = when (position) {
//                0 -> getString(com.nextgis.maplib.R.string.style)
//                1 -> getString(com.nextgis.maplib.R.string.general)
//                else -> null
//            }
//        }.attach()
//    }
//
//    override fun addFragments() {
//    }
//
//    fun proxyChangeVisible(visible: Boolean){
//        super.changeBackgroundVisible(visible)
//    }
//
//    override fun saveSettings() {
//
//
//        val rl = rasterLayer ?: return
//
//        rl.name = layerName
//        val zoomChanged = layerMaxZoom != rl.maxZoom || layerMinZoom != rl.minZoom
//        rl.minZoom = layerMinZoom
//        rl.maxZoom = layerMaxZoom
//
//
//        rl.save()
//
//        if (zoomChanged || clearCacheFlag) {
//            map?.setDirty(true)
//        }
//    }
//
//    inner class TMSAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
//
//        override fun getItemCount(): Int = 2
//
//        override fun createFragment(position: Int): Fragment {
//            return when (position) {
//                0 -> StyleFragment().apply { setLayer(layer) }
//                1 -> LayerGeneralSettingsFragment2().apply {
//                    setLayer(layer)
//                    //setRoot(layer, this@TMSLayerSettingsFragment2)
//                }
//                else -> throw IllegalStateException("Unexpected position: $position")
//            }
//        }
//    }
//
//    // ------------------ Style Fragment ------------------
//
//    class StyleFragment : Fragment(), SeekBar.OnSeekBarChangeListener,
//    //    RangeSlider.OnChangeListener
//        OnRangeChangedListener
//    {
//
//        private var rasterLayer: TMSLayer? = null
//        private var contrast = 0f
//        //private var brightness = 0f
//
//        // range - mix/max value of btightness
//        private var brightnessMin = 0f
//        private var brightnessMax = 1f
//
//        private var alpha = 255
//        private var forceToGrayScale = false
//
//        private var alphaLabel: android.widget.TextView? = null
//        private var brightnessLabel: android.widget.TextView? = null
//        private var contrastLabel: android.widget.TextView? = null
//
//        private var rootContainer: ViewGroup? = null
//
//        fun setLayer(layer: ILayer?) {
//            if (layer is TMSLayer) {
//                rasterLayer = layer
//                val renderer = layer.renderer as? TMSRenderer
//                renderer?.let {
//                    forceToGrayScale = it.isForceToGrayScale
//                    contrast = it.contrast
//                    brightnessMin = it.brightnessMin
//                    brightnessMax = it.brightnessMax
//                    alpha = it.alpha
//                }
//            }
//        }
//
//        fun saveSettings(): Boolean {
//            val renderer = rasterLayer?.renderer as? TMSRenderer ?: return false
//            val changed = renderer.alpha != alpha ||
//                    renderer.brightnessMin != brightnessMin ||
//                    renderer.brightnessMax != brightnessMax ||
//                    renderer.contrast != contrast ||
//                    renderer.isForceToGrayScale != forceToGrayScale
//
//            renderer.setContrastBrightness(contrast, brightnessMin, brightnessMax, forceToGrayScale)
//            renderer.alpha = alpha
//            return changed
//        }
//
//        override fun onCreateView(
//            inflater: LayoutInflater,
//            container: ViewGroup?,
//            savedInstanceState: Bundle?
//        ): View? {
//            val v = inflater.inflate(R.layout.fragment_raster_layer_style, container, false)
//            rootContainer = v.findViewById<ViewGroup>(R.id.root) // или ваш root id
//
//            val grayscaleSwitch = v.findViewById<SwitchMaterial>(R.id.make_grayscale)
//            grayscaleSwitch.isChecked = forceToGrayScale
//            grayscaleSwitch.setOnCheckedChangeListener { _, isChecked ->
//                forceToGrayScale = isChecked
//            }
//
//            contrastLabel = v.findViewById(R.id.contrast_seek)
//            val contrastSeek = v.findViewById<SeekBar>(R.id.contrastSeekBar)
//            contrastSeek.progress = (contrast * 128 + 128 ).toInt()
//            contrastSeek.setOnSeekBarChangeListener(this)
//
//
////              old brightness
////            brightnessLabel = v.findViewById(R.id.brightness_seek)
////            val brightnessSeek = v.findViewById<SeekBar>(R.id.brightnessSeekBar2)
////            brightnessSeek.progress = (brightness + 255).toInt()
////            brightnessSeek.setOnSeekBarChangeListener(this)
//
//            brightnessLabel = v.findViewById(R.id.brightness_seek)
//            val brightnessSeek = v.findViewById<RangeSeekBar>(R.id.brightnessSeekBar2)
//            brightnessSeek.setProgress(brightnessMin * 256, brightnessMax * 256 )
//            brightnessSeek.setOnRangeChangedListener(this)
//
//            alphaLabel = v.findViewById(R.id.alpha_seek)
//            val alphaSeek = v.findViewById<SeekBar>(R.id.alphaSeekBar)
//            alphaSeek.progress = alpha
//            alphaSeek.setOnSeekBarChangeListener(this)
//
//            // Пример: скрываем всё кроме SeekBar'ов при начале касания
//            listOf(contrastSeek, alphaSeek).forEach { seek ->
//                seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//
//                    override fun onProgressChanged(seekBar: SeekBar?, p: Int, fromUser: Boolean) {
//                        this@StyleFragment.onProgressChanged(seekBar, p, fromUser)
//                    }
//
//                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
//                        hideAllExceptSeekBars(true)
//                    }
//
//                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                        hideAllExceptSeekBars(false)
//                    }
//                })
//            }
//
//            return v
//        }
//
//        fun hideAllExceptSeekBars(hide: Boolean) {
//
//            rootContainer?.let { root ->
//                for (i in 0 until root.childCount) {
//                    val child = root.getChildAt(i)
//                    if (child.id != R.id.contrastSeekBar &&
//                        child.id != R.id.brightnessSeekBar2 &&
//                        child.id != R.id.alphaSeekBar  ) {
//                        child.visibility = if (hide) View.INVISIBLE else View.VISIBLE
//                    }
//                }
//            }
//            (parentFragment as? TMSLayerSettingsFragment2)?.proxyChangeVisible(!hide)
//
//        }
//
//        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//
//            when (seekBar?.id) {
//                R.id.alphaSeekBar -> {
//                    if (fromUser) alpha = progress
//                    alphaLabel?.text = ControlHelper.getPercentValue(requireContext(), R.string.alpha,  alpha * 1.0f)
//                }
////                R.id.brightnessSeekBar -> {
////                    if (fromUser) brightness = (progress - 255).toFloat()
////                    brightnessLabel?.text = ControlHelper.getPercentValue(requireContext(), R.string.brightness, brightness)
////                }
//                R.id.contrastSeekBar -> {
//                    if (fromUser) contrast = (progress - 128) / 128f
//                    contrastLabel?.text = String.format(getString(R.string.contrast), contrast)
//                }
//            }
//
//
//            //    String JSON_DEFAULT_FORM_ID   = "default_form_id";
////            const val FIELD_CONTRAST = "contrast"
////            const val FIELD_BRIGHTNESS = "brightness"
////            const val FIELD_ALPHA = "alpha"
//
//            val msg = Intent(Constants.MESSAGE_INTENT_STYLING_RASTER)
//
//            val layerId = (parentFragment as? TMSLayerSettingsFragment2)?.getLayerId()
//            msg.putExtra(LAYER_ID_KEY, layerId )
//            msg.putExtra(FIELD_ALPHA, alpha )
//            msg.putExtra(FIELD_CONTRAST, contrast )
//
//            msg.putExtra(FIELD_BRIGHTNESS_MIN, brightnessMin )
//            msg.putExtra(FIELD_BRIGHTNESS_MAX, brightnessMax )
//            msg.setPackage(requireContext().packageName)
//            requireContext().sendBroadcast(msg)
//
//        }
//
//        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
//
//        override fun onPause() {
//            super.onPause()
//            saveSettings()
//        }
//
////        override fun onValueChange(
////            slider: RangeSlider,
////            value: Float,
////            fromUser: Boolean ) {
////
////                val min = slider.values[0]
////                val max = slider.values[1]
////                if (fromUser) brightnessMin = (min - 255).toFloat()
////                if (fromUser) brightnessMax = (max - 255).toFloat()
////
////            val textToDIsplay = ControlHelper.getPercentValue(requireContext(), R.string.brightness,
////                brightnessMin)+ ControlHelper.getPercentValue(requireContext(), R.string.brightness, brightnessMax)
////                brightnessLabel?.text = textToDIsplay
////
//////                textViewMin.text = "Min: $min"
//////                textViewMax.text = "Max: $max"
////        }
//
//        override fun onRangeChanged(
//            view: RangeSeekBar?,
//            leftValue: Float,
//            rightValue: Float,
//            isFromUser: Boolean){
//            Log.e("BRG", "min=" + leftValue + " max:" + rightValue);
//
//            brightnessMin = ((leftValue ) / 256 ).toFloat()
//            brightnessMax = ((rightValue ) / 256) .toFloat()
//
//            Log.e("BRG", "result min=" + brightnessMin + " max:" + brightnessMax + " cont=" + contrast + " apllha=" +alpha);
//            val textToDIsplay = ControlHelper.getPercentValue(requireContext(), R.string.brightness,
//                brightnessMin)+ ControlHelper.getPercentValue(requireContext(), R.string.brightness, brightnessMax)
//            brightnessLabel?.text = textToDIsplay
//
//            val msg = Intent(Constants.MESSAGE_INTENT_STYLING_RASTER)
//            val layerId = (parentFragment as? TMSLayerSettingsFragment2)?.getLayerId()
//            msg.putExtra(LAYER_ID_KEY, layerId )
//            msg.putExtra(FIELD_ALPHA, alpha )
//            msg.putExtra(FIELD_CONTRAST, contrast )
//            msg.putExtra(FIELD_BRIGHTNESS_MIN, brightnessMin )
//            msg.putExtra(FIELD_BRIGHTNESS_MAX, brightnessMax )
//            msg.setPackage(requireContext().packageName)
//            requireContext().sendBroadcast(msg)
//        }
//
//        override fun onStartTrackingTouch(
//            view: RangeSeekBar?,
//            isLeft: Boolean) {
//
//            hideAllExceptSeekBars(true)
//
//        }
//
//        override fun onStopTrackingTouch(
//            view: RangeSeekBar?,
//            isLeft: Boolean ) {
//            hideAllExceptSeekBars(false)
//        }
//    }
//
//
////    class CacheFragment : Fragment() {
////
////        private var rasterLayer: TMSLayer? = null
////
////        fun setLayer(layer: ILayer?) {
////            if (layer is TMSLayer) rasterLayer = layer
////        }
////
////        override fun onCreateView(
////            inflater: LayoutInflater,
////            container: ViewGroup?,
////            savedInstanceState: Bundle?
////        ): View? {
////            val v = inflater.inflate(R.layout.fragment_raster_layer_cache, container, false)
////            val rl = rasterLayer ?: return v
////
////            val spinner = v.findViewById<Spinner>(R.id.spinner)
////            spinner.setSelection(rl.cacheSizeMultiply)
////            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
////                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
////                    rl.cacheSizeMultiply = pos
////                }
////                override fun onNothingSelected(parent: AdapterView<*>?) {}
////            }
////
////            v.findViewById<Button>(R.id.clear_cache).setOnClickListener {
////                ClearCacheTask(requireActivity()) { clearCacheFlag = true }
////                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, rl.path)
////            }
////
////            return v
////        }
////    }
//
//    companion object {
//        fun newInstance(layerId: Int): TMSLayerSettingsFragment2 {
//            return TMSLayerSettingsFragment2().apply {
//                arguments = Bundle().apply {
//                    putInt(ConstantsUI.KEY_LAYER_ID, layerId)
//                }
//            }
//        }
//
////        fun getFragment( layerId: Int) : Fragment {
////            val fragment = newInstance(layerId)
////            return fragment
//////            activity.supportFragmentManager.beginTransaction()
//////                .replace(R.id.settingsFrame, fragment)
////
////        }
//    }
}
