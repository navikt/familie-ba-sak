package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVent
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import java.time.LocalDate

open class RestSettPåVent(
    open val frist: LocalDate,
    open val årsak: SettPåVentÅrsak,
)

fun SettPåVent.tilRestSettPåVent(): RestSettPåVent = RestSettPåVent(
    frist = this.frist,
    årsak = this.årsak,
)
