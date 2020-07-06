package no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.integrasjoner.IntegrasjonClient
import no.nav.familie.kontrakter.felles.personinfo.Ident
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StatsborgerskapService(
        private val integrasjonClient: IntegrasjonClient
) {

    fun hentStatsborgerskap(ident: Ident): List<String> =
            integrasjonClient.hentStatsborgerskap(ident)
                    .filter { it.gyldigTilOgMed?.isSameOrAfter(LocalDate.now()) ?: true }
                    .map { it.land }

    fun hentStatsborgerskapOgMedlemskap(ident: Ident): Pair<String, Medlemskap> {
        val statsborgerskap = hentStatsborgerskap(ident)

        val norsk = statsborgerskap.filter { it == "NOR" }
        val nordisk = statsborgerskap.filter { erNordisk(it) }
        val eøs = statsborgerskap.filter { erEØS(it) }
        val tredjeland = statsborgerskap.filter { it != "XUK" }

        return when {
            norsk.isNotEmpty() -> Pair(LANDKODE_NORGE, Medlemskap.NORDEN)
            nordisk.isNotEmpty() -> Pair(nordisk.first(), Medlemskap.NORDEN)
            eøs.isNotEmpty() -> Pair(eøs.first(), Medlemskap.EØS)
            tredjeland.isNotEmpty() -> Pair(tredjeland.first(), Medlemskap.TREDJELANDSBORGER)
            else -> Pair(LANDKODE_UKJENT, Medlemskap.UKJENT)
        }
    }

    private fun erEØS(landkode: String): Boolean = integrasjonClient.hentAlleEØSLand().contains(landkode)

    private fun erNordisk(landkode: String): Boolean = Norden.values().map { it.name }.contains(landkode)

    companion object {
        const val LANDKODE_UKJENT = "XUK"
        const val LANDKODE_NORGE = "NOR"
    }
}

enum class Norden {
    NOR,
    SWE,
    FIN,
    DNK,
    ISL,
    FRO,
    GRL,
    ALA
}