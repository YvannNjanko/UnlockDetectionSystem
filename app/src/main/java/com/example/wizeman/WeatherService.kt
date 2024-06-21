package com.example.wizeman

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.io.IOException

data class WeatherResponse(val city: City, val forecast: List<ForecastDay>)
data class City(val name: String, val cp: Int, val latitude: Float, val longitude: Float)
data class ForecastDay(val day: Int, val datetime: String, val tmin: Int, val tmax: Int, val weather: Int)

class WeatherService(private val token: String) {

    private val client = OkHttpClient()
    private val gson = Gson()

    fun getWeather(latitude: Double, longitude: Double): WeatherResponse? {
        val url = "https://api.meteo-concept.com/api/forecast/daily?token=$token"
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            return if (response.isSuccessful) {
                val responseBody = response.body?.string()
                gson.fromJson(responseBody, WeatherResponse::class.java)
            } else {
                null
            }
        }
    }
}
