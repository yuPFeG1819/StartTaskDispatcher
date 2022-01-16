package com.yupfeg.dispatcher.task

import android.os.Process
import java.util.concurrent.ExecutorService

/**
 * 在主线程运行的启动任务
 * @author yuPFeG
 * @date 2022/01/04
 */
@Suppress("unused")
abstract class MainTask : Task(){

    override val isRunOnMainThread: Boolean
        get() = true

    override fun taskPriority(): Int {
        return Process.THREAD_PRIORITY_DEFAULT
    }

    override val dispatchOn: ExecutorService?
        get() = null
}
