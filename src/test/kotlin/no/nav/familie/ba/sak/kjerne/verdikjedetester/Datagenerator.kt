package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.NavnOgIdent
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalføring
import no.nav.familie.ba.sak.ekstern.restDomene.RestJournalpostDokument
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.kontrakter.ba.infotrygd.Barn
import no.nav.familie.kontrakter.ba.infotrygd.Delytelse
import no.nav.familie.kontrakter.ba.infotrygd.Sak
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import no.nav.familie.kontrakter.felles.journalpost.LogiskVedlegg
import java.time.LocalDate
import java.time.LocalDateTime

fun lagMockRestJournalføring(bruker: NavnOgIdent): RestJournalføring = RestJournalføring(
    avsender = bruker,
    bruker = bruker,
    datoMottatt = LocalDateTime.now().minusDays(10),
    journalpostTittel = "Søknad om ordinær barnetrygd",
    kategori = BehandlingKategori.NASJONAL,
    underkategori = BehandlingUnderkategori.ORDINÆR,
    knyttTilFagsak = true,
    opprettOgKnyttTilNyBehandling = true,
    tilknyttedeBehandlingIder = emptyList(),
    dokumenter = listOf(
        RestJournalpostDokument(
            dokumentTittel = "Søknad om barnetrygd",
            brevkode = "mock",
            dokumentInfoId = "1",
            logiskeVedlegg = listOf(LogiskVedlegg("123", "Oppholdstillatelse")),
            eksisterendeLogiskeVedlegg = emptyList()
        ),
        RestJournalpostDokument(
            dokumentTittel = "Ekstra vedlegg",
            brevkode = "mock",
            dokumentInfoId = "2",
            logiskeVedlegg = listOf(LogiskVedlegg("123", "Pass")),
            eksisterendeLogiskeVedlegg = emptyList()
        )
    ),
    navIdent = "09123",
    nyBehandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,
    nyBehandlingsårsak = BehandlingÅrsak.SØKNAD
)

fun lagInfotrygdSak(beløp: Double, identBarn: String, valg: String? = "OR", undervalg: String? = "OS"): Sak {
    return Sak(
        stønad = Stønad(
            barn = listOf(
                Barn(identBarn, barnetrygdTom = "000000")
            ),
            delytelse = listOf(
                Delytelse(
                    fom = LocalDate.now(),
                    tom = null,
                    beløp = beløp,
                    typeDelytelse = "MS",
                    typeUtbetaling = "J",
                )
            ),
            opphørsgrunn = "0"
        ),
        status = "FB",
        valg = valg,
        undervalg = undervalg
    )
}
