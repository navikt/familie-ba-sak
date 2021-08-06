package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.PeriodeResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.personResultaterTilPeriodeResultater
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

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
        var personResultater: Set<PersonResultat> = setOf(),

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

    fun kopier(inkluderAndreVurderinger: Boolean = false): Vilkårsvurdering {
        val nyVilkårsvurdering = Vilkårsvurdering(
                behandling = behandling,
                aktiv = aktiv,
        )

        nyVilkårsvurdering.personResultater = personResultater.map {
            it.kopierMedParent(vilkårsvurdering = nyVilkårsvurdering,
                               inkluderAndreVurderinger = inkluderAndreVurderinger)
        }.toSet()
        return nyVilkårsvurdering
    }

    fun harPersonerManglerOpplysninger(): Boolean =
            personResultater.any { personResultat ->
                personResultat.andreVurderinger.any {
                    it.type == AnnenVurderingType.OPPLYSNINGSPLIKT && it.resultat == Resultat.IKKE_OPPFYLT
                }
            }
}