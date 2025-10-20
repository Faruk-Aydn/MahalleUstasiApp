package com.example.mahalleustasi.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mahalleustasi.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    val context = LocalContext.current
    val currentUser by viewModel.firebaseUser.collectAsState()
    val error by viewModel.error.collectAsState(null)
    val loading by viewModel.loading.collectAsState(false)

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate("home") {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(error) {
        error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ad Soyad") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-posta") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Şifre") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank() && name.isNotBlank() && !loading) {
                    viewModel.register(email, password, name) {
                        Toast.makeText(context, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank() && password.isNotBlank() && name.isNotBlank() && !loading
        ) {
            Text("Hesap Oluştur")
        }
    }
}