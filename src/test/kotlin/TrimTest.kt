import org.junit.Assert
import org.junit.Test
import typed.rocks.witt.trimmedText

class TrimTest {
    @Test
    fun run() {
        Assert.assertEquals("Test   another".trimmedText(10), "Test   ...")

        Assert.assertEquals("{a: string;     b: number}".trimmedText(100), "{a: string;  b: number}")
        Assert.assertEquals("Test   another".trimmedText(100), "Test   another")
    }
}