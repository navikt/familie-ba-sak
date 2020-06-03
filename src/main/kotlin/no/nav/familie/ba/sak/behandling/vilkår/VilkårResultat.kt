package no.nav.familie.ba.sak.behandling.vilkår

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate
import javax.persistence.*

@Entity(name = "VilkårResultat")
@Table(name = "VILKAR_RESULTAT")
class VilkårResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkar_resultat_seq_generator")
        @SequenceGenerator(name = "vilkar_resultat_seq_generator", sequenceName = "vilkar_resultat_seq", allocationSize = 50)
        val id: Long = 0,

        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_periode_resultat_id", nullable = false)
        var personResultat: PersonResultat,

        @Enumerated(EnumType.STRING)
        @Column(name = "vilkar")
        val vilkårType: Vilkår,

        @Enumerated(EnumType.STRING)
        @Column(name = "resultat")
        var resultat: Resultat,

        @Column(name = "periode_fom")
        val periodeFom: LocalDate? = null,

        @Column(name = "periode_tom")
        val periodeTom: LocalDate? = null,

        @Column(name = "begrunnelse", columnDefinition = "TEXT", nullable = false)
        var begrunnelse: String
) : BaseEntitet() {

    fun kopierMedParent(nyPersonResultat: PersonResultat? = null): VilkårResultat {
        return VilkårResultat(
                personResultat = nyPersonResultat ?: personResultat,
                vilkårType = vilkårType,
                resultat = resultat,
                periodeFom = if (periodeFom != null) LocalDate.from(periodeFom) else null,
                periodeTom = if (periodeTom != null) LocalDate.from(periodeTom) else null,
                begrunnelse = begrunnelse
        )
    }

    fun kopierMedNyPeriode(fom: LocalDate, tom: LocalDate): VilkårResultat {
        return VilkårResultat(
                personResultat = personResultat,
                vilkårType = vilkårType,
                resultat = resultat,
                periodeFom = fom,
                periodeTom = tom,
                begrunnelse = begrunnelse
        )
    }
}

fun RestVilkårResultat.mapNyVurdering(gammelVilkårResultat: VilkårResultat) = VilkårResultat(
        id = gammelVilkårResultat.id,
        personResultat = gammelVilkårResultat.personResultat,
        vilkårType = gammelVilkårResultat.vilkårType,
        resultat = this.resultat,
        periodeFom = this.periodeFom,
        periodeTom = this.periodeTom,
        begrunnelse = this.begrunnelse
)