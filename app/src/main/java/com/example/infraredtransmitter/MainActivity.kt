package com.example.infraredtransmitter

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Switch
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

        hexPattern2IntArray("1234AB")
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

    /*
     * 输入作为解码结果的6位16进制码，输出对应的NEC int数组编码，用于ConsumerIrManager的transmit方法
     * 如果输入不合法，返回空串
     */
    fun hexPattern2IntArray(hexPattern: String):IntArray{
        if(hexPattern.length != 6){
            return intArrayOf()
        }
        hexPattern.forEach {
            if(!isHexChar(it)){
                return intArrayOf()
            }
        }
        var addr = hexPattern.substring(0, 4)
        var data = hexPattern.substring(4, 6)


        val addrEncode = reverseEncode(addr, 4)
        val dataEncode = reverseEncode(data, 2)

        val dataComplementSb = StringBuilder()
        val binaryDataEncode = addZeroPrefix(dataEncode.toInt(16).toString(2), 8)
        binaryDataEncode.forEach {
            when(it){
                '0' -> dataComplementSb.append('1')
                '1' -> dataComplementSb.append('0')
            }
        }
        val dataEncodeComplement = addZeroPrefix(dataComplementSb.toString().toInt(2).toString(16), 2)

        val hexPatternEncode = (addrEncode + dataEncode + dataEncodeComplement).uppercase(Locale.getDefault())
        val binaryPatternEncode = addZeroPrefix(hexPatternEncode.toLong(16).toString(2), 32)

        val res = arrayListOf(9000, 4500)
        res.apply {
            binaryPatternEncode.forEach {
                when(it){
                    '0' -> {
                        add(560)
                        add(560)
                    }
                    '1' -> {
                        add(560)
                        add(1680)
                    }
                }
            }
            add(560)
            add(20000)
        }
        println("The hex encoding is $hexPatternEncode !")
        return res.toIntArray()
    }

    fun reverseEncode(addr: String, hexLen: Int): String {
        var tmp = addr.toInt(16).toString(2)
        while (tmp.length < hexLen*4) {
            tmp = "0$tmp"
        }
        var res = tmp.reversed().toInt(2).toString(16)
        while (res.length < hexLen) {
            res = "0$res"
        }
        return res
    }

    fun isHexChar(c: Char):Boolean{
        return c in '0'..'9' || c in 'A'..'F'
    }

    fun addZeroPrefix(s: String, len: Int): String{
        val sb = StringBuilder(s)
        while(sb.length < len){
            sb.insert(0,'0')
        }
        return sb.toString()
    }

}