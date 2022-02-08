package no.nav.familie.ba.sak.config

import io.sentry.Sentry
import io.sentry.SentryOptions
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.NestedExceptionUtils

@Configuration
class SentryConfiguration {
    init {
        Sentry.init { options ->
            options.beforeSend = SentryOptions.BeforeSendCallback { event, hint ->
                val mostSpecificThrowable =
                    if (event.throwable != null) NestedExceptionUtils.getMostSpecificCause(event.throwable!!) else event.throwable
                val metodeSomFeiler = finnMetodeSomFeiler(mostSpecificThrowable)
                logger.info("Sentry før sending: ${hint.toString()}, ${metodeSomFeiler}, ${event.message}")
                logger.info(mostSpecificThrowable?.message, mostSpecificThrowable)
                logger.info(event.throwable?.message, event.throwable)

                event.fingerprints = listOf(
                    "{{ default }}",
                    mostSpecificThrowable?.message,
                    metodeSomFeiler,
                )
                event
            }
        }
    }

    fun finnMetodeSomFeiler(e: Throwable?): String {
        val firstElement = e?.stackTrace?.firstOrNull {
            it.className.startsWith("no.nav.familie.ba.sak") &&
                !it.className.contains("$")
        }
        if (firstElement != null) {
            val className = firstElement.className.split(".").lastOrNull()
            return "$className::${firstElement.methodName}(${firstElement.lineNumber})"
        }
        return e?.cause?.let { finnMetodeSomFeiler(it) } ?: "(Ukjent metode som feiler)"
    }

    companion object {
        val logger = LoggerFactory.getLogger(SentryConfiguration::class.java)
    }
}