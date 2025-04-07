package com.example.gps

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import android.content.Context.MODE_PRIVATE


class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuthScreen {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isLogin) "Login" else "Sign Up",
                style = MaterialTheme.typography.h4
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthForm(isLogin, onAuthSuccess)
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { isLogin = !isLogin }) {
                Text(if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Login")
            }
        }
    }
}

@Composable
fun AuthForm(isLogin: Boolean, onAuthSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current  // Obtendo o contexto

    val dbHelper = remember { MemoryDatabaseHelper(context) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                isLoading = true

                // For Login, verify against the database; for Signup, try to insert a new user.
                if (isLogin) {
                    if (dbHelper.checkUserCredential(email, password)) {
                        dbHelper.updateCurrentUser(email)

                        // Salvar o email nas preferências compartilhadas após o login
                        context.getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                            .putString("userEmail", email)
                            .apply()

                        onAuthSuccess()
                    } else {
                        Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val inserted = dbHelper.insertUserCredential(email, password)
                    if (inserted == -1L) {
                        Toast.makeText(context, "Signup failed: User already exists", Toast.LENGTH_SHORT).show()
                    } else {
                        dbHelper.updateCurrentUser(email)

                        // Salvar o email nas preferências compartilhadas após o cadastro
                        context.getSharedPreferences("AppPrefs", MODE_PRIVATE).edit()
                            .putString("userEmail", email)
                            .apply()

                        onAuthSuccess()
                    }
                }
                isLoading = false
            }) {
                Text(if (isLogin) "Login" else "Sign Up")
            }
        }
    }
}

