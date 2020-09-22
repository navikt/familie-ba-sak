package no.nav.familie.ba.sak.saksstatistikk

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.journalføring.domene.JournalføringRepository
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import java.time.LocalDate
import java.time.ZonedDateTime

class SaksstatistikkService(private val behandlingService: BehandlingService,
                            private val journalføringRepository: JournalføringRepository) {

    fun loggBehandlingStatus(behandlingId: Long): BehandlingDVH {
        val behandling = behandlingService.hent(behandlingId)
        val journalpost = journalføringRepository.findByBehandlingId(behandlingId)

        var datoMottatt: LocalDate
        when (behandling.opprinnelse) {
            BehandlingOpprinnelse.MANUELL -> {
                datoMottatt = journalpost.
            }


        }

        val behandlingDVH = BehandlingDVH(funksjonellTid = ZonedDateTime.now(),
                                          tekniskTid = ZonedDateTime.now(),
                                          mottattDato = LocalDate.now(),
                                          registrertDato = LocalDate.now(), //motta
                                          behandlingId = "behandlingId",
                                          sakId = "sakId",
                                          behandlingType = "behandlingType",
                                          behandlingStatus = "behandlingStatus",
                                          utenlandstilsnitt = "utenlandstilsnitt",
                                          ansvarligEnhetKode = "kode",   //der hvor person hører hjemme
                                          behandlendeEnhetKode = "kode", //der hvor saksbehandler hører hjemme
                                          ansvarligEnhetType = "ansvarligEnhetType",
                                          behandlendeEnhetType = "NORG",
                                          totrinnsbehandling = true,
                                          avsender = "ba-sak",
                                          versjon = 2,
                                          vedtaksDato = LocalDate.now(),
                                          relatertBehandlingId = null,
                                          vedtakId = "vedtakId",
                                          resultat = "resultat",
                                          resultatBegrunnelse = "resultatBegrunnelse",
                                          behandlingTypeBeskrivelse = "behandlingTypeBeskrivelse",
                                          behandlingStatusBeskrivelse = "behandlingStatusBeskrivelse",
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