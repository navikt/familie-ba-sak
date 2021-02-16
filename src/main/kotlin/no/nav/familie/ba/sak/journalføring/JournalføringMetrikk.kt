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
class JournalføringMetrikk {

    private val antallGenerellSak: Counter = Metrics.counter("journalfoering.behandling", "behandlingstype", "Fagsak")

    private val antallTilBehandling = BehandlingType.values().map {
        it to Metrics.counter("journalfoering.behandling", "behandlingstype", it.visningsnavn)
    }.toMap()

    private val journalpostTittel = setOf(
        "Søknad om ordinær barnetrygd",
        "Søknad om utvidet barnetrygd",
        "Ettersendelse til søknad om ordinær barnetrygd",
        "Ettersendelse til søknad om utvidet barnetrygd",
        "Tilleggskjema EØS"
    )

    private val journalpostTittelLower = journalpostTittel.map { it.toLowerCase() }

    private val antallJournalpostTittel = journalpostTittel.map {
        var journalpostTittelKey = it.toLowerCase()
        journalpostTittelKey to Metrics.counter(
            "journalfoering.journalpost",
            "tittel",
            it
        )
    }.toMap()

    private val antallJournalpostTittelFritekst =
        Metrics.counter("journalfoering.journalpost", "tittel", "Fritekst")

    fun tellManuellJournalføringsmetrikker(
        journalpost: Journalpost?,
        oppdatert: RestJournalføring,
        behandlinger: List<Behandling>
    ) {
        if (oppdatert.knyttTilFagsak) {
            behandlinger.forEach {
                LOG.info("Increase counter ${it.type} ${antallTilBehandling[it.type]}")
                antallTilBehandling[it.type]?.increment()
            }
        } else {
            LOG.info("Increase counter ${antallGenerellSak}")
            antallGenerellSak.increment()
        }

        val tittelLower = journalpost?.tittel?.toLowerCase()
        if (journalpostTittelLower.contains(tittelLower)) {
            LOG.info("Increase counter ${tittelLower} ${antallJournalpostTittel[tittelLower]}")
            antallJournalpostTittel[tittelLower]?.increment()
        } else {
            LOG.info("Increase counter ${antallJournalpostTittelFritekst}")
            antallJournalpostTittelFritekst.increment()
        }
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}