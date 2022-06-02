package com.yupfeg.dispatcher.monitor

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.MainThread
import com.yupfeg.dispatcher.task.TaskRunningInfo

/**
 * 任务调度器运行性能的监视器
 * @author yuPFeG
 * @date 2022/01/07
 */
internal class TaskExecuteMonitor(
    private val onRecordListener: OnMonitorRecordListener? = null
) : ITaskExecuteMonitor{

    /**
     * 记录所有任务的执行信息
     */
    private val mTaskExecuteInfoList = mutableListOf<TaskRunningInfo>()

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
     * 所有启动任务完成时间(ms)
     * */
    private var mAllTaskFinishTime: Float = 0f

    /**
     * 需要主线程等待的任务数量
     * */
    private var mNeedWaitAsyncTaskCount: Int = 0

    /**
     * 主线程总计等待时间
     * */
    private var mMainThreadWaitTime : Float = 0f

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
     * @param runningInfo 任务运行时间(ms)
     * */
    override fun recordTaskRunningInfo(runningInfo: TaskRunningInfo) {
        synchronized(mLock) {
            mTaskExecuteInfoList.add(runningInfo)
            if (Looper.getMainLooper().thread == Thread.currentThread()) {
                //当前任务处于主线程，累加到主线程执行任务的总时间
                mAllMainTaskCostTime += (runningInfo.waitTime + runningInfo.runTime)
            }
        }
    }

    /**
     * 分发调度器执行记录信息
     * */
    override fun dispatchExecuteRecordInfo() {
        val recordInfo = ExecuteRecordInfo(
            allMainTaskCostTime = mAllMainTaskCostTime,
            allTaskCostTime = mAllTaskFinishTime,
            waitAsyncTaskCount = mNeedWaitAsyncTaskCount,
            mainThreadWaitTime = mMainThreadWaitTime,
            sortTaskCostTime = mSortTaskCostTime,
            allTaskRunInfoList = mTaskExecuteInfoList
        )
        mHandler.post {
            onRecordListener?.onAllTaskRecordResult(recordInfo)
        }
    }

    /**
     * 记录启动任务调度开启时间
     * */
    fun recordDispatchStartTime() {
        mStartTime = SystemClock.elapsedRealtimeNanos()
    }

    /**
     * 分发记录主线程总计消耗时间（ms）
     * - 调度器实际占用主线程的时间，任务排序+任务调度+主线程任务+主线程等待
     * */
    @MainThread
    fun dispatchMainThreadTimeCost() {
        onRecordListener?.onMainThreadRecord(measureTime(mStartTime) + mSortTaskCostTime)
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
     * 记录主线程等待时间
     * @param action 主线程等待的执行代码块
     * */
    @MainThread
    fun recordMainThreadWaitTime(action: () -> Unit){
        val startTime = SystemClock.elapsedRealtimeNanos()
        action()
        mMainThreadWaitTime = measureTime(startTime)
    }

    /**
     * 测量从指定开始时间到当前时间经过的时间(ms)
     * @param start 开始时间的时间戳(nanoTime)
     * @return 执行经过时间(ms)
     * */
    private fun measureTime(start: Long): Float {
        val endTime = SystemClock.elapsedRealtimeNanos()
        return (endTime - start) / ITaskExecuteMonitor.NANO_TIME_UNIT
    }

}

