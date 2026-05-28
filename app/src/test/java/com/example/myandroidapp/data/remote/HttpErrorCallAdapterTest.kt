package com.example.myandroidapp.data.remote

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class HttpErrorCallAdapterTest {

    @Test
    fun `factory returns adapter for Call return type`() {
        val returnType = object : ParameterizedType {
            override fun getRawType() = Call::class.java
            override fun getActualTypeArguments() = arrayOf<Type>(String::class.java)
            override fun getOwnerType(): Type? = null
        }

        val adapter = HttpErrorCallAdapterFactory()
            .get(returnType, emptyArray(), Retrofit.Builder().baseUrl("http://localhost/").build())

        assertNotNull(adapter)
    }

    @Test
    fun `factory returns null for non-Call return type`() {
        val adapter = HttpErrorCallAdapterFactory()
            .get(String::class.java, emptyArray(), Retrofit.Builder().baseUrl("http://localhost/").build())

        assertNull(adapter)
    }
}
