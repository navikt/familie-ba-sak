package no.nav.familie.ba.sak.kjerne.klage

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.klage.dto.OpprettKlageDto
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KlageService(
    private val fagsakService: FagsakService,
    private val klageClient: KlageClient,
    private val integrasjonClient: IntegrasjonClient
) {

    fun opprettKlage(fagsakId: Long, opprettKlageDto: OpprettKlageDto) {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)

        opprettKlage(fagsak, opprettKlageDto.kravMottattDato)
    }

    fun opprettKlage(fagsak: Fagsak, kravMottattDato: LocalDate) {
        if (kravMottattDato.isAfter(LocalDate.now())) {
            throw FunksjonellFeil("Kan ikke opprette klage med krav mottatt frem i tid")
        }

        val aktivtFødselsnummer = fagsak.aktør.aktivFødselsnummer()
        val enhetId = integrasjonClient.hentBehandlendeEnhetForPersonIdentMedRelasjoner(aktivtFødselsnummer).enhetId

        klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                ident = aktivtFødselsnummer,
                stønadstype = Stønadstype.BARNETRYGD,
                eksternFagsakId = fagsak.id.toString(),
                fagsystem = Fagsystem.BA,
                klageMottatt = kravMottattDato,
                behandlendeEnhet = enhetId
            )
        )
    }

    fun hentKlagebehandlingerPåFagsak(fagsakId: Long): List<KlagebehandlingDto> {
        val klagebehandligerPerFagsak = klageClient.hentKlagebehandlinger(setOf(fagsakId))

        val klagerPåFagsak = klagebehandligerPerFagsak[fagsakId]
            ?: throw Feil("Fikk ikke fagsakId=$fagsakId tilbake fra kallet til klage.")

        return klagerPåFagsak.map { it.brukVedtaksdatoFraKlageinstansHvisOversendt() }
    }
}
