package com.nextgis.maplibui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nextgis.maplib.api.IGISApplication
import com.nextgis.maplib.api.ILayer
import com.nextgis.maplib.map.MapBase
import com.nextgis.maplib.util.Constants
import com.nextgis.maplibui.R
import com.nextgis.maplibui.util.ConstantsUI

abstract class LayerSettingsFragment2
    :
    Fragment()
{

//    protected var layer: ILayer? = null
//    protected lateinit var tabLayout: TabLayout
//    protected lateinit var viewPager2: ViewPager2
//    protected lateinit var adapter: LayerTabsAdapter
//    protected lateinit var mainRoot: View
//
//    protected var map: MapBase? = null
//
//    var layerName: String = ""
//    var layerMinZoom: Float = 0f
//    var layerMaxZoom: Float = 0f
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val layerId = arguments?.getInt(ConstantsUI.KEY_LAYER_ID)
//            ?: savedInstanceState?.getInt(ConstantsUI.KEY_LAYER_ID)
//            ?: Constants.NOT_FOUND
//
//        if (layerId == Constants.NOT_FOUND) {
//            return
//        }
//
//        val app = requireActivity().application as? IGISApplication
//        map = app?.map
//
//        map?.let { mMap ->
//            val foundLayer = mMap.getLayerById(layerId)
//            if (foundLayer != null) {
//                layer = foundLayer
//                layerName = foundLayer.name
//            }
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_layer_settings, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        viewPager2 = view.findViewById(R.id.viewPager2)
//        tabLayout = view.findViewById(R.id.tabs)
//        mainRoot = view.findViewById(R.id.mainroot)
//
//
//        adapter = LayerTabsAdapter(this)
//        addFragments()
//        viewPager2.adapter = adapter
//
//        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
//            tab.text = adapter.getPageTitle(position)
//        }.attach()
//
//        activity?.title = layerName
//    }
//
//    override fun onPause() {
//        super.onPause()
//        saveSettings()
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        layer?.let {
//            outState.putInt(ConstantsUI.KEY_LAYER_ID, it.id)
//        }
//    }
//
//
//
//    open fun onFeaturesCountChanged() {

//    }
//
//    abstract fun addFragments()
//    abstract fun saveSettings()
//
//    inner class LayerTabsAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
//
//        private val fragmentList = mutableListOf<Fragment>()
//        private val fragmentTitleList = mutableListOf<String>()
//
//        fun addFragment(fragment: Fragment, titleResId: Int) {
//            fragmentList.add(fragment)
//            fragmentTitleList.add(getString(titleResId))
//        }
//
//        fun getPageTitle(position: Int): String = fragmentTitleList[position]
//
//        override fun getItemCount(): Int = fragmentList.size
//
//        override fun createFragment(position: Int): Fragment = fragmentList[position]
//    }
//
//    companion object {
//        fun newInstance(layerId: Int): LayerSettingsFragment2 {
//            return object : LayerSettingsFragment2() {
//                override fun addFragments() {

//                }
//                override fun saveSettings() {

//                }
//            }.apply {
//                arguments = Bundle().apply {
//                    putInt(ConstantsUI.KEY_LAYER_ID, layerId)
//                }
//            }
//        }
//    }
//
//    fun changeBackgroundVisible(visible: Boolean){
//        mainRoot.setBackgroundResource( if (visible) R.color.color_white else R.color.color_transparent )
//    }
//
//    fun getLayerId(): Int?{
//        return layer?.id;
//    }

}