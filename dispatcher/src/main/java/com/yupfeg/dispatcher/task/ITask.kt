package com.yupfeg.dispatcher.task

import android.os.Process
import androidx.annotation.IntRange

/**
 * 抽象化的任务流程，应用于页面启用等位置，进行延迟启动或异步启动
 * @author yuPFeG
 * @date 2021/12/11
 */
interface ITask : Runnable{

    /**
     * 是否在主线程
     * */
    val isRunOnMainThread : Boolean

    /**
     * 是否只在主进程运行
     * */
    val isOnlyMainProcess : Boolean

    /**
     * 是否需要等待该任务完成
     * - 与[isRunOnMainThread]配合使用，主线程需要等待这个异步任务执行完毕后才能继续执行
     * - 比如地图的初始化，只有地图初始化完成后，才能进入主页
     * */
    val isNeedWaitTaskOver : Boolean

    /**
     * 任务优先级
     * - 数字越小，优先级越高，通常使用[Process]类常量
     * - 运行在主线程则不需要去改优先级，确保主线程优先抢占CPU
     * */
    @IntRange(
        from = Process.THREAD_PRIORITY_FOREGROUND.toLong(),
        to = Process.THREAD_PRIORITY_LOWEST.toLong()
    )
    fun taskPriority() : Int
}