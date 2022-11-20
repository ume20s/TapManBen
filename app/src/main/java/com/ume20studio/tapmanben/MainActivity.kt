package com.ume20studio.tapmanben

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.RequiresApi
import java.time.LocalTime
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // 乱数生成用変数
    @RequiresApi(Build.VERSION_CODES.O)
    private val r = Random(LocalTime.now().second.toLong())

    // BGM再生用メディアプレーヤーのインスタンス
    private lateinit var mp:MediaPlayer

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        // もとからある初期化
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // オープニングBGMの準備
        if(r.nextInt(4) == 0){
            mp = MediaPlayer.create(this,R.raw.openingbgm2)
        } else {
            mp = MediaPlayer.create(this,R.raw.openingbgm1)
        }
        mp.isLooping = true
        mp.seekTo(0)
        mp.start()

        // スタートボタンへのイベントリスナの紐づけ
        val startListener = StartTap()
        findViewById<ImageButton>(R.id.startButton).setOnClickListener(startListener)
    }

    // ゲームスタートがタップされた時の処理
    private inner class StartTap : View.OnClickListener {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onClick(v: View) {
            // オープニングBGMストップ
            mp.stop()

            // ゲーム画面へ遷移
            val intent = Intent(this@MainActivity, GameActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // ポーズ状態なら音楽もポーズ
    override fun onPause() {
        super.onPause()
        mp.pause()
    }

    // ゲーム再開なら音楽も再開
    override fun onResume() {
        super.onResume()
        mp.start()
    }

    // 終了ならBGMを止めて音関係のメモリを解放
    override fun onDestroy() {
        super.onDestroy()
        mp.stop()
        mp.release()
    }
}
