
import com.github.hatoyuze.protocol.api.PhigrosApiImpl
import com.github.hatoyuze.protocol.api.PhigrosUser
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PhigrosApiTest {
    private val sessionToken = ""

    @Test
    fun testData() {
        runBlocking {
            println(
                PhigrosApiImpl.userData(sessionToken)
            )
        }
    }

    @Test
    fun playData() {
        runBlocking {
            println(
                PhigrosApiImpl.entryData(sessionToken).savePlayScore
            )
        }
    }

    @Test
    fun testPlaySave() {
        runBlocking {
            println(PhigrosApiImpl.playSaveImpl(sessionToken))
        }
    }


    @Test
    fun testUserSave() {
        runBlocking {
            println(
                PhigrosUser(sessionToken).playScore
            )
        }
    }

}