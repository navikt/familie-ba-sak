package no.nav.familie.ba.sak.kjerne.klage

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.TilpassArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.klage.dto.OpprettKlageDto
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class KlagebehandlingOppretter(
    private val fagsakService: FagsakService,
    private val klageKlient: KlageKlient,
    private val integrasjonKlient: IntegrasjonKlient,
    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService,
    private val clockProvider: ClockProvider,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(KlagebehandlingOppretter::class.java)

    fun opprettKlage(
        fagsakId: Long,
        opprettKlageDto: OpprettKlageDto,
    ): UUID {
        val fagsak = fagsakService.hentPåFagsakId(fagsakId)
        return opprettKlage(fagsak, opprettKlageDto.klageMottattDato)
    }

    fun opprettKlage(
        fagsak: Fagsak,
        klageMottattDato: LocalDate,
    ): UUID {
        if (fagsak.type === FagsakType.INSTITUSJON && !featureToggleService.isEnabled(FeatureToggle.SKAL_KUNNE_BEHANDLE_BA_INSTITUSJONSFAGSAKER_I_KLAGE)) {
            throw FunksjonellFeil("Oppretting av klagebehandlinger for institusjonsfagsaker er ikke implementert.")
        }

        if (klageMottattDato.isAfter(LocalDate.now(clockProvider.get()))) {
            throw FunksjonellFeil("Kan ikke opprette klage med krav mottatt frem i tid.")
        }

        val fødselsnummer = fagsak.aktør.aktivFødselsnummer()
        val navIdent = NavIdent(SikkerhetContext.hentSaksbehandler())

        val arbeidsfordelingsenheter = integrasjonKlient.hentBehandlendeEnhet(fødselsnummer)

        if (arbeidsfordelingsenheter.isEmpty()) {
            logger.error("Fant ingen arbeidsfordelingsenheter for aktør. Se SecureLogs for detaljer.")
            secureLogger.error("Fant ingen arbeidsfordelingsenheter for aktør $fødselsnummer.")
            throw Feil("Fant ingen arbeidsfordelingsenhet for aktør.")
        }

        if (arbeidsfordelingsenheter.size > 1) {
            logger.error("Fant flere arbeidsfordelingsenheter for aktør. Se SecureLogs for detaljer.")
            secureLogger.error("Fant flere arbeidsfordelingsenheter for aktør $fødselsnummer.")
            throw Feil("Fant flere arbeidsfordelingsenheter for aktør.")
        }

        val tilpassetArbeidsfordelingsenhet =
            tilpassArbeidsfordelingService.tilpassArbeidsfordelingsenhetTilSaksbehandler(
                arbeidsfordelingsenheter.single(),
                navIdent,
            )

        return klageKlient.opprettKlage(
            OpprettKlagebehandlingRequest(
                ident = fødselsnummer,
                orgNummer = utledOrgNummer(fagsak),
                stønadstype = Stønadstype.BARNETRYGD,
                eksternFagsakId = fagsak.id.toString(),
                fagsystem = Fagsystem.BA,
                klageMottatt = klageMottattDato,
                behandlendeEnhet = tilpassetArbeidsfordelingsenhet.enhetId,
                behandlingsårsak = Klagebehandlingsårsak.ORDINÆR,
            ),
        )
    }

    private fun utledOrgNummer(fagsak: Fagsak): String? =
        if (fagsak.type === FagsakType.INSTITUSJON) {
            val institusjon = fagsak.institusjon
            if (institusjon == null) {
                logger.error("Fant ikke institusjon for fagsak ${fagsak.id} med type ${fagsak.type}.")
                throw Feil("Fant ikke forventet institusjon på fagsak ${fagsak.id}.")
            }
            institusjon.orgNummer
        } else {
            null
        }
}
