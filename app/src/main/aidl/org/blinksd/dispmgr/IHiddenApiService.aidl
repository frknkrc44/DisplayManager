// Copyright (C) 2025 Furkan Karcıoğlu <https://github.com/frknkrc44>
//
// This file is part of DisplayManager project,
// and licensed under GNU Affero General Public License v3.
// See the GNU Affero General Public License for more details.
//
// All rights reserved. See COPYING, AUTHORS.
//

package org.blinksd.dispmgr;

import android.graphics.Point;
import android.os.IBinder;
import android.view.Display.Mode;
import android.view.DisplayInfo;

interface IHiddenApiService {
    void getInitialDisplaySize(int displayId, out Point size);

    void getBaseDisplaySize(int displayId, out Point size);

    void setForcedDisplaySize(int displayId, int width, int height);

    Mode getSystemPreferredDisplayMode(int displayId);

    Mode getUserPreferredDisplayMode(int displayId);

    void setUserPreferredDisplayMode(int displayId, in Mode mode);

    void clearForcedDisplaySize(int displayId);

    int getInitialDisplayDensity(int displayId);

    int getBaseDisplayDensity(int displayId);

    void setForcedDisplayDensityForUser(int displayId, int density, int userId);

    void clearForcedDisplayDensityForUser(int displayId, int userId);

    int getDisplayUserRotation(int displayId);

    void freezeDisplayRotation(int displayId, int rotation, String caller);

    void thawDisplayRotation(int displayId, String caller);

    boolean isDisplayRotationFrozen(int displayId);

    int getWindowingMode(int displayId);

    void setWindowingMode(int displayId, int mode);

    DisplayInfo getDisplayInfo(int displayId);

    int[] getDisplayIds(boolean includeDisabled);

    void enableConnectedDisplay(int displayId);

    void disableConnectedDisplay(int displayId);

    boolean requestDisplayPower(int displayId, int state);

    IBinder getPhysicalDisplayToken(long physicalDisplayId);

    void setDisplayPowerMode(IBinder displayToken, int mode);
}
