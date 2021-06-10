/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.json

@FunctionalInterface
interface FromJsonMapper {
    fun <T> map(json: String, targetClass: Class<T>): T
}

@FunctionalInterface
interface ToJsonMapper {
    fun map(obj: Any): String
}

object JavalinJson {

    @JvmStatic
    var fromJsonMapper = JavalinJackson.defaultFromMapper

    @JvmStatic
    var toJsonMapper = JavalinJackson.defaultToMapper

    @JvmStatic
    fun toJson(obj: Any) = toJsonMapper.map(obj)

    @JvmStatic
    fun <T> fromJson(json: String, targetClass: Class<T>) = fromJsonMapper.map(json, targetClass)

}
