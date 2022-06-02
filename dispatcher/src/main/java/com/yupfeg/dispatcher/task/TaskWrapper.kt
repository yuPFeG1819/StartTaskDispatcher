package com.yupfeg.dispatcher.task

import android.os.Process
import com.yupfeg.dispatcher.ITaskDispatcher
import com.yupfeg.dispatcher.monitor.ITaskExecuteMonitor

/**
 * 抽象启动任务的包装类，实际执行的任务
 * @author yuPFeG
 * @date 2022/01/05
 */
internal class TaskWrapper @JvmOverloads constructor(
    private val originTask: Task,
    private val taskMonitor : ITaskExecuteMonitor,
    private val dispatcher: ITaskDispatcher? = null,
) : Runnable {

    override fun run() {
        Process.setThreadPriority(originTask.taskPriority())
        val waitTime = if (originTask.isNeedAwait()){
            //当前任务存在前置依赖任务
            originTask.onTaskWait(originTask.tag)
            ITaskExecuteMonitor.measureTime {
                //等待前置任务完成
                try {
                    originTask.awaitDependsTask()
                }catch (ignore : Exception){}
            }
        }else 0f
        originTask.onTaskStart(originTask.tag, waitTime)
        val runTime = ITaskExecuteMonitor.measureTime {
            //执行当前任务
            originTask.run()
        }
        //记录任务执行时间
        recordTaskRunningInfo(waitTime, runTime)
        //通知后续任务可以执行
        dispatcher?.markTaskOverDone(originTask)
    }

    /**
     * 记录任务执行信息
     * @param waitTime 等待开始执行时间
     * @param runTime 任务实际执行时间
     * */
    private fun recordTaskRunningInfo(waitTime: Float, runTime: Float) {
        if (!originTask.isMonitorTaskOver) return
        val runningInfo = TaskRunningInfo(
            tag = originTask.tag,
            waitTime = waitTime,
            runTime = runTime,
            isNeedMainWait = originTask.isAsyncTaskNeedMainWaitOver(),
            threadId = Thread.currentThread().id,
            threadName = Thread.currentThread().name
        )
        originTask.onTaskFinish(runningInfo)
        //记录任务的执行信息
        taskMonitor.recordTaskRunningInfo(runningInfo)
    }

}