package com.yupfeg.sampledispatcher

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.yupfeg.dispatcher.DelayTaskDispatcher
import com.yupfeg.dispatcher.ext.*
import com.yupfeg.logger.ext.loggd
import com.yupfeg.sampledispatcher.task.InitBDMapTask

class MainActivity : AppCompatActivity() {

    private val mDispatcher : DelayTaskDispatcher by lazy(LazyThreadSafetyMode.NONE) {
        createDelayDispatcher()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_test_delay_task).setOnClickListener {
            mDispatcher.start()
        }
    }

    private fun createDelayDispatcher() : DelayTaskDispatcher{
        return startUpDelay(this){
            addTask(InitBDMapTask())
            addTask("delay1") {
                loggd("延迟任务1 run")
            }
            addTask("delay2"){
                loggd("延迟任务2 run")
            }
            addTask("delay3") {
                loggd("延迟任务3 run")
            }

            setOnTaskStateListener{
                onStart = {tag, _ ->
                    loggd("$tag 任务开启")
                }
                onFinished = {info->
                    loggd("延迟任务结束 : $info")
                }
            }

            setOnMonitorRecordListener{recordInfo ->
                loggd("延迟任务所有任务执行完毕，分发延迟任务的执行记录信息：\n $recordInfo")
            }

            setOnDispatcherStateListener{
                onStartBefore = {
                    loggd("延迟任务开启执行")
                }

                onFinish = {
                    loggd("延迟任务调度器执行完毕")
                }
            }
        }
    }


}