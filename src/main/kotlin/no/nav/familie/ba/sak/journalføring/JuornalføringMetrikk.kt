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

    private val antallGenerellSak: Counter = Metrics.counter("journalføring.behandling", "behandlingstype", "generell-sak")

    private val antallTilBehandling = BehandlingType.values().map {
        var behandlingtag = formatterTag(it.visningsnavn)
        it to Metrics.counter("journalføring.behandling", "behandlingstype", behandlingtag)
    }.toMap()

    private fun formatterTag(navn: String) = navn.replace(' ', '-').toLowerCase()

    private val journalpostTittel = setOf(
        "søknad om ordinær barnetrygd",
        "søknad om utvidet barnetrygd",
        "ettersendelse til søknad om ordinær barnetrygd",
        "ettersendelse til søknad om utvidet barnetrygd",
        "tilleggskjema eøs"
    )

    private val antallJournalpostTittel = journalpostTittel.map {
        var journalpostTittelTag = formatterTag(it)
        it to Metrics.counter(
            "journalføring.journalpost",
            "tittel",
            journalpostTittelTag
        )
    }.toMap()

    private val antallJournalpostTittelFritekst =
        Metrics.counter("journalføring.journalpost", "tittel", "fritekst")

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