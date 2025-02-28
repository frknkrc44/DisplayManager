// Copyright (C) 2025 Furkan Karcıoğlu <https://github.com/frknkrc44>
//
// This file is part of DisplayManager project,
// and licensed under GNU Affero General Public License v3.
// See the GNU Affero General Public License for more details.
//
// All rights reserved. See COPYING, AUTHORS.
//

package org.blinksd.dispmgr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.SurfaceControl
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class HiddenApiService(private val context: Context) {
    companion object {
        /** Windowing mode is currently not defined.  */
        const val WINDOWING_MODE_UNDEFINED: Int = 0
        /** Occupies the full area of the screen or the parent container.  */
        const val WINDOWING_MODE_FULLSCREEN: Int = 1
        /** Always on-top (always visible). of other siblings in its parent container.  */
        const val WINDOWING_MODE_PINNED: Int = 2
        /** Can be freely resized within its parent container.  */
        const val WINDOWING_MODE_FREEFORM: Int = 5
        /** Generic multi-window with no presentation attribution from the window manager.  */
        const val WINDOWING_MODE_MULTI_WINDOW: Int = 6

        // --- Android 15 ---
        /**
         * Display state: The display is off.
         */
        val STATE_OFF: Int = 1

        /**
         * Display state: The display is on.
         */
        val STATE_ON: Int =  2

        /**
         * Display state: The display is dozing in a low power state; it is still
         * on but is optimized for showing system-provided content while the
         * device is non-interactive.
         */
        val STATE_DOZE: Int = 3

        /**
         * Display state: The display is dozing in a suspended low power state; it is still
         * on but the CPU is not updating it. This may be used in one of two ways: to show
         * static system-provided content while the device is non-interactive, or to allow
         * a "Sidekick" compute resource to update the display. For this reason, the
         * CPU must not control the display in this mode.
         */
        val STATE_DOZE_SUSPEND: Int = 4

        /**
         * Display state: The display is on and optimized for VR mode.
         */
        val STATE_VR: Int = 5

        /**
         * Display state: The display is in a suspended full power state; it is still
         * on but the CPU is not updating it. This may be used in one of two ways: to show
         * static system-provided content while the device is non-interactive, or to allow
         * a "Sidekick" compute resource to update the display. For this reason, the
         * CPU must not control the display in this mode.
         */
        val STATE_ON_SUSPEND: Int = 6

        // --- Pre-Android 15 ---

        /**
         * Display power mode off: used while blanking the screen.
         * Use only with [SurfaceControl.setDisplayPowerMode].
         */
        const val POWER_MODE_OFF: Int = 0

        /**
         * Display power mode doze: used while putting the screen into low power mode.
         * Use only with [SurfaceControl.setDisplayPowerMode].
         */
        const val POWER_MODE_DOZE: Int = 1

        /**
         * Display power mode normal: used while unblanking the screen.
         * Use only with [SurfaceControl.setDisplayPowerMode].
         */
        const val POWER_MODE_NORMAL: Int = 2

        /**
         * Display power mode doze: used while putting the screen into a suspended
         * low power mode.  Use only with [SurfaceControl.setDisplayPowerMode].
         */
        const val POWER_MODE_DOZE_SUSPEND: Int = 3

        /**
         * Display power mode on: used while putting the screen into a suspended
         * full power mode.  Use only with [SurfaceControl.setDisplayPowerMode].
         */
        const val POWER_MODE_ON_SUSPEND: Int = 4

        fun findRotationByMode(mode: Int) = RotationMode.entries.find { it.mode == mode }
    }

    private var mService: IHiddenApiService? = null
    private var mConnection: ServiceConnection? = null
    private var mutex = Mutex()
    private val intent by lazy { Intent(context, HiddenApiService::class.java) }

    fun log(str: String, throwable: Throwable? = null) = Log.d(javaClass.simpleName, str, throwable)

    enum class RotationMode(val mode: Int, val title: String) {
        Rotation0(0, "0°"),
        Rotation90(1, "90°"),
        Rotation180(2, "180°"),
        Rotation270(3, "270°"),
    }

    enum class WindowingMode(val mode: Int, val title: String) {
        Undefined(WINDOWING_MODE_UNDEFINED, "Undefined"),
        FullScreen(WINDOWING_MODE_FULLSCREEN, "Full screen"),
        Pinned(WINDOWING_MODE_PINNED, "Pinned"),
        Freeform(WINDOWING_MODE_FREEFORM, "Freeform"),
        MultiWindow(WINDOWING_MODE_MULTI_WINDOW, "Multi window");

        override fun toString() = "$title ($mode)"
    }

    suspend fun getPowerState(displayId: Int): Boolean {
        return getService().getDisplayInfo(displayId).state != STATE_OFF
    }

    suspend fun setPowerState(displayId: Int, on: Boolean): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return getService().requestDisplayPower(displayId, if (on) STATE_ON else STATE_OFF)
        }

        val token = getService().getPhysicalDisplayToken(displayId.toLong())
        getService().setDisplayPowerMode(token, if (on) POWER_MODE_NORMAL else POWER_MODE_OFF)
        return true
    }

    private class HiddenApiService : RootService() {
        override fun onBind(intent: Intent) = HiddenApiServiceImpl()
    }

    private suspend fun bindService(): IHiddenApiService = run {
        suspendCoroutine { continuation ->
            if (mService == null) {
                mConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) {
                        val msg = "Service connected."
                        log(msg)
                        mService = IHiddenApiService.Stub.asInterface(service)
                        if (continuation.context.isActive) continuation.resume(mService!!)
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        val msg = "Service disconnected."
                        log(msg)
                        destroyService()
                        if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                    }

                    override fun onBindingDied(name: ComponentName) {
                        val msg = "Binding died."
                        log(msg)
                        destroyService()
                        if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                    }

                    override fun onNullBinding(name: ComponentName) {
                        val msg = "Null binding."
                        log(msg)
                        destroyService()
                        if (continuation.context.isActive) continuation.resumeWithException(RemoteException(msg))
                    }
                }
                RootService.bind(intent, mConnection!!)
            }

            mService
        }
    }

    /**
     * Destroy the service.
     */
    fun destroyService(killDaemon: Boolean = false) {
        if (killDaemon) {
            if (mConnection != null) {
                RootService.unbind(mConnection!!)
            }
            RootService.stopOrTask(intent)
        }

        mConnection = null
        mService = null
    }

    suspend fun getService(): IHiddenApiService = mutex.withLock {
        return tryOnScope(
            block = {
                withMainContext {
                    if (mService == null) {
                        bindService()
                    } else if (mService!!.asBinder().isBinderAlive.not()) {
                        mService = null
                        bindService()
                    } else {
                        mService!!
                    }
                }
            },
            onException = {
                withMainContext {
                    log(it.toString(), it)
                    mService = null
                    getService()
                }
            }
        )
    }
}
