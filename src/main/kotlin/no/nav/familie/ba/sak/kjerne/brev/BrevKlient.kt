package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.kallEksternTjeneste
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseMedData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.logger
import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

val FAMILIE_BREV_TJENESTENAVN = "famile-brev"

@Component
class BrevKlient(
    @Value("\${FAMILIE_BREV_API_URL}") private val familieBrevUri: String,
    @Value("\${SANITY_DATASET}") private val sanityDataset: String,
    restTemplate: RestTemplate,
    private val testVerktøyService: TestVerktøyService,
) : AbstractRestClient(restTemplate, "familie-brev") {
    fun genererBrev(
        målform: String,
        brev: Brev,
    ): ByteArray {
        val uri = URI.create("$familieBrevUri/api/$sanityDataset/dokument/$målform/${brev.mal.apiNavn}/pdf")

        secureLogger.info("Kaller familie brev($uri) med data ${brev.data.toBrevString()}")
        return try {
            kallEksternTjeneste(FAMILIE_BREV_TJENESTENAVN, uri, "Hente pdf for vedtaksbrev") {
                postForEntity(uri, brev.data)
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
        val uri = URI.create("$familieBrevUri/ba-sak/begrunnelser/${begrunnelseData.apiNavn}/tekst/")
        secureLogger.info("Kaller familie brev($uri) med data $begrunnelseData")
        logger.info("Henter begrunnelse ${begrunnelseData.apiNavn} på periode ${vedtaksperiode.fom} - ${vedtaksperiode.tom} i behandling ${vedtaksperiode.vedtak.behandling}")

        return try {
            kallEksternTjeneste(FAMILIE_BREV_TJENESTENAVN, uri, "Henter begrunnelsestekst") {
                postForEntity(uri, begrunnelseData)
            }
        } catch (exception: HttpClientErrorException.BadRequest) {
            log.warn("En bad request oppstod ved henting av begrunneelsetekst. Se SecureLogs for detaljer,")
            secureLogger.warn("En bad request oppstod ved henting av begrunnelsetekst", exception)
            throw FunksjonellFeil(
                "Begrunnelsen ${begrunnelseData.apiNavn} passer ikke vedtaksperioden. Hvis du mener dette er feil, ta kontakt med team BAKS.",
            )
        } catch (e: Exception) {
            secureLogger.info("Kall for å hente begrunnelsetekst feilet. Autogenerert test:\"" + testVerktøyService.hentBegrunnelsetest(vedtaksperiode.vedtak.behandling.id))
            throw e
        }
    }
}
