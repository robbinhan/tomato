package com.example.robbin.tomato

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.Period
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong


class MainActivity : AppCompatActivity() {

    private var buttonState = 0
    private var button2State = 0
    private var textViewText: String? = null
    private val workTimeSeconds = 1500L// 25min
    private val resetTimeSeconds = 300L// 5min
    private var startText: String? = null
    private var stopText: String? = null
    private var pauseText: String? = null
    private var resumeText: String? = null

    private lateinit var myFormat: PeriodFormatter

    var stopped = AtomicBoolean()
    var resumed = AtomicBoolean()

    var tickCount = AtomicLong()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()

        myFormat = PeriodFormatterBuilder()
                .printZeroAlways().minimumPrintedDigits(2).appendMinutes()
                .appendSeparator(":")
                .printZeroAlways().minimumPrintedDigits(2).appendSeconds()
                .toFormatter()

        workTimerButtonListener()

        // 暂停、继续
        button2.setOnClickListener { it ->
            if (button2State == 0) {
                button2State = 1
                button2.text = resumeText
                pauseTimer()
            } else if (button2State == 1) {
                button2State = 0
                button2.text = pauseText
                resumeTimer()

                val resumeWorkTimeSeconds = workTimeSeconds - tickCount.get()

                Log.d("button2_tickCount", tickCount.get().toString())
                Log.d("button2", "resume")
                Log.d("button2_tickCount", resumeWorkTimeSeconds.toString())

                Observable.intervalRange(1, resumeWorkTimeSeconds, 0, 1, TimeUnit.SECONDS)
                        .takeWhile { _ ->
                            !stopped.get()
                        }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { i ->
                                    tickCount.addAndGet(1)
                                    val seconds = resumeWorkTimeSeconds - i
                                    val period = Period.seconds(seconds.toInt()).normalizedStandard()

                                    textViewText = myFormat.print(period)
                                    textView.text = textViewText
                                },
                                { error ->
                                    Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
                                },
                                {
                                    // complete
                                    // not click stop button(倒计时完成，正常结束)
                                    if (!stopped.get()) {
                                        // 直接启动休息计时
                                        buttonState = 0

                                        //进入休息时间
                                        textView.text = getString(R.string.reset_time_clock)
                                        // 没有暂停按钮
                                        button2.visibility = INVISIBLE
                                        resetTimerButtonListener()
                                        resetTimer()
                                    }
                                    // 暂停
                                    if (!resumed.get()) {

                                    }
                                })
            }
        }
    }

    private fun init() {
        textView.textSize = 40F
        button2.isEnabled = false

        startText = getString(R.string.start)
        stopText = getString(R.string.stop)
        pauseText = getString(R.string.pause)
        resumeText = getString(R.string.resume)

        resumeTimer()
        startTimer()
    }


    private fun startTimer() {
        stopped.set(false)
    }

    private fun stopTimer() {
        stopped.set(true)
    }


    private fun pauseTimer() {
        resumed.set(false)
        stopTimer()
    }

    private fun resumeTimer() {
        startTimer()
        resumed.set(true)
    }


    private fun resetTimerButtonListener() {
        RxView.clicks(button).subscribe {
            resetTimer()
        }
    }

    private fun resetTimer() {
        if (buttonState == 0) {
            // Start
            buttonState = 1
            button.text = stopText
            startTimer()

            Observable.intervalRange(1, resetTimeSeconds, 0, 1, TimeUnit.SECONDS)
                    .takeWhile { _ ->
                        !stopped.get()
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { i ->
                                val seconds = resetTimeSeconds - i
                                val period = Period.seconds(seconds.toInt()).normalizedStandard()

                                textViewText = myFormat.print(period)
                                textView.text = textViewText
                            },
                            { error ->
                                Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
                            },
                            {
                                // complete
                                buttonState = 0
                                button.text = startText
                                textView.text = getString(R.string.time_clock)
                                button2.visibility = VISIBLE
                                stopTimer()
                                workTimerButtonListener()
                            })
        } else if (buttonState == 1) {
            // Stop
            stopTimer()
        }
    }


    private fun workTimerButtonListener() {

        RxView.clicks(button).subscribe {
            if (buttonState == 0) {
                // Start
                buttonState = 1
                button.text = stopText
                button2.isEnabled = true
                startTimer()

                Observable.intervalRange(1, workTimeSeconds, 0, 1, TimeUnit.SECONDS)
                        .takeWhile { _ ->
                            !stopped.get()
                        }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { i ->
                                    tickCount.set(i)
                                    val seconds = workTimeSeconds - i
                                    val period = Period.seconds(seconds.toInt()).normalizedStandard()

                                    textViewText = myFormat.print(period)
                                    textView.text = textViewText
                                },
                                { error ->
                                    Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
                                },
                                {
                                    // complete
                                    // not click stop button(倒计时完成，正常结束)
                                    if (!stopped.get()) {
                                        // 直接启动休息计时
                                        buttonState = 0

                                        //进入休息时间
                                        textView.text = getString(R.string.reset_time_clock)
                                        // 没有暂停按钮
                                        button2.visibility = INVISIBLE
                                        resetTimerButtonListener()
                                        resetTimer()
                                    }
                                    // 暂停
                                    if (!resumed.get()) {
                                        Log.d("tickCount", tickCount.get().toString())
                                        Log.d("button", "pause")
                                    }
                                }
                        )
            } else if (buttonState == 1) {
                // Stop
                buttonState = 0
                button.text = startText
                stopTimer()
                textView.text = getString(R.string.time_clock)
            }
        }
    }
}
