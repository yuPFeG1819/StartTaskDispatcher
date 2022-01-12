package com.yupfeg.dispatcher

/**
 * 启动任务调度器的执行监听
 * @author yuPFeG
 * @date 2022/01/08
 */
interface OnDispatcherStateListener {

    /**
     * 任务调度器启动前回调
     * - 处于主线程
     * - 通常用于执行一些Head Task
     * */
    fun onStartBefore()

    /**
     * 任务调度器完成后回调
     * - 处于主线程执行
     * - 通常用于在所有任务完成后执行一些Tail Task
     * */
    fun onFinish()
}

/**
 * 启动任务调度器运行状态监听的默认实现类
 * - 简化外部实现逻辑，按需实现
 * */
open class DefaultDispatcherStateListener : OnDispatcherStateListener{

    /**调度器启动前回调*/
    @JvmField
    var onStartBefore : (()->Unit)? = null

    /**所有任务结束后回调*/
    @JvmField
    var onFinish : (()->Unit)? = null

    override fun onStartBefore() {
        onStartBefore?.invoke()
    }

    override fun onFinish() {
        onFinish?.invoke()
    }

}