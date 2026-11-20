package app

import com.github.tomakehurst.wiremock.WireMockServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext

/**
 * Second class (after the first context is closed): verifies that the WireMockServer
 * of the first context is collected by GC. Without clearing WireMock.defaultInstance it is NOT collected:
 * the JUnit worker thread holds it via its inheritableThreadLocals (InheritableThreadLocal
 * is also copied into every new thread created on the worker thread).
 */
// Uncomment to verify the fix (with WireMockLeakCleaner the build is green):
//@Import(WireMockLeakFixConfig::class)
@SpringBootTest
@AutoConfigureWireMock(port = 0)
@DirtiesContext
class SecondWireMockTest(
    @Autowired val server: WireMockServer,
) {

    @BeforeEach
    fun `closed wiremock server must be collected`() {
        System.gc()
        System.gc()
        Thread.sleep(100)
        val leaked = LeakHolder.firstServer?.get()
        check(leaked == null) {
            "REPRODUCED: WireMockServer of the closed context is retained after GC " +
                "(static WireMock.defaultInstance InheritableThreadLocal on the JUnit worker thread): $leaked"
        }
    }

    @Test
    fun `second wiremock server is up`() {
        check(server.isRunning)
    }
}
