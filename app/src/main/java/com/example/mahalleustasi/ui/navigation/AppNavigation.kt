package com.example.mahalleustasi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mahalleustasi.ui.screens.*
import com.example.mahalleustasi.ui.viewmodel.HomeViewModel
import com.example.mahalleustasi.ui.viewmodel.JobsViewModel
import com.example.mahalleustasi.ui.viewmodel.OffersViewModel
import com.example.mahalleustasi.ui.viewmodel.PaymentsViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("register") {
            RegisterScreen(navController = navController)
        }
        composable("home") {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(navController = navController, viewModel = homeViewModel)
        }
        
        // Yeni MVP rotaları
        composable("profile") {
            ProfileScreen(navController = navController)
        }
        composable("jobs") {
            val jobsViewModel: JobsViewModel = hiltViewModel()
            JobsListScreen(navController = navController, viewModel = jobsViewModel)
        }
        composable("my_jobs") {
            MyJobsScreen(navController = navController)
        }
        composable("job_create") {
            val jobsViewModel: JobsViewModel = hiltViewModel()
            JobCreateScreen(navController = navController, viewModel = jobsViewModel)
        }
        composable("job_detail/{jobId}") { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            if (jobId != null) {
                val offersViewModel: OffersViewModel = hiltViewModel()
                JobDetailScreen(
                    navController = navController,
                    jobId = jobId,
                    offersViewModel = offersViewModel
                )
            }
        }
        composable("offers") {
            val offersViewModel: OffersViewModel = hiltViewModel()
            OffersScreen(navController = navController, viewModel = offersViewModel)
        }
        composable("payment_record/{jobId}") { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId")
            if (jobId != null) {
                val paymentsViewModel: PaymentsViewModel = hiltViewModel()
                PaymentRecordScreen(
                    navController = navController,
                    jobId = jobId,
                    paymentsViewModel = paymentsViewModel
                )
            }
        }
        composable("location_picker") {
            LocationPickerScreen(navController = navController)
        }
        composable("chat/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
            if (chatId != null) {
                ChatScreen(navController = navController, chatId = chatId)
            }
        }
        composable("notifications") {
            NotificationsScreen(navController = navController)
        }
        composable("reviews/{revieweeId}/{jobId}") { backStackEntry ->
            val revieweeId = backStackEntry.arguments?.getString("revieweeId")
            val jobId = backStackEntry.arguments?.getString("jobId")
            if (revieweeId != null) {
                ReviewsScreen(navController = navController, revieweeId = revieweeId, jobId = jobId)
            }
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}

