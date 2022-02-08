package no.nav.familie.ba.sak.config

import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.protocol.User
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.NestedExceptionUtils

@Configuration
class SentryConfiguration {
    init {
        Sentry.init { options ->
            options.dsn = "https://dd9a6107bdda4edeb51ece7283f37af4@sentry.gc.nav.no/112"
            options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                Sentry.configureScope { scope ->
                    scope.user = User().apply {
                        email = SikkerhetContext.hentSaksbehandlerEpost()
                        username = SikkerhetContext.hentSaksbehandler()
                    }
                }

                val mostSpecificThrowable =
                    if (event.throwable != null) NestedExceptionUtils.getMostSpecificCause(event.throwable!!) else event.throwable
                val metodeSomFeiler = finnMetodeSomFeiler(mostSpecificThrowable)

                event.setTag("metodeSomFeier", metodeSomFeiler)
                event.setTag("bruker", SikkerhetContext.hentSaksbehandlerEpost())
                event.fingerprints = listOf(
                    "{{ default }}",
                    event.transaction,
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
