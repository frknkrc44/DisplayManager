// Copyright (C) 2025 Furkan Karcıoğlu <https://github.com/frknkrc44>
//
// This file is part of DisplayManager project,
// and licensed under GNU Affero General Public License v3.
// See the GNU Affero General Public License for more details.
//
// All rights reserved. See COPYING, AUTHORS.
//

package org.blinksd.dispmgr

import android.graphics.Point
import android.hardware.display.DisplayManagerGlobal
import android.os.Build
import android.os.IBinder
import android.view.DisplayInfo
import android.view.SurfaceControl
import android.view.WindowManagerGlobal
import androidx.annotation.RequiresApi

class HiddenApiServiceImpl : IHiddenApiService.Stub() {
    private val iWindowManager = WindowManagerGlobal.getWindowManagerService()
    private val displayManager = DisplayManagerGlobal.getInstance()

    override fun getInitialDisplaySize(displayId: Int, size: Point) {
        iWindowManager.getInitialDisplaySize(displayId, size)
    }

    override fun getBaseDisplaySize(displayId: Int, size: Point) {
        iWindowManager.getBaseDisplaySize(displayId, size)
    }

    override fun setForcedDisplaySize(
        displayId: Int,
        width: Int,
        height: Int,
    ) = iWindowManager.setForcedDisplaySize(displayId, width, height)

    override fun clearForcedDisplaySize(displayId: Int) {
        iWindowManager.clearForcedDisplaySize(displayId)
    }

    override fun getInitialDisplayDensity(displayId: Int): Int {
        return iWindowManager.getInitialDisplayDensity(displayId)
    }

    override fun getBaseDisplayDensity(displayId: Int): Int {
        return iWindowManager.getBaseDisplayDensity(displayId)
    }

    override fun setForcedDisplayDensityForUser(
        displayId: Int,
        density: Int,
        userId: Int
    ) = iWindowManager.setForcedDisplayDensityForUser(displayId, density, userId)

    override fun clearForcedDisplayDensityForUser(displayId: Int, userId: Int) {
        iWindowManager.clearForcedDisplayDensityForUser(displayId, userId)
    }

    override fun getDisplayUserRotation(displayId: Int): Int {
        return iWindowManager.getDisplayUserRotation(displayId)
    }

    override fun freezeDisplayRotation(
        displayId: Int,
        rotation: Int,
        caller: String
    ) = iWindowManager.freezeDisplayRotation(displayId, rotation, caller)

    override fun thawDisplayRotation(displayId: Int, caller: String) {
        iWindowManager.thawDisplayRotation(displayId, caller)
    }

    override fun isDisplayRotationFrozen(displayId: Int): Boolean {
        return iWindowManager.isDisplayRotationFrozen(displayId)
    }

    override fun getWindowingMode(displayId: Int): Int {
        return iWindowManager.getWindowingMode(displayId)
    }

    override fun setWindowingMode(displayId: Int, mode: Int) {
        iWindowManager.setWindowingMode(displayId, mode)
    }

    override fun getDisplayInfo(displayId: Int): DisplayInfo? {
        return displayManager.getDisplayInfo(displayId)
    }

    override fun getDisplayIds(includeDisabled: Boolean): IntArray? {
        return displayManager.getDisplayIds(includeDisabled)
    }

    override fun enableConnectedDisplay(displayId: Int) {
        return displayManager.disableConnectedDisplay(displayId)
    }

    override fun disableConnectedDisplay(displayId: Int) {
        return displayManager.enableConnectedDisplay(displayId)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun requestDisplayPower(displayId: Int, state: Int): Boolean {
        return displayManager.requestDisplayPower(displayId, state)
    }

    override fun getPhysicalDisplayToken(physicalDisplayId: Long): IBinder? {
        val method = SurfaceControl::class.java.getDeclaredMethod("setDisplayPowerMode", Long::class.java)
        method.isAccessible = true
        return method.invoke(null, physicalDisplayId) as IBinder?
    }

    override fun setDisplayPowerMode(displayToken: IBinder, mode: Int) {
        SurfaceControl::class.java.getDeclaredMethod("setDisplayPowerMode", IBinder::class.java, Int::class.java).also {
            it.isAccessible = true
            it.invoke(null, displayToken, mode)
        }
    }
}
