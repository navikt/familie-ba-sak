package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadDTO
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.dokument.domene.DokumentHeaderFelter
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DokumentService(
        private val behandlingResultatService: BehandlingResultatService,
        private val dokGenKlient: DokGenKlient,
        private val malerService: MalerService,
        private val persongrunnlagService: PersongrunnlagService
) {

    fun hentStønadBrevMarkdown(
            vedtak: Vedtak,
            søknad: SøknadDTO? = null,
            behandlingResultatType: BehandlingResultatType): String {

        val malMedData = malerService.mapTilBrevfelter(vedtak,
                                                       søknad,
                                                       behandlingResultatType
        )
        return dokGenKlient.hentMarkdownForMal(malMedData)
    }

    fun hentHtmlForVedtak(vedtak: Vedtak): Ressurs<String> {

        val html = Result.runCatching {
            val søker = persongrunnlagService.hentSøker(behandling = vedtak.behandling)
                        ?: error("Finner ikke søker på vedtaket")

            val behandlingResultatType =
                    behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = vedtak.behandling.id)
            dokGenKlient.lagHtmlFraMarkdown(template = behandlingResultatType.brevMal,
                                            markdown = vedtak.stønadBrevMarkdown,
                                            dokumentHeaderFelter = DokumentHeaderFelter(
                                                    fodselsnummer = søker.personIdent.ident,
                                                    returadresse = "",
                                                    dokumentDato = LocalDate.now().tilDagMånedÅr()
                                            )
            )
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

            val søker = persongrunnlagService.hentSøker(behandling = vedtak.behandling)
                        ?: error("Finner ikke søker på vedtaket")

            val behandlingResultatType =
                    behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = vedtak.behandling.id)
            dokGenKlient.lagPdfFraMarkdown(behandlingResultatType.brevMal, markdown, DokumentHeaderFelter(
                    fodselsnummer = søker.personIdent.ident,
                    returadresse = "",
                    dokumentDato = LocalDate.now().tilDagMånedÅr()
            ))
        }
                .fold(
                        onSuccess = { it },
                        onFailure = {
                            throw Exception("Klarte ikke å hente PDF for vedtak med id ${vedtak.id}", it)
                        }
                )
    }
}
