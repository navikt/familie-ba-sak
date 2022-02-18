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
) {
    init {
        Sentry.init { options ->
            options.dsn = dsn
            options.environment = environment
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
                event.setTag("kibanalenke", hentKibanalenke(environment, MDC.get("callId")))

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

    private fun hentKibanalenke(miljø: String, callId: String) =
        "https://logs.adeo.no/app/discover#/view/?_g=(refreshInterval:(pause:!t, value :0), " +
            "time:(from:now-12h,to:now))&_a=(columns:!(message,envclass,environment,level,application,host)," +
            "filters:!(('\$state':(store:appState),meta:(alias:!n,disabled:!f," +
            "index:'96e648c0-980a-11e9-830a-e17bbd64b4db',key:envclass,negate:!f,params:(query:p)," +
            "type:phrase),query:(match_phrase:(envclass:$miljø))),('\$state':(store:appState)," +
            "meta:(alias:!n,disabled:!f,index:'96e648c0-980a-11e9-830a-e17bbd64b4db'," +
            "key:x_callId,negate:!f,params:(query:'65c60464-b295-4421-ba74-18075f0d4e25'),type:phrase)," +
            "query:(match_phrase:(x_callId:'$callId'))))," +
            "index:'logstash-*',interval:auto,query:(language:lucene,query:''),sort:!(!('@timestamp',desc)))"

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
