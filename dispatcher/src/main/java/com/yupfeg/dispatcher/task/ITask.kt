package com.yupfeg.dispatcher.task

import android.os.Process
import androidx.annotation.IntRange
import java.util.concurrent.ExecutorService

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
     * 任务运行目标线程池
     * - 与[isRunOnMainThread]冲突，
     * */
    val dispatchOn : ExecutorService

    /**
     * 任务优先级
     * */
    @IntRange(
        from = Process.THREAD_PRIORITY_FOREGROUND.toLong(),
        to = Process.THREAD_PRIORITY_LOWEST.toLong()
    )
    fun taskPriority() : Int
}