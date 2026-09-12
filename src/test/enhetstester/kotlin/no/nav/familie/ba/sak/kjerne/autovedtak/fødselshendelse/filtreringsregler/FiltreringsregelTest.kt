package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler

import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.tilfeldigSøker
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.Filtreringsregel.Identifikator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

internal class FiltreringsregelTest {
    private val gyldigAktørId = randomAktør()
    private val merEnn5MndSidenForrigeBarnRegel =
        FILTRERINGSREGLER_FØDSELSHENDELSE.single { it.identifikator == Identifikator.MER_ENN_5_MND_SIDEN_FORRIGE_BARN }

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
            merEnn5MndSidenForrigeBarnRegel.evaluer(
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
            merEnn5MndSidenForrigeBarnRegel.evaluer(
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
            merEnn5MndSidenForrigeBarnRegel.evaluer(
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
    fun `Filtreringsreglene for fødselshendelse skal følge en fagbestemt rekkefølge`() {
        assertThat(FILTRERINGSREGLER_FØDSELSHENDELSE.map { it.identifikator }).containsExactly(
            Identifikator.MOR_GYLDIG_FNR,
            Identifikator.BARN_GYLDIG_FNR,
            Identifikator.MOR_LEVER,
            Identifikator.BARN_LEVER,
            Identifikator.MER_ENN_5_MND_SIDEN_FORRIGE_BARN,
            Identifikator.MOR_ER_OVER_18_ÅR,
            Identifikator.MOR_HAR_IKKE_VERGE,
            Identifikator.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET,
            Identifikator.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
            Identifikator.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
            Identifikator.LØPER_IKKE_BARNETRYGD_FOR_BARNET,
            Identifikator.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
            Identifikator.MOR_HAR_IKKE_OPPHØRT_BARNETRYGD,
        )
    }

    @Test
    fun `Filtreringsreglene for søknad skal følge en fagbestemt rekkefølge`() {
        assertThat(FILTRERINGSREGLER_SØKNAD.map { it.identifikator }).containsExactly(
            Identifikator.MOR_GYLDIG_FNR,
            Identifikator.BARN_GYLDIG_FNR,
            Identifikator.MOR_LEVER,
            Identifikator.BARN_LEVER,
            Identifikator.MOR_ER_OVER_18_ÅR,
            Identifikator.MOR_HAR_IKKE_VERGE,
            Identifikator.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET,
            Identifikator.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
            Identifikator.LØPER_IKKE_BARNETRYGD_FOR_BARNET,
            Identifikator.MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO,
        )
    }

    @ParameterizedTest
    @CsvSource(
        "MOR_GYLDIG_FNR, MOR_HAR_GYLDIG_FNR, Mor har gyldig fødselsnummer, MOR_HAR_UGYLDIG_FNR, Mor har ugyldig fødselsnummer",
        "BARN_GYLDIG_FNR, BARN_HAR_GYLDIG_FNR, Barn har gyldig fødselsnummer, BARN_HAR_UGYLDIG_FNR, Barn har ugyldig fødselsnummer",
        "MOR_LEVER, MOR_LEVER, Det er ikke registrert dødsdato på mor., MOR_LEVER_IKKE, Det er registrert dødsdato på mor.",
        "BARN_LEVER, BARNET_LEVER, Det er ikke registrert dødsdato på barnet., BARNET_LEVER_IKKE, Det er registrert dødsdato på barnet.",
        "MER_ENN_5_MND_SIDEN_FORRIGE_BARN, MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL, Det har gått mer enn fem måneder siden forrige barn ble født., MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL, Det har gått mindre enn fem måneder siden forrige barn ble født.",
        "MOR_ER_OVER_18_ÅR, MOR_ER_OVER_18_ÅR, Mor er over 18 år., MOR_ER_UNDER_18_ÅR, Mor er under 18 år.",
        "MOR_HAR_IKKE_VERGE, MOR_ER_MYNDIG, Mor er myndig., MOR_ER_UNDER_VERGEMÅL, Mor er under vergemål.",
        "MOR_MOTTAR_IKKE_LØPENDE_UTVIDET, MOR_MOTTAR_IKKE_LØPENDE_UTVIDET, Mor mottar ikke utvidet barnetrygd., MOR_MOTTAR_LØPENDE_UTVIDET, Mor mottar utvidet barnetrygd.",
        "MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD, MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD, Mor har ikke løpende EØS-barnetrygd, MOR_HAR_LØPENDE_EØS_BARNETRYGD, Mor har EØS-barnetrygd",
        "FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT, FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT, Fagsaken har ikke blitt migrert fra infotrygd etter barn ble født., FAGSAK_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT, Fagsaken ble migrert fra infotrygd etter barn ble født.",
        "LØPER_IKKE_BARNETRYGD_FOR_BARNET, LØPER_IKKE_BARNETRYGD_FOR_BARNET, Det løper ikke barnetrygd for barnet på annen forelder, LØPER_ALLEREDE_FOR_ANNEN_FORELDER, Annen mottaker har barnetrygd for barnet",
        "MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO, MOR_OPPFYLLER_IKKE_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO, Mor oppfyller ikke vilkår for utvidet barnetrygd, MOR_OPPFYLLER_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO, Mor oppfyller vilkår for utvidet barnetrygd",
        "MOR_HAR_IKKE_OPPHØRT_BARNETRYGD, MOR_HAR_IKKE_OPPHØRT_BARNETRYGD, Mor har ikke opphørt barnetrygd, MOR_HAR_OPPHØRT_BARNETRYGD, Mor har vedtak om opphørt barnetrygd.",
    )
    fun `Utfallsnavn og begrunnelse som lagres i FILTRERING_RESULTAT skal være uendret`(
        identifikator: Identifikator,
        forventetOppfyltNavn: String,
        forventetOppfyltBegrunnelse: String,
        forventetIkkeOppfyltNavn: String,
        forventetIkkeOppfyltBegrunnelse: String,
    ) {
        // Act
        val oppfyltEvaluering = identifikator.tilEvaluering(erOppfylt = true)
        val ikkeOppfyltEvaluering = identifikator.tilEvaluering(erOppfylt = false)

        // Assert
        assertThat(oppfyltEvaluering.identifikator).isEqualTo(identifikator.name)
        assertThat(oppfyltEvaluering.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(oppfyltEvaluering.evalueringÅrsaker.single().toString()).isEqualTo(forventetOppfyltNavn)
        assertThat(oppfyltEvaluering.begrunnelse).isEqualTo(forventetOppfyltBegrunnelse)
        assertThat(ikkeOppfyltEvaluering.identifikator).isEqualTo(identifikator.name)
        assertThat(ikkeOppfyltEvaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertThat(ikkeOppfyltEvaluering.evalueringÅrsaker.single().toString()).isEqualTo(forventetIkkeOppfyltNavn)
        assertThat(ikkeOppfyltEvaluering.begrunnelse).isEqualTo(forventetIkkeOppfyltBegrunnelse)
    }

    @Test
    fun `Utfallsnavn skal være unike på tvers av filtreringsreglene`() {
        // Act
        val utfallsnavn =
            Identifikator.entries.flatMap { identifikator ->
                listOf(true, false).map {
                    identifikator
                        .tilEvaluering(it)
                        .evalueringÅrsaker
                        .single()
                        .toString()
                }
            }

        // Assert
        assertThat(utfallsnavn).doesNotHaveDuplicates()
    }
}
