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
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
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

        fun findRotationByMode(mode: Int) = RotationMode.entries.find { it.mode == mode }
        fun findWindowingByMode(mode: Int) = WindowingMode.entries.find { it.mode == mode }
    }

    private var mService: IHiddenApiService? = null
    private var mConnection: ServiceConnection? = null
    private var mutex = Mutex()
    private val intent by lazy { Intent(context, HiddenApiService::class.java) }

    fun log(str: String, throwable: Throwable? = null) = Log.d(javaClass.simpleName, str, throwable)

    enum class RotationMode(val mode: Int, val title: String) {
        rotation0(0, "0°"),
        rotation90(1, "90°"),
        rotation180(2, "180°"),
        rotation270(3, "270°"),
    }

    enum class WindowingMode(val mode: Int, val title: String) {
        undefined(WINDOWING_MODE_UNDEFINED, "Undefined"),
        fullscreen(WINDOWING_MODE_FULLSCREEN, "Full screen"),
        pinned(WINDOWING_MODE_PINNED, "Pinned"),
        freeform(WINDOWING_MODE_FREEFORM, "Freeform"),
        multiWindow(WINDOWING_MODE_MULTI_WINDOW, "Multi window"),
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
