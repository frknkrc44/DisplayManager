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
import android.util.DisplayMetrics
import kotlin.math.min

class DensityHelper private constructor() {
    companion object {
        fun calculateSmallestWidth(size: Point, densityDpi: Int) = dpiFromPx(min(size.x, size.y), densityDpi)

        fun dpiFromPx(size: Int, densityDpi: Int): Float {
            val densityRatio = densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
            return size / densityRatio
        }
    }
}