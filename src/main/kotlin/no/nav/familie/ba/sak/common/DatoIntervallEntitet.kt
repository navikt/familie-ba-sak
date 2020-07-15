package no.nav.familie.ba.sak.common

import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class DatoIntervallEntitet(
        @Column(name = "fom")
        val fom: LocalDate? = null,

        @Column(name = "tom")
        val tom: LocalDate? = null
) {
        fun erInnenfor(dato: LocalDate): Boolean {
                return when {
                        fom == null && tom == null -> true
                        fom == null -> dato.isSameOrBefore(tom!!)
                        tom == null -> dato.isSameOrAfter(fom)
                        else -> dato.isSameOrAfter(fom) && dato.isSameOrBefore(tom)
                }
        }
}
