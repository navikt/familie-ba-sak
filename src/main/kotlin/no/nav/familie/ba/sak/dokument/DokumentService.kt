package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
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
        private val persongrunnlagService: PersongrunnlagService,
        private val søknadGrunnlagService: SøknadGrunnlagRepository
) {

    @Deprecated("Gjøres i hentBrevForVedtak")
    fun hentStønadBrevMarkdown(
            vedtak: Vedtak,
            søknad: SøknadDTO? = null,
            behandlingResultatType: BehandlingResultatType): String {

        val malMedData = malerService.mapTilBrevfelter(vedtak,
                                                       søknad,
                                                       behandlingResultatType
        )
        val markdown = dokGenKlient.hentMarkdownForMal(malMedData)
        return markdown
    }

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        val pdf = vedtak.stønadBrevPdF
                ?: error("Klarte ikke finne brev for vetak med id ${vedtak.id}")
        return Ressurs.success(pdf)
    }

    private fun hentPdfFor(mal: String,
                           markdown: String,
                           headerFelter: DokumentHeaderFelter): ByteArray {

        return Result.runCatching {
             dokGenKlient.lagPdfFraMarkdown(template = mal,
                                             markdown = markdown,
                                             dokumentHeaderFelter = headerFelter)
        }.getOrElse {
            throw Exception("Klarte ikke å hente PDF-utgave av vedtaksbrev", it)
        }
    }

    fun genererBrevForVedtak(vedtak: Vedtak): ByteArray {
        return Result.runCatching {
            val søker = persongrunnlagService.hentSøker(behandling = vedtak.behandling)
                    ?: error("Finner ikke søker på vedtaket")
            val søknad: SøknadDTO? = søknadGrunnlagService.hentAktiv(vedtak.behandling.id)?.hentSøknadDto()

            val behandlingResultatType =
                    behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = vedtak.behandling.id)

            val headerFelter = DokumentHeaderFelter(fodselsnummer = søker.personIdent.ident,
                    navn = søker.navn,
                    returadresse = "NAV Voss, Postboks 143, 5701 VOSS",
                    dokumentDato = LocalDate.now().tilDagMånedÅr())

            val malMedData = malerService.mapTilBrevfelter(vedtak,
                    søknad,
                    behandlingResultatType
            )
            val markdown = dokGenKlient.hentMarkdownForMal(malMedData)

            hentPdfFor(mal = malMedData.mal,
                    markdown = markdown,
                    headerFelter = headerFelter)
        }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            error("Klarte ikke å hente vedtaksbrev")
                        }
                )
    }
}
