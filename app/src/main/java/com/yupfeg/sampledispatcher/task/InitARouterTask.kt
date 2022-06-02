package com.yupfeg.sampledispatcher.task

import com.yupfeg.dispatcher.task.Task
import com.yupfeg.logger.ext.loggd

/**
 * 模拟初始化组件路由
 * @author yuPFeG
 * @date
 */
class InitARouterTask : Task(){

    companion object{
        const val TAG = "InitARouterTask"
    }

    override val tag: String
        get() = TAG

    override val isNeedWaitTaskOver: Boolean
        get() = true

    override fun run() {
        loggd("InitARouterTask run")
        Thread.sleep(300)
    }

}