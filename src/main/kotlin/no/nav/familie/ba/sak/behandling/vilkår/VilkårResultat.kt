package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.nare.core.evaluations.Resultat
import javax.persistence.*

@Entity
@Table(name = "VILKAR_RESULTAT")
class VilkårResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkar_resultat_seq_generator")
        @SequenceGenerator(name = "vilkar_resultat_seq_generator", sequenceName = "vilkar_resultat_seq", allocationSize = 50)
        val id: Long = 0,

        @ManyToOne @JoinColumn(name = "samlet_vilkar_resultat_id")
        var samletVilkårResultat: SamletVilkårResultat? = null,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_person_id", nullable = false, updatable = false)
        val person: Person,

        @Enumerated(EnumType.STRING)
        @Column(name = "vilkar")
        val vilkårType: Vilkår,

        @Enumerated(EnumType.STRING)
        @Column(name = "resultat")
        val resultat: Resultat
) : BaseEntitet()
