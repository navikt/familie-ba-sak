package no.nav.familie.ba.sak.behandling.vilkår

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.util.*
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "PersonResultat")
@Table(name = "PERSON_RESULTAT")
class PersonResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "periode_resultat_seq_generator")
        @SequenceGenerator(name = "periode_resultat_seq_generator",
                           sequenceName = "periode_resultat_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_vilkaarsvurdering_id", nullable = false, updatable = false)
        var vilkårsvurdering: Vilkårsvurdering,

        @Column(name = "person_ident", nullable = false, updatable = false)
        val personIdent: String,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "personResultat",
                   cascade = [CascadeType.ALL],
                   orphanRemoval = true
        )
        val vilkårResultater: MutableSet<VilkårResultat> = sortedSetOf(VilkårResultatComparator)

) : BaseEntitet() {

    fun setVilkårResultater(nyeVilkårResultater: Set<VilkårResultat>) {
        vilkårResultater.clear()
        vilkårResultater.addAll(nyeVilkårResultater.toSortedSet(VilkårResultatComparator))
    }

    fun getVilkårResultat(index: Int): VilkårResultat? {
        return vilkårResultater.toSortedSet(VilkårResultatComparator).elementAtOrNull(index)
    }

    fun addVilkårResultat(vilkårResultat: VilkårResultat) {
        vilkårResultater.add(vilkårResultat)
        setVilkårResultater(vilkårResultater.toSet())
        vilkårResultat.personResultat = this
    }

    fun removeVilkårResultat(vilkårResultatId: Long) {
        vilkårResultater.find { vilkårResultatId == it.id }?.personResultat = null
        setVilkårResultater(vilkårResultater.filter { vilkårResultatId != it.id }.toSet())
    }

    fun slettEllerNullstill(vilkårResultatId: Long) {
        val vilkårResultat = vilkårResultater.find { it.id == vilkårResultatId }
                             ?: throw Feil(message = "Prøver å slette et vilkår som ikke finnes",
                                           frontendFeilmelding = "Vilkåret du prøver å slette finnes ikke i systemet.")

        val perioderMedSammeVilkårType = vilkårResultater
                .filter { it.vilkårType == vilkårResultat.vilkårType && it.id != vilkårResultat.id }

        if (perioderMedSammeVilkårType.isEmpty()) {
            vilkårResultat.nullstill()
        } else {
            removeVilkårResultat(vilkårResultatId)
        }
    }

    fun kopierMedParent(vilkårsvurdering: Vilkårsvurdering): PersonResultat {
        val nyttPersonResultat = PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                personIdent = personIdent
        )
        val kopierteVilkårResultater: SortedSet<VilkårResultat> =
                vilkårResultater.map { it.kopierMedParent(nyttPersonResultat) }.toSortedSet(VilkårResultatComparator)
        nyttPersonResultat.setVilkårResultater(kopierteVilkårResultater)
        return nyttPersonResultat
    }

    companion object {

        val VilkårResultatComparator = compareBy<VilkårResultat>({ it.periodeFom }, { it.resultat }, { it.vilkårType })
    }
}