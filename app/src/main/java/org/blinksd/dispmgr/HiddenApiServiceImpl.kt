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
import android.view.IWindowManager
import android.view.WindowManagerGlobal

class HiddenApiServiceImpl : IHiddenApiService.Stub() {
    private val iWindowManager: IWindowManager = WindowManagerGlobal.getWindowManagerService()

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
}
