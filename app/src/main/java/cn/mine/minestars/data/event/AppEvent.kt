package cn.mine.minestars.data.event

sealed class AppEvent {
    data class Speak(val text: String) : AppEvent()
}
