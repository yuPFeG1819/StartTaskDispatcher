package com.yupfeg.sampledispatcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yupfeg.dispatcher.ext.addTask
import com.yupfeg.dispatcher.ext.buildDelayStartUp
import com.yupfeg.dispatcher.ext.setOnTaskStateListener
import com.yupfeg.logger.ext.logd

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buildDelayStartUp(this){
            addTask {
                logd("延迟任务1 run")
            }
            addTask{
                logd("延迟任务2 run")
            }
            addTask {
                logd("延迟任务3 run")
            }

            setOnTaskStateListener{
                onStart = {tag, _ ->
                    logd("$tag 任务开启")
                }
                onFinished = {info->
                    logd("延迟任务结束 : $info")
                }
            }
        }.start()
    }

}