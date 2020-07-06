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
)
