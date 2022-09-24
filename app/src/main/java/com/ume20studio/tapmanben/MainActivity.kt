package com.ume20studio.tapmanben

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import java.util.*

class MainActivity : AppCompatActivity() {

    // タイマ制御関連
    private var vTimer: Timer? = null
    private val vHandler = Handler()
    private var interval:Int = 100


    // BGM再生用メディアプレーヤーのインスタンス
    private lateinit var mp: MediaPlayer

    // 音声再生用サウンドプールのインスタンス
    private lateinit var soundPool: SoundPool
    private var pinpon = 0
    private var bubuu = 0
    private var countdown = arrayOf(0,0,0,0)

    private var score:Int = 0               // スコア
    private var highscore:Int = 0           // ハイスコア

    // ステージ定数
    private val nene:Int = 0
    private val coco:Int = 1
    private var stage:Int = nene

    // 弁当定数
    private val pla:Int = 0     // プレーン
    private val oni:Int = 1     // おにぎり
    private val ben:Int = 2     // お弁当

    // スクリーンの大きさ
    private var screenWidth:Int = 0
    private var screenHeight:Int = 0

    // カウントダウンのイメージ配列
    private val cd = arrayOf(R.drawable.ichi_test, R.drawable.ni_test, R.drawable.san_test)

    // パネルのイメージボタン配列
    private val panel = arrayOf(R.id.ImgBtn0, R.id.ImgBtn1, R.id.ImgBtn2, R.id.ImgBtn3,
        R.id.ImgBtn4, R.id.ImgBtn5, R.id.ImgBtn6, R.id.ImgBtn7, R.id.ImgBtn8)

    // お弁当おにぎりのイメージ配列
    val bento = arrayOf(R.drawable.plain_test, R.drawable.onigiri_test, R.drawable.bento_test)
    val bento_maru = arrayOf(R.drawable.plain_test, R.drawable.onigiri_maru_test, R.drawable.bento_maru_test)
    val bento_peke = arrayOf(R.drawable.plain_test, R.drawable.onigiri_peke_test, R.drawable.bento_peke_test)

    // パネルの表示状況関連
    private var alive = arrayOf(0,0,0,0,0,0,0,0,0)

    // もとからあるonCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        // もとからある初期化
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // サウンドプールのもろもろの初期化
        val sPattr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        soundPool = SoundPool.Builder().setAudioAttributes(sPattr).setMaxStreams(6).build()

        // 音声データのロード
        pinpon = soundPool.load(this, R.raw.pinpon, 1)
        bubuu = soundPool.load(this, R.raw.buu, 1)
        countdown[0] = soundPool.load(this, R.raw.ichi_test, 1)
        countdown[1] = soundPool.load(this, R.raw.ni_test, 1)
        countdown[2] = soundPool.load(this, R.raw.san_test, 1)
        countdown[3] = soundPool.load(this, R.raw.start_test, 1)


        // スタートボタンへのイベントリスナの紐づけ
        val startListener = StartTap()
        findViewById<ImageButton>(R.id.startButton).setOnClickListener(startListener)

        // パネルへのイベントリスナの紐づけ
        val panelListener = PanelTap()
        for(i in 0 until 9) {
            findViewById<ImageButton>(panel[i]).setOnClickListener(panelListener)
        }


    }

    // スタートボタンがタップされた時の処理
    private inner class StartTap : View.OnClickListener {
        override fun onClick(v: View) {
            // スタートボタンを非表示に
            findViewById<ImageButton>(R.id.startButton).visibility = View.INVISIBLE

            // カウントダウン
            val thread = Thread(Runnable {
                try {
                    var i = 3
                    while (i > 0) {
                        soundPool.play(countdown[i-1], 1.0f, 1.0f, 0, 0, 1.0f)
                        vHandler.post {
                            findViewById<ImageView>(R.id.countdownImage).setImageResource(cd[i-1])
                        }
                        Thread.sleep(1200)
                        i--
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                soundPool.play(countdown[3], 1.0f, 1.0f, 0, 0, 1.0f)
                findViewById<ImageView>(R.id.countdownImage).visibility = View.INVISIBLE
            })
            thread.start()

            // タイマイベント
            vTimer = Timer()
            vTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    vHandler.post {
                        val alivePanel = (0..8).random()
                        if(alive[alivePanel] == pla) {
                            if((0..1).random() == 0){
                                findViewById<ImageButton>(panel[alivePanel]).setImageResource(bento[oni])
                                alive[alivePanel] = oni
                            } else {
                                findViewById<ImageButton>(panel[alivePanel]).setImageResource(bento[ben])
                                alive[alivePanel] = ben
                            }
                        } else {
                            findViewById<ImageButton>(panel[alivePanel]).setImageResource(bento[pla])
                            alive[alivePanel] = pla
                        }
                    }
                }
            }, 500, 500)
        }

    }

    // パネルがタップされた時の処理
    private inner class PanelTap : View.OnClickListener {
        override fun onClick(v: View){
            var pp:Int

            when(v.id){
                panel[0] -> {
                    when(alive[0]) {
                        oni -> {
                            if(stage == nene) {
                                findViewById<ImageButton>(panel[0]).setImageResource(bento_maru[oni])
                                score += 10
                            } else {
                                findViewById<ImageButton>(panel[0]).setImageResource(bento_peke[oni])
                                score -= 5
                            }
                        }
                        ben -> {
                            if(stage == coco) {
                                findViewById<ImageButton>(panel[0]).setImageResource(bento_maru[ben])
                                score += 10
                            } else {
                                findViewById<ImageButton>(panel[0]).setImageResource(bento_peke[ben])
                                score -= 5
                            }
                        }
                    }
                }
            }
            findViewById<TextView>(R.id.Score).setText(score.toString())

            // 仮 動作確認用～
            if(Math.random() > 0.7){
                slideStage()
            }
        }
    }

    // ステージをスライド
    private fun slideStage() {
        val vec:Float

        // ネネとココをチェンジ
        if(stage == nene) {
            stage = coco
            vec = -screenWidth.toFloat() * 0.4f
        } else {
            stage = nene
            vec = 0f
        }

        // スライドアニメーション
        val parts1 = findViewById<ImageView>(R.id.imageNene)
        val parts2 = findViewById<ImageView>(R.id.imageCoco)
        val parts3 = findViewById<TableLayout>(R.id.tablePanel)
        val parts4 = findViewById<LinearLayout>(R.id.areaScore)
        val objectAnimator1 = ObjectAnimator.ofFloat(parts1, "translationX", vec)
        val objectAnimator2 = ObjectAnimator.ofFloat(parts2, "translationX", vec)
        val objectAnimator3 = ObjectAnimator.ofFloat(parts3, "translationX", vec)
        val objectAnimator4 = ObjectAnimator.ofFloat(parts4, "translationX", vec)
        objectAnimator1.duration = 200
        objectAnimator2.duration = 200
        objectAnimator3.duration = 200
        objectAnimator4.duration = 200
        objectAnimator1.start()
        objectAnimator2.start()
        objectAnimator3.start()
        objectAnimator4.start()
    }

    // 画面の横幅サイズを取得
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val view: View = findViewById(R.id.app_scrren)
        screenWidth = view.getWidth()
        screenHeight = view.getHeight()
    }

}