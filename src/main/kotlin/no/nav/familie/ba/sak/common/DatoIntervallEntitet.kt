package no.nav.familie.ba.sak.common

import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
class DatoIntervallEntitet : AbstractLocalDateInterval {
    @Column(name = "fom") override var fomDato: LocalDate? = null
    @Column(name = "tom") override var tomDato: LocalDate? = null

    private constructor() {}
    private constructor(fomDato: LocalDate?, tomDato: LocalDate?) {
        requireNotNull(fomDato) { "Fra og med dato må være satt." }
        requireNotNull(tomDato) { "Til og med dato må være satt." }
    }

    override fun lagNyPeriode(fomDato: LocalDate?, tomDato: LocalDate?): DatoIntervallEntitet {
        return fraOgMedTilOgMed(fomDato, tomDato)
    }

    companion object {
        fun fraOgMedTilOgMed(fomDato: LocalDate?, tomDato: LocalDate?): DatoIntervallEntitet {
            return DatoIntervallEntitet(fomDato, tomDato)
        }

        fun fraOgMed(fomDato: LocalDate?): DatoIntervallEntitet {
            return DatoIntervallEntitet(fomDato, AbstractLocalDateInterval.Companion.TIDENES_ENDE)
        }
    }
}