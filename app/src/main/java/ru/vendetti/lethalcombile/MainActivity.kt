@file:Suppress("SpellCheckingInspection")

package ru.vendetti.lethalcombile

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
import ru.vendetti.lethalcombile.ui.theme.LethalCombileTheme
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalBack
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalBorder
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalRed
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalText
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalTextDark
import ru.vendetti.lethalcombile.ui.theme.LethalTerminalWhite
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private var auth: FirebaseAuth = Firebase.auth
    private var currentUser = auth.currentUser
    private var soundPool: SoundPool? = null
    private val soundIds = IntArray(8)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        //if (currentUser != null) {
        //reload()
        //} else {
        super.onCreate(savedInstanceState)
        setContent { LoginAppScreen() }
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        soundIds[0] = soundPool!!.load(this, R.raw.tap1, 1)
        soundIds[1] = soundPool!!.load(this, R.raw.tap2, 1)
        soundIds[2] = soundPool!!.load(this, R.raw.tap3, 1)
        soundIds[3] = soundPool!!.load(this, R.raw.delete, 1)
        soundIds[4] = soundPool!!.load(this, R.raw.termopen, 1)
        soundIds[5] = soundPool!!.load(this, R.raw.termclose, 1)
        soundIds[6] = soundPool!!.load(this, R.raw.success, 1)
        soundIds[7] = soundPool!!.load(this, R.raw.error, 1)
        soundPool!!.setOnLoadCompleteListener { soundPool, _, status ->
            if (status == 0) {
                soundPool?.play(soundIds[4], 1F, 1F, 1, 0, 1F)
            }
        }
        //}
    }

    @Composable
    fun LoginAppScreen() {
        var errorText by remember { mutableStateOf("") }
        var text by remember { mutableStateOf("") }  // Состояние для хранения введённого текста
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
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
                    //.focusRequester(focusRequester)
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
                                //.padding(top = 20.dp)
                                //.fillMaxSize(),
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

    private fun exitApp(){
        soundPool?.play(soundIds[5], 1F, 1F, 1, 0, 1F)
        Handler(Looper.getMainLooper()).postDelayed({
            exitProcess(-1)
        }, 500)
    }

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
            } else {
                setErrorText("Login error")
                soundPool?.play(soundIds[7], 1F, 1F, 1, 0, 1F)
            }
        }
    }

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
    private fun playkeyboardSound(){
        val rnds = (0..3).random()
        soundPool?.play(soundIds[rnds], 1F, 1F, 0, 0, 1F)
    }
}