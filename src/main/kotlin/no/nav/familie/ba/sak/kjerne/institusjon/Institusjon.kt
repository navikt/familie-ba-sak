package no.nav.familie.ba.sak.kjerne.institusjon

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.math.BigInteger
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Id
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Institusjon")
@Table(name = "INSTITUSJON")
data class Institusjon(
    @Id
    @Column(name = "id", updatable = false)
    val id: BigInteger,

    @Column(name = "org_nummer", updatable = false, length = 50)
    val orgNummer: String,

    @Column(name = "tss_ekstern_id", updatable = false, length = 50)
    val tssEksternId: String,
) : BaseEntitet()
