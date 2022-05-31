package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.kjerne.brev.BrevKlient
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.task.OpprettTaskService.Companion.RETRY_BACKOFF_5000MS
import org.springframework.core.env.Environment
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class SanityService(
    private val cachedSanityKlient: CachedSanityKlient,
    private val brevKlient: BrevKlient,
    private val environment: Environment
) {

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentSanityBegrunnelser(): List<SanityBegrunnelse> = cachedSanityKlient.hentSanityBegrunnelserCached()

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS),
    )
    fun hentSanityEØSBegrunnelser(): List<SanityEØSBegrunnelse> =
        cachedSanityKlient.hentEØSBegrunnelserCached()
}
