package io.github.ieswar23.buddyup.data.remote

import io.github.ieswar23.buddyup.data.remote.dto.FriendshipDto
import io.github.ieswar23.buddyup.data.remote.dto.IncomingRequestDto
import io.github.ieswar23.buddyup.data.remote.dto.MeetupDto
import io.github.ieswar23.buddyup.data.remote.dto.PersonDto
import retrofit2.http.GET

interface BuddyUpApi {

    @GET("v1/people/nearby")
    suspend fun nearbyPeople(): List<PersonDto>

    @GET("v1/waves/incoming")
    suspend fun incomingWaves(): List<IncomingRequestDto>

    @GET("v1/friends")
    suspend fun friends(): List<FriendshipDto>

    @GET("v1/meetups")
    suspend fun meetups(): List<MeetupDto>

    companion object {
        const val BASE_URL = "https://api.buddyup.app/"
    }
}
