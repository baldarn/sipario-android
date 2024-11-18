package it.baldarn.sipario.model

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class User(
    var id: Int,
    var email: String,
    var name: String,
    var language: String,
    var password: String,
) {

    class Deserializer : ResponseDeserializable<Array<User>> {
        override fun deserialize(content: String): Array<User> =
            Gson().fromJson(content, Array<User>::class.java)
    }
}