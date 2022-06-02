package com.yupfeg.sampledispatcher.task

import android.app.Application
import com.yupfeg.dispatcher.task.MainTask
import com.yupfeg.logger.ext.loggd

/**
 * 监听Activity生命周期的任务
 * @author yuPFeG
 * @date
 */
class ActivityLifecycleTask(private val application: Application) : MainTask(){

    companion object{
        const val TAG = "ActivityLifecycleTask"
    }

    override val tag: String
        get() = TAG

    override fun run() {
        loggd("init ActivityLifecycleTask")
    }
}