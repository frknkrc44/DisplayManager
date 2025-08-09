package android.hardware.display;

import android.view.Display;
import android.view.DisplayInfo;

public class DisplayManagerGlobal {
    public static DisplayManagerGlobal getInstance() {
        throw new RuntimeException("Stub!");
    }

    public DisplayInfo getDisplayInfo(int displayId) {
        throw new RuntimeException("Stub!");
    }

    public int[] getDisplayIds(boolean includeDisabled) {
        throw new RuntimeException("Stub!");
    }

    public void enableConnectedDisplay(int displayId) {
        throw new RuntimeException("Stub!");
    }

    public void disableConnectedDisplay(int displayId) {
        throw new RuntimeException("Stub!");
    }

    public boolean requestDisplayPower(int displayId, int state) {
        throw new RuntimeException("Stub!");
    }

    public void setUserPreferredDisplayMode(int displayId, Display.Mode mode) {
        throw new RuntimeException("Stub!");
    }

    public Display.Mode getUserPreferredDisplayMode(int displayId) {
        throw new RuntimeException("Stub!");
    }

    public Display.Mode getSystemPreferredDisplayMode(int displayId) {
        throw new RuntimeException("Stub!");
    }
}
