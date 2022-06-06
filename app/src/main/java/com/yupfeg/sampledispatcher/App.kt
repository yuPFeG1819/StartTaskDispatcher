package com.yupfeg.sampledispatcher

import android.app.Application
import android.os.SystemClock
import android.util.Log
import com.yupfeg.dispatcher.OnDispatcherStateListener
import com.yupfeg.dispatcher.TaskDispatcher
import com.yupfeg.dispatcher.TaskDispatcherBuilder
import com.yupfeg.dispatcher.ext.*
import com.yupfeg.dispatcher.monitor.ExecuteRecordInfo
import com.yupfeg.dispatcher.monitor.OnMonitorRecordListener
import com.yupfeg.dispatcher.task.MainTask
import com.yupfeg.dispatcher.task.OnTaskStatusListener
import com.yupfeg.dispatcher.task.TaskRunningInfo
import com.yupfeg.executor.ExecutorProvider
import com.yupfeg.executor.ext.buildDefCPUThreadPoolFactory
import com.yupfeg.executor.ext.ioThreadPoolBuilder
import com.yupfeg.executor.ext.prepareExecutor
import com.yupfeg.logger.Logger
import com.yupfeg.logger.ext.loggd
import com.yupfeg.logger.ext.loggi
import com.yupfeg.logger.printer.LogcatPrinter
import com.yupfeg.sampledispatcher.task.*

@Suppress("unused")
class App : Application() {

    override fun onCreate() {
        super.onCreate()
//        normalInit()
        dslTaskDispatcher().start()
    }

    /**
     * 传统串行初始化
     * */
    @Suppress("unused")
    private fun normalInit() {
        initExecutor()
        initLogger()
        val startTime = SystemClock.elapsedRealtimeNanos()
        ActivityLifecycleTask(this).run()
        UncaughtCrashTask().run()
        InitToastTask(this).run()
        InitARouterTask().run()
        InitSharedTask().run()
        InitUMTask().run()
        InitJPushTask().run()
        InitBDMapTask().run()
        InitBuglyTask().run()
        DelayMainInitTask().run()
        AsyncInitTask().run()
        mainTask("initWebView") { loggd("run init WebView") }
        loggi(
            "正常串行初始化耗时 ：${
                (SystemClock.elapsedRealtimeNanos() - startTime) / 1000000f
            } ms"
        )
    }

    /**
     * 使用Kotlin-DSL方式构建任务调度器
     * */
    private fun dslTaskDispatcher(): TaskDispatcher {
        initExecutor()
        initLogger()
        val anchorTaskTag = "anchorTagTask"
        return startUp(this) {
            //设置调度器线程池
            setExecutorService(ExecutorProvider.cpuExecutor)
            //设置最大超时时间（ms）
            setMaxWaitTimeout(10 * 1000)
            //锚点任务
            addAnchorTask(anchorTaskTag) {
                add(ActivityLifecycleTask.TAG)
                add(UncaughtCrashTask.TAG)
                add(InitToastTask.TAG)
            }
            //极光推送
            addTask(InitJPushTask()) {
                add(anchorTaskTag)
            }
            //组件路由
            addTask(InitARouterTask()) {
                add(anchorTaskTag)
            }
            //分享组件
            addTask(InitSharedTask()) {
                add(anchorTaskTag)
            }
            //toast组件
            addTask(InitToastTask(this@App)) {
                add(UncaughtCrashTask.TAG)
            }
            //注册生命周期
            addTask(ActivityLifecycleTask(this@App))
            //注册未捕获异常
            addTask(UncaughtCrashTask())
            //友盟组件
            addTask(InitUMTask()) { add(anchorTaskTag) }
            //百度地图
            addTask(InitBDMapTask()) { add(anchorTaskTag) }
            //bugly
            addTask(InitBuglyTask()) { add(UncaughtCrashTask.TAG) }
            addTask(DelayMainInitTask()).dependsOn(InitBDMapTask.TAG)
            addTask(AsyncInitTask()).dependsOn(InitUMTask.TAG)
            //测试简单任务
            addTask(mainTask("initWebView") { loggd("run init WebView") }).dependsOn(InitBuglyTask.TAG)

            setOnDispatcherStateListener {
                onStartBefore = {
                    //Head Task
                    loggd("启动任务调度器开始执行调度 Head Task")
                }

                onFinish = {
                    //Tail Task
                    loggd("启动任务调度器执行完成 Tail Task")
                }
            }

            setOnMonitorRecordListener {
                isDebugTaskSort = false

                onTaskSorted = { tasksInfo ->
                    loggi("任务排序结果 :\n$tasksInfo")
                }

                onMainThreadOverRecord = { costTime ->
                    loggi("任务调度器主线程总计耗时 : $costTime ms")
                }

                onAllTaskRecordResult = { timeInfo ->
                    loggi("任务调度器的所有任务性能监控记录 : \n $timeInfo")
                }
            }

            //任务执行状态监听
            setOnTaskStateListener {
                onWait = { tag ->
                    loggi("$tag 任务开始等待")
                }
                onStart = { tag, waitTime ->
                    loggi("$tag 任务开始执行 , 已等待前置任务 $waitTime ms")
                }
                onFinished = { runningInfo ->
                    loggi("任务已执行完成 : $runningInfo")
                }
            }
        }
    }

    /**
     * 传统Java方式构建启动任务调度器
     * @return
     */
    @Suppress("unused")
    fun normalInitDispatcher(): TaskDispatcher {
        initExecutor()
        initLogger()

        val dispatcherBuilder = TaskDispatcherBuilder(this)
            //设置调度线程池
            .setExecutorService(ExecutorProvider.cpuExecutor)
            //极光推送
            .addTask(InitJPushTask())
            .dependsOn(ActivityLifecycleTask.TAG, UncaughtCrashTask.TAG)
            //组件路由
            .addTask(InitARouterTask())
            .dependsOn(ActivityLifecycleTask.TAG, InitToastTask.TAG, UncaughtCrashTask.TAG)
            //分享组件
            .addTask(InitSharedTask()).dependsOn(ActivityLifecycleTask.TAG, InitToastTask.TAG)
            //toast组件
            .addTask(InitToastTask(this)).dependsOn(UncaughtCrashTask.TAG)
            //注册Activity生命周期
            .addTask(ActivityLifecycleTask(this))
            //友盟
            .addTask(InitUMTask()).dependsOn(UncaughtCrashTask.TAG)
            //注册未捕获异常
            .addTask(UncaughtCrashTask())
            //百度地图
            .addTask(InitBDMapTask()).dependsOn(UncaughtCrashTask.TAG)
            //bugly
            .addTask(InitBuglyTask()).dependsOn(UncaughtCrashTask.TAG)
            .addTask(DelayMainInitTask()).dependsOn(InitBDMapTask.TAG)
            .addTask(AsyncInitTask()).dependsOn(InitUMTask.TAG)
            .addTask(object : MainTask() {
                override val tag: String
                    get() = "initWebView"

                override fun run() {
                    loggd("run init WebView")
                }
            })

        dispatcherBuilder.setOnMonitorRecordListener(object : OnMonitorRecordListener {
            override val isPrintSortedList: Boolean
                get() = true

            override fun onTaskSorted(tasksInfo: String) {
                Log.i("logger", "启动任务排序结果 :\n$tasksInfo")
            }

            override fun onMainThreadCostRecord(costTime: Float) {
                loggi("调度器主线程耗时：${costTime}")
            }


            override fun onAllTaskRecordResult(timeInfo: ExecuteRecordInfo) {
                loggi("启动任务调度器性能监控记录 : $timeInfo")
            }
        })
            .setOnDispatcherStateListener(object : OnDispatcherStateListener {
                override fun onStartBefore() {
                    //Head Task
                    loggd("启动任务调度器开始执行调度")
                }

                override fun onFinish() {
                    loggd("启动任务调度器执行完成")
                }
            })
            .setOnTaskStateListener(object : OnTaskStatusListener {
                override fun onTaskWait(tag: String) {
                    loggi("$tag 任务开始等待")
                }

                override fun onTaskStart(tag: String, waitTime: Float) {
                    loggi("$tag 任务开始执行 , 已等待前置任务 $waitTime ms")
                }

                override fun onTaskFinish(runningInfo: TaskRunningInfo) {
                    loggi("任务已执行完成 : $runningInfo")
                }

            })

        return dispatcherBuilder.build()
    }

    private fun initExecutor() {
        prepareExecutor {
            //创建默认cpu线程池的工厂类
            buildDefCPUThreadPoolFactory {
                //配置io密集型线程池
                ioThreadPoolBuilder {
                    //限制最大并发数
                    maxPoolSize = 30
                    keepAliveTime = 10
                }
            }
        }
    }

    private fun initLogger() {
        //开启调用位置追踪
        Logger.prepare {
            //开启调用位置追踪
            isDisplayClassInfo = true
            logPrinters = listOf(LogcatPrinter(enable = true))
        }
    }
}