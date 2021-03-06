package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalpostDokument
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import java.time.LocalDateTime


fun lagMockRestJournalføring(bruker: NavnOgIdent): RestJournalføring = RestJournalføring(
        avsender = bruker,
        bruker = bruker,
        datoMottatt = LocalDateTime.now().minusDays(10),
        journalpostTittel = "Søknad om ordinær barnetrygd",
        knyttTilFagsak = true,
        opprettOgKnyttTilNyBehandling = true,
        tilknyttedeBehandlingIder = emptyList(),
        dokumenter = listOf(
                RestJournalpostDokument(dokumentTittel = "Søknad om barnetrygd",
                                        brevkode = "mock",
                                        dokumentInfoId = "1",
                                        logiskeVedlegg = listOf(LogiskVedlegg("123", "Oppholdstillatelse")),
                                        eksisterendeLogiskeVedlegg = emptyList()
                ),
                RestJournalpostDokument(dokumentTittel = "Ekstra vedlegg",
                                        brevkode = "mock",
                                        dokumentInfoId = "2",
                                        logiskeVedlegg = listOf(LogiskVedlegg("123", "Pass")),
                                        eksisterendeLogiskeVedlegg = emptyList())
        ),
        navIdent = "09123",
        nyBehandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,
        nyBehandlingsårsak = BehandlingÅrsak.SØKNAD
)