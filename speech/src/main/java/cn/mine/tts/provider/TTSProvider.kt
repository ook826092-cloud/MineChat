package cn.mine.tts.provider

import android.content.Context
import kotlinx.coroutines.flow.Flow
import cn.mine.tts.model.AudioChunk
import cn.mine.tts.model.TTSRequest

interface TTSProvider<T : TTSProviderSetting> {
    fun generateSpeech(
        context: Context,
        providerSetting: T,
        request: TTSRequest
    ): Flow<AudioChunk>
}
