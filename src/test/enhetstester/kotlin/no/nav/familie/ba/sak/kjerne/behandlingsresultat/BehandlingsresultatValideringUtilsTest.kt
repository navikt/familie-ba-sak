package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

internal class BehandlingsresultatValideringUtilsTest {

    @Test
    fun `Valider eksplisitt avlag - Skal kaste feil hvis eksplisitt avslått for barn det ikke er fremstilt krav for`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val barn1PersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = barn1.aktør,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = LocalDate.now().minusMonths(5),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erEksplisittAvslagPåSøknad = true
        )
        val barn2PersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = barn2.aktør,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = LocalDate.now().minusMonths(5),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erEksplisittAvslagPåSøknad = true
        )

        assertThrows<Feil> {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(barn1PersonResultat, barn2PersonResultat),
                personerFremstiltKravFor = listOf(barn2.aktør)
            )
        }
    }

    @Test
    fun `Valider eksplisitt avslag - Skal ikke kaste feil hvis søker er eksplisitt avslått`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søker = lagPerson(type = PersonType.SØKER)

        val søkerPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = søker.aktør,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = LocalDate.now().minusMonths(5),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
            erEksplisittAvslagPåSøknad = true
        )

        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(søkerPersonResultat),
                personerFremstiltKravFor = emptyList()
            )
        }
    }

    @Test
    fun `Valider eksplisitt avslag - Skal ikke kaste feil hvis person med eksplsitt avslag er fremstilt krav for`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)

        val barn1PersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = barn1.aktør,
            resultat = Resultat.IKKE_OPPFYLT,
            periodeFom = LocalDate.now().minusMonths(5),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erEksplisittAvslagPåSøknad = true
        )
        val barn2PersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = barn2.aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.now().minusMonths(5),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erEksplisittAvslagPåSøknad = false
        )

        assertDoesNotThrow {
            BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(
                personResultater = setOf(barn1PersonResultat, barn2PersonResultat),
                personerFremstiltKravFor = listOf(barn1.aktør, barn2.aktør)
            )
        }
    }
}
