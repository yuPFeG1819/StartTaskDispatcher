package com.yupfeg.dispatcher.task


/**
 * 抽象任务执行状态的回调监听
 * - 由于任务调度可能在子线程执行，需要注意线程安全
 * @author yuPFeG
 * @date 2022/01/07
 */
interface OnTaskStatusListener {
    /**
     * 在任务进入等待前回调
     * - 可能在子线程，注意线程安全
     * - 如果任务存在前置任务，会先等待前置任务完成后再执行，这是最早的任务调度状态
     * - 如果不存在前置任务，不会调用该方法
     * @param tag 任务唯一标识
     * */
    fun onTaskWait(tag: String)

    /**
     * 在任务开始前回调
     * - 可能在子线程，注意线程安全
     * @param tag 任务唯一标识
     * @param waitTime 等待前置任务的时间(ms)
     * */
    fun onTaskStart(tag : String,waitTime: Float)

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
open class DefaultTaskStatusListener : OnTaskStatusListener{

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
    var onStart : ((tag : String,waitTime : Float)->Unit)? = null

    /**
     * 任务完成后回调，返回任务执行信息记录
     * - 可能在子线程，注意线程安全
     * */
    var onFinished : ((runningInfo: TaskRunningInfo)->Unit)? = null

    override fun onTaskWait(tag: String) {
        onWait?.invoke(tag)
    }

    override fun onTaskStart(tag: String, waitTime: Float) {
        onStart?.invoke(tag, waitTime)
    }

    override fun onTaskFinish(runningInfo: TaskRunningInfo) {
        onFinished?.invoke(runningInfo)
    }

}

/**
 * 任务运行状态信息
 * */
data class TaskRunningInfo(
    val tag : String,
    /**任务等待前置任务时间(ms)*/
    val waitTime: Float,
    /**任务执行时间(ms)*/
    val runTime: Float,
    /**是否需要主线程等待*/
    val isNeedMainWait : Boolean,
    /**执行线程id*/
    val threadId : Long,
    /**执行线程名称*/
    val threadName : String
){
    override fun toString(): String {
        return "task tag : $tag , waitTime : ${String.format("%.2f",waitTime)} ms ," +
                "runTime : ${String.format("%.2f",runTime)} ms , isNeedMainWait : $isNeedMainWait , \n" +
                "RunOn : ======>>> ThreadId : $threadId, ThreadName : $threadName"
    }
}