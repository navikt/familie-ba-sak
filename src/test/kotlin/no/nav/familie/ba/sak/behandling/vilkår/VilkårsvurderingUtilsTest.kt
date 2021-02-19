package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.nare.Resultat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class VilkårsvurderingUtilsTest {

    private val uvesentligVilkårsvurdering = lagVilkårsvurdering(randomFnr(), lagBehandling(), Resultat.IKKE_VURDERT)

    @Test
    fun `feil kastes når det finnes løpende oppfylt ved forsøk på å legge til avslag uten periode`() {
        val personResultat = PersonResultat(vilkårsvurdering = uvesentligVilkårsvurdering, personIdent = randomFnr())
        val løpendeOppfylt = VilkårResultat(personResultat = personResultat,
                                            periodeFom = LocalDate.of(2020, 1, 1),
                                            periodeTom = null,
                                            vilkårType = Vilkår.BOR_MED_SØKER,
                                            resultat = Resultat.OPPFYLT,
                                            begrunnelse = "",
                                            behandlingId = 0,
                                            regelInput = null,
                                            regelOutput = null)
        personResultat.vilkårResultater.add(løpendeOppfylt)

        val avslagUtenPeriode = RestVilkårResultat(id = 0,
                                                   vilkårType = Vilkår.BOR_MED_SØKER,
                                                   resultat = Resultat.IKKE_OPPFYLT,
                                                   periodeFom = null,
                                                   periodeTom = null,
                                                   begrunnelse = "",
                                                   endretAv = "",
                                                   endretTidspunkt = LocalDateTime.now(),
                                                   behandlingId = 0,
                                                   erEksplisittAvslagPåSøknad = true)

        assertThrows<FunksjonellFeil> {
            VilkårsvurderingUtils.validerAvslagUtenPeriodeMedLøpende(personSomEndres = personResultat,
                                                                     vilkårSomEndres = avslagUtenPeriode)
        }
    }

    @Test
    fun `feil kastes når det finnes avslag uten periode ved forsøk på å legge til løpende oppfylt`() {
        val personResultat = PersonResultat(vilkårsvurdering = uvesentligVilkårsvurdering, personIdent = randomFnr())
        val avslagUtenPeriode = VilkårResultat(personResultat = personResultat,
                                               periodeFom = null,
                                               periodeTom = null,
                                               vilkårType = Vilkår.BOR_MED_SØKER,
                                               resultat = Resultat.IKKE_OPPFYLT,
                                               begrunnelse = "",
                                               behandlingId = 0,
                                               regelInput = null,
                                               regelOutput = null,
                                               erEksplisittAvslagPåSøknad = true)
        personResultat.vilkårResultater.add(avslagUtenPeriode)

        val løpendeOppfylt = RestVilkårResultat(id = 0,
                                                vilkårType = Vilkår.BOR_MED_SØKER,
                                                resultat = Resultat.OPPFYLT,
                                                periodeFom = LocalDate.of(2020, 1, 1),
                                                periodeTom = null,
                                                begrunnelse = "",
                                                endretAv = "",
                                                endretTidspunkt = LocalDateTime.now(),
                                                behandlingId = 0)

        assertThrows<FunksjonellFeil> {
            VilkårsvurderingUtils.validerAvslagUtenPeriodeMedLøpende(personSomEndres = personResultat,
                                                                     vilkårSomEndres = løpendeOppfylt)
        }
    }

    @Test
    fun `feil kastes ikke når når ingen periode er løpende`() {
        val personResultat = PersonResultat(vilkårsvurdering = uvesentligVilkårsvurdering, personIdent = randomFnr())
        val avslagUtenPeriode = VilkårResultat(personResultat = personResultat,
                                               periodeFom = null,
                                               periodeTom = null,
                                               vilkårType = Vilkår.BOR_MED_SØKER,
                                               resultat = Resultat.IKKE_OPPFYLT,
                                               begrunnelse = "",
                                               behandlingId = 0,
                                               regelInput = null,
                                               regelOutput = null,
                                               erEksplisittAvslagPåSøknad = true)
        personResultat.vilkårResultater.add(avslagUtenPeriode)

        val løpendeOppfylt = RestVilkårResultat(id = 0,
                                                vilkårType = Vilkår.BOR_MED_SØKER,
                                                resultat = Resultat.OPPFYLT,
                                                periodeFom = LocalDate.of(2020, 1, 1),
                                                periodeTom = LocalDate.of(2020, 6, 1),
                                                begrunnelse = "",
                                                endretAv = "",
                                                endretTidspunkt = LocalDateTime.now(),
                                                behandlingId = 0)

        assertDoesNotThrow {
            VilkårsvurderingUtils.validerAvslagUtenPeriodeMedLøpende(personSomEndres = personResultat,
                                                                     vilkårSomEndres = løpendeOppfylt)
        }
    }
}