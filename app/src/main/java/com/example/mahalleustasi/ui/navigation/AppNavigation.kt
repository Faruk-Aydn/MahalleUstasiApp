package com.example.mahalleustasi.ui.navigation

import androidx.compose.animation.SharedTransitionLayout // <-- 1. IMPORT EKLENDİ
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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // 2. TÜM NavHost'u SharedTransitionLayout İLE SARMALADIK
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = "auth_gate",
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable("auth_gate") {
                AuthGateScreen(navController = navController)
            }
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

            // ... diğer rotalar ...
            composable("profile") {
                ProfileScreen(navController = navController)
            }

            // 3. JobsListScreen'E KAPSAMLAR (SCOPES) İLETİLDİ
            composable("jobs") {
                val jobsViewModel: JobsViewModel = hiltViewModel()
                JobsListScreen(
                    navController = navController,
                    viewModel = jobsViewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this
                )
            }
            composable("my_jobs") {
                MyJobsScreen(navController = navController)
            }
            composable("my_jobs_assigned") {
                MyJobsScreen(navController = navController, initialTab = 1)
            }
            composable("job_create") {
                val jobsViewModel: JobsViewModel = hiltViewModel()
                JobCreateScreen(navController = navController, viewModel = jobsViewModel)
            }

            // 4. JobDetailScreen'E KAPSAMLAR (SCOPES) İLETİLDİ
            composable("job_detail/{jobId}") { backStackEntry ->
                val jobId = backStackEntry.arguments?.getString("jobId")
                if (jobId != null) {
                    val offersViewModel: OffersViewModel = hiltViewModel()
                    JobDetailScreen(
                        navController = navController,
                        jobId = jobId,
                        offersViewModel = offersViewModel,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this
                    )
                }
            }

            // ... diğer rotalarınız ...
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
            composable("public_profile/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                if (!userId.isNullOrBlank()) {
                    PublicProfileScreen(navController = navController, userId = userId)
                }
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
}