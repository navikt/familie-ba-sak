package no.nav.familie.ba.sak.common

import java.time.LocalDate
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class DatoIntervallEntitet(
    @Column(name = "fom")
    val fom: LocalDate? = null,

    @Column(name = "tom")
    val tom: LocalDate? = null
)
