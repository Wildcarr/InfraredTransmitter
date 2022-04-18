package com.example.infraredtransmitter

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

lateinit var mFreqsText: TextView
lateinit var mCIR: ConsumerIrManager
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mCIR = getSystemService(Context.CONSUMER_IR_SERVICE) as ConsumerIrManager
        mFreqsText = findViewById(R.id.textView)
        initViewsAndEvents()
    }

    fun initViewsAndEvents(){
        findViewById<Button>(R.id.send_button).setOnClickListener(mSendClickListener)
        findViewById<Button>(R.id.get_freqs_button).setOnClickListener(mOnClickListener)
    }

    var mSendClickListener = View.OnClickListener {
        if (!mCIR.hasIrEmitter()) {
            Log.e("logtag", "未找到红外发射器！")
            return@OnClickListener
        }
        // 一种交替的载波序列模式，通过毫秒测量
//        val pattern = intArrayOf(
//            1901, 4453, 625, 1614, 625, 1588, 625, 1614, 625,
//            442, 625, 442, 625, 468, 625, 442, 625, 494, 572, 1614,
//            625, 1588, 625, 1614, 625, 494, 572, 442, 651, 442, 625,
//            442, 625, 442, 625, 1614, 625, 1588, 651, 1588, 625, 442,
//            625, 494, 598, 442, 625, 442, 625, 520, 572, 442, 625, 442,
//            625, 442, 651, 1588, 625, 1614, 625, 1588, 625, 1614, 625,
//            1588, 625, 48958
//        )

        val pattern = intArrayOf(9000,4500, // 开头两个数字表示引导码
            // 下面两行表示用户码 0x4055
            560,560, 560,1680, 560,560, 560,560, 560,560, 560,560, 560,560, 560,560,
            560,560, 560,1680, 560,560, 560,1680, 560,560, 560,1680, 560,560, 560,1680,
            // 下面一行表示数据码 0x44
            560,560, 560,1680, 560,560, 560,560, 560,560, 560,1680, 560,560, 560,560,
            // 下面一行表示数据反码
            560,1680, 560,560, 560,1680, 560,1680, 560,1680, 560,560, 560,1680, 560,1680,
            560,20000) // 末尾两个数字表示结束码
        // 在38.4KHz条件下进行模式转换
        mCIR.transmit(38400, pattern)
    }

    var mOnClickListener = View.OnClickListener {
        val b = StringBuilder()
        if (!mCIR.hasIrEmitter()) {
            mFreqsText.text = "未找到红外发射器！"
            return@OnClickListener
        }
        // 获得可用的载波频率范围
        val freqs = mCIR.carrierFrequencies
        b.append("IR Carrier Frequencies:\n") // 红外载波频率
        // 边里获取频率段
        for (range in freqs) {
            b.append(
                String.format(
                    "  %d - %d\n",
                    range.minFrequency, range.maxFrequency
                )
            )
        }
        mFreqsText.text = b.toString() // 显示结果
    }

}