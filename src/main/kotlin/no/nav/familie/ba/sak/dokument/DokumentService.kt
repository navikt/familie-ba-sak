package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.stereotype.Service

@Service
class DokumentService(
        private val vedtakService: VedtakService,
        private val behandlingResultatService: BehandlingResultatService,
        private val dokGenKlient: DokGenKlient
) {

    fun hentHtmlForVedtak(vedtakId: Long): Ressurs<String> {

        val html = Result.runCatching {
            val vedtak = vedtakService.hent(vedtakId)

            val behandlingResultatType =
                    behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = vedtak.behandling.id)
            dokGenKlient.lagHtmlFraMarkdown(behandlingResultatType.brevMal,
                                            vedtak.stønadBrevMarkdown)
        }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            return Ressurs.failure("Klarte ikke å hent vedtaksbrev", e)
                        }
                )

        return Ressurs.success(html)
    }

    internal fun hentPdfForVedtak(vedtak: Vedtak): ByteArray {
        return Result.runCatching {
            BehandlingService.LOG.debug("henter stønadsbrevMarkdown fra behandlingsVedtak")
            val markdown = vedtak.stønadBrevMarkdown
            BehandlingService.LOG.debug("kaller lagPdfFraMarkdown med stønadsbrevMarkdown")

            val behandlingResultatType =
                    behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = vedtak.behandling.id)
            dokGenKlient.lagPdfFraMarkdown(behandlingResultatType.brevMal, markdown)
        }
                .fold(
                        onSuccess = { it },
                        onFailure = {
                            throw Exception("Klarte ikke å hente PDF for vedtak med id ${vedtak.id}", it)
                        }
                )
    }
}
