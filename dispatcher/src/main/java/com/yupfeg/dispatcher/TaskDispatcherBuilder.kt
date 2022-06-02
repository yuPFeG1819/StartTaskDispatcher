package com.yupfeg.dispatcher

import android.content.Context
import androidx.annotation.MainThread
import com.yupfeg.dispatcher.monitor.OnMonitorRecordListener
import com.yupfeg.dispatcher.monitor.TaskExecuteMonitor
import com.yupfeg.dispatcher.tools.TaskSortTools
import com.yupfeg.dispatcher.task.OnTaskStateListener
import com.yupfeg.dispatcher.task.Task
import com.yupfeg.dispatcher.tools.AppProcessTools
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates

/**
 * 构建启动任务调度器Builder类
 * @author yuPFeG
 * @date 2022/01/05
 */
class TaskDispatcherBuilder(
    private val context: Context,
) {

    companion object {
        private var isFirstInit: Boolean = false

        /**
         * 是否处于主进程
         * */
        var isMainProcess = false
            private set
    }

    /**
     * 全部的启动任务集合
     */
    internal var allTasks: MutableList<Task> = mutableListOf()
        private set

    /**
     * 主线程需要等待完成任务的数量
     * */
    internal val needWaitTaskCount = AtomicInteger()

    /**
     * task任务类型与该任务的子任务集合依赖关系的Map
     * - 在对应任务完成后，通知后续子任务
     * */
    internal val taskDependsClazzMap = HashMap<String, MutableList<Task>>()

    /**
     * 单个任务执行状态回调监听
     * */
    internal var taskStatusListener: OnTaskStateListener? = null

    /**
     * 性能监控回调监听
     * */
    private var mOnMonitorRecordListener: OnMonitorRecordListener? = null

    /**
     * 调度器状态回调监听
     * */
    internal var onDispatcherStatusListener: OnDispatcherStateListener? = null

    /**
     * 任务执行性能监控
     * */
    internal var executeMonitor: TaskExecuteMonitor by Delegates.notNull()
        private set

    /**
     * 执行异步任务的线程池
     * */
    internal var executorService: ExecutorService? = null

    /**
     * 最新添加的任务
     * - 仅用于快捷添加该任务的依赖任务
     * */
    private var mCacheTask: Task? = null

    /**最大主线程等待超时时间(ms)*/
    internal var maxWaitTime : Long = 0

    init {
        if (!isFirstInit) {
            isMainProcess = AppProcessTools.isMainProcess(context)
            isFirstInit = true
        }
    }

    /**
     * 设置调度器监控记录监听
     * @param listener 性能监控记录回调
     * @return builder类型，便于链式调用
     * */
    @Suppress("unused")
    fun setOnMonitorRecordListener(listener: OnMonitorRecordListener): TaskDispatcherBuilder {
        this.mOnMonitorRecordListener = listener
        return this
    }

    /**
     * 设置调度器运行状态监听
     * @param listener 调度器状态回调
     * @return builder类型，便于链式调用
     * */
    @Suppress("unused")
    fun setOnDispatcherStateListener(
        listener: OnDispatcherStateListener
    ): TaskDispatcherBuilder {
        this.onDispatcherStatusListener = listener
        return this
    }

    /**
     * 设置启动任务的执行状态回调监听
     * @param listener 任务执行状态监听，每个任务执行完成后回调
     * @return builder类型，便于链式调用
     */
    @Suppress("unused")
    fun setOnTaskStateListener(listener: OnTaskStateListener): TaskDispatcherBuilder {
        this.taskStatusListener = listener
        return this
    }

    /**
     * 添加需要执行的任务
     * @param task 抽象的启动任务
     * @return builder类型，便于链式调用
     * */
    @Suppress("unused")
    @MainThread
    fun addTask(task: Task?): TaskDispatcherBuilder {
        task ?: return this
        mCacheTask = task
        task.setContext(context)

        allTasks.add(task)
        if (task.isAsyncTaskNeedMainWaitOver()) {
            //需要等待该任务完成后才能执行主线程，则主线程的同步阻塞锁数量+1
            needWaitTaskCount.getAndIncrement()
        }
        return this
    }

    /**
     * 给最近添加的任务添加依赖任务
     * - 添加给最近调用[addTask]添加的任务中的前置依赖任务
     * @param taskTag 依赖任务的类型，可变参数
     * @return builder类型，便于链式调用
     * */
    @Suppress("unused")
    @MainThread
    fun dependsOn(vararg taskTag: String): TaskDispatcherBuilder {
        mCacheTask ?: return this
        for (taskClazz in taskTag) {
            mCacheTask?.addDependsOnTaskTag(taskClazz)
        }
        return this
    }

    /**
     * 设置异步任务执行的线程池
     * - 推荐控制最大并发数，避免长时间占用时间片，导致主线程无法抢占CPU时间片
     * @param executorService 线程池
     * @return builder类型，便于链式调用
     */
    @Suppress("unused")
    @MainThread
    fun setExecutorService(executorService: ExecutorService): TaskDispatcherBuilder {
        this.executorService = executorService
        return this
    }

    /**
     * 设置最大主线程超时时间
     * @param maxTime 最大时间（ms）
     * */
    @Suppress("unused")
    fun setMaxWaitTimeout(maxTime : Long) : TaskDispatcherBuilder{
        maxWaitTime = maxTime
        return this
    }

    /**
     * 构建启动任务调度器
     * @return 任务调度器实例
     */
    @Suppress("unused")
    @Throws(NullPointerException::class)
    @MainThread
    fun build(): TaskDispatcher {
        executorService ?: throw NullPointerException("you should set async task executor")

        executeMonitor = TaskExecuteMonitor(mOnMonitorRecordListener)
        if (allTasks.isNotEmpty()) {
            //进行拓扑排序
            executeMonitor.recordSortTaskListTime {
                allTasks = TaskSortTools.getSortedList(allTasks, taskDependsClazzMap)
            }
            printDependsTaskInfo()
        }
        mCacheTask = null
        return TaskDispatcher(this)
    }

    /**
     * 输出被依赖的任务信息
     */
    private fun printDependsTaskInfo() {
        if (mOnMonitorRecordListener?.isPrintSortedList == false) return
        if (allTasks.isEmpty() || taskDependsClazzMap.isEmpty()) return
        val stringBuilder = StringBuilder().apply {
            append("task sort list : \n")
            for (task in allTasks) {
                append("task tag : ${task.tag} ,\n")
            }
            append("all dependsOn relation : \n")
            for (taskTag in taskDependsClazzMap.keys) {
                val tasks = taskDependsClazzMap[taskTag]
                append(
                    "the $taskTag task is preTask of ${tasks?.size ?: 0} count task \n"
                )
                if (tasks.isNullOrEmpty()) continue
                for (task in tasks) {
                    append("depend from task :  ${task.tag} \n")
                }
            }
            this.deleteAt(this.lastIndex)
        }
        mOnMonitorRecordListener?.onTaskSorted(stringBuilder.toString())
    }

}