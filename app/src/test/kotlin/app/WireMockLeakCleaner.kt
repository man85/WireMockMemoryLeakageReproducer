package app

import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy

/**
 * DISABLED in this reproducer: uncomment @Import in the tests — the leak disappears, the build is green.
 */
class WireMockLeakCleaner : DisposableBean {
    override fun destroy() {
        runCatching {
            val wireMockClass = Class.forName("com.github.tomakehurst.wiremock.client.WireMock")
            val defaultInstanceField = wireMockClass.getDeclaredField("defaultInstance")
            defaultInstanceField.isAccessible = true
            val inheritableThreadLocal = defaultInstanceField[null] as ThreadLocal<*>

            val inheritableThreadLocalsField = Thread::class.java.getDeclaredField("inheritableThreadLocals")
            inheritableThreadLocalsField.isAccessible = true
            val mapRemoveMethod = Class.forName("java.lang.ThreadLocal\$ThreadLocalMap")
                .getDeclaredMethod("remove", ThreadLocal::class.java)
            mapRemoveMethod.isAccessible = true

            Thread.getAllStackTraces().keys.forEach { thread ->
                runCatching {
                    val map = inheritableThreadLocalsField.get(thread) ?: return@runCatching
                    mapRemoveMethod.invoke(map, inheritableThreadLocal)
                }
            }
        }.onFailure { e ->
            System.err.println("Failed to clear WireMock defaultInstance ThreadLocal: ${e.message}")
        }
    }
}

@TestConfiguration
open class WireMockLeakFixConfig {
    @Bean
    @Lazy(false)
    open fun wireMockLeakCleaner() = WireMockLeakCleaner()
}
