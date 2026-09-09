package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.tilfeldigSøker
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FiltreringsregelTest {
    private val gyldigAktørId = randomAktør()

    @Test
    fun `Regelevaluering skal resultere i JA når det har gått mer enn 5 måneder siden forrige barn ble født`() {
        // Arrange
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet1 = tilfeldigPerson(LocalDate.now().plusMonths(0)).copy(aktør = gyldigAktørId)
        val barnet2 = tilfeldigPerson(LocalDate.now().minusMonths(1)).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> =
            listOf(
                PersonInfo(LocalDate.now().minusMonths(8).minusDays(1)),
                PersonInfo(LocalDate.now().minusMonths(8)),
            )

        // Act
        val evaluering =
            FiltreringsregelEvaluering.merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(
                FiltreringsreglerFaktaFødselshendelse(
                    søker = mor,
                    barnaSomSkalVurderes = listOf(barnet1, barnet2),
                    restenAvBarna = restenAvBarna,
                    søkerLever = true,
                    barnaLever = true,
                    søkerHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det har gått mindre enn 5 måneder siden forrige barn ble født`() {
        // Arrange
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet1 = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val barnet2 = tilfeldigPerson(LocalDate.now().minusMonths(1)).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> =
            listOf(
                PersonInfo(LocalDate.now().minusMonths(5).minusDays(1)),
                PersonInfo(LocalDate.now().minusMonths(8)),
            )

        // Act
        val evaluering =
            FiltreringsregelEvaluering.merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(
                FiltreringsreglerFaktaFødselshendelse(
                    søker = mor,
                    barnaSomSkalVurderes = listOf(barnet1, barnet2),
                    restenAvBarna = restenAvBarna,
                    søkerLever = true,
                    barnaLever = true,
                    søkerHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Tvillinger født på samme dag skal gi oppfylt`() {
        // Arrange
        val mor =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør(randomFnr()))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = lagAktør(randomFnr()))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-10-23"))

        // Act
        val evaluering =
            FiltreringsregelEvaluering.merEnn5mndEllerMindreEnnFemDagerSidenForrigeBarn(
                FiltreringsreglerFaktaFødselshendelse(
                    søker = mor,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    søkerLever = true,
                    barnaLever = true,
                    søkerHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Filtreringsreglene skal følge en fagbestemt rekkefølge`() {
        // Arrange
        val fagbestemtFiltreringsregelrekkefølge =
            listOf(
                Filtreringsregel.Identifikator.MOR_GYLDIG_FNR,
                Filtreringsregel.Identifikator.BARN_GYLDIG_FNR,
                Filtreringsregel.Identifikator.MOR_LEVER,
                Filtreringsregel.Identifikator.BARN_LEVER,
                Filtreringsregel.Identifikator.MER_ENN_5_MND_SIDEN_FORRIGE_BARN,
                Filtreringsregel.Identifikator.MOR_ER_OVER_18_ÅR,
                Filtreringsregel.Identifikator.MOR_HAR_IKKE_VERGE,
                Filtreringsregel.Identifikator.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET,
                Filtreringsregel.Identifikator.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
                Filtreringsregel.Identifikator.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
                Filtreringsregel.Identifikator.LØPER_IKKE_BARNETRYGD_FOR_BARNET,
                Filtreringsregel.Identifikator.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
                Filtreringsregel.Identifikator.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD,
            )

        // Assert
        assertThat(Filtreringsregel.Identifikator.entries.size).isEqualTo(fagbestemtFiltreringsregelrekkefølge.size)
        assertThat(
            Filtreringsregel.Identifikator
                .entries
                .zip(fagbestemtFiltreringsregelrekkefølge)
                .all { (x, y) -> x == y },
        ).isTrue
    }
}
