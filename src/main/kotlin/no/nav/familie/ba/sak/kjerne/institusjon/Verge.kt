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
@Entity(name = "Verge")
@Table(name = "VERGE")
data class Verge(
    @Id
    @Column(name = "id", updatable = false)
    val id: BigInteger,

    @Column(name = "navn", updatable = true, length = 100)
    val Navn: String,

    @Column(name = "adresse", updatable = true, length = 500)
    val Adresse: String,

    @Column(name = "ident", updatable = true, length = 20)
    val Ident: String?
) : BaseEntitet()
