package ru.vendetti.lethalcombile

import android.content.Intent
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalOrange

class ClickerActivity : ComponentActivity() {
    private var soundPool: SoundPool? = null
    private val soundIds = IntArray(1)
    private lateinit var mediaPlayer: MediaPlayer
    private var cashGained = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onStart()
        val receivedNumber = intent.getIntExtra("EXTRA_NUMBER", 0)
        setContent { Clicker { backOnTrack(cashGained) } }
        soundPool = SoundPool.Builder().setMaxStreams(3).build()
        soundIds[0] = soundPool!!.load(this, R.raw.knife, 1)
        mediaPlayer = MediaPlayer.create(this, R.raw.ambient)
        mediaPlayer.isLooping = false
        mediaPlayer.setVolume(0.8F, 0.8F)
    }

    @Composable
    private fun Clicker(onTimeUp: () -> Unit) {
        var timeLeft by remember { mutableIntStateOf(180) } // 3 минуты (180 секунд)
        var scale by remember { mutableFloatStateOf(1f) }
        //var counter by remember { mutableIntStateOf(0) }
        val scope = rememberCoroutineScope()
        var tapPosition by remember { mutableStateOf<Offset?>(null) }
        val animatedOffsetY = remember { Animatable(0f) }
        val animatedAlpha = remember { Animatable(1f) }
        //val density = LocalDensity.current
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.dine),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            LaunchedEffect(Unit) {
                object : CountDownTimer(180000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        timeLeft = (millisUntilFinished / 1000).toInt()

                        // Проигрывание звуков каждые 60 секунд
                        when (timeLeft) {
                            120 -> playMusic(2)
                            165 -> playMusic(5)
                            178 -> playMusic(1)
                            60 -> playMusic(3)
                            90 -> playMusic(6)
                            20 -> playMusic(4)
                        }
                    }

                    override fun onFinish() {
                        onTimeUp()
                    }
                }.start()
            }

            TimerUI(timeLeft)
            Image(painter = painterResource(id = R.drawable.butler),
                contentDescription = null,
                modifier = Modifier
                    .size(450.dp)
                    .scale(scale)
                    .align(Alignment.Center)
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = { offset ->
                            scale = 0.9f // Уменьшение размера при нажатии
                            cashGained++ // Увеличиваем числовую переменную
                            tapPosition = offset // Сохраняем позицию нажатия
                            playSound()
                            scope.launch {
                                animatedOffsetY.snapTo(0f)
                                animatedAlpha.snapTo(1f)
                                animatedOffsetY.animateTo(
                                    targetValue = -30.dp.toPx(),
                                    animationSpec = tween(durationMillis = 2000)
                                )
                                animatedAlpha.animateTo(
                                    targetValue = 0f, animationSpec = tween(durationMillis = 2000)
                                )
                                delay(2000)
                                tapPosition = null
                            }
                            tryAwaitRelease()
                            scale = 1f // Возвращение размера после отпускания
                        })
                    })
            // Отображение текста "+1" в месте нажатия, если позиция нажатия не null
            tapPosition?.let { position ->
                val offset = IntOffset(position.x.toInt(), position.y.toInt())
                Text(
                    text = "+1",
                    style = TextStyle(fontSize = 42.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.graphicsLayer(
                        translationX = offset.x.toFloat(),
                        translationY = offset.y.toFloat() + animatedOffsetY.value + 200,
                        alpha = animatedAlpha.value
                    )
                )
            }
        }
    }

    @Composable
    private fun TimerUI(timeLeft: Int) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 130.dp, end = 130.dp)
        ) {
//            // Фоновое изображение
//            Image(
//                painter = painterResource(id = R.drawable.butler),
//                contentDescription = null,
//                contentScale = ContentScale.Crop,
//                modifier = Modifier.fillMaxSize()
//            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(3.dp, LethalTerminalOrange),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Time left:",
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LethalTerminalOrange,
                    modifier = Modifier.padding(top = 20.dp)
                )

                Text(
                    text = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60),
                    fontSize = 48.sp,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LethalTerminalOrange
                )
            }
        }
    }

    private fun backOnTrack(cashGained: Int) {
        startActivity(Intent(
            this, GameActivity::class.java
        ).apply { putExtra("EXTRA_NUMBER", cashGained) })
        mediaPlayer.release()
        soundPool?.release()
        finish()
    }

    private fun playSound() {
            soundPool?.play(soundIds[0], 0.2F, 0.2F, 1, 0, 1F)
    }

    private fun playMusic(musicID: Int) {
        var media = MediaPlayer.create(this, R.raw.day1)
        when (musicID) {
            1 -> media = MediaPlayer.create(this, R.raw.day1)
            2 -> media = MediaPlayer.create(this, R.raw.day2)
            3 -> media = MediaPlayer.create(this, R.raw.day3)
            4 -> media = MediaPlayer.create(this, R.raw.day4)
            5 -> media = MediaPlayer.create(this, R.raw.back1)
            6 -> media = MediaPlayer.create(this, R.raw.back2)
        }
        media.start()
    }
}