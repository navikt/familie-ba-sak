package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.til18ÅrsVilkårsdato
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBarnVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagSøkerVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingUtils
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.finnAktørerMedUtvidetFraAndeler
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class VilkårsvurderingForNyBehandlingUtilsTest {
    @Test
    fun `Skal kun ta med aktører som hadde andeler med utvidet barnetrygd`() {
        // Arrange
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)

        val andeler =
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(2).plusMonths(1),
                    tom = YearMonth.now(),
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                    person = barn,
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.now().minusYears(1),
                    tom = YearMonth.now(),
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                    person = søker,
                ),
            )

        // Act
        val aktørerMedUtvidet =
            finnAktørerMedUtvidetFraAndeler(
                andeler = andeler,
            )

        // Assert
        assertThat(aktørerMedUtvidet).containsExactly(søker.aktør)
    }

    @Test
    fun `Skal lage vilkårsvurdering med søkers vilkår satt med tom=dødsdato`() {
        // Arrange
        val søker = lagPerson(type = PersonType.SØKER).also { it.dødsfall = Dødsfall(person = it, dødsfallDato = LocalDate.now(), dødsfallAdresse = "Adresse 1", dødsfallPostnummer = "1234", dødsfallPoststed = "Oslo") }
        val barn = lagPerson(type = PersonType.BARN)
        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val tomPåFørsteUtvidetVilkår = LocalDate.now().minusMonths(8)

        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        val søkerVilkårResultater =
            lagSøkerVilkårResultat(søkerPersonResultat = søkerPersonResultat, periodeFom = LocalDate.now().minusYears(2), periodeTom = null, behandlingId = behandling.id) +
                setOf(
                    VilkårResultat(
                        personResultat = søkerPersonResultat,
                        vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.now().minusYears(2),
                        periodeTom = tomPåFørsteUtvidetVilkår,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                        utdypendeVilkårsvurderinger = emptyList(),
                    ),
                    VilkårResultat(
                        personResultat = søkerPersonResultat,
                        vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = tomPåFørsteUtvidetVilkår.plusMonths(1),
                        periodeTom = null,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                        utdypendeVilkårsvurderinger = emptyList(),
                    ),
                )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnVilkårResultater =
            lagBarnVilkårResultat(
                barnPersonResultat = barnPersonResultat,
                barnetsFødselsdato = barn.fødselsdato,
                periodeFom = LocalDate.now().minusYears(2),
                behandlingId = behandling.id,
            )

        søkerPersonResultat.setSortedVilkårResultater(søkerVilkårResultater)
        barnPersonResultat.setSortedVilkårResultater(barnVilkårResultater)

        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        // Act
        val nyVilkårsvurdering =
            VilkårsvurderingForNyBehandlingUtils(personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id, personer = mutableSetOf(barn, søker))).hentVilkårsvurderingMedDødsdatoSomTomDato(
                vilkårsvurdering = vilkårsvurdering,
            )

        // Assert
        val søkersVilkårResultater = nyVilkårsvurdering.personResultater.find { it.erSøkersResultater() }?.vilkårResultater
        val søkersUtvidetVilkår = søkersVilkårResultater?.filter { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        assertThat(søkersUtvidetVilkår).hasSize(2)

        val utvidetVilkårSortert = søkersUtvidetVilkår?.sortedBy { it.periodeTom }

        assertThat(utvidetVilkårSortert?.first()?.periodeTom).isEqualTo(tomPåFørsteUtvidetVilkår)
        assertThat(utvidetVilkårSortert?.first()?.periodeFom).isEqualTo(LocalDate.now().minusYears(2))

        assertThat(utvidetVilkårSortert?.last()?.periodeTom).isEqualTo(søker.dødsfall?.dødsfallDato)
        assertThat(utvidetVilkårSortert?.last()?.periodeFom).isEqualTo(tomPåFørsteUtvidetVilkår.plusMonths(1))

        assertThat(søkerVilkårResultater.filter { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }).hasSize(1)
        assertThat(søkerVilkårResultater.first { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }.periodeTom).isEqualTo(søker.dødsfall?.dødsfallDato)

        assertThat(søkerVilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }).hasSize(1)
        assertThat(søkerVilkårResultater.first { it.vilkårType == Vilkår.BOSATT_I_RIKET }.periodeTom).isEqualTo(søker.dødsfall?.dødsfallDato)
    }

    @Test
    fun `Skal lage initiell vilkårsvurdering med under 18-vilkåret fra barnets fødselsdato for automatisk behandling av søknad`() {
        // Arrange
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2024, 2, 4))
        val behandling =
            lagBehandling(
                årsak = BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD,
                skalBehandlesAutomatisk = true,
            )

        // Act
        val vilkårsvurdering =
            VilkårsvurderingForNyBehandlingUtils(
                personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id, personer = mutableSetOf(barn, søker)),
            ).genererInitiellVilkårsvurdering(
                behandling = behandling,
                barnaAktørSomAlleredeErVurdert = emptyList(),
            )

        // Assert
        val under18Vilkår =
            vilkårsvurdering.personResultater
                .single { it.aktør == barn.aktør }
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.UNDER_18_ÅR }

        assertThat(under18Vilkår).hasSize(1)
        assertThat(under18Vilkår.single().periodeFom).isEqualTo(barn.fødselsdato)
        assertThat(under18Vilkår.single().periodeTom).isEqualTo(barn.fødselsdato.til18ÅrsVilkårsdato())
        assertThat(under18Vilkår.single().resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Skal lage tom initiell vilkårsvurdering for andre automatiske behandlinger enn automatisk behandling av søknad`() {
        // Arrange
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2024, 2, 4))
        val behandling =
            lagBehandling(
                årsak = BehandlingÅrsak.SATSENDRING,
                skalBehandlesAutomatisk = true,
            )

        // Act
        val vilkårsvurdering =
            VilkårsvurderingForNyBehandlingUtils(
                personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id, personer = mutableSetOf(barn, søker)),
            ).genererInitiellVilkårsvurdering(
                behandling = behandling,
                barnaAktørSomAlleredeErVurdert = emptyList(),
            )

        // Assert
        val under18Vilkår =
            vilkårsvurdering.personResultater
                .single { it.aktør == barn.aktør }
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.UNDER_18_ÅR }

        assertThat(under18Vilkår).hasSize(1)
        assertThat(under18Vilkår.single().periodeFom).isNull()
        assertThat(under18Vilkår.single().resultat).isEqualTo(Resultat.IKKE_VURDERT)
    }
}
