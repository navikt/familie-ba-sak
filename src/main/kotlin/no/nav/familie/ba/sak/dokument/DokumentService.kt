package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.toDokGenTemplate
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.stereotype.Service

@Service
class DokumentService(
        private val vedtakService: VedtakService,
        private val dokGenKlient: DokGenKlient
) {

    fun hentHtmlVedtakForBehandling(behandlingId: Long): Ressurs<String> {
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId)
                     ?: return Ressurs.failure("Vedtak ikke funnet")
        val html = Result.runCatching {
                    dokGenKlient.lagHtmlFraMarkdown(vedtak.behandling.resultat.toDokGenTemplate(),
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
                    dokGenKlient.lagPdfFraMarkdown(vedtak.behandling.resultat.toDokGenTemplate(), markdown)
                }
                .fold(
                        onSuccess = { it },
                        onFailure = {
                            throw Exception("Klarte ikke å hente PDF for vedtak med id ${vedtak.id}", it)
                        }
                )
    }
}