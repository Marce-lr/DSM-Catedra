package com.example.asistenteestudiantil.utils

import com.example.asistenteestudiantil.models.CalendarEvent
import com.example.asistenteestudiantil.models.News
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("news")
    suspend fun getNews(): Response<List<News>>
    
    @GET("calendar/events")
    suspend fun getCalendarEvents(): Response<List<CalendarEvent>>
}

