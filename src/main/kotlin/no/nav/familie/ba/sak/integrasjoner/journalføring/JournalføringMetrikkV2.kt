package no.nav.familie.ba.sak.integrasjoner.journalføring

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.ekstern.restDomene.TilknyttetBehandling
import no.nav.familie.ba.sak.integrasjoner.journalføring.domene.Journalføringsbehandlingstype
import org.springframework.stereotype.Component

@Component
class JournalføringMetrikkV2 {
    private val antallTilBehandling =
        Journalføringsbehandlingstype.entries.associateWith {
            Metrics.counter("journalfoering.behandling", "behandlingstype", it.tilVisningsnavn())
        }

    private val journalpostTittelMap =
        mapOf(
            "søknad om ordinær barnetrygd" to "Søknad om ordinær barnetrygd",
            "søknad om barnetrygd ordinær" to "Søknad om ordinær barnetrygd",
            "søknad om utvidet barnetrygd" to "Søknad om utvidet barnetrygd",
            "søknad om barnetrygd utvidet" to "Søknad om utvidet barnetrygd",
            "ettersendelse til søknad om ordinær barnetrygd" to "Ettersendelse til søknad om ordinær barnetrygd",
            "ettersendelse til søknad om barnetrygd ordinær" to "Ettersendelse til søknad om ordinær barnetrygd",
            "ettersendelse til søknad om utvidet barnetrygd" to "Ettersendelse til søknad om utvidet barnetrygd",
            "ettersendelse til søknad om barnetrygd utvidet" to "Ettersendelse til søknad om utvidet barnetrygd",
            "tilleggskjema eøs" to "Tilleggskjema EØS",
            "klage" to "Klage",
        )

    private val antallJournalpostTittel =
        journalpostTittelMap.values.toSet().associateWith {
            Metrics.counter(
                "journalfoering.journalpost",
                "tittel",
                it,
            )
        }

    private val antallJournalpostTittelFritekst =
        Metrics.counter("journalfoering.journalpost", "tittel", "Fritekst")

    fun tellManuellJournalføringsmetrikker(
        journalpostTittel: String?,
        tilknyttetBehandlinger: List<TilknyttetBehandling>,
    ) {
        tilknyttetBehandlinger.forEach { antallTilBehandling[it.behandlingstype]?.increment() }
        val kjentTittel = journalpostTittelMap[journalpostTittel?.lowercase()]
        if (kjentTittel != null) {
            antallJournalpostTittel[kjentTittel]?.increment()
        } else {
            antallJournalpostTittelFritekst.increment()
        }
    }
}
