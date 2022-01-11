package com.yupfeg.dispatcher

/**
 * 启动任务调度器的执行监听
 * @author yuPFeG
 * @date 2022/01/08
 */
interface OnDispatcherStateListener {

    /**
     * 是否输出依赖关系的哈希表
     * */
    val isPrintDependsMap : Boolean
        get() = false

    /**
     * 任务排序完成回调
     * @param dependsInfo 依赖关系的遍历信息字符串，用于快捷检查依赖关系
     * */
    fun onTaskSorted(dependsInfo : String) = Unit

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

    /**
     * 是否处于调试状态
     * 用于是否遍历输出所有任务的依赖关系，默认为false不会遍历输出依赖关系，提高性能
     * */
    @JvmField
    var isDebug : Boolean = false

    /**任务排序完成回调，返回整理后的依赖关系字符串*/
    @JvmField
    var onTaskSorted : ((String)->Unit)? = null

    /**调度器启动前回调*/
    @JvmField
    var onStartBefore : (()->Unit)? = null

    /**所有任务结束后回调*/
    @JvmField
    var onFinish : (()->Unit)? = null

    override val isPrintDependsMap: Boolean
        get() = isDebug

    override fun onTaskSorted(dependsInfo: String) {
        onTaskSorted?.invoke(dependsInfo)
    }

    override fun onStartBefore() {
        onStartBefore?.invoke()
    }

    override fun onFinish() {
        onFinish?.invoke()
    }

}