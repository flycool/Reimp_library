package com.compose.sample.reimp_library.eventbus

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.jvmErasure

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subscribe

object SimpleEventBus {

    private val registerMap = HashMap<KClass<*>, MutableList<Pair<Any, KFunction<*>>>>()

    fun register(subscriber: Any) {
        subscriber::class.declaredFunctions
            .filter {
                it.annotations.any { it is Subscribe } && it.parameters.size == 2 &&
                        it.returnType.jvmErasure == Unit::class
            }
            .forEach {
                val eventType = it.parameters[1].type.jvmErasure
                if (eventType !in registerMap) {
                    registerMap[eventType] = arrayListOf()
                }
                registerMap[eventType]?.add(subscriber to it)
            }
    }

    fun post(event: Any) {
        registerMap[event::class]?.forEach {
            it.second.call(it.first, event)
        }
    }
}

/**
 * //in Activity
 *
 * SimpleEventBus.register(this)
 *
 * button.setOnClickListener {
 *      SimpleEventBus.post("hi max")
 * }
 *
 * @Subscribe
 * fun onEvent(event:String) {
 *
 * }
 *
 */