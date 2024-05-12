@file:Suppress("SpellCheckingInspection")

package ru.vendetti.lethalcombile

import android.content.Intent
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.vendetti.lethalcombile.ui.theme.LethalCombileTheme
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalBack
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalBorder
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalRed
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalText
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalTextDark
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalWhite
import kotlin.system.exitProcess

class GameActivity : ComponentActivity() {
    //Подключаем систему авторизации и БД Firebase
    private var auth: FirebaseAuth = Firebase.auth
    private var currentUser = auth.currentUser
    private val database = Firebase.database

    //Инициируем поля для звуковых эффектов
    private var soundPool: SoundPool? = null
    private val soundIds = IntArray(9)

    //Для фоновой музыки
    private lateinit var mediaPlayer1: MediaPlayer
    private lateinit var mediaPlayer2: MediaPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onStart()
        //Отрисовываем экран
        setContent { TerminalScreen() }
        //Инициируем пул звуков и предзагружаем их
        soundPool = SoundPool.Builder().setMaxStreams(3).build()
        soundIds[0] = soundPool!!.load(this, R.raw.tap1, 1)
        soundIds[1] = soundPool!!.load(this, R.raw.tap2, 1)
        soundIds[2] = soundPool!!.load(this, R.raw.tap3, 1)
        soundIds[3] = soundPool!!.load(this, R.raw.delete, 1)
        soundIds[4] = soundPool!!.load(this, R.raw.termopen, 1)
        soundIds[5] = soundPool!!.load(this, R.raw.termclose, 1)
        soundIds[6] = soundPool!!.load(this, R.raw.success, 1)
        soundIds[7] = soundPool!!.load(this, R.raw.error, 1)
        soundIds[8] = soundPool!!.load(this, R.raw.lever, 1)
        //После загрузки звуков проигрываем звук открытия терминала
        soundPool!!.setOnLoadCompleteListener { soundPool, _, status ->
            if (status == 0) {
                soundPool?.play(soundIds[4], 1F, 1F, 1, 0, 1F)
            }
        }
        //Настраиваем систему асинхронного цикличного бесшовного воспроизведения фоновой музыки
        mediaPlayer1 = MediaPlayer.create(this, R.raw.ambient)
        mediaPlayer1.isLooping = false
        mediaPlayer1.setVolume(0.8F, 0.8F)
        mediaPlayer2 = MediaPlayer.create(this, R.raw.ambient)
        mediaPlayer2.isLooping = false
        mediaPlayer2.setVolume(0.8F, 0.8F)
        //Запускаем цикл фоновой музыки, дальше он поддерживает сам себя до кончины активити
        musicFun1()
    }

    //Функция для задания графики (и ее логики) активити
    @Composable
    private fun ShipScreen() {
        var quoteText by remember { mutableStateOf("") }
        var quoteDays by remember { mutableIntStateOf(3) }
    }

    //Функция для задания графики (и ее логики) активити
    @Composable
    fun TerminalScreen() {
        //Текст ответной информации, появляется как результат любого воздействия с терминалом
        var errorText by remember { mutableStateOf("") }
        //Состояние для хранения введённого текста
        var text by remember { mutableStateOf("") }
        //Все, что касается клавиатуры и автофокуса курсора при запуске активити
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
        //Вообще вся разметка Jetpack Compose
        LethalCombileTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = LethalTerminalBack) {
                Box(
                    modifier = Modifier
                        .border(30.dp, LethalTerminalBorder)
                        .padding(30.dp)
                        .fillMaxSize()
                        .clickable(onClick = {
                            keyboardController?.show()
                        })
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text("[WELCOME]", color = LethalTerminalText, fontSize = 25.sp)
                        HorizontalDivider(color = LethalTerminalText)
                        Text(
                            "All available commands:", color = LethalTerminalText, fontSize = 20.sp
                        )
                        Text(
                            "moons (WIP)\nstore (WIP)\nlogout\nexit\nhelp (this message)",
                            color = LethalTerminalTextDark,
                            fontSize = 16.sp
                        )
                        HorizontalDivider(color = LethalTerminalText)
                        Text(
                            "You can type your command right below:",
                            color = LethalTerminalText,
                            fontSize = 20.sp
                        )
                        Text(errorText, color = LethalTerminalRed, fontSize = 20.sp)
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it; playkeyboardSound() },
                            singleLine = true,
                            textStyle = TextStyle(color = LethalTerminalWhite, fontSize = 20.sp),
                            cursorBrush = SolidColor(LethalTerminalText),
                            modifier = Modifier
                                .height(30.dp)
                                .focusRequester(focusRequester),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                executeCommand(text) { newErrorText ->
                                    errorText = newErrorText
                                }
                            })
                        )
                    }
                }
            }
        }
    }

    //Просто гениальная реализация взаимодействия с терминалом, горжусь ей
    private fun executeCommand(
        text: String, setErrorText: (String) -> Unit
    ) {
        when (text.trim().split(" ")[0]) {
            "exit" -> exitApp()
            "logout" -> logout(setErrorText)
            else -> {
                setErrorText("This command doesn't exist!")
                soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
            }
        }
    }

    //Функции для бесшовного воспроизведения зацикленного эмбиента
    private fun musicFun1() {
        CoroutineScope(Dispatchers.Main).launch {
            mediaPlayer1.start()
            delay(16000)
            if (isActivityActive()) {
                musicFun2()
            }
        }
    }

    private fun musicFun2() {
        CoroutineScope(Dispatchers.Main).launch {
            mediaPlayer2.start()
            delay(16000)
            if (isActivityActive()) {
                musicFun1()
            }
        }
    }

    //Функция выхода из аккаунта. В будущем вероятно будет удалена за ненадобностью
    private fun logout(setErrorText: (String) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            auth.signOut()
            currentUser = auth.currentUser
            setErrorText("Logged out")
            soundPool?.play(soundIds[5], 1F, 1F, 1, 0, 1F)
            delay(500)
            switchActivity()
        }
    }

    //Функция для асинхронного выхода из приложения
    private fun exitApp() {
        CoroutineScope(Dispatchers.Main).launch {
            soundPool?.play(soundIds[5], 1F, 1F, 1, 0, 1F)
            delay(500)
            exitProcess(-1)
        }
    }

    private fun playkeyboardSound() {
        CoroutineScope(Dispatchers.Main).launch {
            val rnds = (0..3).random()
            soundPool?.play(soundIds[rnds], 0.7F, 0.7F, 0, 0, 1F)
        }
    }

    private fun switchActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            mediaPlayer1.release()
            mediaPlayer2.release()
            soundPool?.release()
            finish()
        }, 1000)
    }

    private fun isActivityActive(): Boolean {
        return !isDestroyed && !isFinishing
    }
}