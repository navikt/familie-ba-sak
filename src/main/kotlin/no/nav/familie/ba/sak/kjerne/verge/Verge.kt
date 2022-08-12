package no.nav.familie.ba.sak.kjerne.verge

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Verge")
@Table(name = "VERGE")
data class Verge(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "verge_seq_generator")
    @SequenceGenerator(name = "verge_seq_generator", sequenceName = "verge_seq", allocationSize = 50)
    val id: Long = 0,

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
