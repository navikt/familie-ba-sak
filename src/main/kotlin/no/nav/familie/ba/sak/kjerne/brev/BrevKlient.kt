package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseMedData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

val FAMILIE_BREV_TJENESTENAVN = "famile-brev"
private val log = LoggerFactory.getLogger(BrevKlient::class.java)

@Component
class BrevKlient(
    @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
    @Value("\${SANITY_DATASET}") private val sanityDataset: String,
    @Qualifier("utenAuthRestClient") private val restClient: RestClient,
    private val testVerktøyService: TestVerktøyService,
) {
    fun genererBrev(
        målform: String,
        brev: Brev,
    ): ByteArray {
        val uri = URI.create("$familieBrevUri/api/$sanityDataset/dokument/$målform/${brev.mal.apiNavn}/pdf")

        secureLogger.info("Kaller familie brev($uri) med data ${brev.data.toBrevString()}")
        return try {
            kallEksternTjeneste(FAMILIE_BREV_TJENESTENAVN, uri, "Hente pdf for vedtaksbrev") {
                restClient
                    .post()
                    .uri(uri)
                    .body(brev.data)
                    .retrieve()
                    .body()!!
            }
        } catch (exception: HttpClientErrorException.BadRequest) {
            log.warn("En bad request oppstod ved henting av pdf. Se SecureLogs for detaljer,")
            secureLogger.warn("En bad request oppstod ved henting av pdf", exception)
            throw FunksjonellFeil(
                "Det oppsto en feil ved generering av brev. Sjekk at begrunnelsene som er valgt er riktige og kontakt brukerstøtte hvis problemet vedvarer.",
            )
        }
    }

    @Cacheable("begrunnelsestekst", cacheManager = "shortCache")
    fun hentBegrunnelsestekst(
        begrunnelseData: BegrunnelseMedData,
        vedtaksperiode: VedtaksperiodeMedBegrunnelser,
    ): String {
        val behandlingId = vedtaksperiode.vedtak.behandling.id
        val uri = URI.create("$familieBrevUri/ba-sak/begrunnelser/${begrunnelseData.apiNavn}/tekst/")
        secureLogger.info("Kaller familie brev($uri) med data $begrunnelseData for behandlingId=$behandlingId")
        log.info("Henter begrunnelse ${begrunnelseData.apiNavn} på periode ${vedtaksperiode.fom} - ${vedtaksperiode.tom} for behandlingId=$behandlingId.")

        return try {
            kallEksternTjeneste(FAMILIE_BREV_TJENESTENAVN, uri, "Henter begrunnelsestekst") {
                restClient
                    .post()
                    .uri(uri)
                    .body(begrunnelseData)
                    .retrieve()
                    .body()!!
            }
        } catch (exception: HttpClientErrorException.BadRequest) {
            log.warn("En bad request oppstod ved henting av begrunneelsetekst for behandlingId=$behandlingId. Se SecureLogs for detaljer,")
            secureLogger.warn("En bad request oppstod ved henting av begrunnelsetekst for behandlingId=$behandlingId. Autogenerert test:" + testVerktøyService.hentBegrunnelsetest(behandlingId), exception)
            throw FunksjonellFeil(
                "Begrunnelsen ${begrunnelseData.apiNavn} passer ikke vedtaksperioden. Hvis du mener dette er feil, ta kontakt med team BAKS.",
            )
        } catch (e: Exception) {
            secureLogger.info("Kall for å hente begrunnelsetekst feilet for behandlingId=$behandlingId. Autogenerert test:\"" + testVerktøyService.hentBegrunnelsetest(behandlingId))
            throw e
        }
    }
}
