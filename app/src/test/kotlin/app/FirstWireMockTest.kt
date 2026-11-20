package app

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.annotation.DirtiesContext
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

/**
 * First class: starts a context with @AutoConfigureWireMock.
 * In @PostConstruct spring-cloud-contract-wiremock calls WireMock.configureFor(server) —
 * this writes to the STATIC InheritableThreadLocal WireMock.defaultInstance on the JUnit worker thread.
 * Neither stop() nor context closing ever clears this value.
 */
// Uncomment to verify the fix (with WireMockLeakCleaner the build is green):
//@Import(WireMockLeakFixConfig::class)
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@DirtiesContext
class FirstWireMockTest(
    @Autowired val server: WireMockServer,
) {

    @Test
    fun `wiremock server is up`() {
        check(server.isRunning)
        LeakHolder.firstServer = WeakReference(server)
        // a long-lived thread
        // created on the JUnit worker thread WHILE this context is active. Thread.init()
        // copies WireMock.defaultInstance (InheritableThreadLocal) into its
        // inheritableThreadLocals — forever, the library has no remove API.
        thread(isDaemon = true, name = "long-lived-child-of-worker") {
            Thread.sleep(Long.MAX_VALUE)
        }
    }
}

object LeakHolder {
    var firstServer: WeakReference<WireMockServer>? = null
}
