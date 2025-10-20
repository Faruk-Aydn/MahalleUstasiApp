package com.example.mahalleustasi.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mahalleustasi.data.model.Post
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.platform.LocalContext

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun home_shows_quick_action_buttons_and_posts() {
        val sample = listOf(Post(id = "p1", title = "Başlık", description = "Açıklama"))

        composeRule.setContent {
            val navController = TestNavHostController(LocalContext.current)
            HomeScreenContent(navController = navController, posts = sample)
        }

        composeRule.onNodeWithText("Profil").assertIsDisplayed()
        composeRule.onNodeWithText("İşler").assertIsDisplayed()
        composeRule.onNodeWithText("Tekliflerim").assertIsDisplayed()
        composeRule.onNodeWithText("Ayarlar").assertIsDisplayed()
        composeRule.onNodeWithText("Başlık").assertIsDisplayed()
        composeRule.onNodeWithText("Açıklama").assertIsDisplayed()
    }
}
