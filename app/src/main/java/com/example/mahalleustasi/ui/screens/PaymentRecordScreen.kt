package com.example.mahalleustasi.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mahalleustasi.data.model.Payment
import com.example.mahalleustasi.ui.viewmodel.PaymentsViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mahalleustasi.ui.viewmodel.JobDetailViewModel

@Composable
fun PaymentRecordScreen(navController: NavController, jobId: String, paymentsViewModel: PaymentsViewModel) {
    var amount by remember { mutableStateOf("") }
    var tip by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    // İşi Hilt VM üzerinden yükle ve assignedProId'yi kullan
    val jobDetailViewModel: JobDetailViewModel = hiltViewModel()
    val jobState by jobDetailViewModel.job.collectAsState()
    var assignedProId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(jobId) {
        jobDetailViewModel.load(jobId)
        paymentsViewModel.loadForJob(jobId)
    }
    LaunchedEffect(jobState) {
        assignedProId = jobState?.assignedProId
    }

    val payments by paymentsViewModel.payments.collectAsState()
    val currentUid = Firebase.auth.currentUser?.uid

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Elden Ödeme Kaydı: $jobId")
        if (assignedProId.isNullOrBlank()) {
            Text("Uyarı: Bu iş için atanmış usta bulunamadı. Önce teklifi kabul ederek işi atayın.", modifier = Modifier.padding(top = 8.dp))
        }

        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Tutar") }, modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(value = tip, onValueChange = { tip = it }, label = { Text("Bahşiş (opsiyonel)") }, modifier = Modifier.padding(top = 8.dp))

        Button(onClick = {
            error = null
            info = null
            val amt = amount.toDoubleOrNull()
            if (amt == null || amt <= 0.0) {
                error = "Geçerli bir tutar giriniz."
                return@Button
            }
            val payee = assignedProId
            if (payee.isNullOrBlank()) {
                error = "Atanmış usta bulunamadı."
                return@Button
            }

            val payment = Payment(
                jobId = jobId,
                payerId = "", // repo currentUser ile doldurur
                payeeId = payee,
                amount = amt,
                tip = tip.toDoubleOrNull()
            )
            paymentsViewModel.recordCash(payment) {
                info = "Ödeme kaydedildi."
                // Payer tarafından otomatik onaylayalım
                // Not: Payee tarafı uygulamasından ayrıca onaylayabilir
                // Hata olursa sessiz geçer, UI'ı basit tuttuk
            }
        }, modifier = Modifier.padding(top = 12.dp), enabled = !assignedProId.isNullOrBlank()) {
            Text("Kaydet")
        }

        error?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        info?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }

        // Mevcut ödemeler ve onaylar
        Text("Kayıtlı Ödemeler", modifier = Modifier.padding(top = 16.dp))
        payments.forEach { p ->
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Tutar: ${p.amount} | Bahşiş: ${p.tip ?: 0.0}")
                Text("Ödeyen onayı: ${p.confirmedByPayer} | Alan onayı: ${p.confirmedByPayee}")
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    val canPayerConfirm = currentUid != null && p.payerId == currentUid && !p.confirmedByPayer
                    val canPayeeConfirm = currentUid != null && p.payeeId == currentUid && !p.confirmedByPayee
                    Button(onClick = { paymentsViewModel.confirm(p.id, byPayer = true) }, enabled = canPayerConfirm) { Text("Ben Ödedim") }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(onClick = { paymentsViewModel.confirm(p.id, byPayee = true) }, enabled = canPayeeConfirm) { Text("Aldım") }
                }
            }
        }
    }
}
