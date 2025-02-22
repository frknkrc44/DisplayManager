// Copyright (C) 2025 Furkan Karcıoğlu <https://github.com/frknkrc44>
//
// This file is part of DisplayManager project,
// and licensed under GNU Affero General Public License v3.
// See the GNU Affero General Public License for more details.
//
// All rights reserved. See COPYING, AUTHORS.
//

package android.view;

import android.graphics.Point;

public interface IWindowManager {
    void setForcedDisplaySize(int displayId, int width, int height);

    void clearForcedDisplaySize(int displayId);

    void setForcedDisplayDensityForUser(int displayId, int density, int userId);

    void clearForcedDisplayDensityForUser(int displayId, int userId);

    void getInitialDisplaySize(int displayId, Point size);

    int getInitialDisplayDensity(int displayId);

    int getBaseDisplayDensity(int displayId);

    void getBaseDisplaySize(int displayId, Point size);

    int getDisplayUserRotation(int displayId);

    void freezeDisplayRotation(int displayId, int rotation, String caller);

    void thawDisplayRotation(int displayId, String caller);

    boolean isDisplayRotationFrozen(int displayId);

    int getWindowingMode(int displayId);

    void setWindowingMode(int displayId, int mode);
}
