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

data class UserData(
    var cash: Int,
    var quoteNeeded: Int,
    var quoteGained: Int,
    var quoteNum: Int,
    var quoteDays: Int,
    var selectedMoon: Int
)

class GameActivity : ComponentActivity() {
    //Подключаем систему авторизации и БД Firebase
    private var auth: FirebaseAuth = Firebase.auth
    private var currentUser = auth.currentUser
    private val database = Firebase.database

    //Инициируем поля для звуковых эффектов
    private var soundPool: SoundPool? = null
    private val soundIds = IntArray(11)

    //Для фоновой музыки
    private lateinit var mediaPlayer1: MediaPlayer
    private lateinit var mediaPlayer2: MediaPlayer

    //Игровая информация
    private var quoteNeeded = 126
    private var quoteGained = 0
    private var cash = 35
    private var quoteDays = 3
    private var quoteNum = 1

    //Луны
    private var selectedMoon = 0
    private var moonName = "Experimentation"

    //Магаз

    //Строковая информация
    private var upperPrompt = "All available commands:"
    private var prompt =
        "moons\n" + "store\n" + "start\n" + "logout\n" + "exit\n" + "help (this message)"

    //Мутаблы
    private var upperPromptState = mutableStateOf(upperPrompt)
    private var promptState = mutableStateOf(prompt)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onStart()
        database.setPersistenceEnabled(true)
        //getUserData()
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
        soundIds[9] = soundPool!!.load(this, R.raw.departure, 1)
        soundIds[10] = soundPool!!.load(this, R.raw.arrival, 1)
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
                            "Profit quota: $quoteGained / $quoteNeeded\nDeadline: in $quoteDays days\nMoon: $moonName",
                            color = LethalTerminalText,
                            fontSize = 20.sp
                        )
                        HorizontalDivider(color = LethalTerminalText)
                        Text(
                            upperPromptState.value, color = LethalTerminalText, fontSize = 20.sp
                        )
                        Text(
                            promptState.value, color = LethalTerminalTextDark, fontSize = 16.sp
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
    private fun executeCommand(text: String, setErrorText: (String) -> Unit) {
        when (text.trim().split(" ")[0]) {
            "exit" -> exitApp(setErrorText)
            "logout" -> logout(setErrorText)
            "start" -> start(selectedMoon, setErrorText)
            "store" -> store()
            "moons" -> moons()
            "help" -> help()
            "experimentation" -> selectMoon(0, setErrorText)
            "assurance" -> selectMoon(1, setErrorText)
            else -> {
                setErrorText("This command doesn't exist!")
                soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
            }
        }
    }

    //Функция смены выбранной планеты
    private fun selectMoon(moon: Int, setErrorText: (String) -> Unit) {
        when (moon) {
            0 -> moonName = "Experimentation"
            1 -> moonName = "Assurance"
            2 -> moonName = "Vow"
            3 -> moonName = "Offense"
            4 -> moonName = "March"
            5 -> moonName = "Adamance"
            6 -> moonName = "Rend"
            7 -> moonName = "Dine"
            8 -> moonName = "Titan"
            9 -> moonName = "Artifice"
        }
        if (selectedMoon == moon) {
            soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
            setErrorText("You are already at $moonName!")
            return
        }
        //soundPool?.play(soundIds[5], 1F, 1F, 1, 0, 1F)
        CoroutineScope(Dispatchers.Main).launch {
            setErrorText("Flying to $moonName...")
            soundPool?.play(soundIds[9], 0.8F, 0.8F, 1, 0, 1F)
            delay(5000)
            setErrorText("You are at $moonName")
            soundPool?.play(soundIds[10], 0.8F, 0.8F, 1, 0, 1F)
            selectedMoon = moon
        }
    }

    //Функция магазина предметов
    private fun store() {
        upperPromptState.value = "Your balance: $cash\nAll available items:"
        promptState.value = "Flashlight (WIP): 15\n" + "Shovel (WIP): 60\n" + "Boombox (WIP): 120"
        soundPool?.play(soundIds[5], 1F, 1F, 1, 0, 1F)
    }

    //Функция вызова списка команд
    private fun help() {
        upperPromptState.value = "All available commands:"
        promptState.value =
            "moons\n" + "store\n" + "start\n" + "logout\n" + "exit\n" + "help (this message)"
        soundPool?.play(soundIds[5], 1F, 1F, 1, 0, 1F)
    }

    //Функция выбора луны
    private fun moons() {
        upperPromptState.value = "All available moons:"
        promptState.value =
            "Experimentation\n" + "Assurance (WIP)\n" + "Vow (WIP)\n\n" + "Offense (WIP)\n" + "March (WIP)\n" + "Adamance (WIP)\n\n" + "Rend (WIP)\n" + "Dine (WIP)\n" + "Titan (WIP)"
        soundPool?.play(soundIds[5], 1F, 1F, 1, 0, 1F)
    }

    //Функция начала раунда
    private fun start(moon: Int, setErrorText: (String) -> Unit) {
        when (moon) {
            0 -> switchActivity(0)
            1 -> switchActivity(1)
            2 -> switchActivity(2)
            3 -> switchActivity(3)
            4 -> switchActivity(4)
            5 -> switchActivity(5)
            6 -> switchActivity(6)
            7 -> switchActivity(7)
            8 -> switchActivity(8)
            9 -> switchActivity(9)
            else -> {
                setErrorText("Selected moon index out of array!")
                soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
                return
            }
        }
        setErrorText("Landing...")
        soundPool?.play(soundIds[8], 1F, 1F, 1, 0, 1F)
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
            setErrorText("Logging out...")
            soundPool?.play(soundIds[5], 1F, 1F, 1, 0, 1F)
            delay(500)
            switchActivity(10)
        }
    }

    //Функция для выхода из приложения
    private fun exitApp(setErrorText: (String) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            setErrorText("Exiting app...")
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

    private fun switchActivity(mode: Int) {
        when (mode) {
            10 -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, MainActivity::class.java))
                    mediaPlayer1.release()
                    mediaPlayer2.release()
                    soundPool?.release()
                    finish()
                }, 1000)
            }

            in 0..9 -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    //ClickerActivity.moon = mode
                    //startActivity(Intent(this, ClickerActivity::class.java))
                    mediaPlayer1.release()
                    mediaPlayer2.release()
                    soundPool?.release()
                    finish()
                }, 5000)
            }

            else -> {
                return
            }
        }
    }

    private fun isActivityActive(): Boolean {
        return !isDestroyed && !isFinishing
    }

    fun saveUserData(data: UserData) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(uid)
        userRef.setValue(data).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                println("Data saved successfully.")
            } else {
                println("Failed to save data: ${task.exception}")
            }
        }
    }

    fun getUserData() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(uid)
        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val data = task.result?.getValue(UserData::class.java)
                println("Retrieved data: $data")
            } else {
                println("Failed to retrieve data: ${task.exception}")
            }
        }
    }
}