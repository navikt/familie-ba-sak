package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultatType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagRepository
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.dokument.domene.DokumentHeaderFelter
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.http.HttpStatus
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

    @Deprecated("Brevflyt endret til å hente og lagre PDF direkte")
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

    @Deprecated("henter og viser PDF istedet")
    fun hentHtmlForVedtak(vedtak: Vedtak): Ressurs<String> {

        val html = Result.runCatching {
            val søker = persongrunnlagService.hentSøker(behandling = vedtak.behandling)
                        ?: error("Finner ikke søker på vedtaket")
            val søknad: SøknadDTO? = søknadGrunnlagService.hentAktiv(vedtak.behandling.id)?.hentSøknadDto()

            val behandlingResultatType =
                    behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = vedtak.behandling.id)

            dokGenKlient.lagHtmlFraMarkdown(template = MalerService.malNavnForTypeSøkerOgResultatType(
                    typeSøker = søknad?.typeSøker,
                    resultatType = behandlingResultatType),
                                            markdown = vedtak.stønadBrevMarkdown.takeIf { it.isNotEmpty() }
                                                       ?: hentStønadBrevMarkdown(vedtak, søknad, behandlingResultatType),
                                            dokumentHeaderFelter = DokumentHeaderFelter(
                                                    fodselsnummer = søker.personIdent.ident,
                                                    navn = søker.navn,
                                                    returadresse = "NAV Voss, Postboks 143, 5701 VOSS",
                                                    dokumentDato = LocalDate.now().tilDagMånedÅr()
                                            )
            )
        }
                .fold(
                        onSuccess = { it },
                        onFailure = { e ->
                            return Ressurs.failure(errorMessage = "Klarte ikke å hent vedtaksbrev", error = e)
                        }
                )

        return Ressurs.success(html)
    }

    @Deprecated("Kun midlertidig brakt tilbake for å brukes til å konvertere vedtaksbrev lagret som markdown til PDF")
    fun hentPdfForVedtak(vedtak: Vedtak): ByteArray {
        return Result.runCatching {
            val markdown = vedtak.stønadBrevMarkdown

            val søker = persongrunnlagService.hentSøker(behandling = vedtak.behandling)
                        ?: error("Finner ikke søker på vedtaket")
            val søknad: SøknadDTO? = søknadGrunnlagService.hentAktiv(vedtak.behandling.id)?.hentSøknadDto()

            val behandlingResultatType =
                    behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = vedtak.behandling.id)
            dokGenKlient.lagPdfFraMarkdown(template = MalerService.malNavnForTypeSøkerOgResultatType(
                    typeSøker = søknad?.typeSøker,
                    resultatType = behandlingResultatType),
                                           markdown = markdown,
                                           dokumentHeaderFelter = DokumentHeaderFelter(
                                                   fodselsnummer = søker.personIdent.ident,
                                                   navn = søker.navn,
                                                   returadresse = "NAV Voss, Postboks 143, 5701 VOSS",
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

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        val pdf = vedtak.stønadBrevPdF
                  ?: error("Klarte ikke finne brev for vetak med id ${vedtak.id}")
        return Ressurs.success(pdf)
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
            dokGenKlient.lagPdfForMal(malMedData, headerFelter)
        }
                .fold(
                        onSuccess = { it },
                        onFailure = { throwable ->
                            throw Feil(message = "Klarte ikke generere vedtaksbrev",
                                       frontendFeilmelding = "Noe gikk galt ved generering av vedtaksbrev og systemansvarlige er varslet. Prøv igjen senere, men hvis problemet vedvarer kontakt brukerstøtte",
                                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                       throwable = throwable)
                        }
                )
    }
}
