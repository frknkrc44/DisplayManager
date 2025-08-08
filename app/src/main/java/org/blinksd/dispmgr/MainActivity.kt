// Copyright (C) 2025 Furkan Karcıoğlu <https://github.com/frknkrc44>
//
// This file is part of DisplayManager project,
// and licensed under GNU Affero General Public License v3.
// See the GNU Affero General Public License for more details.
//
// All rights reserved. See COPYING, AUTHORS.
//

package org.blinksd.dispmgr

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.blinksd.dispmgr.DensityHelper.Companion.calculateSmallestWidth
import org.blinksd.dispmgr.HiddenApiService.Companion.findRotationByMode
import org.lsposed.hiddenapibypass.HiddenApiBypass
import org.lsposed.hiddenapibypass.LSPass

@Suppress("deprecation")
class MainActivity : AppCompatActivity() {
    private lateinit var displays: Array<Display>
    private lateinit var displayManager: DisplayManager
    private lateinit var hiddenApi: HiddenApiService
    private lateinit var displaySpinner: TextInputLayout
    private lateinit var windowingSpinner: TextInputLayout
    private lateinit var stateSwitch: MaterialSwitch
    private val myScope = CoroutineScope(Dispatchers.IO)
    private val windowingValues = HiddenApiService.WindowingMode.entries.toTypedArray()

    val onWindowingModeSelectedListener =
        AdapterView.OnItemClickListener { parent, view, position, id ->
            val selectedDisplay = getSelectedDisplay()

            myScope.launch {
                val value = windowingValues[position]
                hiddenApi.getService().setWindowingMode(selectedDisplay.displayId, value.mode)
            }

            launchScope(selectedDisplay)
        }

    val onSwitchListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        val selectedDisplay = getSelectedDisplay()

        myScope.launch {
            hiddenApi.setPowerState(
                selectedDisplay.displayId,
                isChecked
            )
        }
    }

    @SuppressLint("ResourceType", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        enableEdgeToEdge()
        LSPass.setHiddenApiExemptions("L")

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        displaySpinner = findViewById(R.id.display_selector)
        windowingSpinner = findViewById(R.id.windowing_selector)
        stateSwitch = findViewById(R.id.screen_state_switch)

        displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        displays = displayManager.displays
        hiddenApi = HiddenApiService(this)

        setAdapter()

        (displaySpinner.getEditText() as MaterialAutoCompleteTextView)
            .onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id -> launchScope(displays[position]) }

        myScope.launch {
            val values = windowingValues.filter {
                it.mode != HiddenApiService.WINDOWING_MODE_MULTI_WINDOW ||
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            }.map { it.toString() }.toTypedArray()
            val selectedDisplay = getSelectedDisplay()
            val screenIsOn = hiddenApi.getPowerState(selectedDisplay.displayId)
            val windowingMode = hiddenApi.getService().getWindowingMode(selectedDisplay.displayId)
            val idxWMode = windowingValues.indexOfFirst { it.mode == windowingMode }

            withMainContext {
                stateSwitch.isChecked = screenIsOn
                stateSwitch.setOnCheckedChangeListener(onSwitchListener)

                val autoCTVW = (windowingSpinner.editText as MaterialAutoCompleteTextView)
                autoCTVW.setSimpleItems(values)
                autoCTVW.setText(values[idxWMode], false)
                autoCTVW.onItemClickListener = onWindowingModeSelectedListener
            }
        }

        launchScope(getSelectedDisplay())

        setOnClickListener(
            R.id.display_resolution_container
        ) {
            val display = getSelectedDisplay()

            val view = layoutInflater.inflate(R.layout.display_size_dialog, null, false)
            val width = view.findViewById<EditText>(R.id.display_width)
            val height = view.findViewById<EditText>(R.id.display_height)
            val modes = display.supportedModes.toList()
            val modeSelector = view.findViewById<RadioGroup>(R.id.mode_selector)
            modeSelector.setOnCheckedChangeListener { _, checkedId ->
                width.isEnabled = checkedId == -999
                height.isEnabled = checkedId == -999
            }

            for (mode in modes) {
                val radioButton = RadioButton(this)
                radioButton.layoutParams = RadioGroup.LayoutParams(-1, -2)
                radioButton.id = mode.modeId
                radioButton.text = "${mode.physicalWidth}x${mode.physicalHeight}@${mode.refreshRate}"
                modeSelector.addView(radioButton)
            }

            val radioButton = RadioButton(this)
            radioButton.layoutParams = RadioGroup.LayoutParams(-1, -2)
            radioButton.text = "Custom"
            radioButton.id = -999
            modeSelector.addView(radioButton)

            modeSelector.check(modes.first().modeId)

            myScope.launch {
                val point = Point()
                hiddenApi.getService().getBaseDisplaySize(display.displayId, point)

                withMainContext {
                    width.setText(point.x.toString())
                    height.setText(point.y.toString())

                    MaterialAlertDialogBuilder(this@MainActivity).apply {
                        setView(view)

                        setNeutralButton("(↻)") { dialog, which ->
                            myScope.launch {
                                hiddenApi.getService().clearForcedDisplaySize(display.displayId)
                            }
                        }

                        setPositiveButton(
                            android.R.string.ok
                        ) { dialog, which ->
                            myScope.launch {
                                if (modeSelector.checkedRadioButtonId == -999) {
                                    hiddenApi.getService().setForcedDisplaySize(
                                        display.displayId,
                                        Integer.valueOf(width.text.toString()),
                                        Integer.valueOf(height.text.toString()),
                                    )
                                } else {
                                    hiddenApi.getService().setUserPreferredDisplayMode(
                                        display.displayId,
                                        modes.first { it.modeId == modeSelector.checkedRadioButtonId }
                                    )
                                }
                            }
                        }
                    }.show()
                }
            }
        }

        setOnClickListener(
            R.id.density_container
        ) {
            val displayId = getSelectedDisplayId()

            val view = layoutInflater.inflate(R.layout.density_dialog, null, false) as ViewGroup
            val densityView = view.findViewById<EditText>(R.id.display_density)

            myScope.launch {
                val density = hiddenApi.getService().getBaseDisplayDensity(displayId)
                val userId = try {
                    val method1 = UserHandle::class.java.getDeclaredMethod("myUserId")
                    method1.isAccessible = true
                    method1.invoke(null) as Int
                } catch (e: Throwable) {
                    throw e
                }

                withMainContext {
                    densityView.setText(density.toString())

                    MaterialAlertDialogBuilder(this@MainActivity).apply {
                        setView(view)

                        setNeutralButton("(↻)") { dialog, which ->
                            myScope.launch {
                                hiddenApi.getService().clearForcedDisplayDensityForUser(
                                    displayId,
                                    userId,
                                )
                            }
                        }

                        setPositiveButton(
                            android.R.string.ok
                        ) { dialog, which ->
                            myScope.launch {
                                hiddenApi.getService().setForcedDisplayDensityForUser(
                                    displayId,
                                    Integer.valueOf(densityView.text.toString()),
                                    userId,
                                )
                            }
                        }
                    }.show()
                }
            }
        }

        setOnClickListener(
            R.id.display_rotation_container
        ) {
            val display = getSelectedDisplay()

            myScope.launch {
                hiddenApi.getService().freezeDisplayRotation(
                    display.displayId,
                    (display.rotation + 1) % 4,
                    "android"
                )
            }
        }

        displayManager.registerDisplayListener(object : DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                val displayIdx = displays.indexOfFirst { it.displayId == displayId }
                displays[displayIdx] = displayManager.getDisplay(displayId)

                val currentDisplayId = getSelectedDisplayId()
                if (currentDisplayId == displayId) {
                    launchScope(displays[displayIdx])
                }
            }
            override fun onDisplayAdded(displayId: Int) = onDisplayRemoved(displayId)
            override fun onDisplayRemoved(displayId: Int) {
                displays = displayManager.displays
                setAdapter()
            }
        }, findViewById<View?>(android.R.id.content)!!.handler)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        launchScope(getSelectedDisplay())
    }

    fun setAdapter() {
        (displaySpinner.getEditText() as MaterialAutoCompleteTextView).apply {
            val names = getDisplayNames()
            setSimpleItems(names)
            setText(names.first(), false)
        }
    }

    private fun getDisplayNames() = displays.map { "${it.name} (${it.displayId})" }.toTypedArray()

    private fun getSelectedDisplay(): Display {
        val autoCTV = displaySpinner.getEditText() as MaterialAutoCompleteTextView
        return displays[getDisplayNames().indexOfFirst { it == autoCTV.text.toString() }]
    }

    private fun getSelectedDisplayId(): Int = getSelectedDisplay().displayId

    private fun setOnClickListener(resId: Int, listener: View.OnClickListener) {
        findViewById<View?>(resId)?.setOnClickListener(listener)
    }

    private fun launchScope(display: Display) {
        myScope.launch {
            try {
                withMainContext {
                    val point1 = Point()
                    val point2 = Point()
                    hiddenApi.getService().getBaseDisplaySize(display.displayId, point1)
                    hiddenApi.getService().getInitialDisplaySize(display.displayId, point2)

                    setText(
                        R.id.display_resolution_container,
                        "Resolution",
                        "${point1.x}x${point1.y}" + (if (point1.x == point2.x && point1.y == point2.y) "" else " (Default: ${point2.x}x${point2.y})")
                    )

                    setText(
                        R.id.display_rotation_container,
                        "Rotation",
                        (findRotationByMode(display.rotation)?.title ?: "Unknown (${display.rotation})") + " - Press to rotate"
                    )

                    setText(
                        R.id.refresh_rate_container,
                        "Refresh rate",
                        "${display.refreshRate.toInt()}"
                    )

                    val density1 = hiddenApi.getService().getBaseDisplayDensity(display.displayId)
                    val density2 = hiddenApi.getService().getInitialDisplayDensity(display.displayId)

                    setText(
                        R.id.density_container,
                        "Density",
                        if (density1 == density2) { density1.toString() } else { "$density1 (Default: $density2)" }
                    )

                    val sw1 = calculateSmallestWidth(point1, density1).toInt()
                    val sw2 = calculateSmallestWidth(point2, density2).toInt()

                    setText(
                        R.id.smallest_width_container,
                        "Smallest width",
                        if (sw1 == sw2) { sw1.toString() } else { "$sw1 (Default: $sw2)" }
                    )

                    val isEnabled = hiddenApi.getPowerState(display.displayId)
                    if (isEnabled != stateSwitch.isChecked) {
                        stateSwitch.setOnCheckedChangeListener(null)
                        stateSwitch.isChecked = isEnabled
                        stateSwitch.setOnCheckedChangeListener(onSwitchListener)
                    }
                }
            } catch (e: Throwable) {
                Log.d(MainActivity::class.simpleName, e.message, e)
            }
        }
    }

    private fun setText(resId: Int, text1: String?, text2: String?) {
        val view = findViewById<View>(resId)
        val resTitle = view.findViewById<TextView>(android.R.id.text1)
        resTitle.text = text1
        val res = view.findViewById<TextView>(android.R.id.text2)
        res.text = text2
    }
}
