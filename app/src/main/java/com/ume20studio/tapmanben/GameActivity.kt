package com.ume20studio.tapmanben

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
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
import java.time.LocalTime
import java.util.*
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    // タイマ制御関連
    private var vTimer: Timer? = null
    private val vHandler = Handler()

    // 乱数生成用変数
    @RequiresApi(Build.VERSION_CODES.O)
    val r: Random = Random(LocalTime.now().second)

    // BGM再生用メディアプレーヤーのインスタンス
    private lateinit var mp: MediaPlayer

    // 音声再生用サウンドプールのインスタンス
    private lateinit var soundPool: SoundPool
    private var vhit = Array(2) { IntArray(3) }
    private var vmiss = arrayOf(0,0)
    private var vstageclear = Array(2) { IntArray(3) }
    private var vgameover = 0
    private var vcountdown = arrayOf(0,0,0)
    private var vstage = arrayOf(0,0)
    private var spID:Int = 0                // サウンド停止用ストリームID

    private var tappoint:Int = 0            // タップポイント
    private var score:Int = 0               // スコア
    private var highscore:Int = 0           // ハイスコア
    private var level:Int = 0               // ステージレベル
    private var isGaming:Boolean = false    // ゲーム進行中フラグ
    private var isPausing:Boolean = false   // 一時停止中フラグ

    // ステージレベル別のポイントとBGM速度、間隔
    private val point = arrayOf(10, 20, 30, 40, 60, 80, 100, 120, 150, 200, 300)
    private val bgmspeed = arrayOf(0.80f, 0.90f, 1.00f, 1.05f, 1.10f, 1.15f, 1.20f, 1.25f, 1.30f, 1.40f, 1.50f)
    private val interval = arrayOf(100, 90, 80, 70, 65, 60, 55, 50, 45, 40, 50)
    private val ttlMax = arrayOf(300, 250, 200, 175, 150, 120, 100, 80, 60, 40, 20)
    private val stageChangeRate = arrayOf(10, 11, 12, 12, 14, 14, 16, 18, 20, 22, 30)
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
    private val cd = arrayOf(R.drawable.countdown1, R.drawable.countdown2, R.drawable.countdown3)

    // パネルのイメージボタン配列
    private val panel = arrayOf(R.id.ImgBtn0, R.id.ImgBtn1, R.id.ImgBtn2, R.id.ImgBtn3,
        R.id.ImgBtn4, R.id.ImgBtn5, R.id.ImgBtn6, R.id.ImgBtn7, R.id.ImgBtn8)

    // お弁当おにぎりのイメージ配列
    val bento = arrayOf(R.drawable.plain, R.drawable.onigiri, R.drawable.obento)
    val bentomaru = arrayOf(R.drawable.plain, R.drawable.onigiri_maru, R.drawable.obento_maru)
    val bentopeke = arrayOf(R.drawable.plain, R.drawable.onigiri_peke, R.drawable.obento_peke)

    // タップポイントのイメージ配列
    private val tpoint = arrayOf(R.id.point01, R.id.point02, R.id.point03, R.id.point04, R.id.point05,
        R.id.point06, R.id.point07, R.id.point08, R.id.point09, R.id.point10,
        R.id.point11, R.id.point12, R.id.point13, R.id.point14, R.id.point15,
        R.id.point16, R.id.point17, R.id.point18, R.id.point19, R.id.point20 )

    // パネルの表示状況関連
    private var alive = arrayOf(0,0,0,0,0,0,0,0,0,0)
    private var ttl = arrayOf(0,0,0,0,0,0,0,0,0,0)
    private var alivenum:Int = 0

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        // もとからある初期化
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // ハイスコア読み込み（HighScoreファイルが存在しなかったら0）
        val pref = getSharedPreferences("strage", Context.MODE_PRIVATE)
        highscore = pref.getInt("HighScore", 0)
        findViewById<TextView>(R.id.HighScore).text = highscore.toString()

        // ステージBGMの準備
        mp = MediaPlayer.create(this,R.raw.stagebgm_test)
        mp.setOnCompletionListener(PlayerCompletionListener())

        // サウンドプールのもろもろの初期化
        val sPattr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        soundPool = SoundPool.Builder().setAudioAttributes(sPattr).setMaxStreams(16).build()

        // 音声データのロード
        vhit[nene][0] = soundPool.load(this, R.raw.nene_hit1, 1)
        vhit[nene][1] = soundPool.load(this, R.raw.nene_hit2, 1)
        vhit[nene][2] = soundPool.load(this, R.raw.nene_hit3, 1)
        vhit[coco][0] = soundPool.load(this, R.raw.coco_hit1, 1)
        vhit[coco][1] = soundPool.load(this, R.raw.coco_hit2, 1)
        vhit[coco][2] = soundPool.load(this, R.raw.coco_hit3, 1)
        vmiss[nene] = soundPool.load(this, R.raw.nene_miss, 1)
        vmiss[coco] = soundPool.load(this, R.raw.coco_miss, 1)
        vstageclear[nene][0] = soundPool.load(this, R.raw.nene_clear1, 1)
        vstageclear[nene][1] = soundPool.load(this, R.raw.nene_clear2, 1)
        vstageclear[nene][2] = soundPool.load(this, R.raw.nene_clear3, 1)
        vstageclear[coco][0] = soundPool.load(this, R.raw.coco_clear1, 1)
        vstageclear[coco][1] = soundPool.load(this, R.raw.coco_clear2, 1)
        vstageclear[coco][2] = soundPool.load(this, R.raw.coco_clear3, 1)
        vgameover = soundPool.load(this, R.raw.gameover_test, 1)
        vcountdown[0] = soundPool.load(this, R.raw.ringo_1, 1)
        vcountdown[1] = soundPool.load(this, R.raw.ringo_2, 1)
        vcountdown[2] = soundPool.load(this, R.raw.ringo_3, 1)
        vstage[nene] =  soundPool.load(this, R.raw.nene_start, 1)
        vstage[coco] =  soundPool.load(this, R.raw.coco_start, 1)

        // パネルへのイベントリスナの紐づけ
        val panelListener = PanelTap()
        for(i in 0 until 9) {
            findViewById<ImageButton>(panel[i]).setOnClickListener(panelListener)
        }

        // ステージクリア画像へのイベントリスナの紐づけ
        val clearListener = ClearTap()
        findViewById<ImageButton>(R.id.stageclearButton).setOnClickListener(clearListener)

        // ゲームオーバー画像へのイベントリスナの紐づけ
        val gameoverListener = GameoverTap()
        findViewById<ImageButton>(R.id.gameoverButton).setOnClickListener(gameoverListener)

        // soundpool読み込み完了へのイベントリスナの紐づけ
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                // カウントダウンの「さん！」が読み込み完了したらゲームスタート
                when (sampleId) {
                    vcountdown[2] -> gameStart()
                }
            }
        }
    }

    // カウントダウンしてゲームスタート
    @RequiresApi(Build.VERSION_CODES.M)
    private fun gameStart() {
        // カウントダウン
        val thread = Thread {
            try {
                var i = 3
                while (i > 0) {
                    spID = soundPool.play(vcountdown[i - 1], 1.0f, 1.0f, 0, 0, 1.0f)
                    vHandler.post {
                        findViewById<ImageView>(R.id.countdownImage).setImageResource(cd[i - 1])
                    }
                    Thread.sleep(1000)
                    i--
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            // ステージ宣言してカウントダウン画像を非表示に
            spID = soundPool.play(vstage[stage], 1.0f, 1.0f, 0, 0, 1.0f)
            findViewById<ImageView>(R.id.countdownImage).visibility = View.INVISIBLE

            // ゲームBGMスタート
            mp.playbackParams = mp.playbackParams.setSpeed(1.001f)
            mp.start()
            mp.seekTo(0)
            mp.playbackParams = mp.playbackParams.setSpeed(bgmspeed[level])
        }
        thread.start()

        // タイマイベント
        vTimer = Timer()
        vTimer!!.schedule(object : TimerTask() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {
                vHandler.post {
                    if(isGaming){
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
                                val alivePanel = r.nextInt(9)
                                if(alive[alivePanel] == pla) {
                                    if(r.nextInt(2) == 0){
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

                        // ネネココステージチェンジ
                        if(r.nextInt(10000) < stageChangeRate[level]){
                            slideStage()
                        }
                    }
                }
            }
        }, 3800, 10)

        // ゲーム進行中フラグをtrueに
        isGaming = true
    }

    // パネルがタップされた時の処理
    private inner class PanelTap : View.OnClickListener {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onClick(v: View){
            val pref = getSharedPreferences("strage", Context.MODE_PRIVATE)
            // ゲーム進行中のみ処理
            if(isGaming) {
                var pp = 0
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
                    // おにぎりがタップされた
                    oni -> {
                        if(stage == coco) { // ココステージなら正解
                            // マルつけてニッコリ
                            findViewById<ImageButton>(panel[pp]).setImageResource(bentomaru[oni])
                            findViewById<ImageView>(R.id.imageCoco).setImageResource(R.drawable.coco1)
                            spID = soundPool.play(vhit[coco][r.nextInt(3)], 1.0f, 1.0f, 0, 0, 1.0f)
                            // スコアとタップポイント加算
                            score += point[level]
                            findViewById<ImageView>(tpoint[tappoint]).setImageResource(R.drawable.pink)
                            tappoint++
                        } else {            // ネネステージなら減点
                            // バツつけてプンスカ
                            findViewById<ImageButton>(panel[pp]).setImageResource(bentopeke[oni])
                            findViewById<ImageView>(R.id.imageNene).setImageResource(R.drawable.nene2)
                            spID = soundPool.play(vmiss[nene], 1.0f, 1.0f, 0, 0, 1.0f)
                            // スコアとタップポイント減算
                            score -= point[level] / 2
                            tappoint--
                            if(tappoint <= 0) tappoint = 0
                            findViewById<ImageView>(tpoint[tappoint]).setImageResource(R.drawable.white)
                        }
                        alive[pp] = taped
                    }
                    // 弁当がタップされた
                    ben -> {
                        if(stage == nene) {     // ネネステージなら正解
                            // マルつけてニッコリ
                            findViewById<ImageButton>(panel[pp]).setImageResource(bentomaru[ben])
                            findViewById<ImageView>(R.id.imageNene).setImageResource(R.drawable.nene1)
                            spID = soundPool.play(vhit[nene][r.nextInt(3)], 1.0f, 1.0f, 0, 0, 1.0f)
                            // スコアとタップポイント加算
                            score += point[level]
                            findViewById<ImageView>(tpoint[tappoint]).setImageResource(R.drawable.pink)
                            tappoint++
                        } else {                // ココステージなら減点
                            // バツつけてシクシク
                            findViewById<ImageButton>(panel[pp]).setImageResource(bentopeke[ben])
                            findViewById<ImageView>(R.id.imageCoco).setImageResource(R.drawable.coco2)
                            spID = soundPool.play(vmiss[coco], 1.0f, 1.0f, 0, 0, 1.0f)
                            // スコアとタップポイント減算
                            score -= point[level] / 2
                            tappoint--
                            if(tappoint <= 0) tappoint = 0
                            findViewById<ImageView>(tpoint[tappoint]).setImageResource(R.drawable.white)
                        }
                        alive[pp] = taped
                    }
                }

                // スコア表示
                findViewById<TextView>(R.id.Score).text = score.toString()

                // ハイスコア処理
                if(score >= highscore) {
                    highscore = score
                    findViewById<TextView>(R.id.HighScore).text = highscore.toString()
                    pref.edit().putInt("HighScore", highscore).apply()
                }

                // ステージクリア
                if(tappoint >= 5) {
                    stageClear()
                }
            }
        }
    }

    // ステージクリア＆次のステージへ
    @RequiresApi(Build.VERSION_CODES.O)
    private fun stageClear() {
        @RequiresApi(Build.VERSION_CODES.M)
        isGaming = false
        mp.seekTo(0)
        mp.pause()
        soundPool.stop(spID)

        // レベル処理
        if(level >= 10) {
            level = 10
        } else {
            level++
        }

        // タップポイントクリア
        tappoint = 0
        for (i in 0..19) {
            findViewById<ImageView>(tpoint[i]).setImageResource(R.drawable.white)
        }

        // ステージクリア画面を表示
        val thread = Thread {
            try {
                vHandler.post {
                    findViewById<ImageButton>(R.id.stageclearButton).visibility = View.VISIBLE
                }
                spID = soundPool.play(vstageclear[stage][r.nextInt(3)], 1.0f, 1.0f, 0, 0, 1.0f)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        thread.start()
    }

    // ステージクリア画像がタップされた時の処理
    private inner class ClearTap : View.OnClickListener {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onClick(v: View){

            // ステージクリア画面を非表示にしてカウントダウン画面を表示
            findViewById<ImageButton>(R.id.stageclearButton).visibility = View.INVISIBLE
            findViewById<ImageView>(R.id.countdownImage).visibility = View.VISIBLE

            // カウントダウン
            val thread = Thread {
                try {
                    var i = 3
                    while (i > 0) {
                        spID = soundPool.play(vcountdown[i - 1], 1.0f, 1.0f, 0, 0, 1.0f)
                        vHandler.post {
                            findViewById<ImageView>(R.id.countdownImage).setImageResource(cd[i - 1])
                        }
                        Thread.sleep(1000)
                        i--
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                // ステージ宣言してカウントダウン画像を非表示に
                spID = soundPool.play(vstage[stage], 1.0f, 1.0f, 0, 0, 1.0f)
                findViewById<ImageView>(R.id.countdownImage).visibility = View.INVISIBLE

                // ゲームBGMスタート
                mp.seekTo(0)
                mp.start()
                mp.playbackParams = mp.playbackParams.setSpeed(bgmspeed[level])

                // ゲーム進行中フラグをtrueに
                isGaming = true
            }
            thread.start()
        }
    }

    // BGM終了（つまりゲームオーバー）時の処理
    private inner class PlayerCompletionListener: MediaPlayer.OnCompletionListener {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onCompletion(mp: MediaPlayer?) {
            isGaming = false
            mp?.seekTo(0)
            val thread = Thread {
                // ゲーム進行中フラグをfalseに
                isGaming = false

                // ゲームオーバー画面を表示
                try {
                    vHandler.post {
                        findViewById<ImageButton>(R.id.gameoverButton).visibility = View.VISIBLE
                    }
                    spID = soundPool.play(vgameover, 1.0f, 1.0f, 0, 0, 1.0f)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            thread.start()
        }
    }

    // ゲームオーバー画像がタップされた時の処理
    private inner class GameoverTap : View.OnClickListener {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onClick(v: View){
            // ゲームオーバー画面を非表示
            val thread = Thread {
                findViewById<ImageButton>(R.id.gameoverButton).visibility = View.INVISIBLE
            }
            thread.start()

            // オープニング画面へ遷移
            val intent = Intent(this@GameActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
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
        spID = soundPool.play(vstage[stage], 1.0f, 1.0f, 0, 0, 1.0f)

        // スライドアニメーション
        val parts1 = findViewById<ImageView>(R.id.imageNene)
        val parts2 = findViewById<ImageView>(R.id.imageCoco)
        val parts3 = findViewById<TableLayout>(R.id.tablePanel)
        val parts4 = findViewById<LinearLayout>(R.id.areaScore)
        val parts5 = findViewById<ImageView>(R.id.countdownImage)
        val objectAnimator1 = ObjectAnimator.ofFloat(parts1, "translationX", vec)
        val objectAnimator2 = ObjectAnimator.ofFloat(parts2, "translationX", vec)
        val objectAnimator3 = ObjectAnimator.ofFloat(parts3, "translationX", vec)
        val objectAnimator4 = ObjectAnimator.ofFloat(parts4, "translationX", vec)
        val objectAnimator5 = ObjectAnimator.ofFloat(parts5, "translationX", vec)
        objectAnimator1.duration = 200
        objectAnimator2.duration = 200
        objectAnimator3.duration = 200
        objectAnimator4.duration = 200
        objectAnimator5.duration = 200
        objectAnimator1.start()
        objectAnimator2.start()
        objectAnimator3.start()
        objectAnimator4.start()
        objectAnimator5.start()
    }

    // 画面の横幅サイズを取得
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val view: View = findViewById(R.id.app_screen)
        screenWidth = view.width
        screenHeight = view.height
    }

    // ポーズ状態なら音楽もポーズ
    override fun onPause() {
        super.onPause()
        mp.pause()
        isGaming = false
        isPausing = true
    }

    // ゲーム再開なら音楽も再開
    override fun onResume() {
        super.onResume()
        if(isPausing == true) mp.start()
        isGaming = true
        isPausing = false
    }

    // 終了ならBGMを止めて音関係のメモリを解放
    override fun onDestroy() {
        super.onDestroy()
        mp.stop()
        mp.release()
        soundPool.release()
    }
}