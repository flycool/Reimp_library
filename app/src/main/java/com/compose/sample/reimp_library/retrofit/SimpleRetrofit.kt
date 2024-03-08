package com.compose.sample.reimp_library.retrofit

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GET(val baseUrl: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Query(val parameterName: String)

interface Call<out T> {
    fun execute(): T
}

/**
 *  [[2023-8-16 Let Kotlin do the code for you — Part II Retrofit and Proxy]]
 */
class SimpleRetrofit {
    val objectMapper: ObjectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val httpClient = OkHttpClient()

    private fun createRequest(method: Method, args: Array<Any?>): Request {
        val baseUrl = method.getAnnotation(GET::class.java).baseUrl
        val paramNames = method.parameterAnnotations
            .flatten().map { (it as Query).parameterName }
        val url = baseUrl.toHttpUrlOrNull()!!.newBuilder().apply {
            paramNames.forEachIndexed { index, paramName ->
                addQueryParameter(paramName, args[index].toString())
            }
        }.build()
        println("url:  $url")
        return Request.Builder().url(url).build()
    }

    fun extractResponseType(method: Method) =
        (method.genericReturnType as ParameterizedType).actualTypeArguments[0] as Class<*>

    private fun <T> createCall(request: Request, responseClass: Class<T>): Call<T> {
        return object : Call<T> {
            override fun execute(): T {
                val response = httpClient.newCall(request).execute().body.toString()
                return objectMapper.readValue(response, responseClass)
            }
        }
    }

    fun <T> createService(serviceClass: Class<T>): T = Proxy.newProxyInstance(
        serviceClass.classLoader,
        arrayOf(serviceClass)
    ) { thiz: Any, method: Method, args: Array<Any?> ->
        val request = createRequest(method, args)
        val responseType = extractResponseType(method)
        createCall(request, responseType)
    } as T
}


@Serializable
data class Main(val temp: Double)

@Serializable
data class Weather(val main: Main)

@Serializable
data class UvIndex(val value: Double)

interface OpenWeatherMapApi {

    @GET("http://samples.openweathermap.org/data/2.5/weather")
    fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String
    ): Call<Weather>

    @GET("http://samples.openweathermap.org/data/2.5/uvi")
    fun getUvIndex(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String
    ): Call<UvIndex>
}

@Serializable
class A(val name: String, val language: String) {

    @GET("http://www.baidu.com")
    fun getMethod(
        @Query("q") city: String,
        @Query("appid") apiKey: String
    ): Call<Weather> {
        return object : Call<Weather> {
            override fun execute(): Weather {
                return Weather(Main(1.0))
            }
        }
    }
}


fun main() {

    val service = SimpleRetrofit().createService(OpenWeatherMapApi::class.java)
    val weather = service.getWeather("lechange", "gksldjkd").execute()
    println(weather)


    val weatherData = """
        {"main":{"temp":3.4}}
    """.trimIndent()

    val methods = A::class.java.declaredMethods
    val method = methods.get(1)
    val responseType = SimpleRetrofit().extractResponseType(method)

    val objectMapper: ObjectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val value = objectMapper.readValue(weatherData, responseType)
    println(value)

//    val data = Weather(Main(3.4))
//    val s = Json.encodeToString(data)
//    println(s)
}


























