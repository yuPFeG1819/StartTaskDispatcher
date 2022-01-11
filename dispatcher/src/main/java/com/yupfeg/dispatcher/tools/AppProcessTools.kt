package com.yupfeg.dispatcher.tools

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder

/**
 * 应用进程相关工具类
 * @author yuPFeG
 * @date 2022/01/07
 */
object AppProcessTools{

    /**
     * 校验是否为主进程
     * - 有些任务只能在主进程初始化
     * @param context [Context]
     */
    @JvmStatic
    fun isMainProcess(context: Context): Boolean {
        val processName = getCurrProcessName(context)
        if (processName.isNullOrEmpty()) return true
        return if (processName.contains(":")) false
        else processName == context.packageName
    }

    /**
     * 根据Pid得到进程名
     * @param context
     */
    @JvmStatic
    fun getCurrProcessName(context: Context): String? {
        val pid = Process.myPid()
        val am = context.getSystemService(Application.ACTIVITY_SERVICE) as? ActivityManager
        val runningApps = am?.runningAppProcesses
        runningApps?: run {
            return getCurrProcessNameFromLocalFile() ?:""
        }
        for (processInfo in runningApps) {
            if (processInfo.pid == pid) {
                return processInfo.processName
            }
        }
        return ""
    }

    /**
     * 从本地系统文件中获取进程名称
     * - 通常不会执行
     * */
    private fun getCurrProcessNameFromLocalFile() : String?{
        var cmdlineReader: BufferedReader? = null
        try {
            cmdlineReader = BufferedReader(
                InputStreamReader(
                    FileInputStream(
                        "/proc/" + Process.myPid() + "/cmdline"
                    ),
                    "iso-8859-1"
                )
            )
            var c: Int
            val processName = StringBuilder()
            while (cmdlineReader.read().also { c = it } > 0) {
                processName.append(c.toChar())
            }
            return processName.toString()
        } catch (e: Throwable) {
            // ignore
        } finally {
            if (cmdlineReader != null) {
                try {
                    cmdlineReader.close()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        return null
    }
}
