package com.yupfeg.dispatcher

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.MainThread
import com.yupfeg.dispatcher.monitor.TaskExecuteMonitor
import com.yupfeg.dispatcher.task.OnTaskStateListener
import com.yupfeg.dispatcher.task.Task
import com.yupfeg.dispatcher.task.TaskWrapper
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 启动任务调度器
 * @author yuPFeG
 * @date 2022/01/05
 */
class TaskDispatcher internal constructor(builder: TaskDispatcherBuilder){

    companion object{
        /**
         * 最大等待时间(ms)
         * * 主线程等待锁的超时时间
         */
        private const val MAX_WAIT_TIME = 10 * 1000
    }

    /**
     * 异步任务的运行结果集合
     * - 仅用于方便结束任务
     * */
    private val mTaskFutures: MutableList<Future<*>> = mutableListOf()

    /**
     * 全部启动任务的集合
     */
    private val mAllTasks : MutableList<Task> = builder.allTasks

    /**
     * 主线程等待的阻塞同步锁
     * */
    private var mCountDownLatch : CountDownLatch? = null

    /**
     * 主线程需要等待的任务数量
     * */
    private var mNeedWaitTaskCount : AtomicInteger = builder.needWaitTaskCount

    /**
     * 已完成的任务数量
     * */
    private val mFinishTaskCount : AtomicInteger = AtomicInteger(0)

    /**
     * 任务标识与所有的子任务集合的依赖关系哈希表
     * */
    private val mTaskDependsClazzMap : HashMap<String, MutableList<Task>>
        = builder.taskDependsClazzMap

    /**
     * 在主线程执行的任务集合
     * */
    private val mMainThreadTasks : MutableList<Task> = mutableListOf()

    /**
     * 启动任务执行性能监视器
     * */
    internal val taskExecuteMonitor : TaskExecuteMonitor = builder.executeMonitor

    /**
     * 启动任务执行状态回调监听
     * */
    private val mTaskStatusListener : OnTaskStateListener? = builder.taskStatusListener

    /**
     * 任务调度器的执行状态监听
     * */
    private val mDispatcherStateListener : OnDispatcherStateListener?
        = builder.onDispatcherStatusListener

    /**
     * 当前是否处于主进程
     * */
    @get:JvmName("isMainProcess")
    val isMainProcess : Boolean = TaskDispatcherBuilder.isMainProcess

    /**主线程Handler*/
    private val mHandler : Handler by lazy(LazyThreadSafetyMode.SYNCHRONIZED){
        Handler(Looper.getMainLooper())
    }

    /**
     * 取消所有执行中的任务
     */
    @Suppress("unused")
    fun cancel() {
        for (future in mTaskFutures) {
            future.cancel(true)
        }
    }

    /**
     * 开始任务调度
     */
    @Suppress("unused")
    @MainThread
    fun start(){
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw RuntimeException("must be called from UiThread")
        }
        taskExecuteMonitor.recordDispatchStartTime()
        //执行任务开始前的Head Task
        mDispatcherStateListener?.onStartBefore()
        if (mAllTasks.isNotEmpty()){
            mCountDownLatch = CountDownLatch(mNeedWaitTaskCount.get())
            //开始调度任务
            dispatchAsyncTasks()
            executeMainThreadTask()
            //阻塞等待必须执行完毕的异步任务
            await()
        }
        taskExecuteMonitor.recordMainThreadTimeCost()
    }

    /**
     * 调度异步任务
     * */
    private fun dispatchAsyncTasks(){
        for (task in mAllTasks){
            //设置任务执行回调监听
            task.setStatusListener(mTaskStatusListener)

            if (task.isOnlyMainProcess && !isMainProcess){
                //只在主进程中调用的任务，且非主进程时，直接完成该任务
                markTaskOverDone(task)
                continue
            }

            if (task.isRunOnMainThread){
                mMainThreadTasks.add(task)
                continue
            }
            //调度异步任务
            val wrapper = TaskWrapper(task,this)
            val future = task.dispatchOn.submit(wrapper)
            mTaskFutures.add(future)
        }
    }

    /**
     * 执行主线程的任务
     * */
    private fun executeMainThreadTask(){
        val startTime = SystemClock.elapsedRealtime()
        for (task in mMainThreadTasks) {
            TaskWrapper(task,this).run()
        }
        taskExecuteMonitor.recordMainTaskTimeCost(startTime)
    }

    /**
     * 主线程进入等待状态
     * - 只在存在需要主线程等待的异步任务时才会执行
     * */
    @MainThread
    private fun await(){
        //存在需要等待的任务，主线程进入等待状态，等待所有前置任务执行完毕后才继续执行主线程
        if (mNeedWaitTaskCount.get() <= 0) return
        taskExecuteMonitor.recordWaitAsyncTaskCount(mNeedWaitTaskCount.get())
        mCountDownLatch?.await(MAX_WAIT_TIME.toLong(), TimeUnit.MILLISECONDS)
    }

    /**
     * 标记指定任务已完成
     * @param task 已完成的任务
     */
    fun markTaskOverDone(task : Task){
        notifyAllDependsWhenTaskDone(task.tag)
        if (task.isNeedMainWaitOver()){
            //如果该任务是需要主线程等待的，则主线程同步阻塞锁数量-1
            mCountDownLatch?.countDown()
            mNeedWaitTaskCount.getAndDecrement()
        }
        recordTaskFinish()
    }

    /**
     * 通知所有依赖目标任务的任务集合，目标任务已完成
     * @param taskTag 已完成任务的唯一标识
     */
    private fun notifyAllDependsWhenTaskDone(taskTag : String){
        val tasks = mTaskDependsClazzMap[taskTag]
        if (tasks.isNullOrEmpty()) return
        for (task in tasks){
            task.dependOverDone()
        }
    }

    /**
     * 记录任务完成
     * */
    private fun recordTaskFinish(){
        mFinishTaskCount.getAndIncrement()
        //所有任务已完成
        if (mFinishTaskCount.get() == mAllTasks.size){
            taskExecuteMonitor.recordAllTaskFinishTimeCost()
            taskExecuteMonitor.dispatchExecuteRecordInfo()
            //在所有任务执行完成后的Tail Task
            mDispatcherStateListener?.also {
                mHandler.post{ it.onFinish() }
            }
        }
    }

}