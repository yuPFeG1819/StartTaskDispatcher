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
    private val onRecordListener : OnMonitorRecordListener? = null
) {

    /**
     * 记录所有任务的消耗时间
     * * 任务标识 - 消耗时间(ms)
     */
    private val mTaskExecuteCostTimeMap = HashMap<String,Long>()

    /**
     * 拓扑排序任务列表消耗时间(ms)
     * */
    private var mSortTaskCostTime : Long = 0

    /**
     * 调度启动任务的起始时间
     * * 调度器开始调度的时间戳
     * */
    private var mStartTime : Long = 0

    /**
     * 所有主线程任务完成消耗时间(ms)
     * */
    private var mAllMainTaskCostTime : Long = 0

    /**
     * 主线程启动消耗时间(ms)
     * * 主线程任务时间 + 主线程等待时间
     * */
    private var mMainThreadCostTime : Long = 0

    /**
     * 所有启动任务完成时间(ms)
     * */
    private var mAllTaskFinishTime : Long = 0

    /**
     * 需要主线程等待的任务数量
     * */
    private var mNeedWaitAsyncTaskCount : Int = 0

    private val mHandler : Handler by lazy(LazyThreadSafetyMode.SYNCHRONIZED){
        Handler(Looper.getMainLooper())
    }

    /**
     * 记录拓扑排序任务列表消耗的时间
     * @param time 消耗时间(ms)
     */
    fun recordSortTaskListTime(time : Long){
        mSortTaskCostTime = time
    }

    /**
     * 记录任务的运行消耗时间
     * @param tag 任务的唯一标识
     * @param runTime 任务运行时间(ms)
     * */
    fun recordTaskCostTime(tag : String,runTime : Long){
        mTaskExecuteCostTimeMap[tag] = runTime
    }

    /**
     * 记录启动任务调度开启时间
     * */
    fun recordDispatchStartTime(){
        mStartTime = SystemClock.elapsedRealtime()
    }

    /**
     * 记录主线程启动消耗时间
     * - 也就是实际主线程的占用的时间
     * */
    fun recordMainThreadTimeCost(){
        mMainThreadCostTime = measureTime(mStartTime)
    }

    /**
     * 记录主线程任务消耗时间
     * @param startTime 主线程任务启动时间(ms)
     */
    fun recordMainTaskTimeCost(startTime : Long){
        mAllMainTaskCostTime = measureTime(startTime)
    }

    /**
     * 记录所有任务完成消耗时间
     * */
    fun recordAllTaskFinishTimeCost(){
        mAllTaskFinishTime = measureTime(mStartTime)
    }

    /**
     * 记录需要主线程等待的异步任务数量
     * */
    fun recordWaitAsyncTaskCount(count : Int){
        mNeedWaitAsyncTaskCount = count
    }

    /**
     * 测量从指定开始时间到当前时间经过的时间(ms)
     * @param start 开始时间的时间戳(ms)
     * */
    private fun measureTime(start : Long) : Long{
        return (SystemClock.elapsedRealtime() - start)
    }

    /**
     * 分发调度器执行记录信息
     * */
    fun dispatchExecuteRecordInfo(){
        val recordInfo = ExecuteRecordInfo(
            startCostTime = mMainThreadCostTime,
            mainCostTime = mAllMainTaskCostTime,
            allTaskCostTime = mAllTaskFinishTime,
            waitAsyncTaskCount = mNeedWaitAsyncTaskCount,
            sortTaskCostTime = mSortTaskCostTime,
            allTaskCostMap = mTaskExecuteCostTimeMap
        )
        mHandler.post {
            onRecordListener?.onMonitorRecordResult(recordInfo)
        }
    }

}

