package com.yupfeg.dispatcher

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

/**
 * 异步启动任务所执行的线程池提供类
 * @author yuPFeG
 * @date 2022/01/04
 */
@Suppress("unused")
object ExecutorProvider {

    /**
     * IO密集型线程池
     * - 通常用于占用时间短的任务，实际并发量很小的任务
     * */
    @JvmStatic
    val ioExecutor : ThreadPoolExecutor

    /**
     * CPU密集型线程池
     * - 用于线程占用时间长的任务
     * - 占据CPU的时间片过多的话会影响性能，需要控制最大并发，防止主线程的抢占的时间片减少
     * */
    @JvmStatic
    val cpuExecutor : ThreadPoolExecutor

    /**CPU 核数*/
    private val CPU_COUNT = Runtime.getRuntime().availableProcessors()
    /**
     * CPU线程池的核心线程数 (2 ~ 5)
     * */
    private val CORE_POOL_SIZE = max(2, min(CPU_COUNT - 1, 5))
    /**CPU线程池线程数的最大值*/
    private val CPU_MAXIMUM_POOL_SIZE = CORE_POOL_SIZE
    /**
     * CPU线程池的核心线程存活时间（s）
     * - 线程数量大于corePoolSize（核心线程数量）或
     * 设置了allowCoreThreadTimeOut（是否允许空闲核心线程超时）时，
     * 线程会根据keepAliveTime的值进行活性检查，一旦超时便销毁线程。
     * 否则，线程会永远等待新的工作。
     * */
    private const val CPU_KEEP_ALIVE_SECONDS = 10

    /**
     * 自定义拒绝执行处理器
     * - 由于CPU线程池缓冲队列是Int.MAX_VALUE长度，理论上来说不会执行
     */
    private val mRejectedHandler : RejectedExecutionHandler

    //初始化线程池
    init {
        ioExecutor = createIoExecutor()
        mRejectedHandler = RejectedExecutionHandler { r, _ ->
            Executors.newCachedThreadPool().execute(r) //如果cpu线程池满后，交由新开线程池执行，通常不会执行
        }
        cpuExecutor = createCpuExecutor()
    }

    /**
     * 创建IO密集型的线程池
     * - 无核心线程的无界无队列线程池，没有缓冲队列，有任务就会直接给线程执行
     * */
    private fun createIoExecutor() : ThreadPoolExecutor{
        return ThreadPoolExecutor(
            0, Int.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            SynchronousQueue(), DefaultThreadFactory()
        )
    }

    /**
     * 创建CPU密集型的线程池
     * - 只有固定核心线程的有界有队列线程池，无界缓冲队列（Integer.MAX_VALUE），会在无空闲线程时缓存任务
     * */
    private fun createCpuExecutor() : ThreadPoolExecutor{
        return ThreadPoolExecutor(
            CORE_POOL_SIZE, CPU_MAXIMUM_POOL_SIZE,
            CPU_KEEP_ALIVE_SECONDS.toLong(),TimeUnit.SECONDS,
            LinkedBlockingQueue(), DefaultThreadFactory(), mRejectedHandler
        ).apply {
            //允许核心线程超时销毁
            allowCoreThreadTimeOut(true)
        }
    }

}

private class DefaultThreadFactory : ThreadFactory{
    companion object{
        /**线程池的数量*/
        private val poolNumber = AtomicInteger(0)
    }

    private val mThreadGroup : ThreadGroup
    private val threadNumber = AtomicInteger(1)
    private val mThreadNamePrefix : String

    init {
        val s = System.getSecurityManager()
        mThreadGroup = s?.threadGroup ?: Thread.currentThread().threadGroup!!
        mThreadNamePrefix = "pool-${getThreadPoolName()}-Thread"
    }

    private fun getThreadPoolName() : String{
        poolNumber.getAndIncrement()
        return when(poolNumber.get()){
            1  -> "io"
            2  -> "cpu"
            else -> "other"
        }
    }

    override fun newThread(r: Runnable?): Thread {
        val t = Thread(
            mThreadGroup, r,
            "$mThreadNamePrefix-${threadNumber.getAndIncrement()}",
            0
        )
        //转化为非守护线程
        if (t.isDaemon) {
            t.isDaemon = false
        }

        //线程优先级调整为默认，避免抢占主线程CPU时间片
        if (t.priority != Thread.NORM_PRIORITY){
            t.priority = Thread.NORM_PRIORITY
        }

        return t
    }

}