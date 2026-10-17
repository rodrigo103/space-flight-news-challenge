package com.example.myandroidapp.data.remote

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Unit test del [HttpErrorCallAdapterFactory]: verifica el contrato de `get(...)` — devuelve un
 * adaptador solo cuando el tipo de retorno es `Call<T>` y `null` en caso contrario. JVM puro:
 * solo reflexión y tipos.
 *
 * **Dobles usados:** ninguno. El `ParameterizedType` anónimo es un dato de prueba (construye el
 * tipo `Call<String>` a mano) y el `Retrofit` real solo cubre el tercer parámetro de `get`, que el
 * factory no usa (dummy en el sentido clásico, pero no un test double).
 *
 * **Estilos (state/behavioral):** 100% STATE — se assertea el valor devuelto por el factory
 * (adaptador no nulo / nulo).
 */
class HttpErrorCallAdapterTest {

    // STATE — el estímulo es el tipo `Call<String>` construido a mano; se assertea el valor de
    // retorno del factory (no nulo). Sin dobles ni interacciones.
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

    // STATE — caso negativo del mismo contrato: un tipo que no es `Call` produce `null`. Se
    // assertea el valor de retorno, sin verificar cómo se decidió.
    @Test
    fun `factory returns null for non-Call return type`() {
        val adapter = HttpErrorCallAdapterFactory()
            .get(
                String::class.java,
                emptyArray(),
                Retrofit.Builder().baseUrl("http://localhost/").build()
            )

        assertNull(adapter)
    }
}
