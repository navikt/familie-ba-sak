package no.nav.familie.ba.sak.kjerne.verge

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.math.BigInteger
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Verge")
@Table(name = "VERGE")
data class Verge(
    @Id
    @Column(name = "id", updatable = false)
    val id: BigInteger,

    @Column(name = "navn", updatable = true, length = 100)
    var navn: String,

    @Column(name = "adresse", updatable = true, length = 500)
    var adresse: String,

    @Column(name = "ident", updatable = true, length = 20)
    var ident: String?,

    @OneToOne(optional = false)
    @JoinColumn(
        name = "fk_behandling_id",
        nullable = false,
        updatable = false
    )
    val behandling: Behandling,
) : BaseEntitet()
