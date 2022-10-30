package com.ume20studio.tapmanben

import android.animation.ObjectAnimator
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // タイマ制御関連
    private var vTimer: Timer? = null
    private val vHandler = Handler()

    // BGM再生用メディアプレーヤーのインスタンス
    private lateinit var mpop:MediaPlayer
    private lateinit var mp:MediaPlayer

    // 音声再生用サウンドプールのインスタンス
    private lateinit var soundPool: SoundPool
    private var vpinpon = 0
    private var vbubuu = 0
    private var vstageclear = 0
    private var vcountdown = arrayOf(0,0,0)
    private var vstage = arrayOf(0,0)

    private var score:Int = 0               // スコア
    private var highscore:Int = 0           // ハイスコア
    private var level:Int = 0               // ステージレベル
    private var isGaming:Boolean = false    // ゲーム進行中フラグ

    // ステージレベル別のポイントとBGM速度、間隔
    private var point = arrayOf(10,20,30,40,60,80,100,120,150,200)
    private var bgmspeed = arrayOf(0.80f, 0.90f, 1.00f, 1.05f, 1.10f, 1.15f, 1.20f, 1.25f, 1.30f, 1.40f)
    private var interval = arrayOf(100, 90, 80, 70, 60, 50, 50, 40, 40, 50)
    private var ttlMax = arrayOf(300, 250, 200, 150, 120, 100, 80, 40, 20, 10)
    private var remain:Int = interval[level]

    // ステージ定数
    private val nene:Int = 0
    private val coco:Int = 1
    private var stage:Int = nene

    // 弁当定数
    private val pla:Int = 0     // プレーン
    private val oni:Int = 1     // おにぎり
    private val ben:Int = 2     // お弁当
    private val taped:Int = 3   // タップ終了

    // スクリーンの大きさ
    private var screenWidth:Int = 0
    private var screenHeight:Int = 0

    // カウントダウンのイメージ配列
    private val cd = arrayOf(R.drawable.ichi_test, R.drawable.ni_test, R.drawable.san_test)

    // パネルのイメージボタン配列
    private val panel = arrayOf(R.id.ImgBtn0, R.id.ImgBtn1, R.id.ImgBtn2, R.id.ImgBtn3,
        R.id.ImgBtn4, R.id.ImgBtn5, R.id.ImgBtn6, R.id.ImgBtn7, R.id.ImgBtn8)

    // お弁当おにぎりのイメージ配列
    val bento = arrayOf(R.drawable.plain, R.drawable.onigiri, R.drawable.obento)
    val bentomaru = arrayOf(R.drawable.plain, R.drawable.onigiri_maru, R.drawable.obento_maru)
    val bentopeke = arrayOf(R.drawable.plain, R.drawable.onigiri_peke, R.drawable.obento_peke)

    // パネルの表示状況関連
    private var alive = arrayOf(0,0,0,0,0,0,0,0,0)
    private var ttl = arrayOf(0,0,0,0,0,0,0,0,0)
    private var alivenum:Int = 0

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        // もとからある初期化
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // オープニングBGMスタート
        mpop = MediaPlayer.create(this,R.raw.openingbgm)
        mpop.isLooping = true
        mpop.seekTo(0)
        mpop.start()

        // ステージBGMの準備
        mp = MediaPlayer.create(this,R.raw.stagebgm_test)
        mp.setOnCompletionListener(PlayerCompletionListener())

        // サウンドプールのもろもろの初期化
        val sPattr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        soundPool = SoundPool.Builder().setAudioAttributes(sPattr).setMaxStreams(9).build()

        // 音声データのロード
        vpinpon = soundPool.load(this, R.raw.pinpon_test, 1)
        vbubuu = soundPool.load(this, R.raw.bubuu_test, 1)
        vstageclear = soundPool.load(this, R.raw.clear, 1)
        vcountdown[0] = soundPool.load(this, R.raw.ichi_test, 1)
        vcountdown[1] = soundPool.load(this, R.raw.ni_test, 1)
        vcountdown[2] = soundPool.load(this, R.raw.san_test, 1)
        vstage[nene] =  soundPool.load(this, R.raw.nenestage_test, 1)
        vstage[coco] =  soundPool.load(this, R.raw.cocostage_test, 1)

        // スタートボタンへのイベントリスナの紐づけ
        val startListener = StartTap()
        findViewById<ImageButton>(R.id.startButton).setOnClickListener(startListener)

        // パネルへのイベントリスナの紐づけ
        val panelListener = PanelTap()
        for(i in 0 until 9) {
            findViewById<ImageButton>(panel[i]).setOnClickListener(panelListener)
        }

        // ステージクリア画像へのイベントリスナの紐づけ
        val clearListener = ClearTap()
        findViewById<ImageButton>(R.id.stageclearButton).setOnClickListener(clearListener)
    }

    // ゲームスタート時の処理
    private inner class StartTap : View.OnClickListener {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onClick(v: View) {
            // スタートボタンを非表示に
            findViewById<ImageButton>(R.id.startButton).visibility = View.INVISIBLE

            // オープニングBGMストップ
            mpop.stop()

            // カウントダウン
            val thread = Thread(Runnable {
                try {
                    var i = 3
                    while (i > 0) {
                        soundPool.play(vcountdown[i-1], 1.0f, 1.0f, 0, 0, 1.0f)
                        vHandler.post {
                            findViewById<ImageView>(R.id.countdownImage).setImageResource(cd[i-1])
                        }
                        Thread.sleep(1200)
                        i--
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                // ステージ宣言してカウントダウン画像を非表示に
                soundPool.play(vstage[stage], 1.0f, 1.0f, 0, 0, 1.0f)
                findViewById<ImageView>(R.id.countdownImage).visibility = View.INVISIBLE

                // ゲームBGMスタート
                mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(1.001f))
                mp.start()
                mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(bgmspeed[level]))
            })
            thread.start()

            // タイマイベント
            vTimer = Timer()
            vTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    vHandler.post {
                        // 残り時間減算
                        remain--

                        // パネル表示リミットがきたら消す
                        for(i in 0..8){
                            if(ttl[i] > 0) {
                                ttl[i]--
                                if(ttl[i] == 0) {
                                    findViewById<ImageButton>(panel[i]).setImageResource(bento[pla])
                                    alive[i] = pla
                                    alivenum--
                                }
                            }
                        }

                        // インターバル毎にランダムにパネルを表示
                        if(remain <=0) {
                            for (i in 0..8) {
                                // タップ済みのパネルを消去
                                if(alive[i] == taped) {
                                    findViewById<ImageButton>(panel[i]).setImageResource(bento[pla])
                                    alive[i] = pla
                                    alivenum--
                                }
                            }

                            // 表示パネルは最大４枚にする
                            if(alivenum < 4) {
                                val alivePanel = Random.nextInt(9)
                                if(alive[alivePanel] == pla) {
                                    if(Random.nextInt(2) == 0){
                                        findViewById<ImageButton>(panel[alivePanel]).setImageResource(bento[oni])
                                        alive[alivePanel] = oni
                                    } else {
                                        findViewById<ImageButton>(panel[alivePanel]).setImageResource(bento[ben])
                                        alive[alivePanel] = ben
                                    }
                                    ttl[alivePanel] = ttlMax[level]
                                    alivenum++
                                }
                            }
                            remain = interval[level]
                        } else {
                            remain--
                        }
                    }
                }
            }, 3800, 10)

            // ゲーム進行中フラグをtrueに
            isGaming = true
        }
    }

    // パネルがタップされた時の処理
    private inner class PanelTap : View.OnClickListener {
        override fun onClick(v: View){

            var pp:Int = 0
            // ゲーム進行中のみ処理
            if(isGaming) {
                when(v.id){
                    panel[0] -> {
                        pp = 0
                    }
                    panel[1] -> {
                        pp = 1
                    }
                    panel[2] -> {
                        pp = 2
                    }
                    panel[3] -> {
                        pp = 3
                    }
                    panel[4] -> {
                        pp = 4
                    }
                    panel[5] -> {
                        pp = 5
                    }
                    panel[6] -> {
                        pp = 6
                    }
                    panel[7] -> {
                        pp = 7
                    }
                    panel[8] -> {
                        pp = 8
                    }
                }

                // タップしたアイテムによって処理がいろいろ
                when(alive[pp]) {
                    oni -> {
                        if(stage == nene) {
                            findViewById<ImageButton>(panel[pp]).setImageResource(bentomaru[oni])
                            findViewById<ImageView>(R.id.imageNene).setImageResource(R.drawable.nene1)
                            soundPool.play(vpinpon, 1.0f, 1.0f, 0, 0, 1.0f)
                            score += point[level]
                        } else {
                            findViewById<ImageButton>(panel[pp]).setImageResource(bentopeke[oni])
                            findViewById<ImageView>(R.id.imageCoco).setImageResource(R.drawable.coco2)
                            soundPool.play(vbubuu, 1.0f, 1.0f, 0, 0, 1.0f)
                            score -= point[level] / 2
                        }
                        alive[pp] = taped
                    }
                    ben -> {
                        if(stage == coco) {
                            findViewById<ImageButton>(panel[pp]).setImageResource(bentomaru[ben])
                            findViewById<ImageView>(R.id.imageCoco).setImageResource(R.drawable.coco1)
                            soundPool.play(vpinpon, 1.0f, 1.0f, 0, 0, 1.0f)
                            score += point[level]
                        } else {
                            findViewById<ImageButton>(panel[pp]).setImageResource(bentopeke[ben])
                            findViewById<ImageView>(R.id.imageNene).setImageResource(R.drawable.nene2)
                            soundPool.play(vbubuu, 1.0f, 1.0f, 0, 0, 1.0f)
                            score -= point[level] / 2
                        }
                        alive[pp] = taped
                    }
                }

                // スコア表示
                findViewById<TextView>(R.id.Score).text = score.toString()
                findViewById<TextView>(R.id.HighScore).text = level.toString()
            }
            // 仮 動作確認用～　ネネココステージチェンジ
            if(Random.nextInt(10) > 8){
                slideStage()
            }
        }
    }

    // ステージクリア画像がタップされた時の処理
    private inner class ClearTap : View.OnClickListener {
        override fun onClick(v: View){
            isGaming = true
        }
    }

    // BGM終了（つまりステージ終了）時の処理
    private inner class PlayerCompletionListener: MediaPlayer.OnCompletionListener {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onCompletion(mp: MediaPlayer?) {
            val thread = Thread(Runnable {
                // ゲーム進行中フラグをfalseに
                isGaming = false

                mp?.seekTo(0)
                if(level >= 9) {
                    level = 9
                } else {
                    level++
                }
                try {
                    vHandler.post {
                        findViewById<ImageButton>(R.id.stageclearButton).visibility = View.VISIBLE
                    }
                    soundPool.play(vstageclear, 1.0f, 1.0f, 0, 0, 1.0f)
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                findViewById<ImageView>(R.id.stageclearButton).visibility = View.INVISIBLE

                // ゲームBGMスタート
                mp?.start()
                mp?.setPlaybackParams(mp.getPlaybackParams().setSpeed(bgmspeed[level]))

                // ゲーム進行中フラグをtrueに
                isGaming = true
            })
            thread.start()
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

        // ステージアナウンス
        soundPool.play(vstage[stage], 1.0f, 1.0f, 0, 0, 1.0f)

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
        val view: View = findViewById(R.id.app_screen)
        screenWidth = view.width
        screenHeight = view.height
    }

    // ポーズでもゲーム終了
    override fun onPause() {
        super.onPause()
        val thread = Thread(Runnable {
            soundPool.release()
            mpop?.stop()
            mpop?.release()
            mp?.stop()
            mp?.release()
        })
        thread.start()
        finish()
    }

    // ポーズでもゲーム終了
    override fun onStop() {
        super.onStop()
        val thread = Thread(Runnable {
            soundPool.release()
            mpop?.stop()
            mpop?.release()
            mp?.stop()
            mp?.release()
        })
        thread.start()
        finish()
    }

    // 終了なら音関係のメモリを解放
    override fun onDestroy() {
        super.onDestroy()
        val thread = Thread(Runnable {
            soundPool.release()
            mpop?.stop()
            mpop?.release()
            mp?.stop()
            mp?.release()
        })
        thread.start()
        finish()
    }
}
