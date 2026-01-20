package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.ekstern.restDomene.VilkårResultatDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EndringIPreutfyltVilkårLoggTest {
    @Test
    fun `opprettLoggForEndringIPreutfyltVilkår mapper forrige og nye verdier`() {
        val behandling = lagBehandling(id = 1234)
        val forrigeVilkår =
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.IKKE_OPPFYLT,
                resultatBegrunnelse = null,
                periodeFom = LocalDate.of(2020, 1, 1),
                periodeTom = LocalDate.of(2020, 12, 31),
                begrunnelse = "forrige begrunnelse",
                sistEndretIBehandlingId = behandling.id,
                erOpprinneligPreutfylt = true,
                vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
            )

        val nyttVilkår =
            VilkårResultatDto(
                id = forrigeVilkår.id,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.of(2021, 1, 1),
                periodeTom = LocalDate.of(2021, 12, 31),
                begrunnelse = "ny begrunnelse",
                endretAv = "saksbehandler",
                endretTidspunkt = LocalDateTime.now(),
                behandlingId = behandling.id,
                erVurdert = true,
                erAutomatiskVurdert = false,
                erEksplisittAvslagPåSøknad = false,
                avslagBegrunnelser = emptyList(),
                vurderesEtter = Regelverk.NASJONALE_REGLER,
                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                resultatBegrunnelse = null,
                begrunnelseForManuellKontroll = null,
            )

        val logg =
            EndringIPreutfyltVilkårLogg.opprettLoggForEndringIPreutfyltVilkår(
                behandling = behandling,
                forrigeVilkår = forrigeVilkår,
                nyttVilkår = nyttVilkår,
            )

        assertThat(logg.behandling).isEqualTo(behandling)
        assertThat(logg.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
        assertThat(logg.begrunnelse).isEqualTo("ny begrunnelse")
        assertThat(logg.forrigeFom).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(logg.nyFom).isEqualTo(LocalDate.of(2021, 1, 1))
        assertThat(logg.forrigeTom).isEqualTo(LocalDate.of(2020, 12, 31))
        assertThat(logg.nyTom).isEqualTo(LocalDate.of(2021, 12, 31))
        assertThat(logg.forrigeResultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertThat(logg.nyResultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(logg.forrigeVurderesEtter).isEqualTo(Regelverk.EØS_FORORDNINGEN)
        assertThat(logg.nyVurderesEtter).isEqualTo(Regelverk.NASJONALE_REGLER)
        assertThat(logg.forrigeUtdypendeVilkårsvurdering)
            .containsExactly(UtdypendeVilkårsvurdering.DELT_BOSTED)
        assertThat(logg.nyUtdypendeVilkårsvurdering)
            .containsExactly(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
    }

    @Test
    fun `opprettLoggForEndringIPreutfyltVilkår håndterer nullfelt`() {
        val behandling = lagBehandling(id = 5678)
        val forrigeVilkår =
            VilkårResultat(
                personResultat = null,
                vilkårType = Vilkår.GIFT_PARTNERSKAP,
                resultat = Resultat.IKKE_VURDERT,
                resultatBegrunnelse = null,
                periodeFom = null,
                periodeTom = null,
                begrunnelse = "",
                sistEndretIBehandlingId = behandling.id,
                erOpprinneligPreutfylt = true,
                vurderesEtter = null,
                utdypendeVilkårsvurderinger = emptyList(),
            )

        val nyttVilkår =
            VilkårResultatDto(
                id = forrigeVilkår.id,
                vilkårType = Vilkår.GIFT_PARTNERSKAP,
                resultat = Resultat.IKKE_VURDERT,
                periodeFom = null,
                periodeTom = null,
                begrunnelse = "",
                endretAv = "saksbehandler",
                endretTidspunkt = LocalDateTime.now(),
                behandlingId = behandling.id,
                erVurdert = false,
                erAutomatiskVurdert = false,
                erEksplisittAvslagPåSøknad = null,
                avslagBegrunnelser = emptyList(),
                vurderesEtter = null,
                utdypendeVilkårsvurderinger = emptyList(),
                resultatBegrunnelse = null,
                begrunnelseForManuellKontroll = null,
            )

        val logg =
            EndringIPreutfyltVilkårLogg.opprettLoggForEndringIPreutfyltVilkår(
                behandling = behandling,
                forrigeVilkår = forrigeVilkår,
                nyttVilkår = nyttVilkår,
            )

        assertThat(logg.forrigeFom).isNull()
        assertThat(logg.nyFom).isNull()
        assertThat(logg.forrigeTom).isNull()
        assertThat(logg.nyTom).isNull()
        assertThat(logg.forrigeResultat).isEqualTo(Resultat.IKKE_VURDERT)
        assertThat(logg.nyResultat).isEqualTo(Resultat.IKKE_VURDERT)
        assertThat(logg.forrigeVurderesEtter).isNull()
        assertThat(logg.nyVurderesEtter).isNull()
        assertThat(logg.forrigeUtdypendeVilkårsvurdering).isEmpty()
        assertThat(logg.nyUtdypendeVilkårsvurdering).isEmpty()
    }
}
