package com.yupfeg.dispatcher

import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import com.yupfeg.dispatcher.monitor.TaskExecuteMonitor
import com.yupfeg.dispatcher.task.OnTaskStatusListener
import com.yupfeg.dispatcher.task.Task
import com.yupfeg.dispatcher.task.TaskWrapper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 启动任务调度器
 * @author yuPFeG
 * @date 2022/01/05
 */
class TaskDispatcher internal constructor(builder: TaskDispatcherBuilder) : ITaskDispatcher {

    companion object {
        /**
         * 默认最大等待时间(ms)
         * * 主线程等待锁的超时时间
         */
        private const val DEF_MAX_WAIT_TIME : Long = 10 * 1000
    }

    /**
     * 异步任务的运行结果集合
     * - 仅用于方便结束异步任务
     * */
    private val mTaskFutures: MutableList<Future<*>> = mutableListOf()

    /**
     * 全部启动任务的集合
     */
    private val mAllTasks: MutableList<Task> = builder.allTasks

    /**
     * 主线程等待的阻塞同步锁
     * */
    private var mCountDownLatch: CountDownLatch? = null

    /**
     * 主线程需要等待的任务数量
     * */
    private var mNeedWaitTaskCount: AtomicInteger = builder.needWaitTaskCount

    /**
     * 已完成的任务数量
     * */
    private val mFinishTaskCount: AtomicInteger = AtomicInteger(0)

    /**
     * 任务标识与所有的子任务集合的依赖关系哈希表
     * */
    private val mTaskDependsClazzMap: HashMap<String, MutableList<Task>> =
        builder.taskDependsClazzMap

    /**
     * 启动任务执行性能监视器
     * */
    private val mTaskExecuteMonitor: TaskExecuteMonitor = builder.executeMonitor

    /**
     * 启动任务执行状态回调监听
     * */
    private val mTaskStatusListener: OnTaskStatusListener? = builder.taskStatusListener

    /**
     * 任务调度器的执行状态监听
     * */
    private val mDispatcherStateListener: OnDispatcherStateListener? =
        builder.onDispatcherStatusListener

    /**
     * 异步任务调度的线程池
     * */
    private val mExecutorService: ExecutorService = builder.executorService!!

    /**
     * 最大主线程等待时间（ms）
     * */
    private val mMaxWaitTime : Long
        = if (builder.maxWaitTime > 0) builder.maxWaitTime else DEF_MAX_WAIT_TIME

    /**
     * 当前是否处于主进程
     * */
    @get:JvmName("isMainProcess")
    val isMainProcess: Boolean = TaskDispatcherBuilder.isMainProcess

    /**是否处于正在运行状态*/
    @Suppress("MemberVisibilityCanBePrivate")
    var isRunning: Boolean = false
        private set

    /**主线程Handler*/
    private val mHandler: Handler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Handler(Looper.getMainLooper())
    }

    /**
     * 支持等待前置依赖任务
     * */
    override val isSupportAwaitDepends: Boolean
        get() = true

    /**
     * 总计需要执行的任务数
     * */
    private var totalTaskSize: Int = mAllTasks.size

    /**
     * 取消所有执行中的异步任务
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
    fun start() {
        if (Looper.getMainLooper().thread != Thread.currentThread()) {
            throw RuntimeException("must be called from UiThread")
        }
        if (isRunning) return
        mTaskExecuteMonitor.recordDispatchStartTime()
        isRunning = true
        //执行任务开始前的Head Task
        mDispatcherStateListener?.onStartBefore()
        if (mAllTasks.isNotEmpty()) {
            //开始调度任务
            dispatchAllTasks()
        }
        //记录主线程结束等待时间
        mTaskExecuteMonitor.dispatchMainThreadTimeCost()
    }

    /**
     * 获取已开启任务的集合
     * - 避免在调度任务进行时修改阻塞锁数量，导致多线程情况下不可预知情况
     * */
    private fun getEnableTaskList(): List<Task> {
        val enableTasks = mutableListOf<Task>()
        val needRemoveTaskTagSet = mutableSetOf<String>()
        for (task in mAllTasks) {
            //过滤未开启任务及其所有子任务
            if (needRemoveTaskTagSet.contains(task.tag) || !task.isEnable) {
                //记录依赖当前任务的子任务项
                mTaskDependsClazzMap[task.tag]
                    ?.takeUnless { it.isNullOrEmpty() }
                    ?.forEach { needRemoveTaskTagSet.add(it.tag) }
                if (task.isAsyncTaskNeedMainWaitOver()) {
                    //该任务需要主线程等待，等待数量-1
                    mNeedWaitTaskCount.getAndDecrement()
                }
                continue
            }
            enableTasks.add(task)
        }
        return enableTasks
    }

    /**
     * 调度所有待执行任务
     * */
    @MainThread
    private fun dispatchAllTasks() {
        val enableTaskList = getEnableTaskList()
        val mainThreadTasks = mutableListOf<TaskWrapper>()
        totalTaskSize = enableTaskList.size
        val needWaitCount = mNeedWaitTaskCount.get()
        //记录需要主线程等待的任务数量
        mTaskExecuteMonitor.recordWaitAsyncTaskCount(needWaitCount)
        if (needWaitCount > 0){
            mCountDownLatch = CountDownLatch(needWaitCount)
        }
        for (task in enableTaskList) {
            //设置任务执行回调监听
            task.setTaskStateListener(mTaskStatusListener)

            if (task.isOnlyMainProcess && !isMainProcess) {
                //只在主进程中调用的任务，且非主进程时，直接完成该任务
                markTaskOverDone(task)
                continue
            }

            val wrapper = TaskWrapper(task, mTaskExecuteMonitor, this)
            if (task.isRunOnMainThread) {
                //主线程任务
                mainThreadTasks.add(wrapper)
                continue
            }
            //调度异步任务
            val future = mExecutorService.submit(wrapper)
            mTaskFutures.add(future)
        }
        // 在调度异步执行后，再开始依次执行主线程任务，确保异步任务能够尽可能快速调度
        // 避免主线程等待前置任务阻塞，无法调度异步任务
        for (taskWrapper in mainThreadTasks) {
            taskWrapper.run()
        }
        //阻塞等待必须执行完毕的异步任务
        mTaskExecuteMonitor.recordMainThreadWaitTime {
            //存在需要等待的任务，主线程进入等待状态，等待所有前置任务执行完毕后才继续执行主线程
            mCountDownLatch?.await(mMaxWaitTime, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * 标记指定任务已完成
     * @param task 已完成的任务
     */
    override fun markTaskOverDone(task: Task) {
        notifyAllDependsWhenTaskDone(task.tag)
        if (task.isAsyncTaskNeedMainWaitOver()) {
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
    private fun notifyAllDependsWhenTaskDone(taskTag: String) {
        val tasks = mTaskDependsClazzMap[taskTag]
        if (tasks.isNullOrEmpty()) return
        for (task in tasks) {
            task.dependOverDone()
        }
    }

    /**
     * 记录任务完成
     * */
    private fun recordTaskFinish() {
        mFinishTaskCount.getAndIncrement()
        //所有任务已完成
        if (mFinishTaskCount.get() == totalTaskSize) {
            synchronized(this) {
                doOnAllTaskOver()
            }
        }
    }

    /**
     * 所有任务执行完成时执行
     * */
    private fun doOnAllTaskOver() {
        isRunning = false
        //记录与分发所有任务的执行性能
        mTaskExecuteMonitor.recordAllTaskFinishTimeCost()
        mTaskExecuteMonitor.dispatchExecuteRecordInfo()
        //在所有任务执行完成后的Tail Task
        mDispatcherStateListener?.also {
            mHandler.post { it.onFinish() }
        }
    }

}