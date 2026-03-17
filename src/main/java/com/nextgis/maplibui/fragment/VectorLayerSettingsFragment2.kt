package com.nextgis.maplibui.fragment

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015-2019 NextGIS, info@nextgis.com
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

import android.accounts.Account
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.PeriodicSync
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nextgis.maplib.api.IGISApplication
import com.nextgis.maplib.api.IRenderer
import com.nextgis.maplib.datasource.Field
import com.nextgis.maplib.display.FieldStyleRule
import com.nextgis.maplib.display.RuleFeatureRenderer
import com.nextgis.maplib.display.SimpleFeatureRenderer
import com.nextgis.maplib.display.Style
import com.nextgis.maplib.map.NGWVectorLayer
import com.nextgis.maplib.map.VectorLayer
import com.nextgis.maplib.util.AccountUtil
import com.nextgis.maplib.util.Constants
import com.nextgis.maplib.util.GeoConstants
import com.nextgis.maplib.util.LayerUtil
import com.nextgis.maplibui.R
import com.nextgis.maplibui.display.RendererUI
import com.nextgis.maplibui.display.RuleFeatureRendererUI
import com.nextgis.maplibui.display.SimpleFeatureRendererUI
import com.nextgis.maplibui.service.RebuildCacheService
import com.nextgis.maplibui.util.ConstantsUI
import com.nextgis.maplibui.util.ControlHelper
import com.nextgis.maplibui.util.LayerUtil.getGeometryName
import com.nextgis.maplibui.util.SettingsConstantsUI

class VectorLayerSettingsFragment2
//    : LayerSettingsFragment2(), View.OnClickListener
{

//    private var vectorLayer: VectorLayer? = null
//    private var renderer: IRenderer? = null
//    private var rebuildCacheReceiver: BroadcastReceiver? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val layer = layer
//        if (layer != null && (layer.type == Constants.LAYERTYPE_LOCAL_VECTOR ||
//                    layer.type == Constants.LAYERTYPE_NGW_VECTOR ||
//                    layer.type == Constants.LAYERTYPE_NGW_WEBMAP)) {
//            vectorLayer = layer as VectorLayer
//            layerMinZoom = vectorLayer?.minZoom ?: 0f
//            layerMaxZoom = vectorLayer?.maxZoom ?: 0f
//            renderer = vectorLayer?.renderer
//        }
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
//        val tabLayout = view.findViewById<TabLayout>(R.id.tabs)
//
//        val adapter = VectorLayerPagerAdapter(this)
//        viewPager.adapter = adapter
//
//        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
//            tab.text = when (position) {
//                0 -> getString(com.nextgis.maplib.R.string.style)
//                1 -> getString(com.nextgis.maplib.R.string.fields)
//                2 -> if (vectorLayer is NGWVectorLayer) getString(R.string.sync) else null
//                3 -> getString(com.nextgis.maplib.R.string.general)
//                4 -> getString(com.nextgis.maplib.R.string.cache)
//                else -> null
//            }
//        }.attach()
//
//        updateSubtitle()
//    }
//
//    private fun updateSubtitle() {
//        val vl = vectorLayer ?: return
//        var subtitle = getGeometryName(requireContext(), vl.geometryType).lowercase()
//
//        val formPrefix = "${vl.id}_"
//        val formFile = java.io.File(vl.path, "$formPrefix${ConstantsUI.FILE_FORM}")
//        if (formFile.exists()) {
//            subtitle += " ${getString(R.string.layer_has_form)}"
//        }
//
//        subtitle = getString(R.string.feature_count, vl.count, subtitle)
//
//        activity?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.main_toolbar)?.subtitle = subtitle
//    }
//
//    override fun addFragments() {

//    }
//
//    override fun saveSettings() {
//        val vl = vectorLayer ?: return
//
//        vl.name = layerName
//        vl.minZoom = layerMinZoom
//        vl.maxZoom = layerMaxZoom
//
//        val changes = renderer != vl.renderer ||
//                layerMinZoom != vl.minZoom ||
//                layerMaxZoom != vl.maxZoom
//
//        vl.save()
//
//        if (changes) {
//            map?.setDirty(true)
//        }
//    }
//
//    override fun onClick(v: View?) {
//        val vl = vectorLayer ?: return
//        val intent = Intent(requireContext(), RebuildCacheService::class.java).apply {
//            putExtra(ConstantsUI.KEY_LAYER_ID, vl.id)
//        }
//
//        when (v?.id) {
//            R.id.rebuild_cache -> {
//                intent.action = RebuildCacheService.ACTION_ADD_TASK
//                ContextCompat.startForegroundService(requireContext(), intent)
//                v.isEnabled = false
//                v.rootView.findViewById<View>(R.id.rebuild_progress)?.visibility = View.VISIBLE
//            }
//            R.id.cancelBuildCacheButton -> {
//                intent.action = RebuildCacheService.ACTION_STOP
//                ContextCompat.startForegroundService(requireContext(), intent)
//                v.rootView.findViewById<View>(R.id.rebuild_cache)?.isEnabled = true
//                v.rootView.findViewById<View>(R.id.rebuild_progress)?.visibility = View.GONE
//            }
//        }
//    }
//
//    override fun onFeaturesCountChanged() {
//        updateSubtitle()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        registerRebuildCacheReceiver()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        unregisterRebuildCacheReceiver()
//    }
//
//    private fun registerRebuildCacheReceiver() {
//        val filter = IntentFilter(RebuildCacheService.ACTION_UPDATE)
//        rebuildCacheReceiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context?, intent: Intent?) {
//                val layerId = intent?.getIntExtra(ConstantsUI.KEY_LAYER_ID, Constants.NOT_FOUND) ?: return
//                if (layerId != vectorLayer?.id) return
//
//                val max = intent.getIntExtra(RebuildCacheService.KEY_MAX, 0)
//                val progress = intent.getIntExtra(RebuildCacheService.KEY_PROGRESS, 0)
//
//                view?.findViewById<ProgressBar>(R.id.rebuildCacheProgressBar)?.apply {
//                    this.max = max
//                    this.progress = progress
//                }
//
//                val progressView = view?.findViewById<View>(R.id.rebuild_progress)
//                val rebuildButton = view?.findViewById<Button>(R.id.rebuild_cache)
//
//                if (progress == 0) {
//                    rebuildButton?.isEnabled = true
//                    progressView?.visibility = View.GONE
//                } else {
//                    rebuildButton?.isEnabled = false
//                    progressView?.visibility = View.VISIBLE
//                }
//
//                onFeaturesCountChanged()
//            }
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            requireActivity().registerReceiver(rebuildCacheReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
//        } else {
//            requireActivity().registerReceiver(rebuildCacheReceiver, filter)
//        }
//    }
//
//    private fun unregisterRebuildCacheReceiver() {
//        rebuildCacheReceiver?.let {
//            try {
//                requireActivity().unregisterReceiver(it)
//            } catch (ignored: IllegalArgumentException) {
//            }
//            rebuildCacheReceiver = null
//        }
//    }
//
//    // ──────────────────────────────────────────────────────────────
//    // Pager Adapter
//    // ──────────────────────────────────────────────────────────────
//
//    inner class VectorLayerPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
//        override fun getItemCount(): Int {
//            var count = 4  // style, fields, general, cache
//            if (vectorLayer is NGWVectorLayer) count++
//            return count
//        }
//
//        override fun createFragment(position: Int): Fragment {
//            return when (position) {
//                0 -> StyleFragment()
//                1 -> FieldsFragment()
//                2 -> if (vectorLayer is NGWVectorLayer) SyncFragment() else GeneralFragment()
//                3 -> if (vectorLayer is NGWVectorLayer) GeneralFragment() else CacheFragment()
//                4 -> CacheFragment()
//                else -> throw IllegalArgumentException("Invalid position: $position")
//            }
//        }
//    }
//
//    // ──────────────────────────────────────────────────────────────
//    // Вложенные фрагменты (Style, Fields, Cache, Sync, General)
//    // ──────────────────────────────────────────────────────────────
//
//    class StyleFragment : Fragment() {
//
//        private val renderers = mutableListOf<RendererUI>()
//
//        override fun onCreateView(
//            inflater: LayoutInflater,
//            container: ViewGroup?,
//            savedInstanceState: Bundle?
//        ): View? {
//            val view = inflater.inflate(R.layout.fragment_vector_layer_style, container, false)
//            val vl = (parentFragment as? VectorLayerSettingsFragment2)?.vectorLayer ?: return view
//
//            val spinner = view.findViewById<Spinner>(R.id.renderer)
//
//            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
//                    val rendererUI = renderers.getOrNull(pos) ?: return
//                    vl.renderer = rendererUI.renderer
//
//                    childFragmentManager.beginTransaction()
//                        .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
//                        .replace(R.id.settings, rendererUI.getSettingsScreen(vl))
//                        .commit()
//                }
//
//                override fun onNothingSelected(parent: AdapterView<*>?) {}
//            }
//
//            val defaultStyle = try {
//                vl.defaultStyle
//            } catch (e: Exception) {
//                e.printStackTrace()
//                null
//            }
//
//            val currentRenderer = (parentFragment as? VectorLayerSettingsFragment2)?.renderer
//
//            when (currentRenderer) {
//                is RuleFeatureRenderer -> {
//                    renderers.add(SimpleFeatureRendererUI(SimpleFeatureRenderer(vl, defaultStyle)))
//                    renderers.add(RuleFeatureRendererUI(currentRenderer, vl))
//                    spinner.setSelection(1)
//                }
//                is SimpleFeatureRenderer -> {
//                    renderers.add(SimpleFeatureRendererUI(currentRenderer))
//                    renderers.add(RuleFeatureRendererUI(RuleFeatureRenderer(vl, FieldStyleRule(vl), defaultStyle), vl))
//                    spinner.setSelection(0)
//                }
//                else -> {
//                    // fallback
//                    renderers.add(SimpleFeatureRendererUI(SimpleFeatureRenderer(vl, defaultStyle)))
//                    spinner.setSelection(0)
//                }
//            }
//
//            return view
//        }
//
//        override fun onDestroyView() {
//            super.onDestroyView()
//            renderers.clear()
//        }
//    }
//
//    class FieldsFragment : Fragment() {
//
//        private val fieldNames = mutableListOf<String>()
//        private val fieldAliases = mutableListOf<String>()
//        private var defaultPosition = 0
//
//        override fun onCreateView(
//            inflater: LayoutInflater,
//            container: ViewGroup?,
//            savedInstanceState: Bundle?
//        ): View? {
//            val view = inflater.inflate(R.layout.fragment_vector_layer_fields, container, false)
//            val vl = (parentFragment as? VectorLayerSettingsFragment2)?.vectorLayer ?: return view
//
//            fillFields(vl)
//
//            val listView = view.findViewById<ListView>(R.id.listView)
//            val adapter = ArrayAdapter(
//                requireContext(),
//                android.R.layout.simple_list_item_single_choice,
//                fieldAliases
//            )
//
//            listView.adapter = adapter
//            listView.setItemChecked(defaultPosition, true)
//
//            listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
//                val fieldName = fieldNames[position]
//                vl.preferences.edit()
//                    .putString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, fieldName)
//                    .apply()
//
//                Toast.makeText(
//                    context,
//                    getString(R.string.label_field_toast, fieldName),
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//
//            return view
//        }
//
//        private fun fillFields(vl: VectorLayer) {
//            fieldNames.clear()
//            fieldAliases.clear()
//
//            fieldNames.add(Constants.FIELD_ID)
//            fieldAliases.add("${Constants.FIELD_ID} - ${LayerUtil.typeToString(requireContext(), GeoConstants.FTInteger)}")
//
//            val labelField = vl.preferences.getString(SettingsConstantsUI.KEY_PREF_LAYER_LABEL, Constants.FIELD_ID)
//
//            vl.fields.forEachIndexed { index, field ->
//                val info = "${field.alias} - ${LayerUtil.typeToString(requireContext(), field.type)}"
//                if (field.name == labelField) defaultPosition = index + 1
//
//                fieldNames.add(field.name)
//                fieldAliases.add(info)
//            }
//        }
//    }
//
//    class CacheFragment : Fragment() {
//
//        override fun onCreateView(
//            inflater: LayoutInflater,
//            container: ViewGroup?,
//            savedInstanceState: Bundle?
//        ): View? {
//            val view = inflater.inflate(R.layout.fragment_vector_layer_cache, container, false)
//            val parent = parentFragment as? VectorLayerSettingsFragment2 ?: return view
//
//            view.findViewById<Button>(R.id.rebuild_cache)?.setOnClickListener(parent)
//            view.findViewById<ImageButton>(R.id.cancelBuildCacheButton)?.setOnClickListener(parent)
//
//            return view
//        }
//    }
//
//    class SyncFragment : Fragment() {
//
//        override fun onCreateView(
//            inflater: LayoutInflater,
//            container: ViewGroup?,
//            savedInstanceState: Bundle?
//        ): View? {
//            val view = inflater.inflate(R.layout.fragment_ngw_vector_layer_sync, container, false)
//            val ngwLayer = (parentFragment as? VectorLayerSettingsFragment2)?.vectorLayer as? NGWVectorLayer ?: return view
//
//            val app = requireActivity().application as IGISApplication
//            val account = app.getAccount(ngwLayer.accountName) ?: return view
//
//            view.findViewById<TextView>(R.id.account_name)?.text =
//                getString(R.string.ngw_account, account.name)
//
//            val directionSpinner = view.findViewById<Spinner>(R.id.sync_direction)
//            val enabledCheck = view.findViewById<CheckBox>(R.id.sync_enabled)
//            enabledCheck.isChecked = ngwLayer.syncType != Constants.SYNC_NONE
//
//            enabledCheck.setOnCheckedChangeListener { button, isChecked ->
//                if (!button.isPressed) return@setOnCheckedChangeListener
//
//                if (isChecked) {
//                    showAttentionDialog(
//                        requireContext(),
//                        R.string.sync_delete_alert_turnon,
//                        R.string.sync_turn_on_confirm,
//                        yes = {
//                            ngwLayer.syncType = Constants.SYNC_ALL
//                            ngwLayer.save()
//                            directionSpinner.isEnabled = true
//                        },
//                        no = { enabledCheck.isChecked = false },
//                        cancel = { enabledCheck.isChecked = false }
//                    )
//                } else {
//                    showAttentionDialog(
//                        requireContext(),
//                        R.string.sync_delete_alert_turnoff,
//                        R.string.sync_turn_off_confirm,
//                        yes = {
//                            ngwLayer.syncType = Constants.SYNC_NONE
//                            ngwLayer.save()
//                            directionSpinner.isEnabled = false
//                        },
//                        no = { enabledCheck.isChecked = true },
//                        cancel = { enabledCheck.isChecked = true }
//                    )
//                }
//            }
//
//            directionSpinner.setSelection(ngwLayer.syncDirection - 1)
//            directionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
//                    ngwLayer.syncDirection = pos + 1
//                }
//                override fun onNothingSelected(parent: AdapterView<*>?) {}
//            }
//
//            val periodSpinner = view.findViewById<Spinner>(R.id.sync_interval)
//            val autoCheck = view.findViewById<CheckBox>(R.id.sync_auto)
//
//            val isAccountSyncEnabled = NGWSettingsFragment.isAccountSyncEnabled(account, app.authority)
//            autoCheck.isChecked = isAccountSyncEnabled
//            autoCheck.setOnCheckedChangeListener { _, checked ->
//                NGWSettingsFragment.setAccountSyncEnabled(account, app.authority, checked)
//                periodSpinner.isEnabled = checked
//            }
//
//            periodSpinner.isEnabled = autoCheck.isChecked
//
//            // период синхронизации
//            var prefValue = Constants.DEFAULT_SYNC_PERIOD.toString()
//            val syncs = ContentResolver.getPeriodicSyncs(account, app.authority)
//            syncs?.firstOrNull()?.extras?.getString(SettingsConstantsUI.KEY_PREF_SYNC_PERIOD)?.let {
//                prefValue = it
//            }
//
//            val keys = NGWSettingsFragment.getPeriodTitles(requireContext())
//            val values = NGWSettingsFragment.getPeriodValues()
//
//            periodSpinner.adapter = ArrayAdapter(
//                requireContext(),
//                android.R.layout.simple_spinner_dropdown_item,
//                keys
//            )
//
//            values.indexOfFirst { it == prefValue }.let { idx ->
//                if (idx >= 0) periodSpinner.setSelection(idx)
//            }
//
//            periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
//                    val value = values[pos].toString()
//                    val interval = value.toLongOrNull() ?: Constants.NOT_FOUND.toLong()
//
//                    val bundle = Bundle().apply {
//                        putString(SettingsConstantsUI.KEY_PREF_SYNC_PERIOD, value)
//                    }
//
//                    if (interval == Constants.NOT_FOUND.toLong()) {
//                        ContentResolver.removePeriodicSync(account, app.authority, bundle)
//                    } else {
//                        ContentResolver.addPeriodicSync(account, app.authority, bundle, interval)
//                    }
//                }
//
//                override fun onNothingSelected(parent: AdapterView<*>?) {}
//            }
//
//            // блокировка если нет авторизации
//            if (!AccountUtil.isUserExists(requireContext())) {
//                view.findViewById<View>(R.id.overlay)?.visibility = View.VISIBLE
//                view.findViewById<View>(R.id.locked)?.setOnClickListener {
//                    ControlHelper.showNoLoginDialog(requireContext())
//                }
//            }
//
//            return view
//        }
//
//        private fun showAttentionDialog(
//            context: Context,
//            messageRes: Int,
//            positiveRes: Int,
//            yes: () -> Unit,
//            no: () -> Unit,
//            cancel: () -> Unit
//        ) {
//            AlertDialog.Builder(context)
//                .setMessage(messageRes)
//                .setNegativeButton(R.string.cancel) { _, _ -> no() }
//                .setPositiveButton(positiveRes) { _, _ -> yes() }
//                .setCancelable(false)
//                .setOnCancelListener { cancel() }
//                .show()
//        }
//    }
//
//    class GeneralFragment : LayerGeneralSettingsFragment() {
//        // просто алиас — используем уже готовый фрагмент общих настроек
//    }
//
//    companion object {
//        fun newInstance(layerId: Int): VectorLayerSettingsFragment2 {
//            return VectorLayerSettingsFragment2().apply {
//                arguments = Bundle().apply {
//                    putInt(ConstantsUI.KEY_LAYER_ID, layerId)
//                }
//            }
//        }
//    }
}