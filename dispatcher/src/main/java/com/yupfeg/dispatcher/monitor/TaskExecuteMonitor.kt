package com.yupfeg.dispatcher.monitor

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * 任务调度器运行性能的监视器
 * @author yuPFeG
 * @date 2022/01/07
 */
internal class TaskExecuteMonitor(
    private val onRecordListener: OnMonitorRecordListener? = null
) {

    companion object {
        private const val NANO_TIME_UNIT = 1000000f

        /**
         * 测量从指定开始时间到当前时间经过的时间(ms)
         * @param action 测量执行时间的代码块
         * @return 执行经过时间(ms)
         * */
        inline fun measureTime(action: () -> Unit): Float {
            val startTime = SystemClock.elapsedRealtimeNanos()
            action()
            return (SystemClock.elapsedRealtimeNanos() - startTime) / NANO_TIME_UNIT
        }
    }

    /**
     * 记录所有任务的消耗时间Map
     * * 任务标识 - 消耗时间(ms)
     */
    private val mTaskExecuteCostTimeMap = HashMap<String, Float>()

    /**
     * 拓扑排序任务列表消耗时间(ms)
     * */
    private var mSortTaskCostTime: Float = 0f

    /**
     * 调度启动任务的起始时间 nanoTime
     * * 调度器开始调度的时间戳
     * */
    private var mStartTime: Long = 0

    /**
     * 所有主线程任务完成消耗时间(ms)
     * */
    private var mAllMainTaskCostTime: Float = 0f

    /**
     * 主线程启动消耗时间(ms)
     * * 主线程任务时间 + 主线程等待时间
     * */
    private var mMainThreadCostTime: Float = 0f

    /**
     * 所有启动任务完成时间(ms)
     * */
    private var mAllTaskFinishTime: Float = 0f

    /**
     * 需要主线程等待的任务数量
     * */
    private var mNeedWaitAsyncTaskCount: Int = 0

    private val mHandler: Handler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Handler(Looper.getMainLooper())
    }

    private val mLock = Any()

    /**
     * 记录拓扑排序任务列表消耗的时间
     * @param action 遍历排序执行代码块
     */
    fun recordSortTaskListTime(action: () -> Unit) {
        val startTime = SystemClock.elapsedRealtimeNanos()
        action()
        mSortTaskCostTime = measureTime(startTime)
    }

    /**
     * 记录任务的运行消耗时间
     * - 注意线程安全问题
     * @param tag 任务的唯一标识
     * @param runTime 任务运行时间(ms)
     * @param isMainThread 是否为主线程执行的任务
     * */
    fun recordTaskCostTime(tag: String, runTime: Float, isMainThread: Boolean) {
        synchronized(mLock) {
            mTaskExecuteCostTimeMap[tag] = runTime
            if (isMainThread) {
                //累加主线程执行任务的总时间
                mAllMainTaskCostTime += runTime
            }
        }
    }

    /**
     * 记录启动任务调度开启时间
     * */
    fun recordDispatchStartTime() {
        mStartTime = SystemClock.elapsedRealtimeNanos()
    }

    /**
     * 记录主线程消耗时间
     * - 也就是实际主线程的占用时间
     * */
    fun recordMainThreadTimeCost() {
        mMainThreadCostTime = measureTime(mStartTime)
    }

    /**
     * 记录所有任务完成消耗时间
     * */
    fun recordAllTaskFinishTimeCost() {
        mAllTaskFinishTime = measureTime(mStartTime)
    }

    /**
     * 记录需要主线程等待的异步任务数量
     * */
    fun recordWaitAsyncTaskCount(count: Int) {
        mNeedWaitAsyncTaskCount = count
    }

    /**
     * 测量从指定开始时间到当前时间经过的时间(ms)
     * @param start 开始时间的时间戳(nanoTime)
     * @return 执行经过时间(ms)
     * */
    private fun measureTime(start: Long): Float {
        return (SystemClock.elapsedRealtimeNanos() - start) / NANO_TIME_UNIT
    }

    /**
     * 分发调度器执行记录信息
     * */
    fun dispatchExecuteRecordInfo() {
        val recordInfo = ExecuteRecordInfo(
            mainThreadCostTime = mMainThreadCostTime,
            allMainTaskCostTime = mAllMainTaskCostTime,
            allTaskCostTime = mAllTaskFinishTime,
            waitAsyncTaskCount = mNeedWaitAsyncTaskCount,
            sortTaskCostTime = mSortTaskCostTime,
            allTaskCostMap = mTaskExecuteCostTimeMap
        )
        mHandler.post {
            onRecordListener?.onAllTaskRecordResult(recordInfo)
        }
    }

}

