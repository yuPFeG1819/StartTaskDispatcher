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
import com.yupfeg.dispatcher.task.OnTaskStateListener
import com.yupfeg.dispatcher.task.TaskRunningInfo
import com.yupfeg.logger.ext.logd
import com.yupfeg.logger.ext.logi
import com.yupfeg.logger.ext.setDslLoggerConfig
import com.yupfeg.logger.printer.LogcatPrinter
import com.yupfeg.sampledispatcher.task.*

class App : Application(){

    companion object{
        val instance : Application
            get() = mInstance

        private lateinit var mInstance : Application
    }

    override fun onCreate() {
        super.onCreate()
        mInstance = this
//        normalInit()
        dslTaskDispatcher().start()
    }

    /**
     * 传统串行初始化
     * */
    private fun normalInit(){
        initLogger()
        val startTime = SystemClock.elapsedRealtime()
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
        mainTask("initWebView"){ logd("run init WebView") }
        logi("正常串行初始化耗时 ：${(SystemClock.elapsedRealtime() - startTime)} ms") //大约1160ms
    }

    /**
     * 使用Kotlin-DSL方式构建任务调度器
     * */
    private fun dslTaskDispatcher() : TaskDispatcher {
        val anchorTaskTag = "anchorTagTask"
        return startUp(this){
            //锚点任务
            addAnchorTask(anchorTaskTag){
                add(ActivityLifecycleTask.TAG)
                add(UncaughtCrashTask.TAG)
                add(InitToastTask.TAG)
            }
            //极光推送
            addTask(InitJPushTask()){
                add(anchorTaskTag)
            }
            //组件路由
            addTask(InitARouterTask()){
                add(anchorTaskTag)
            }
            //分享组件
            addTask(InitSharedTask()){
                add(anchorTaskTag)
            }
            //toast组件
            addTask(InitToastTask(this@App)){
                add(UncaughtCrashTask.TAG)
            }
            //注册生命周期
            addTask(ActivityLifecycleTask(this@App))
            //注册未捕获异常
            addTask(UncaughtCrashTask())
            //友盟组件
            addTask(InitUMTask())
            //百度地图
            addTask(InitBDMapTask()){
                add(anchorTaskTag)
            }
            //bugly
            addTask(InitBuglyTask()){ add(UncaughtCrashTask.TAG) }
            addTask(DelayMainInitTask()).dependsOn(anchorTaskTag)
            addTask(AsyncInitTask())
            //测试简单任务
            addTask(mainTask("initWebView"){ logd("run init WebView") })

            setOnDispatcherStateListener {
                isDebug = true

                onTaskSorted = {dependsInfo->
                    Log.i("Logger","启动任务的依赖关系 :\n$dependsInfo")
                }

                onStartBefore = {
                    //Head Task
                    initLogger()
                    logd("启动任务调度器开始执行调度")
                }

                onFinish = {
                    //Tail Task
                    logd("启动任务调度器执行完成")
                }
            }

            setOnMonitorRecordListener {recordInfo->
                logi("启动任务调度器性能监控记录 : \n$recordInfo")
            }

            //任务执行状态监听
            setOnTaskStateListener {
                onWait = {tag ->
                    logi("$tag 任务开始等待")
                }

                onStart = {tag,waitTime->
                    logi("$tag 任务开始执行 , 已等待前置任务 $waitTime ms")
                }
                onFinished = {runningInfo->
                    logi("任务已执行完成 : $runningInfo")
                }
            }
        }
    }

    /**
     * 传统方式构建启动任务调度器
     * @return
     */
    fun normalInitDispatcher() : TaskDispatcher{
        val dispatcherBuilder = TaskDispatcherBuilder(this)
            //极光推送
            .addTask(InitJPushTask())
            .dependsOn(ActivityLifecycleTask.TAG, UncaughtCrashTask.TAG)
            //组件路由
            .addTask(InitARouterTask())
            .dependsOn(ActivityLifecycleTask.TAG, InitToastTask.TAG,UncaughtCrashTask.TAG)
            //分享组件
            .addTask(InitSharedTask()).dependsOn(ActivityLifecycleTask.TAG,InitToastTask.TAG)
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
            .addTask(DelayMainInitTask())
            .addTask(AsyncInitTask())
            .addTask(object : MainTask(){
                override val tag: String
                    get() = "initWebView"

                override fun run() {
                    logd("run init WebView")
                }
            })

        dispatcherBuilder.setOnMonitorRecordListener(object : OnMonitorRecordListener {
            override fun onMonitorRecordResult(timeInfo: ExecuteRecordInfo) {
                logi("启动任务调度器性能监控记录 : $timeInfo")
            }
        })
            .setOnDispatcherStateListener(object : OnDispatcherStateListener {

                override val isPrintDependsMap: Boolean
                    get() = true

                override fun onTaskSorted(dependsInfo: String) {
                    Log.i("logger","启动任务的依赖关系 :\n$dependsInfo")
                }

                override fun onStartBefore() {
                    //Head Task
                    initLogger()
                    logd("启动任务调度器开始执行调度")
                }

                override fun onFinish() {
                    logd("启动任务调度器执行完成")
                }
            })
            .setOnTaskStateListener(object : OnTaskStateListener {
                override fun onTaskWait(tag: String) {
                    logi("$tag 任务开始等待")
                }

                override fun onTaskStart(tag: String, waitTime: Long){
                    logi("$tag 任务开始执行 , 已等待前置任务 $waitTime ms")
                }

                override fun onTaskFinish(runningInfo: TaskRunningInfo) {
                    logi("任务已执行完成 : $runningInfo")
                }

            })

        return dispatcherBuilder.build()
    }

    private fun initLogger(){
        setDslLoggerConfig {
            //开启调用位置追踪
            isDisplayClassInfo = true
            //添加线上捕获异常的日志输出
            logPrinters = listOf(LogcatPrinter(enable = true))
        }
    }
}