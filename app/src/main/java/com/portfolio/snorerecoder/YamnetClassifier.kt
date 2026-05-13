package com.portfolio.snorerecoder

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class YamnetClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null

    val SAMPLE_RATE = 16000
    val INPUT_SIZE = 15600

    init {
        interpreter = Interpreter(loadModelFile())
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fd = context.assets.openFd("yamnet.tflite")
        val inputStream = FileInputStream(fd.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }

    fun classify(audioBuffer: FloatArray): ClassificationResult {
        val scores = Array(1) { FloatArray(521) }
        val input = Array(1) { audioBuffer.copyOf(INPUT_SIZE) }
        interpreter?.run(input, scores)

        val topIndex = scores[0].indices.maxByOrNull { scores[0][it] } ?: 0
        val topScore = scores[0][topIndex]
        val label = YAMNET_LABELS.getOrElse(topIndex) { "Unknown" }
        val category = mapToCategory(label)

        return ClassificationResult(label, category, topScore)
    }

    data class ClassificationResult(
        val label: String,
        val category: SoundCategory,
        val confidence: Float
    )

    enum class SoundCategory(val emoji: String, val displayName: String) {
        SNORING("😴", "Snoring"),
        BREATHING("🫁", "Breathing"),
        SPEECH("🗣️", "Speech / Voice"),
        ANIMAL("🐾", "Animal Sound"),
        MUSIC("🎵", "Music"),
        NOISE("🔊", "Noise / Environment"),
        SILENCE("🔇", "Silence"),
        UNKNOWN("❓", "Unknown")
    }

    private fun mapToCategory(label: String): SoundCategory {
        val lower = label.lowercase()
        return when {
            lower.contains("snor") -> SoundCategory.SNORING
            lower.contains("breath") || lower.contains("wheez") ||
                    lower.contains("gasp") || lower.contains("exhale") ||
                    lower.contains("inhale") -> SoundCategory.BREATHING
            lower.contains("speech") || lower.contains("talk") ||
                    lower.contains("shout") || lower.contains("sing") -> SoundCategory.SPEECH
            lower.contains("dog") || lower.contains("cat") ||
                    lower.contains("bird") || lower.contains("bark") -> SoundCategory.ANIMAL
            lower.contains("music") || lower.contains("song") ||
                    lower.contains("drum") || lower.contains("guitar") -> SoundCategory.MUSIC
            lower.contains("silence") || lower.contains("quiet") -> SoundCategory.SILENCE
            lower.contains("noise") || lower.contains("static") ||
                    lower.contains("hum") -> SoundCategory.NOISE
            else -> SoundCategory.UNKNOWN
        }
    }

    fun close() {
        interpreter?.close()
    }

    companion object {
        val YAMNET_LABELS = listOf(
            "Speech", "Male speech, man speaking", "Female speech, woman speaking",
            "Child speech, kid speaking", "Conversation", "Narration, monologue",
            "Babbling", "Speech synthesizer", "Shout", "Bellow", "Whoop",
            "Yell", "Children shouting", "Screaming", "Whispering", "Laughter",
            "Baby laughter", "Giggle", "Snicker", "Belly laugh", "Chuckle, chortle",
            "Crying, sobbing", "Baby cry, infant cry", "Whimper", "Wail, moan",
            "Sigh", "Singing", "Choir", "Yodeling", "Chant", "Mantra",
            "Male singing", "Female singing", "Child singing", "Synthetic singing",
            "Rapping", "Humming", "Groan", "Grunt", "Beatboxing",
            "Whistling", "Breathing", "Wheeze", "Snore", "Snort",
            "Gasp", "Pant", "Sniff", "Run", "Shuffle",
            "Walk, footsteps", "Chew, bite", "Burp, eructation", "Hiccup",
            "Fart", "Hands", "Finger snapping", "Clapping",
            "Heart sounds, heartbeat", "Heart murmur", "Cheering", "Applause",
            "Chatter", "Crowd", "Hubbub, speech noise", "Children playing",
            "Animal", "Domestic animals, pets", "Dog", "Bark", "Yip",
            "Howl", "Bow-wow", "Growling", "Cat", "Purr", "Meow",
            "Hiss", "Horse", "Neigh, whinny", "Cattle, bovinae", "Moo",
            "Pig", "Oink", "Sheep", "Bleat", "Chicken, rooster", "Cluck",
            "Duck", "Quack", "Goose", "Honk", "Wild animals",
            "Noise", "Environmental noise", "Static", "Silence"
        )
    }
}