package com.example.mahalleustasi.ui.viewmodel

import com.example.mahalleustasi.data.model.Offer
import com.example.mahalleustasi.data.repository.OffersRepositoryContract
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Rule
import com.example.mahalleustasi.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class OffersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadMyOffers updates offers state`() = runTest {
        val fakeOffers = listOf(
            Offer(id = "o1", jobId = "j1", proId = "u1", amount = 100.0),
            Offer(id = "o2", jobId = "j2", proId = "u1", amount = 200.0)
        )
        val repo = mockk<OffersRepositoryContract>()
        coEvery { repo.listOffersByPro() } returns fakeOffers

        val vm = OffersViewModel(repo)
        vm.loadMyOffers()
        advanceUntilIdle()

        assertEquals(2, vm.offers.value.size)
        assertEquals("o1", vm.offers.value.first().id)
    }
}
