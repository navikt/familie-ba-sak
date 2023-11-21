package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilKortString
import java.time.LocalDate

fun List<LocalDate>.tilSammenslåttKortString(): String = Utils.slåSammen(this.sorted().map { it.tilKortString() })
