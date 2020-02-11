package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.common.BaseEntitet
import javax.persistence.*

@Entity
@Table(name = "VILKAR_RESULTAT")
class VilkårResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkar_resultat_seq")
        @SequenceGenerator(name = "vilkar_resultat_seq")
        val id: Long? = null,

        @ManyToOne @JoinColumn(name = "samlet_vilkar_resultat_id")
        val samletVilkårResultat: SamletVilkårResultat? = null,

        @ManyToOne(optional = false) @JoinColumn(name = "fk_person_id", nullable = false, updatable = false)
        val person: Person,

        @Enumerated(EnumType.STRING)
        @Column(name = "vilkar")
        val vilkårType: VilkårType,

        @Enumerated(EnumType.STRING)
        @Column(name = "utfall")
        val utfallType: UtfallType
): BaseEntitet()

enum class UtfallType(val beskrivelse: String) {
    IKKE_OPPFYLT("Ikke oppfylt"),
    OPPFYLT("Oppfylt")
}
