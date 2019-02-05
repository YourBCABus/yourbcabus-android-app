package com.yourbcabus.android.yourbcabus

typealias Observer = (Any?) -> Unit

interface EventEmitter {
    val observers: MutableMap<String, MutableList<Observer>>
    val onceObservers: MutableMap<String, MutableList<Observer>>

    fun on(event: String, observer: Observer) {
        if (observers[event] == null) {
            observers[event] = ArrayList<Observer>()
        }

        observers.getValue(event).add(observer)
    }

    fun once(event: String, observer: Observer) {
        if (onceObservers[event] == null) {
            onceObservers[event] = ArrayList<Observer>()
        }

        onceObservers.getValue(event).add(observer)
    }

    fun off(event: String, observer: Observer) {
        if (observers[event] != null) {
            observers.getValue(event).removeAll { it === observer }
        }
    }

    fun offOnce(event: String, observer: Observer) {
        if (onceObservers[event] != null) {
            onceObservers.getValue(event).removeAll { it === observer }
        }
    }

    fun emit(event: String, argument: Any?) {
        if (observers[event] != null) {
            observers.getValue(event).forEach { it(argument) }
        }

        if (onceObservers[event] != null) {
            onceObservers.getValue(event).forEach{ it(argument) }
            onceObservers.remove(event)
        }
    }
}