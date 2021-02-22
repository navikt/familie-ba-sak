package no.nav.familie.ba.sak.annenvurdering

import no.nav.familie.ba.sak.behandling.vilkår.PersonResultat
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "AnnenVurdering")
@Table(name = "ANNEN_VURDERING")
data class AnnenVurdering(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "annen_vurdering_seq_generator")
        @SequenceGenerator(name = "annen_vurdering_seq_generator", sequenceName = "annen_vurdering_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne @JoinColumn(name = "fk_person_resultat_id")
        var personResultat: PersonResultat?,

        @Enumerated(EnumType.STRING)
        @Column(name = "resultat")
        var resultat: Resultat = Resultat.IKKE_VURDERT,

        @Enumerated(EnumType.STRING)
        @Column(name = "type")
        var type: AnnenVurderingType,

        @Column(name = "begrunnelse")
        var begrunnelse: String? = null
) : BaseEntitet() {
}

enum class AnnenVurderingType {
    OPPLYSNINGSPLIKT
}
