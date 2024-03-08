package com.compose.sample.reimp_library.butterknife

import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.IdRes
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class IdForView(@IdRes val id: Int)

fun bind(activity: ComponentActivity) {
    activity::class.declaredMemberProperties.asSequence()
        .filter { it is KMutableProperty1 && it.annotations.any { it is IdForView } }
        .forEach {
            val viewId = it.findAnnotation<IdForView>()?.id!!
            val view: View = activity.findViewById<View>(viewId)
            it as KMutableProperty1<ComponentActivity, View>
            it.isAccessible = true
            it.set(activity, view)
        }
}
