package no.nav.familie.ba.sak.config

import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.protocol.User
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.NestedExceptionUtils

@Configuration
class SentryConfiguration(
    @Value("\${sentry.environment}") val environment: String,
    @Value("\${sentry.dsn}") val dsn: String,
    @Value("\${sentry.logging.enabled}") val enabled: Boolean,
) {
    init {
        Sentry.init { options ->
            options.dsn = if (enabled) dsn else "" // Tom streng betryr at Sentry er disabled
            options.environment = environment
            options.beforeSend =
                SentryOptions.BeforeSendCallback { event, _ ->
                    Sentry.configureScope { scope ->
                        scope.user =
                            User().apply {
                                id = SikkerhetContext.hentSaksbehandler()
                                email = SikkerhetContext.hentSaksbehandlerEpost()
                                username = SikkerhetContext.hentSaksbehandler()
                            }
                    }

                    val mostSpecificThrowable =
                        if (event.throwable != null) NestedExceptionUtils.getMostSpecificCause(event.throwable!!) else event.throwable
                    val metodeSomFeiler = finnMetodeSomFeiler(mostSpecificThrowable)
                    val prosess = MDC.get("prosess")

                    event.setTag("metodeSomFeiler", metodeSomFeiler)
                    event.setTag("bruker", SikkerhetContext.hentSaksbehandlerEpost())
                    event.setExtra("logAzUrl", hentKibanalenke(MDC.get("callId")))
                    event.setExtra("grafanaUrl", hentGrafanaUrl(MDC.get("callId")))
                    event.setTag("prosess", prosess)

                    event.fingerprints =
                        listOf(
                            "{{ default }}",
                            prosess,
                            event.transaction,
                            mostSpecificThrowable?.message,
                        )

                    if (metodeSomFeiler != UKJENT_METODE_SOM_FEILER) {
                        event.fingerprints = (event.fingerprints ?: emptyList()) +
                            listOf(
                                metodeSomFeiler,
                            )
                    }

                    event
                }
        }
    }

    private fun hentKibanalenke(callId: String) = "https://logs.az.nav.no/app/data-explorer/discover?security_tenant=navlogs#?_g=(time:(from:now-2w,to:now))&_q=(filters:!(('\$state':(store:appState),meta:(key:x_callId,params:(query:'$callId'),type:phrase),query:(match_phrase:(x_callId:'$callId'))),('\$state':(store:appState),meta:(key:application,params:(query:familie-ba-sak),type:phrase),query:(match_phrase:(application:familie-ba-sak)))))"

    private fun hentGrafanaUrl(callId: String): String = "https://grafana.nav.cloud.nais.io/d/dsaSDAsadsaDSA/baks-logs?orgId=1&from=now-14d&to=now&timezone=Europe%2FOslo&var-cluster=PD969E40991D5C4A8&var-app=familie-ba-sak&var-level=\$__all&var-content=&var-Filters=callId%7C%3D%7C$callId"

    fun finnMetodeSomFeiler(e: Throwable?): String {
        val firstElement =
            e?.stackTrace?.firstOrNull {
                it.className.startsWith("no.nav.familie.ba.sak") &&
                    !it.className.contains("$")
            }
        if (firstElement != null) {
            val className = firstElement.className.split(".").lastOrNull()
            return "$className::${firstElement.methodName}(${firstElement.lineNumber})"
        }
        return e?.cause?.let { finnMetodeSomFeiler(it) } ?: UKJENT_METODE_SOM_FEILER
    }

    companion object {
        val logger = LoggerFactory.getLogger(SentryConfiguration::class.java)
        const val UKJENT_METODE_SOM_FEILER = "(Ukjent metode som feiler)"
    }
}
