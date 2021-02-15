package no.nav.familie.ba.sak.journalføring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.restDomene.RestJournalføring
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.springframework.stereotype.Component

@Component
class JuornalføringMetrikk {

    private val antallGenerellSak: Counter = Metrics.counter("journalføring.generellSak", "journalføring", "generellSak")

    private val antallTilBehandling = BehandlingType.values().map {
        var behandlingtypeNavn = formatterCounterNavn(it.visningsnavn)
        it to Metrics.counter("journalføring.behandling.${behandlingtypeNavn}", "journalføring", "behandling", behandlingtypeNavn)
    }.toMap()

    private fun formatterCounterNavn(navn: String) = navn.replace(' ', '-')

    private val journalpostTittel = setOf(
        "søknad om ordinær barnetrygd",
        "søknad om utvidet barnetrygd",
        "ettersendelse til søknad om ordinær barnetrygd",
        "ettersendelse til søknad om utvidet barnetrygd",
        "tilleggskjema eøs"
    )

    private val antallJournalpostTittel = journalpostTittel.map {
        var journalpostTittelNavn = formatterCounterNavn(it)
        it to Metrics.counter(
            "journalføring.journalpostTittel.${journalpostTittelNavn}",
            "journalføring",
            "journalpostTittel",
            journalpostTittelNavn
        )
    }.toMap()

    private val antallJournalpostTittelFritekst =
        Metrics.counter("journalføring.journalpostTittel.fritekst", "journalføring", "journalpostTittel", "fritekst")

    fun oppdaterJournalføringMetrikk(journalpost: Journalpost?, updatert: RestJournalføring, behandlinger: List<Behandling>) {
        if (updatert.knyttTilFagsak) {
            behandlinger.forEach {
                antallTilBehandling[it.type]?.increment()
            }
        } else {
            antallGenerellSak.increment()
        }

        val journalpostTittelLower = journalpost?.tittel?.toLowerCase()
        if (journalpostTittel.contains(journalpostTittelLower)) {
            antallJournalpostTittel[journalpostTittelLower]?.increment()
        } else {
            antallJournalpostTittelFritekst.increment()
        }
    }
}