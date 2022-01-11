package com.yupfeg.dispatcher.task

import android.content.Context
import android.os.Process
import com.yupfeg.dispatcher.ExecutorProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * 抽象启动任务的默认抽象实现类
 * - 注意所有任务子类需要混淆保护，否则会导致排序异常
 * - 子类需要注意内存泄漏情况，避免传入短周期对象
 * @author yuPFeG
 * @date 2022/01/04
 */
abstract class Task : ITask, OnTaskStateListener {

    companion object{
        /**最大等待前置任务时间(s)*/
        internal const val MAX_AWAIT_TIME = 10
    }

    /**
     * 任务的唯一标识
     * - 默认为类型名称，注意不要与其他任务重名，导致无法进行拓扑排序
     * - 如果使用默认名称，需要避免混淆，从而导致出现重名情况
     * */
    abstract val tag : String

    /**
     * 是否需要尽快执行，解决特殊场景的问题：一个Task耗时非常多但是优先级却一般，很有可能开始的时间较晚，
     * 导致最后只是在等它，这种可以早开始。
     * */
    open val isNeedRunAsSoon : Boolean = false

    /**
     * 当前任务依赖的前置任务数量（需要等待被依赖的Task执行完毕才能执行自己），默认没有依赖
     * */
    @Volatile
    private var mDependsLatch : CountDownLatch?= null

    /**
     * 提供给子类使用的上下文
     * - 注意如果直接创建任务并执行run函数时，该上下文为null，必须在任务调度器内使用才会赋值
     * */
    @Suppress("unused")
    protected val context : Context
        get() = mContext ?: throw NullPointerException("you need add this task to Dispatcher")

    /**
     * 通常为applicationContext
     * */
    private var mContext : Context?= null

    /**
     * 是否监控任务执行
     * */
    internal val isMonitorTaskOver : Boolean
        get() = mStatusListener != null

    /**
     * 任务执行状态回调
     * */
    private var mStatusListener : OnTaskStateListener? = null

    // <editor-fold desc="抽象任务接口实现">

    override val isOnlyMainProcess: Boolean
        get() = true

    override val isRunOnMainThread: Boolean
        get() = false

    override val isNeedWaitTaskOver: Boolean
        get() = false

    /**
     * 任务的依赖关系集合，对外为只读状态
     * - 需要先执行的前置任务集合
     * */
    internal val taskDependsOn : List<String>
        get() = mTaskDependsOnList
    /**
     * 任务的依赖关系集合，缓存所有前置任务的唯一标识
     * */
    private val mTaskDependsOnList : MutableList<String> = mutableListOf()


    /**
     * 异步任务调度执行的线程池
     * - 默认为CPU密集型线程池
     * 控制最大并发数，避免长时间占用线程，导致主线程无法抢占CPU时间片
     * */
    override val dispatchOn: ExecutorService
        get() = ExecutorProvider.cpuExecutor

    /**
     * Task的优先级，运行在主线程则不需要去改优先级，确保主线程优先抢占CPU
     */
    override fun taskPriority(): Int {
        return Process.THREAD_PRIORITY_BACKGROUND
    }

    // </editor-fold>

    // <editor-fold desc="任务调度状态监听">

    override fun onTaskWait(tag: String) {
        mStatusListener?.onTaskWait(tag)
    }

    override fun onTaskStart(tag: String,waitTime: Long) {
        mStatusListener?.onTaskStart(tag,waitTime)
    }

    override fun onTaskFinish(runningInfo: TaskRunningInfo) {
        mStatusListener?.onTaskFinish(runningInfo)
    }

    // </editor-fold>

    // <editor-fold desc="前置依赖锁">

    /**
     * 等待所有前置依赖任务先执行完成，然后再执行当前任务
     * */
    @Throws(InterruptedException::class)
    internal fun awaitDependsTask(){
        val latch = tryGetCountDownLatch()
        latch.await(MAX_AWAIT_TIME.toLong(),TimeUnit.SECONDS)
    }

    /**
     * 前置任务完成一个，减少同步锁的数量
     * - 需要注意多线程并发安全问题
     * */
    @Suppress("unused")
    internal fun dependOverDone(){
        val latch = tryGetCountDownLatch()
        latch.countDown()
    }

    /**
     * 尝试获取当前任务的阻塞同步锁
     * */
    private fun tryGetCountDownLatch() : CountDownLatch{
        return mDependsLatch?: synchronized(this) {
            mDependsLatch ?: run {
                CountDownLatch(
                    if (mTaskDependsOnList.isNullOrEmpty()) 0 else mTaskDependsOnList.size
                ).apply { mDependsLatch = this }
            }
        }
    }

    // </editor-fold>

    /**
     * 内部使用的赋值Context
     * - 在添加该任务时赋值上下文，通常为ApplicationContext
     * @param context
     * */
    internal fun setContext(context: Context){
        this.mContext = context.applicationContext
    }

    /**
     * 校验当前任务是否需要主线程等待其完成
     * */
    internal fun isNeedMainWaitOver() : Boolean{
        return !isRunOnMainThread && isNeedWaitTaskOver
    }

    /**
     * 添加该任务依赖的前置任务类型
     * @param tag 任务的唯一标识
     */
    internal fun addDependsOnTaskTag(tag : String){
        //过滤重复任务，避免依赖成环
        if (mTaskDependsOnList.contains(tag)) return
        mTaskDependsOnList.add(tag)
    }

    /**
     * 添加该任务依赖的前置任务集合
     * @param list 任务标识集合
     * */
    internal fun addDependsOnList(list : List<String>){
        for (tag in list) {
            addDependsOnTaskTag(tag)
        }
    }

    /**
     * 设置执行状态监听
     * */
    fun setStatusListener(listener: OnTaskStateListener?){
        mStatusListener = listener
    }

}