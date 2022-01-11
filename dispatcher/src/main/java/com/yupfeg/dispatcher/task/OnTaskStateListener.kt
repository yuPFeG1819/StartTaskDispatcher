package com.yupfeg.dispatcher.task


/**
 * 抽象任务执行状态的回调监听
 * - 由于任务调度可能在子线程执行，需要注意线程安全
 * @author yuPFeG
 * @date 2022/01/07
 */
interface OnTaskStateListener {
    /**
     * 在任务进入等待前回调
     * - 可能在子线程，注意线程安全
     * - 如果任务存在前置任务，会先等待前置任务完成后再执行，这是最早的任务调度状态
     * @param tag 任务唯一标识
     * */
    fun onTaskWait(tag: String)

    /**
     * 任务开始前回调
     * - 可能在子线程，注意线程安全
     * @param tag 任务唯一标识
     * @param waitTime 等待前置任务的时间(ms)
     * */
    fun onTaskStart(tag : String,waitTime: Long)

    /**
     * 任务完成回调
     * - 可能在子线程，注意线程安全
     * @param runningInfo 任务运行信息
     * */
    fun onTaskFinish(runningInfo: TaskRunningInfo)
}

/**
 * 默认实现的启动任务状态监听
 */
open class DefaultTaskStateListener : OnTaskStateListener{

    /**
     * 任务开始等待前回调，返回任务标识
     * - 可能在子线程，注意线程安全
     * - 如果任务存在前置任务，会先等待前置任务完成后再执行，这是最早的任务调度状态
     * */
    @Suppress("MemberVisibilityCanBePrivate")
    var onWait : ((tag : String)->Unit)? = null

    /**
     * 任务开始前回调，返回任务标识与等待前置任务时间(ms)
     * - 可能在子线程，注意线程安全
     * */
    var onStart : ((tag : String,waitTime : Long)->Unit)? = null

    /**
     * 任务完成后回调，返回任务执行信息记录
     * - 可能在子线程，注意线程安全
     * */
    var onFinished : ((runningInfo: TaskRunningInfo)->Unit)? = null

    override fun onTaskWait(tag: String) {
        onWait?.invoke(tag)
    }

    override fun onTaskStart(tag: String, waitTime: Long) {
        onStart?.invoke(tag, waitTime)
    }

    override fun onTaskFinish(runningInfo: TaskRunningInfo) {
        onFinished?.invoke(runningInfo)
    }

}

/**
 * 任务允许状态信息
 * */
data class TaskRunningInfo(
    val tag : String,
    /**任务等待前置任务时间*/
    val waitTime: Long,
    /**任务执行时间(ms)*/
    val runTime: Long,
    /**是否需要主线程等待*/
    val isNeedMainWait : Boolean,
    /**执行线程id*/
    val threadId : Long,
    /**执行线程名称*/
    val threadName : String
){
    override fun toString(): String {
        return "$tag task waitTime : $waitTime ms, runTime : $runTime ms, " +
                "isNeedMainWait : $isNeedMainWait \n" +
                "runOn ThreadId : $threadId, ThreadName : $threadName"
    }
}