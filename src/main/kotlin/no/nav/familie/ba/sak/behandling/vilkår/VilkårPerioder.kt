package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import java.time.LocalDate

fun genererPerioderUnder18År(fakta: Fakta): List<Periode> {
    return listOf(Periode(fom = fakta.personForVurdering.fødselsdato, tom = fakta.personForVurdering.fødselsdato.plusYears(18)))
}

fun genererPerioderLovligOpphold(fakta: Fakta, minLocalDate: LocalDate): List<Periode> {
    if (fakta.personForVurdering.statsborgerskap == null) {
        return genererPerioderStandard(fakta)
    }

    val filtrertPerioder = fakta.personForVurdering.statsborgerskap!!.filter {
        when {
            it.gyldigPeriode == null -> {
                false
            }
            it.gyldigPeriode.tom == null -> {
                true
            }
            it.gyldigPeriode.tom.isBefore(minLocalDate) -> {
                false
            }
            else -> true
        }
    }

    return if (filtrertPerioder.isEmpty()) genererPerioderStandard(fakta) else filtrertPerioder.map {
        Periode(fom = it.gyldigPeriode?.fom ?: TIDENES_MORGEN, tom = it.gyldigPeriode?.tom ?: TIDENES_ENDE)
    }
}

fun genererPerioderStandard(fakta: Fakta): List<Periode> {
    val fom =
            if (fakta.personForVurdering.type === PersonType.BARN)
                fakta.personForVurdering.fødselsdato
            else LocalDate.now()

    return listOf(Periode(fom = fom, tom = TIDENES_ENDE))
}