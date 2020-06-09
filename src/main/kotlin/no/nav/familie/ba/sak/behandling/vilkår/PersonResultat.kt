package no.nav.familie.ba.sak.behandling.vilkår

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.nare.core.evaluations.Resultat
import javax.persistence.*

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
        @ManyToOne @JoinColumn(name = "fk_behandling_resultat_id", nullable = false, updatable = false)
        var behandlingResultat: BehandlingResultat,

        @Column(name = "person_ident", nullable = false, updatable = false)
        val personIdent: String,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "personResultat",
                   cascade = [CascadeType.ALL]
        )
        @OrderBy("periode_fom")
        var vilkårResultater: Set<VilkårResultat> = setOf()

) : BaseEntitet() {

    fun getVilkårResultat(index: Int): VilkårResultat? {
        return vilkårResultater.elementAtOrNull(index)
    }

    fun nextVilkårResultat(vilkårResultat: VilkårResultat): VilkårResultat? {
        var next = false
        vilkårResultater.forEach {
            if (next) {
                return it
            }

            if (it.id == vilkårResultat.id) {
                next = true
            }
        }

        return null
    }

    fun addVilkårResultat(vilkårResultat: VilkårResultat) {
        vilkårResultater = vilkårResultater.plus(vilkårResultat)
        vilkårResultat.personResultat = this
    }

    fun removeVilkårResultat(vilkårResultatId: Long) {
        vilkårResultater.find { vilkårResultatId == it.id }?.personResultat = null
        vilkårResultater = vilkårResultater.filter { vilkårResultatId != it.id }.toSet()
    }

    fun sorterVilkårResultater() {
        vilkårResultater = vilkårResultater.toSortedSet(compareBy({ it.periodeFom }, { it.vilkårType }))
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

    fun hentSamletResultat(): BehandlingResultatType {
        return when {
            vilkårResultater.all { it.resultat == Resultat.JA } -> {
                BehandlingResultatType.INNVILGET
            }
            else -> {
                BehandlingResultatType.AVSLÅTT
            }
        }
    }
}