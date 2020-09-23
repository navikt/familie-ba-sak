package no.nav.familie.ba.sak.saksstatistikk

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.journalføring.JournalføringService
import no.nav.familie.ba.sak.journalføring.domene.JournalføringRepository
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class SaksstatistikkService(private val behandlingService: BehandlingService,
                            private val journalføringRepository: JournalføringRepository,
                            private val journalføringService: JournalføringService,
                            private val arbeidsfordelingService: ArbeidsfordelingService) {

    fun loggBehandlingStatus(behandlingId: Long): BehandlingDVH {
        val behandling = behandlingService.hent(behandlingId)
        val journalpost = journalføringRepository.findByBehandlingId(behandlingId)

        var datoMottatt: LocalDateTime // TODO: Endre til LocalDateTime i kontrakten
        when (behandling.opprinnelse) {
            BehandlingOpprinnelse.MANUELL -> {
                datoMottatt = journalpost.mapNotNull { journalføringService.hentJournalpost(it.journalpostId).data }
                                      .filter { it.journalposttype == Journalposttype.I }
                                      .filter { it.tittel != null && it.tittel!!.contains("søknad", ignoreCase = true) }
                                      .mapNotNull { it.datoMottatt }
                                      .minOrNull() ?: throw error("")
            }
            BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE -> {
                datoMottatt = behandling.opprettetTidspunkt  // TODO: trenger avklaring
            }
        }

        val behandlendeEnhetsKode = arbeidsfordelingService.bestemBehandlendeEnhet(behandling)
        val ansvarligEnhetKode = arbeidsfordelingService.hentBehandlendeEnhet(behandling.fagsak).firstOrNull()?.enhetId

        val behandlingDVH = BehandlingDVH(funksjonellTid = ZonedDateTime.now(),
                                          tekniskTid = ZonedDateTime.now(),
                                          mottattDato = LocalDate.now(),
                                          registrertDato = LocalDate.now(), //motta
                                          behandlingId = behandling.id.toString(),
                                          sakId = behandling.fagsak.id.toString(),
                                          behandlingType = behandling.type.name,
                                          behandlingStatus = behandling.status.name,
                                          utenlandstilsnitt = "NASJONAL", // TODO
                                          ansvarligEnhetKode = ansvarligEnhetKode!!,
                                          behandlendeEnhetKode = behandlendeEnhetsKode, //TODO Hva gjør vi hvis behandlendeEnhetsKode ikke finnes? //der hvor saksbehandler hører hjemme
                                          ansvarligEnhetType = "NORG",
                                          behandlendeEnhetType = "NORG",
                                          totrinnsbehandling = behandling.opprinnelse == BehandlingOpprinnelse.MANUELL,
                                          avsender = "familie-ba-sak",
                                          versjon = 2, // TODO kan vurdere å bumpe den manuelt sammen med en test som feiler hvis vi glemmer det. Kan også undersøke om det går an å plukke ut noe fra manifestet
                                          vedtaksDato = LocalDate.now(),
                                          relatertBehandlingId = null,
                                          vedtakId = "vedtakId",
                                          resultat = "resultat",
                                          resultatBegrunnelse = "resultatBegrunnelse",
                                          behandlingTypeBeskrivelse = behandling.type.visningsnavn,
                                          behandlingStatusBeskrivelse = null,
                                          resultatBegrunnelseBeskrivelse = "resultatBegrunnelseBeskrivelse",
                                          utenlandstilsnittBeskrivelse = "relatertButenlandstilsnittBeskrivelseehandlingId",
                                          beslutter = "beslutter",
                                          saksbehandler = "saksbehandler",
                                          behandlingOpprettetAv = "behandlingOpprettetAv",
                                          behandlingOpprettetType = "behandlingOpprettetType",
                                          behandlingOpprettetTypeBeskrivelse = "behandlingOpprettetTypeBeskrivelse"
        )
        return behandlingDVH
    }
}