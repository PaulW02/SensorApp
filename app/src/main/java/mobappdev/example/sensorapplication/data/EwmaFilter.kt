package mobappdev.example.sensorapplication.data

class EwmaFilter(alpha: Float) {
    private var alpha: Float = alpha.coerceIn(0f, 1f)
    private var output: Float = 0f

    fun filter(input: Float): Float {
        output = alpha * input + (1 - alpha) * output
        return output
    }
}
