package com.nextgis.maplibui.fragment

/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2016-2017, 2019 NextGIS, info@nextgis.com
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

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.appyvet.materialrangebar.RangeBar
import com.nextgis.maplib.api.ILayer
import com.nextgis.maplib.api.IProgressor
import com.nextgis.maplib.map.NGWVectorLayer
import com.nextgis.maplib.map.RemoteTMSLayer
import com.nextgis.maplib.map.VectorLayer
import com.nextgis.maplib.util.GeoConstants
import com.nextgis.maplibui.R
import com.nextgis.maplibui.activity.LayerSettingsActivity
import com.nextgis.maplibui.util.ControlHelper

class LayerGeneralSettingsFragment2
    :
    Fragment()
{

//    private var editText: EditText? = null
//    private var rangeBar: RangeBar? = null
//    private var layer: ILayer? = null
//    private var settingsFragment: LayerSettingsFragment2? = null  /
//
//
//    fun init(layer: ILayer?, settingsFragment: LayerSettingsFragment2): LayerGeneralSettingsFragment2 {
//        this.layer = layer
//        this.settingsFragment = settingsFragment
//        return this
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        activity?.let { act ->
//            val imm = act.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
//            imm?.hideSoftInputFromWindow(view.windowToken, 0)
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//
//        editText?.text?.toString()?.let {
//            settingsFragment?.layerName = it
//        }
//        rangeBar?.let {
//            settingsFragment?.layerMinZoom = it.leftIndex.toFloat()
//            settingsFragment?.layerMaxZoom = it.rightIndex.toFloat()
//        }
//    }
//
//    fun setLayer(layer: ILayer?): Fragment {
//        this.layer = layer
//        return this
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_layer_general, container, false)
//
//        val currentLayer = layer ?: return view
//
//        view.findViewById<TextView>(R.id.layer_local_lath)?.text =
//            getString(R.string.layer_local_path, currentLayer.path)
//

//        val remoteUrl = when (currentLayer) {
//            is NGWVectorLayer -> currentLayer.remoteUrl
//            is RemoteTMSLayer -> currentLayer.url
//            else -> null
//        }
//
//        val remoteTextView = view.findViewById<TextView>(R.id.layer_remote_path)
//        if (remoteUrl != null) {
//            remoteTextView.text = getString(R.string.layer_remote_path, remoteUrl)
//            remoteTextView.visibility = View.VISIBLE
//        }
//
//        editText = view.findViewById(R.id.layer_name)
//        editText?.setText(currentLayer.name)
//        editText?.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                val name = s?.toString() ?: ""
//                settingsFragment?.layerName = name
//                activity?.title = name
//            }
//        })
//
//        val leftIndexValue = view.findViewById<TextView>(R.id.leftIndexValue)
//        val rightIndexValue = view.findViewById<TextView>(R.id.rightIndexValue)
//
//        rangeBar = view.findViewById(R.id.rangebar)
//
//        val maxZoom = GeoConstants.DEFAULT_MAX_ZOOM
//        var minZoom = settingsFragment?.layerMinZoom?.toInt() ?: 0
//        var maxZoomSet = settingsFragment?.layerMaxZoom?.toInt() ?: maxZoom
//
//        minZoom = minZoom.coerceIn(0, maxZoom)
//        maxZoomSet = maxZoomSet.coerceIn(0, maxZoom)
//
//        rangeBar?.setOnRangeBarChangeListener(object : RangeBar.OnRangeBarChangeListener {
//            override fun onRangeChangeListener(
//                rangeBar: RangeBar?,
//                leftPinIndex: Int,
//                rightPinIndex: Int,
//                leftPinValue: String?,
//                rightPinValue: String?
//            ) {
//                var correctedLeft = leftPinIndex
//                var correctedRight = rightPinIndex
//
//                if (leftPinIndex < 0) correctedLeft = 0
//                if (rightPinIndex > maxZoom) correctedRight = maxZoom
//
//                if (correctedLeft != leftPinIndex || correctedRight != rightPinIndex) {
//                    rangeBar?.setRangePinsByIndices(correctedLeft, correctedRight)
//                    return
//                }
//
//                settingsFragment?.layerMinZoom = correctedLeft.toFloat()
//                settingsFragment?.layerMaxZoom = correctedRight.toFloat()
//
//                ControlHelper.setZoomText(activity, leftIndexValue, R.string.min, correctedLeft)
//                ControlHelper.setZoomText(activity, rightIndexValue, R.string.max, correctedRight)
//            }
//
//            override fun onTouchStarted(rangeBar: RangeBar?) {}
//            override fun onTouchEnded(rangeBar: RangeBar?) {}
//        })
//
//        rangeBar?.setRangePinsByIndices(minZoom, maxZoomSet)
//
//        if (currentLayer is VectorLayer) {
//            val deleteButton = view.findViewById<Button>(R.id.delete_features)
//            deleteButton.visibility = View.VISIBLE
//            deleteButton.setOnClickListener {
//                AlertDialog.Builder(requireContext())
//                    .setTitle(R.string.are_you_sure)
//                    .setMessage(R.string.delete_features)
//                    .setNegativeButton(R.string.cancel, null)
//                    .setPositiveButton(R.string.ok) { _, _ ->
//                        DeleteFeaturesTask().execute(currentLayer)
//                    }
//                    .show()
//            }
//        }
//
//        return view
//    }
//
//    inner class DeleteFeaturesTask : AsyncTask<VectorLayer, Int, Void>(), IProgressor {
//
//        private var progressDialog: ProgressDialog? = null
//
//        override fun onPreExecute() {
//            super.onPreExecute()
//            progressDialog = ProgressDialog(requireContext()).apply {
//                setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
//                setMessage(getString(R.string.waiting))
//                show()
//            }
//        }
//
//        override fun doInBackground(vararg params: VectorLayer): Void? {
//            params.firstOrNull()?.deleteAllFeatures(this)
//            return null
//        }
//
//        override fun onProgressUpdate(vararg values: Int?) {
//            super.onProgressUpdate(*values)
//            progressDialog?.let { dialog ->
//                values.getOrNull(0)?.let { dialog.progress = it }
//                values.getOrNull(1)?.let { if (it > 0) dialog.max = it }
//            }
//        }
//
//        override fun onPostExecute(result: Void?) {
//            super.onPostExecute(result)
//            settingsFragment?.onFeaturesCountChanged()
//            progressDialog?.dismissIfShowing()
//        }
//
//        override fun setMax(maxValue: Int) {
//            publishProgress(0, maxValue)
//        }
//
//        override fun isCanceled(): Boolean =
//            progressDialog == null || !progressDialog!!.isShowing
//
//        override fun setValue(value: Int) {
//            publishProgress(value)
//        }
//
//        override fun setIndeterminate(indeterminate: Boolean) {}
//        override fun setMessage(message: String?) {}
//
//        private fun ProgressDialog?.dismissIfShowing() {
//            this?.takeIf { it.isShowing }?.dismiss()
//        }
//    }
//
//    companion object {
//        fun newInstance(): LayerGeneralSettingsFragment = LayerGeneralSettingsFragment()
//    }
}