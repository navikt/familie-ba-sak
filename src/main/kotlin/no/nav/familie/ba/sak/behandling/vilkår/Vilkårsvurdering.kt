package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.beregning.domene.personResultaterTilPeriodeResultater
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "Vilkårsvurdering")
@Table(name = "VILKAARSVURDERING")
data class Vilkårsvurdering(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkaarsvurdering_seq_generator")
        @SequenceGenerator(name = "vilkaarsvurdering_seq_generator",
                           sequenceName = "vilkaarsvurdering_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @ManyToOne(optional = false)
        @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
        val behandling: Behandling,

        @Column(name = "aktiv", nullable = false)
        var aktiv: Boolean = true,

        @OneToMany(fetch = FetchType.EAGER,
                   mappedBy = "vilkårsvurdering",
                   cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH]
        )
        var personResultater: Set<PersonResultat> = setOf()

) : BaseEntitet() {

    override fun toString(): String {
        return "Vilkårsvurdering(id=$id, behandling=${behandling.id})"
    }

    fun periodeResultater(brukMåned: Boolean): Set<PeriodeResultat> = this.personResultaterTilPeriodeResultater(brukMåned)

    fun hentInnvilgedePerioder(personopplysningGrunnlag: PersonopplysningGrunnlag): Pair<List<PeriodeResultat>, List<PeriodeResultat>> {
        val periodeResultater = periodeResultater(false)

        val identBarnMap = personopplysningGrunnlag.barna
                .associateBy { it.personIdent.ident }

        val innvilgetPeriodeResultatSøker = periodeResultater.filter {
            it.personIdent == personopplysningGrunnlag.søker.personIdent.ident && it.allePåkrevdeVilkårErOppfylt(
                    PersonType.SØKER
            )
        }
        val innvilgedePeriodeResultatBarna = periodeResultater.filter {
            identBarnMap.containsKey(it.personIdent) && it.allePåkrevdeVilkårErOppfylt(
                    PersonType.BARN
            )
        }

        return Pair(innvilgetPeriodeResultatSøker, innvilgedePeriodeResultatBarna)
    }

    fun kopier(): Vilkårsvurdering {
        val nyVilkårsvurdering = Vilkårsvurdering(
                behandling = behandling,
                aktiv = aktiv,
        )

        nyVilkårsvurdering.personResultater = personResultater.map { it.kopierMedParent(nyVilkårsvurdering) }.toSet()
        return nyVilkårsvurdering
    }
}

data class OppfyltPeriode(
        val barn: PeriodeResultat,
        val søker: PeriodeResultat
)