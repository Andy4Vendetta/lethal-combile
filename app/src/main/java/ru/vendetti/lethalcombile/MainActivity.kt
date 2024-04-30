@file:Suppress("SpellCheckingInspection")

package ru.vendetti.lethalcombile

import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private var auth: FirebaseAuth = Firebase.auth
    private var currentUser = auth.currentUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        //if (currentUser != null) {
        //reload()
        //} else {
        super.onCreate(savedInstanceState)
        setContent { LoginAppScreen() }
        //}
    }

    @Composable
    fun LoginAppScreen() {
        var errorText by remember { mutableStateOf("") }
        var text by remember { mutableStateOf("") }  // Состояние для хранения введённого текста
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            keyboardController?.show()
        }
        LethalCombileTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = LethalTerminalBack) {
                Box(
                    modifier = Modifier
                        .border(30.dp, LethalTerminalBorder)
                        .padding(30.dp)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text("[WELCOME]\n", color = LethalTerminalText, fontSize = 25.sp)
                        HorizontalDivider(color = LethalTerminalText)
                        Text(
                            "\nRegister or login before starting the game. You can do it by typing:",
                            color = LethalTerminalText,
                            fontSize = 20.sp
                        )
                        Text(
                            "register <emal> <password> <password>\n",
                            color = LethalTerminalTextDark,
                            fontSize = 16.sp
                        )
                        Text(
                            "login <email> <password>\n",
                            color = LethalTerminalTextDark,
                            fontSize = 16.sp
                        )
                        HorizontalDivider(color = LethalTerminalText)
                        Text(
                            "\nAll available commands:",
                            color = LethalTerminalText,
                            fontSize = 20.sp
                        )
                        Text(
                            "register\nlogin\nlogout\nexit",
                            color = LethalTerminalTextDark,
                            fontSize = 16.sp
                        )
                        Text(
                            "\nYou can type your command right below:",
                            color = LethalTerminalText,
                            fontSize = 20.sp
                        )
                        Text(errorText, color = LethalTerminalRed, fontSize = 20.sp)
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            textStyle = TextStyle(color = LethalTerminalText, fontSize = 20.sp),
                            cursorBrush = SolidColor(LethalTerminalText),
                            modifier = Modifier
                                .padding(top = 30.dp)
                                .fillMaxSize(),
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
            "exit" -> exitProcess(-1)
            "login" -> checkLogin(text, setErrorText)
            "register" -> checkRegister(text, setErrorText)
            "logout" -> logout(setErrorText)
            else -> {
                setErrorText("This command doesn't exist!")
            }
        }
    }

    private fun checkLogin(text: String, setErrorText: (String) -> Unit) {
        val parts = text.split(" ")
        if (parts.size == 3) {
            if (isValidEmail(parts[1]) && parts[2].length > 5) {
                setErrorText("Logging in...")
                login(parts[1], parts[2], setErrorText)
            } else {
                setErrorText("Incorrect email/passwd (len must be > 5 chars)")
            }
        } else {
            setErrorText("Wrong syntax")
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
            }
        } else {
            setErrorText("Wrong syntax")
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
                } else {
                    setErrorText("Register error")
                }
            }
    }

    private fun login(email: String, password: String, setErrorText: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    currentUser = auth.currentUser
                    setErrorText("Successful")
                    //reload()
                    //updateUI(user)
                } else {
                    setErrorText("Login error")
                }
            }
    }

    private fun logout(setErrorText: (String) -> Unit) {
        if (currentUser == null) {
            setErrorText("You're not logged in")
        } else {
            Firebase.auth.signOut()
            currentUser = auth.currentUser
            setErrorText("Logged out")
        }
    }
}