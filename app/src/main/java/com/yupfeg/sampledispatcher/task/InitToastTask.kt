package com.yupfeg.sampledispatcher.task

import android.app.Application
import com.yupfeg.logger.ext.logd

/**
 * 模拟初始化Toast框架
 * @author yuPFeG
 * @date
 */
class InitToastTask(private val application: Application) : BaseAsyncTask(){
    companion object{
        const val TAG = "InitToastTask"
    }

    override val tag: String
        get() = TAG

    //需要主线程等待
    override val isNeedWaitTaskOver: Boolean
        get() = true

    override fun run() {
        logd("init InitToastTask run")
    }
}