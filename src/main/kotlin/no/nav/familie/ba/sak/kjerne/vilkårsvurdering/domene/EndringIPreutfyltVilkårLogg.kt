package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ba.sak.ekstern.restDomene.VilkårResultatDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.sikkerhet.RollestyringMotDatabase
import java.time.LocalDate

@EntityListeners(RollestyringMotDatabase::class)
@Entity
@Table(name = "endring_i_preutfylt_vilkar_logg")
data class EndringIPreutfyltVilkårLogg(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "endring_i_preutfylt_vilkar_logg_seq_generator")
    @SequenceGenerator(
        name = "endring_i_preutfylt_vilkar_logg_seq_generator",
        sequenceName = "endring_i_preutfylt_vilkar_logg_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @ManyToOne
    @JoinColumn(name = "fk_behandling_id")
    val behandling: Behandling,
    @Enumerated(EnumType.STRING)
    @Column(name = "vilkar_type")
    val vilkårType: Vilkår,
    @Column(name = "begrunnelse")
    val begrunnelse: String?,
    @Column(name = "forrige_fom")
    val forrigeFom: LocalDate?,
    @Column(name = "ny_fom")
    val nyFom: LocalDate?,
    @Column(name = "forrige_tom")
    val forrigeTom: LocalDate?,
    @Column(name = "ny_tom")
    val nyTom: LocalDate?,
    @Enumerated(EnumType.STRING)
    @Column(name = "forrige_resultat")
    val forrigeResultat: Resultat?,
    @Enumerated(EnumType.STRING)
    @Column(name = "ny_resultat")
    val nyResultat: Resultat?,
    @Enumerated(EnumType.STRING)
    @Column(name = "forrige_vurderes_etter")
    val forrigeVurderesEtter: Regelverk?,
    @Enumerated(EnumType.STRING)
    @Column(name = "ny_vurderes_etter")
    val nyVurderesEtter: Regelverk?,
    @Convert(converter = UtdypendeVilkårsvurderingerConverter::class)
    @Column(name = "forrige_utdypende_vilkårsvurdering")
    val forrigeUtdypendeVilkårsvurdering: List<UtdypendeVilkårsvurdering>?,
    @Convert(converter = UtdypendeVilkårsvurderingerConverter::class)
    @Column(name = "ny_utdypende_vilkårsvurdering")
    val nyUtdypendeVilkårsvurdering: List<UtdypendeVilkårsvurdering>?,
) {
    companion object {
        fun opprettLoggForEndringIPreutfyltVilkår(
            behandling: Behandling,
            forrigeVilkår: VilkårResultat,
            nyttVilkår: VilkårResultatDto,
        ): EndringIPreutfyltVilkårLogg =
            EndringIPreutfyltVilkårLogg(
                behandling = behandling,
                vilkårType = nyttVilkår.vilkårType,
                begrunnelse = nyttVilkår.begrunnelse,
                forrigeFom = forrigeVilkår.periodeFom,
                nyFom = nyttVilkår.periodeFom,
                forrigeTom = forrigeVilkår.periodeTom,
                nyTom = nyttVilkår.periodeTom,
                forrigeResultat = forrigeVilkår.resultat,
                nyResultat = nyttVilkår.resultat,
                forrigeVurderesEtter = forrigeVilkår.vurderesEtter,
                nyVurderesEtter = nyttVilkår.vurderesEtter,
                forrigeUtdypendeVilkårsvurdering = forrigeVilkår.utdypendeVilkårsvurderinger,
                nyUtdypendeVilkårsvurdering = nyttVilkår.utdypendeVilkårsvurderinger,
            )
    }
}
