package com.yupfeg.sampledispatcher

import android.os.Bundle
import android.os.Looper
import android.os.MessageQueue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.yupfeg.dispatcher.DelayTaskDispatcher
import com.yupfeg.dispatcher.ext.*
import com.yupfeg.logger.ext.loggd
import com.yupfeg.logger.ext.loggi
import java.util.*

class MainActivity : AppCompatActivity() {

    private val mDispatcher : DelayTaskDispatcher by lazy(LazyThreadSafetyMode.NONE) {
        createDelayDispatcher()
    }

    private var addTaskNum : Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        Looper.myLooper()?.setMessageLogging {
//            loggi("looper的消息取出内容：${it}")
//        }
        findViewById<View>(R.id.btn_add_new_task).setOnClickListener {
            addTaskNum++
            mDispatcher.addTask(this,mainTask("delay_new_${addTaskNum}"){
                loggd("新延迟任务${addTaskNum} run")
            })
        }
        findViewById<View>(R.id.btn_test_delay_task).setOnClickListener {
            mDispatcher.start()
        }

        findViewById<View>(R.id.btn_test_delay_task_normal).setOnClickListener {
            Looper.myQueue().addIdleHandler(createNormalIdleHandlerMsgList())
        }
    }

    private fun createNormalIdleHandlerMsgList() : MessageQueue.IdleHandler{
        val taskQueue = ArrayDeque<Runnable>()
        for (i in 0..10){
            taskQueue.offer(Runnable {
                loggd("normal idle handle run task $i")
            })
        }
        return MessageQueue.IdleHandler {
            var newTask: Runnable? = null
            while (taskQueue.isNotEmpty()) {
                val task = taskQueue.poll()
                task ?: continue
                newTask = task
                break
            }
            loggi("logger", "延迟任务：idleHandler - ，剩余任务数量：${taskQueue.size}")
            newTask?.run()
            return@IdleHandler !taskQueue.isEmpty()
        }
    }

    private fun createDelayDispatcher() : DelayTaskDispatcher{
        return startUpDelay(this){
            for (i in 0..10){
                addTask("delay${i}") {
                    loggd("延迟任务${i} run")
                }
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