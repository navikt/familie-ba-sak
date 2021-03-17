package no.nav.familie.ba.sak.behandling.vilkår

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import no.nav.familie.ba.sak.nare.Resultat
import java.time.LocalDate
import javax.persistence.*

@EntityListeners(RollestyringMotDatabase::class)
@Entity(name = "VilkårResultat")
@Table(name = "VILKAR_RESULTAT")
class VilkårResultat(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vilkar_resultat_seq_generator")
        @SequenceGenerator(name = "vilkar_resultat_seq_generator", sequenceName = "vilkar_resultat_seq", allocationSize = 50)
        val id: Long = 0,

        // Denne må være nullable=true slik at man kan slette vilkår fra person resultat
        @JsonIgnore
        @ManyToOne @JoinColumn(name = "fk_person_resultat_id")
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
        var begrunnelse: String,

        @Column(name = "fk_behandling_id", nullable = false)
        var behandlingId: Long,

        @Column(name = "er_automatisk_vurdert", nullable = false)
        var erAutomatiskVurdert: Boolean = false,

        @Column(name = "er_eksplisitt_avslag_paa_soknad")
        var erEksplisittAvslagPåSøknad: Boolean? = null,

        @Column(name = "evaluering_aarsak")
        @Convert(converter = StringListConverter::class)
        val evalueringÅrsaker: List<String> = emptyList(),

        @Column(name = "regel_input", columnDefinition = "TEXT")
        var regelInput: String?,

        @Column(name = "regel_output", columnDefinition = "TEXT")
        var regelOutput: String?,

        @OneToMany(
                fetch = FetchType.LAZY,
                mappedBy = "vilkårResultat",
                cascade = [CascadeType.ALL],
                orphanRemoval = true,
        )
        val avslagBegrunnelser: MutableSet<VedtakBegrunnelse> = mutableSetOf(),

        ) : BaseEntitet() {

    override fun toString(): String {
        return "VilkårResultat(" +
               "id=$id, " +
               "vilkårType=$vilkårType, " +
               "periodeFom=$periodeFom, " +
               "periodeTom=$periodeTom, " +
               "resultat=$resultat, " +
               "evalueringÅrsaker=$evalueringÅrsaker" +
               ")"
    }

    fun nullstill() {
        periodeFom = null
        periodeTom = null
        begrunnelse = ""
        resultat = Resultat.IKKE_VURDERT
    }

    fun oppdater(restVilkårResultat: RestVilkårResultat) {
        periodeFom = restVilkårResultat.periodeFom
        periodeTom = restVilkårResultat.periodeTom
        begrunnelse = restVilkårResultat.begrunnelse
        resultat = restVilkårResultat.resultat
        erAutomatiskVurdert = false
        erEksplisittAvslagPåSøknad = restVilkårResultat.erEksplisittAvslagPåSøknad
        oppdaterPekerTilBehandling()
    }

    fun kopierMedParent(nyPersonResultat: PersonResultat? = null): VilkårResultat {
        return VilkårResultat(
                personResultat = nyPersonResultat ?: personResultat,
                erAutomatiskVurdert = erAutomatiskVurdert,
                vilkårType = vilkårType,
                resultat = resultat,
                periodeFom = if (periodeFom != null) LocalDate.from(periodeFom) else null,
                periodeTom = if (periodeTom != null) LocalDate.from(periodeTom) else null,
                begrunnelse = begrunnelse,
                behandlingId = behandlingId,
                regelInput = regelInput,
                regelOutput = regelOutput,
                erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
        )
    }

    fun kopierMedNyPeriode(fom: LocalDate, tom: LocalDate, behandlingId: Long): VilkårResultat {
        return VilkårResultat(
                personResultat = personResultat,
                erAutomatiskVurdert = erAutomatiskVurdert,
                vilkårType = vilkårType,
                resultat = resultat,
                periodeFom = if (fom == TIDENES_MORGEN) null else fom,
                periodeTom = if (tom == TIDENES_ENDE) null else tom,
                begrunnelse = begrunnelse,
                regelInput = regelInput,
                regelOutput = regelOutput,
                behandlingId = behandlingId,
                erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
        )
    }

    fun oppdaterPekerTilBehandling() {
        behandlingId = personResultat!!.vilkårsvurdering.behandling.id
    }

    fun erAvslagUtenPeriode() = this.erEksplisittAvslagPåSøknad == true && this.periodeFom == null && this.periodeTom == null
    fun harFremtidigTom() = this.periodeTom == null || this.periodeTom!!.isAfter(LocalDate.now().sisteDagIMåned())

    companion object {

        val VilkårResultatComparator = compareBy<VilkårResultat>({ it.periodeFom }, { it.resultat }, { it.vilkårType })
    }
}