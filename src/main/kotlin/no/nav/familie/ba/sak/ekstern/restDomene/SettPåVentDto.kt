package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVent
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import java.time.LocalDate

data class SettPåVentDto(
    val frist: LocalDate,
    val årsak: SettPåVentÅrsak,
)

fun SettPåVent.tilSettPåVentDto(): SettPåVentDto =
    SettPåVentDto(
        frist = this.frist,
        årsak = this.årsak,
    )
