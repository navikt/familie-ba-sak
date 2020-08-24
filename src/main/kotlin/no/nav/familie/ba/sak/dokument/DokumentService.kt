package no.nav.familie.ba.sak.dokument

import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
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
        private val persongrunnlagService: PersongrunnlagService
) {

    fun hentBrevForVedtak(vedtak: Vedtak): Ressurs<ByteArray> {
        val pdf = vedtak.stønadBrevPdF
                  ?: error("Klarte ikke finne brev for vetak med id ${vedtak.id}")
        return Ressurs.success(pdf)
    }

    fun genererBrevForVedtak(vedtak: Vedtak): ByteArray {
        return Result.runCatching {
            val søker = persongrunnlagService.hentSøker(behandling = vedtak.behandling)
                        ?: error("Finner ikke søker på vedtaket")

            val behandlingResultatType =
                    behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = vedtak.behandling.id)

            val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                           ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                         frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")

            val headerFelter = DokumentHeaderFelter(fodselsnummer = søker.personIdent.ident,
                                                    navn = søker.navn,
                                                    antallBarn = if (vedtak.behandling.opprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)
                                                        personopplysningGrunnlag.barna.size else null,
                                                    dokumentDato = LocalDate.now().tilDagMånedÅr())

            val malMedData = malerService.mapTilBrevfelter(vedtak,
                                                           personopplysningGrunnlag,
                                                           behandlingResultatType
            )
            dokGenKlient.lagPdfForMal(malMedData, headerFelter)
        }
                .fold(
                        onSuccess = { it },
                        onFailure = {
                            throw Feil(message = "Klarte ikke generere vedtaksbrev",
                                       frontendFeilmelding = "Noe gikk galt ved generering av vedtaksbrev og systemansvarlige er varslet. Prøv igjen senere, men hvis problemet vedvarer kontakt brukerstøtte",
                                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                                       throwable = it)
                        }
                )
    }
}
