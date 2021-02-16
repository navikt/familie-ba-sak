package no.nav.familie.ba.sak.journalføring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.restDomene.RestJournalføring
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class JuornalføringMetrikk {

    private val antallGenerellSak: Counter = Metrics.counter("journalføring.behandling", "behandlingstype", "Fagsak")

    private val antallTilBehandling = BehandlingType.values().map {
        it to Metrics.counter("journalføring.behandling", "behandlingstype", it.visningsnavn)
    }.toMap()

    private val journalpostTittel = setOf(
        "Søknad om ordinær barnetrygd",
        "Søknad om utvidet barnetrygd",
        "Ettersendelse til søknad om ordinær barnetrygd",
        "Ettersendelse til søknad om utvidet barnetrygd",
        "Tilleggskjema EØS"
    )

    private val antallJournalpostTittel = journalpostTittel.map {
        var journalpostTittelKey = it.toLowerCase()
        journalpostTittelKey to Metrics.counter(
            "journalføring.journalpost",
            "tittel",
            it
        )
    }.toMap()

    private val antallJournalpostTittelFritekst =
        Metrics.counter("journalføring.journalpost", "tittel", "Fritekst")

    fun oppdaterJournalføringMetrikk(journalpost: Journalpost?, updatert: RestJournalføring, behandlinger: List<Behandling>) {
        if (updatert.knyttTilFagsak) {
            behandlinger.forEach {
                LOG.info("Increase counter ${it.type} ${antallTilBehandling[it.type]}")
                antallTilBehandling[it.type]?.increment()
            }
        } else {
            LOG.info("Increase counter ${antallGenerellSak}")
            antallGenerellSak.increment()
        }

        val journalpostTittelLower = journalpost?.tittel?.toLowerCase()
        if (journalpostTittel.contains(journalpostTittelLower)) {
            LOG.info("Increase counter ${journalpostTittelLower} ${antallJournalpostTittel[journalpostTittelLower]}")
            antallJournalpostTittel[journalpostTittelLower]?.increment()
        } else {
            LOG.info("Increase counter ${antallJournalpostTittelFritekst}")
            antallJournalpostTittelFritekst.increment()
        }
    }

    companion object{
        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}