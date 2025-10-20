package com.example.mahalleustasi.ui.viewmodel

import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.data.repository.JobsRepository
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
class JobsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh loads open jobs`() = runTest {
        val jobs = listOf(Job(id = "j1", title = "Musluk Tamiri"))
        val repo = mockk<JobsRepository>()
        coEvery { repo.listOpenJobs() } returns jobs

        val vm = JobsViewModel(repo)
        vm.refresh()
        advanceUntilIdle()

        assertEquals(1, vm.jobs.value.size)
        assertEquals("j1", vm.jobs.value.first().id)
    }
}
