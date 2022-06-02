package com.yupfeg.dispatcher.monitor.delay

import android.os.Looper
import com.yupfeg.dispatcher.monitor.ITaskExecuteMonitor
import com.yupfeg.dispatcher.task.TaskRunningInfo

/**
 * 延迟任务的执行性能监控类
 * @author yuPFeG
 * @date 2022/06/01
 */
internal class DelayTaskExecuteMonitor(
    private val onRecordListener: OnDelayTaskRecordListener?
) : ITaskExecuteMonitor {

    /**
     * 记录所有任务的执行信息集合
     */
    private val mTaskExecuteInfoList = mutableListOf<TaskRunningInfo>()

    /**
     * 所有主线程任务完成消耗时间(ms)
     * */
    private var mAllMainTaskCostTime: Float = 0f

    override fun recordTaskRunningInfo(runningInfo : TaskRunningInfo) {
        synchronized(this){
            mTaskExecuteInfoList.add(runningInfo)
            if (Looper.getMainLooper().thread == Thread.currentThread()) {
                //当前任务处于主线程，累加主线程执行任务的总时间
                mAllMainTaskCostTime += (runningInfo.runTime + runningInfo.waitTime)
            }
        }
    }

    /**
     * 分发调度任务执行记录信息
     * */
    override fun dispatchExecuteRecordInfo() {
        onRecordListener?.onAllTaskRecordResult(
            DelayTaskRecordInfo(
                allTaskCostTime = mAllMainTaskCostTime,
                allTaskRunInfoList = mTaskExecuteInfoList
            )
        )
    }

}