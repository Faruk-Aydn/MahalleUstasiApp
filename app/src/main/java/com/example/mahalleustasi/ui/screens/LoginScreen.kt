package com.example.mahalleustasi.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mahalleustasi.ui.viewmodel.AuthViewModel
import com.example.mahalleustasi.ui.viewmodel.ProfileGateViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.mahalleustasi.R
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun AuthGateScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    gateVm: ProfileGateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.firebaseUser.collectAsState()
    val needsProfile by gateVm.needsProfile.collectAsState(null)
    var navigated by remember { mutableStateOf(false) }

    // Kullanıcı yoksa login ekranına gönder
    LaunchedEffect(currentUser) {
        if (!navigated) {
            if (currentUser == null) {
                navigated = true
                navController.navigate("login") {
                    popUpTo(0)
                }
            } else {
                // Kullanıcı var, profil durumu kontrol edilsin
                gateVm.check()
            }
        }
    }

    // Profil gerekiyor mu bilgisine göre home veya profile'a yönlendir
    LaunchedEffect(needsProfile) {
        if (!navigated && currentUser != null && needsProfile != null) {
            navigated = true
            if (needsProfile == true) {
                navController.navigate("profile") {
                    popUpTo(0)
                }
            } else {
                navController.navigate("home") {
                    popUpTo(0)
                }
            }
        }
    }

    // Sadece basit bir loading ekranı göster
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val currentUser by viewModel.firebaseUser.collectAsState()
    val error by viewModel.error.collectAsState(null)
    val loading by viewModel.loading.collectAsState(false)
    val gateVm: ProfileGateViewModel = hiltViewModel()
    val needsProfile by gateVm.needsProfile.collectAsState(null)
    var navigated by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            gateVm.check()
        }
    }

    LaunchedEffect(needsProfile) {
        if (needsProfile != null && !navigated) {
            navigated = true
            if (needsProfile == true) {
                navController.navigate("profile") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(error) {
        error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    // Google Sign-In launcher
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.loginWithGoogleIdToken(idToken)
            }
        } catch (_: Exception) { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Email/Şifre giriş alanları
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-posta") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    viewModel.login(email, password)
                } else {
                    Toast.makeText(context, "E-posta ve şifre zorunludur.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) { Text(if (loading) "Giriş Yapılıyor..." else "Giriş Yap") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { navController.navigate("register") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) { Text("Hesabın yok mu? Kayıt Ol") }
        Spacer(modifier = Modifier.height(16.dp))
        // Google ile giriş
        Button(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, gso)
                googleLauncher.launch(client.signInIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading
        ) { Text("Google ile Giriş Yap") }
    }
}