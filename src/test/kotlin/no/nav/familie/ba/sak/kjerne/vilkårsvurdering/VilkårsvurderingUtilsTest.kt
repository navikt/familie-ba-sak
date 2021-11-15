package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.RestVilkårResultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.validerAvslagsbegrunnelse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class VilkårsvurderingUtilsTest {

    private val uvesentligVilkårsvurdering =
        lagVilkårsvurdering(randomFnr(), randomAktørId(), lagBehandling(), Resultat.IKKE_VURDERT)

    private val vilkårResultatAvslag = lagVilkårResultat(
        personResultat = mockk(),
        vilkårType = Vilkår.BOSATT_I_RIKET
    )

    @Test
    fun `feil kastes når det finnes løpende oppfylt ved forsøk på å legge til avslag uten periode`() {
        val personResultat = PersonResultat(
            vilkårsvurdering = uvesentligVilkårsvurdering,
            personIdent = randomFnr(),
            aktørId = randomAktørId()
        )
        val løpendeOppfylt = VilkårResultat(
            personResultat = personResultat,
            periodeFom = LocalDate.of(2020, 1, 1),
            periodeTom = null,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            begrunnelse = "",
            behandlingId = 0
        )
        personResultat.vilkårResultater.add(løpendeOppfylt)

        val avslagUtenPeriode = RestVilkårResultat(
            id = 123,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = null,
            periodeTom = null,
            begrunnelse = "",
            endretAv = "",
            endretTidspunkt = LocalDateTime.now(),
            behandlingId = 0,
            erEksplisittAvslagPåSøknad = true
        )

        assertThrows<FunksjonellFeil> {
            VilkårsvurderingUtils.validerAvslagUtenPeriodeMedLøpende(
                personSomEndres = personResultat,
                vilkårSomEndres = avslagUtenPeriode
            )
        }
    }

    @Test
    fun `feil kastes når det finnes avslag uten periode ved forsøk på å legge til løpende oppfylt`() {
        val personResultat = PersonResultat(
            vilkårsvurdering = uvesentligVilkårsvurdering,
            personIdent = randomFnr(),
            aktørId = randomAktørId()
        )
        val avslagUtenPeriode = VilkårResultat(
            personResultat = personResultat,
            periodeFom = null,
            periodeTom = null,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.IKKE_OPPFYLT,
            begrunnelse = "",
            behandlingId = 0,
            erEksplisittAvslagPåSøknad = true
        )
        personResultat.vilkårResultater.add(avslagUtenPeriode)

        val løpendeOppfylt = RestVilkårResultat(
            id = 123,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(2020, 1, 1),
            periodeTom = null,
            begrunnelse = "",
            endretAv = "",
            endretTidspunkt = LocalDateTime.now(),
            behandlingId = 0
        )

        assertThrows<FunksjonellFeil> {
            VilkårsvurderingUtils.validerAvslagUtenPeriodeMedLøpende(
                personSomEndres = personResultat,
                vilkårSomEndres = løpendeOppfylt
            )
        }
    }

    @Test
    fun `feil kastes ikke når når ingen periode er løpende`() {
        val personResultat = PersonResultat(
            vilkårsvurdering = uvesentligVilkårsvurdering,
            personIdent = randomFnr(),
            aktørId = randomAktørId()
        )
        val avslagUtenPeriode = VilkårResultat(
            personResultat = personResultat,
            periodeFom = null,
            periodeTom = null,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.IKKE_OPPFYLT,
            begrunnelse = "",
            behandlingId = 0,
            erEksplisittAvslagPåSøknad = true
        )
        personResultat.vilkårResultater.add(avslagUtenPeriode)

        val løpendeOppfylt = RestVilkårResultat(
            id = 123,
            vilkårType = Vilkår.BOR_MED_SØKER,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(2020, 1, 1),
            periodeTom = LocalDate.of(2020, 6, 1),
            begrunnelse = "",
            endretAv = "",
            endretTidspunkt = LocalDateTime.now(),
            behandlingId = 0
        )

        assertDoesNotThrow {
            VilkårsvurderingUtils.validerAvslagUtenPeriodeMedLøpende(
                personSomEndres = personResultat,
                vilkårSomEndres = løpendeOppfylt
            )
        }
    }

    @Test
    fun `Oppdatering av avslagbegrunnelse som ikke samsvarer med vilkår kaster feil`() {
        assertThrows<IllegalStateException> {
            validerAvslagsbegrunnelse(
                triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD)),
                vilkårResultatAvslag
            )
        }
    }
}
