@file:Suppress("SpellCheckingInspection")

package ru.vendetti.lethalcombile

import android.content.Intent
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Patterns
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

class MainActivity : ComponentActivity() {
    //Подключаем систему авторизации Firebase
    private var auth: FirebaseAuth = Firebase.auth
    private var currentUser = auth.currentUser

    //Инициируем поля для звуковых эффектов
    private var soundPool: SoundPool? = null
    private val soundIds = IntArray(8)

    //Для фоновой музыки
    private lateinit var mediaPlayer1: MediaPlayer
    private lateinit var mediaPlayer2: MediaPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onStart()
        //Если был выполнен вход в пользователя, происходит переход на следующее активити. Если нет, дальше выполняется текущее
        if (currentUser != null) {
            switchActivity(false)
        } else {
            //Отрисовываем экран
            setContent { LoginAppScreen() }
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
            //После загрузки звуков проигрываем звук открытия терминала
            soundPool!!.setOnLoadCompleteListener { soundPool, _, status ->
                if (status == 0) {
                    soundPool?.play(soundIds[4], 0.8F, 0.8F, 1, 0, 1F)
                }
            }
            //Настраиваем систему асинхронного цикличного бесшовного воспроизведения фоновой музыки
            mediaPlayer1 = MediaPlayer.create(this, R.raw.ambient)
            mediaPlayer1.isLooping = false
            mediaPlayer1.setVolume(0.6F, 0.6F)
            mediaPlayer2 = MediaPlayer.create(this, R.raw.ambient)
            mediaPlayer2.isLooping = false
            mediaPlayer2.setVolume(0.6F, 0.6F)
            //Запускаем цикл фоновой музыки, дальше он поддерживает сам себя до кончины активити
            musicFun1()
        }
    }

    //Функция для задания графики (и ее логики) активити. Вызывалась выше в OnCreate
    @Composable
    fun LoginAppScreen() {
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
                            "Register or login before starting the game. You can do it by typing:",
                            color = LethalTerminalText,
                            fontSize = 20.sp
                        )
                        Text(
                            "register <emal> <password> <password>\n",
                            color = LethalTerminalTextDark,
                            fontSize = 16.sp
                        )
                        Text(
                            "login <email> <password>",
                            color = LethalTerminalTextDark,
                            fontSize = 16.sp
                        )
                        HorizontalDivider(color = LethalTerminalText)
                        Text(
                            "All available commands:", color = LethalTerminalText, fontSize = 20.sp
                        )
                        Text(
                            "register\nlogin\nlogout\nexit",
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
            "login" -> checkLogin(text, setErrorText)
            "register" -> checkRegister(text, setErrorText)
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

    //Функция для асинхронного выхода из приложения
    private fun exitApp() {
        soundPool?.play(soundIds[5], 1F, 1F, 1, 0, 1F)
        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            exitProcess(-1)
        }
    }

    //Функция проверки валидности введенных данных при вызове login
    private fun checkLogin(text: String, setErrorText: (String) -> Unit) {
        val parts = text.split(" ")
        if (parts.size == 3) {
            if (isValidEmail(parts[1]) && parts[2].length > 5) {
                setErrorText("Logging in...")
                login(parts[1], parts[2], setErrorText)
            } else {
                setErrorText("Incorrect email/passwd (len must be > 5 chars)")
                soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
            }
        } else {
            setErrorText("Wrong syntax")
            soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
        }
    }

    //Функция проверки валидности введенных данных при вызове register
    private fun checkRegister(text: String, setErrorText: (String) -> Unit) {
        val parts = text.split(" ")
        if (parts.size == 4) {
            if (isValidEmail(parts[1]) && parts[2].length > 5 && parts[2] == parts[3]) {
                setErrorText("Registering...")
                register(parts[1], parts[2], setErrorText)
            } else {
                setErrorText("Incorrect email/passwd (len must be > 5 chars)")
                soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
            }
        } else {
            setErrorText("Wrong syntax")
            soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
        }
    }

    //Функция проверки валидности email-адреса
    private fun isValidEmail(target: CharSequence?): Boolean {
        return !TextUtils.isEmpty(target) && target?.let {
            Patterns.EMAIL_ADDRESS.matcher(it).matches()
        } == true
    }

    private fun register(email: String, password: String, setErrorText: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                currentUser = auth.currentUser
                setErrorText("Successful")
                soundPool?.play(soundIds[6], 1F, 1F, 1, 0, 1F)
                switchActivity(true)
            } else {
                setErrorText("Register error")
                soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
            }
        }
    }

    private fun login(email: String, password: String, setErrorText: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                currentUser = auth.currentUser
                setErrorText("Successful")
                soundPool?.play(soundIds[6], 1F, 1F, 1, 0, 1F)
                switchActivity(true)
            } else {
                setErrorText("Login error")
                soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
            }
        }
    }

    //Функция выхода из аккаунта. В будущем вероятно будет удалена за ненадобностью
    private fun logout(setErrorText: (String) -> Unit) {
        if (currentUser == null) {
            setErrorText("You're not logged in")
            soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
        } else {
            auth.signOut()
            currentUser = auth.currentUser
            setErrorText("Logged out")
            soundPool?.play(soundIds[6], 1F, 1F, 1, 0, 1F)
        }
    }

    //Функция проигрывания звуков клавиатуры
    private fun playkeyboardSound() {
        CoroutineScope(Dispatchers.Main).launch {
            val rnds = (0..3).random()
            soundPool?.play(soundIds[rnds], 0.7F, 0.7F, 0, 0, 1F)
        }
    }

    //Функция смены активити. Имеет 2 режима: моментальная смена (используется в самом начале в проверке аккаунта пользователя) и смена динамическая, после использования register/login
    private fun switchActivity(mode: Boolean) {
        if (mode) {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, GameActivity::class.java))
                mediaPlayer1.release()
                mediaPlayer2.release()
                soundPool?.release()
                finish()
            }, 1000)
        } else {
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }

    }

    //Функция для борьбы с вылетами и исключениями, возникающими из-за некоторых асинхронных функций, пытающихся обратиться к тому, чего нет
    private fun isActivityActive(): Boolean {
        return !isDestroyed && !isFinishing
    }
}