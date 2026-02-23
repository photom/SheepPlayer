package com.hitsuji.sheepplayer2.domain.usecase

import com.hitsuji.sheepplayer2.fakes.FakeArtistImageRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchArtistImagesUseCaseTest {

    private lateinit var fakeRepository: FakeArtistImageRepository
    private lateinit var useCase: SearchArtistImagesUseCase

    @Before
    fun setup() {
        fakeRepository = FakeArtistImageRepository()
        useCase = SearchArtistImagesUseCase(fakeRepository)
    }

    @Test
    fun `invoke returns list of urls from repository`() = runBlocking {
        val urls = listOf("url1", "url2")
        fakeRepository.addSearchResult("Artist", urls)
        
        val result = useCase("Artist")
        
        assertEquals(urls, result)
    }

    @Test
    fun `invoke returns empty list for unknown artist`() = runBlocking {
        val result = useCase("Unknown")
        assertEquals(0, result.size)
    }
}
