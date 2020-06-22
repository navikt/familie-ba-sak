package no.nav.familie.ba.sak.behandling.vilkår

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
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

        // Denne må være nullable=true slik at man kan slette vilkår fra person resultat
        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_periode_resultat_id")
        var personResultat: PersonResultat?,

        @Enumerated(EnumType.STRING)
        @Column(name = "vilkar")
        val vilkårType: Vilkår,

        @Enumerated(EnumType.STRING)
        @Column(name = "resultat")
        var resultat: Resultat,

        @Column(name = "periode_fom")
        var periodeFom: LocalDate? = null,

        @Column(name = "periode_tom")
        var periodeTom: LocalDate? = null,

        @Column(name = "begrunnelse", columnDefinition = "TEXT", nullable = false)
        var begrunnelse: String
) : BaseEntitet() {

    fun nullstill() {
        periodeFom = null
        periodeTom = null
        begrunnelse = ""
        resultat = Resultat.KANSKJE
    }

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
                periodeFom = if (fom == TIDENES_MORGEN) null else fom,
                periodeTom = if (tom == TIDENES_ENDE) null else tom,
                begrunnelse = begrunnelse
        )
    }
}