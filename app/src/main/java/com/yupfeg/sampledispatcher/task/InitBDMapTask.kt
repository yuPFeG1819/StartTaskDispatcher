package com.yupfeg.sampledispatcher.task

import com.yupfeg.logger.ext.logd

/**
 * 模拟初始化百度地图的任务
 * @author yuPFeG
 * @date
 */
class InitBDMapTask : BaseAsyncTask() {

    companion object{
        const val TAG = "InitBDMapTask"
    }

    override val tag: String
        get() = TAG

    //需要主线程等待该任务完成
    override val isNeedWaitTaskOver: Boolean
        get() = true

    override fun run() {
        logd("InitBDMapTask run")
        Thread.sleep(200)
    }
}