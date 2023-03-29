package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.dataGenerator.vilkårsvurdering.lagBarnVilkårResultat
import no.nav.familie.ba.sak.dataGenerator.vilkårsvurdering.lagSøkerVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingUtils
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårsvurderingForNyBehandlingUtilsTest {

    @Test
    fun `Skal lage vilkårsvurdering med søkers vilkår satt med tom=dødsdato`() {
        val søker = lagPerson(type = PersonType.SØKER).also {
            it.dødsfall = Dødsfall(
                person = it,
                dødsfallDato = LocalDate.now(),
                dødsfallAdresse = "Adresse 1",
                dødsfallPostnummer = "1234",
                dødsfallPoststed = "Oslo"
            )
        }
        val barn = lagPerson(type = PersonType.BARN)
        val behandling = lagBehandling()
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)

        val tomPåFørsteUtvidetVilkår = LocalDate.now().minusMonths(8)

        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        val søkerVilkårResultater = lagSøkerVilkårResultat(
            søkerPersonResultat = søkerPersonResultat,
            periodeFom = LocalDate.now().minusYears(2),
            periodeTom = null,
            behandlingId = behandling.behandlingId
        ) + setOf(
            VilkårResultat(
                personResultat = søkerPersonResultat,
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now().minusYears(2),
                periodeTom = tomPåFørsteUtvidetVilkår,
                begrunnelse = "",
                behandlingId = vilkårsvurdering.behandling.behandlingId,
                utdypendeVilkårsvurderinger = emptyList()
            ),
            VilkårResultat(
                personResultat = søkerPersonResultat,
                vilkårType = Vilkår.UTVIDET_BARNETRYGD,
                resultat = Resultat.OPPFYLT,
                periodeFom = tomPåFørsteUtvidetVilkår.plusMonths(1),
                periodeTom = null,
                begrunnelse = "",
                behandlingId = vilkårsvurdering.behandling.behandlingId,
                utdypendeVilkårsvurderinger = emptyList()
            )
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnVilkårResultater = lagBarnVilkårResultat(
            barnPersonResultat = barnPersonResultat,
            barnetsFødselsdato = barn.fødselsdato,
            periodeFom = LocalDate.now().minusYears(2),
            behandlingId = behandling.behandlingId
        )

        søkerPersonResultat.setSortedVilkårResultater(søkerVilkårResultater)
        barnPersonResultat.setSortedVilkårResultater(barnVilkårResultater)

        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        val nyVilkårsvurdering = VilkårsvurderingForNyBehandlingUtils(
            personopplysningGrunnlag = PersonopplysningGrunnlag(
                behandlingId = behandling.behandlingId,
                personer = mutableSetOf(barn, søker)
            )
        ).hentVilkårsvurderingMedDødsdatoSomTomDato(
            vilkårsvurdering = vilkårsvurdering
        )
        val søkersVilkårResultater =
            nyVilkårsvurdering.personResultater.find { it.erSøkersResultater() }?.vilkårResultater
        val søkersUtvidetVilkår = søkersVilkårResultater?.filter { it.vilkårType == Vilkår.UTVIDET_BARNETRYGD }

        Assertions.assertEquals(2, søkersUtvidetVilkår?.size)

        val utvidetVilkårSortert = søkersUtvidetVilkår?.sortedBy { it.periodeTom }
        Assertions.assertEquals(tomPåFørsteUtvidetVilkår, utvidetVilkårSortert?.first()?.periodeTom)
        Assertions.assertEquals(LocalDate.now().minusYears(2), utvidetVilkårSortert?.first()?.periodeFom)

        Assertions.assertEquals(søker.dødsfall?.dødsfallDato, utvidetVilkårSortert?.last()?.periodeTom)
        Assertions.assertEquals(tomPåFørsteUtvidetVilkår.plusMonths(1), utvidetVilkårSortert?.last()?.periodeFom)

        Assertions.assertEquals(1, søkerVilkårResultater.filter { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }.size)
        Assertions.assertEquals(
            søker.dødsfall?.dødsfallDato,
            søkerVilkårResultater.first { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }.periodeTom
        )

        Assertions.assertEquals(1, søkerVilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }.size)
        Assertions.assertEquals(
            søker.dødsfall?.dødsfallDato,
            søkerVilkårResultater.first { it.vilkårType == Vilkår.BOSATT_I_RIKET }.periodeTom
        )
    }
}
