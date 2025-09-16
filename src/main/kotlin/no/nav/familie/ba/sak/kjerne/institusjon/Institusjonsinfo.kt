package no.nav.familie.ba.sak.kjerne.institusjon

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Institusjonsinfo")
@Table(name = "institusjon_info")
data class Institusjonsinfo(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "institusjon_info_seq_generator")
    @SequenceGenerator(
        name = "institusjon_info_seq_generator",
        sequenceName = "institusjon_info_seq",
        allocationSize = 50,
    )
    var id: Long? = null,
    @ManyToOne(optional = false)
    @JoinColumn(
        name = "fk_institusjon_id",
        nullable = false,
        updatable = false,
    )
    val institusjon: Institusjon,
    @Column(name = "fk_behandling_id")
    val behandlingId: Long,
    @Column(name = "type")
    val type: String,
    @Column(name = "navn")
    val navn: String,
    @Column(name = "adresselinje1")
    val adresselinje1: String?,
    @Column(name = "adresselinje2")
    val adresselinje2: String?,
    @Column(name = "adresselinje3")
    val adresselinje3: String?,
    @Column(name = "postnummer")
    val postnummer: String,
    @Column(name = "poststed")
    val poststed: String,
    @Column(name = "kommunenummer")
    val kommunenummer: String?,
    @Embedded
    open var gyldighetsperiode: DatoIntervallEntitet,
) : BaseEntitet()
