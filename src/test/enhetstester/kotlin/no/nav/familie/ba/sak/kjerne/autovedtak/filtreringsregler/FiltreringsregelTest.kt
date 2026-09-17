package no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler

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
    private val merEnn5MndEllerMindreEnnFemDagerSidenForrigeBarn =
        FILTRERINGSREGLER_FØDSELSHENDELSE.single { it.identifikator == Filtreringsregel.Identifikator.MER_ENN_5_MND_SIDEN_FORRIGE_BARN }

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
            merEnn5MndEllerMindreEnnFemDagerSidenForrigeBarn.evaluer(
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
            merEnn5MndEllerMindreEnnFemDagerSidenForrigeBarn.evaluer(
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
            merEnn5MndEllerMindreEnnFemDagerSidenForrigeBarn.evaluer(
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
    fun `skal kjøre filtreringsreglene for fødselshendelse i fagbestemt rekkefølge`() {
        // Act
        val identifikatorer = FILTRERINGSREGLER_FØDSELSHENDELSE.map { it.identifikator }

        // Assert
        assertThat(identifikatorer).containsExactly(
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
    }

    @Test
    fun `skal kjøre filtreringsreglene for søknad i fagbestemt rekkefølge`() {
        // Act
        val identifikatorer = FILTRERINGSREGLER_SØKNAD.map { it.identifikator }

        // Assert
        assertThat(identifikatorer).containsExactly(
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_D_NUMMER,
            Filtreringsregel.Identifikator.BARN_HAR_IKKE_D_NUMMER,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19,
            Filtreringsregel.Identifikator.BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19,
            Filtreringsregel.Identifikator.SØKER_GYLDIG_FNR,
            Filtreringsregel.Identifikator.BARN_GYLDIG_FNR,
            Filtreringsregel.Identifikator.SØKER_LEVER,
            Filtreringsregel.Identifikator.BARN_LEVER,
            Filtreringsregel.Identifikator.SØKER_ER_OVER_18_ÅR,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_VERGE,
            Filtreringsregel.Identifikator.SØKER_MOTTAR_IKKE_LØPENDE_UTVIDET,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
            Filtreringsregel.Identifikator.UTBETALES_IKKE_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN,
            Filtreringsregel.Identifikator.SØKNADEN_INNEHOLDER_IKKE_VEDLEGG,
        )
    }

    @Test
    fun `skal ha de aktive filtreringsreglene for fødselshendelse og søknad`() {
        // Act
        val identifikatorer = (FILTRERINGSREGLER_FØDSELSHENDELSE + FILTRERINGSREGLER_SØKNAD).map { it.identifikator }.toSet()

        // Assert
        assertThat(identifikatorer).containsExactlyInAnyOrder(
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
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_D_NUMMER,
            Filtreringsregel.Identifikator.BARN_HAR_IKKE_D_NUMMER,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19,
            Filtreringsregel.Identifikator.BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19,
            Filtreringsregel.Identifikator.SØKER_GYLDIG_FNR,
            Filtreringsregel.Identifikator.SØKER_LEVER,
            Filtreringsregel.Identifikator.SØKER_ER_OVER_18_ÅR,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_VERGE,
            Filtreringsregel.Identifikator.SØKER_MOTTAR_IKKE_LØPENDE_UTVIDET,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD,
            Filtreringsregel.Identifikator.UTBETALES_IKKE_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN,
            Filtreringsregel.Identifikator.SØKER_HAR_IKKE_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN,
            Filtreringsregel.Identifikator.SØKNADEN_INNEHOLDER_IKKE_VEDLEGG,
        )
    }

    @Test
    fun `skal beholde utfallsnavn, identifikator og beskrivelse som er persistert i FILTRERING_RESULTAT`() {
        // Arrange
        val persisterteUtfall =
            listOf(
                PersistertUtfall("MOR_HAR_GYLDIG_FNR", "MOR_GYLDIG_FNR", "Mor har gyldig fødselsnummer"),
                PersistertUtfall("MOR_HAR_UGYLDIG_FNR", "MOR_GYLDIG_FNR", "Mor har ugyldig fødselsnummer"),
                PersistertUtfall("BARN_HAR_GYLDIG_FNR", "BARN_GYLDIG_FNR", "Barn har gyldig fødselsnummer"),
                PersistertUtfall("BARN_HAR_UGYLDIG_FNR", "BARN_GYLDIG_FNR", "Barn har ugyldig fødselsnummer"),
                PersistertUtfall("MOR_LEVER", "MOR_LEVER", "Det er ikke registrert dødsdato på mor."),
                PersistertUtfall("MOR_LEVER_IKKE", "MOR_LEVER", "Det er registrert dødsdato på mor."),
                PersistertUtfall("BARNET_LEVER", "BARN_LEVER", "Det er ikke registrert dødsdato på barnet."),
                PersistertUtfall("BARNET_LEVER_IKKE", "BARN_LEVER", "Det er registrert dødsdato på barnet."),
                PersistertUtfall("MER_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL", "MER_ENN_5_MND_SIDEN_FORRIGE_BARN", "Det har gått mer enn fem måneder siden forrige barn ble født."),
                PersistertUtfall("MINDRE_ENN_5_MND_SIDEN_FORRIGE_BARN_UTFALL", "MER_ENN_5_MND_SIDEN_FORRIGE_BARN", "Det har gått mindre enn fem måneder siden forrige barn ble født."),
                PersistertUtfall("MOR_ER_OVER_18_ÅR", "MOR_ER_OVER_18_ÅR", "Mor er over 18 år."),
                PersistertUtfall("MOR_ER_UNDER_18_ÅR", "MOR_ER_OVER_18_ÅR", "Mor er under 18 år."),
                PersistertUtfall("MOR_ER_MYNDIG", "MOR_HAR_IKKE_VERGE", "Mor er myndig."),
                PersistertUtfall("MOR_ER_UNDER_VERGEMÅL", "MOR_HAR_IKKE_VERGE", "Mor er under vergemål."),
                PersistertUtfall("MOR_MOTTAR_IKKE_LØPENDE_UTVIDET", "MOR_MOTTAR_IKKE_LØPENDE_UTVIDET", "Mor mottar ikke utvidet barnetrygd."),
                PersistertUtfall("MOR_MOTTAR_LØPENDE_UTVIDET", "MOR_MOTTAR_IKKE_LØPENDE_UTVIDET", "Mor mottar utvidet barnetrygd."),
                PersistertUtfall("MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD", "MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD", "Mor har ikke løpende EØS-barnetrygd"),
                PersistertUtfall("MOR_HAR_LØPENDE_EØS_BARNETRYGD", "MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD", "Mor har EØS-barnetrygd"),
                PersistertUtfall("FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT", "FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT", "Fagsaken har ikke blitt migrert fra infotrygd etter barn ble født."),
                PersistertUtfall("FAGSAK_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT", "FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT", "Fagsaken ble migrert fra infotrygd etter barn ble født."),
                PersistertUtfall("LØPER_IKKE_BARNETRYGD_FOR_BARNET", "LØPER_IKKE_BARNETRYGD_FOR_BARNET", "Det løper ikke barnetrygd for barnet på annen forelder"),
                PersistertUtfall("LØPER_ALLEREDE_FOR_ANNEN_FORELDER", "LØPER_IKKE_BARNETRYGD_FOR_BARNET", "Annen mottaker har barnetrygd for barnet"),
                PersistertUtfall("MOR_OPPFYLLER_IKKE_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO", "MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO", "Mor oppfyller ikke vilkår for utvidet barnetrygd"),
                PersistertUtfall("MOR_OPPFYLLER_VILKÅR_FOR_UTVIDET_BARNETRYGD_VED_FØDSELSDATO", "MOR_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR_VED_FØDSELSDATO", "Mor oppfyller vilkår for utvidet barnetrygd"),
                PersistertUtfall("MOR_HAR_IKKE_OPPHØRT_BARNETRYGD", "MOR_HAR_IKKE_OPPHØRT_BARNETRYGD", "Mor har ikke opphørt barnetrygd"),
                PersistertUtfall("MOR_HAR_OPPHØRT_BARNETRYGD", "MOR_HAR_IKKE_OPPHØRT_BARNETRYGD", "Mor har vedtak om opphørt barnetrygd."),
                PersistertUtfall("SØKER_HAR_GYLDIG_FNR", "SØKER_GYLDIG_FNR", "Søker har gyldig fødselsnummer"),
                PersistertUtfall("SØKER_HAR_UGYLDIG_FNR", "SØKER_GYLDIG_FNR", "Søker har ugyldig fødselsnummer"),
                PersistertUtfall("SØKER_HAR_IKKE_D_NUMMER", "SØKER_HAR_IKKE_D_NUMMER", "Søker har ikke d-nummer"),
                PersistertUtfall("SØKER_HAR_D_NUMMER", "SØKER_HAR_IKKE_D_NUMMER", "Søker har d-nummer"),
                PersistertUtfall("BARN_HAR_IKKE_D_NUMMER", "BARN_HAR_IKKE_D_NUMMER", "Barn har ikke d-nummer"),
                PersistertUtfall("BARN_HAR_D_NUMMER", "BARN_HAR_IKKE_D_NUMMER", "Barn har d-nummer"),
                PersistertUtfall(
                    "SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
                    "SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
                    "Søker har ikke adressebeskyttelse gradering 6 eller 19",
                ),
                PersistertUtfall(
                    "SØKER_HAR_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
                    "SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
                    "Søker har adressebeskyttelse gradering 6 eller 19",
                ),
                PersistertUtfall(
                    "BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
                    "BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
                    "Barn har ikke adressebeskyttelse gradering 6 eller 19",
                ),
                PersistertUtfall(
                    "BARN_HAR_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
                    "BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19",
                    "Barn har adressebeskyttelse gradering 6 eller 19",
                ),
                PersistertUtfall("SØKER_LEVER", "SØKER_LEVER", "Det er ikke registrert dødsdato på søker."),
                PersistertUtfall("SØKER_LEVER_IKKE", "SØKER_LEVER", "Det er registrert dødsdato på søker."),
                PersistertUtfall("SØKER_ER_OVER_18_ÅR", "SØKER_ER_OVER_18_ÅR", "Søker er over 18 år."),
                PersistertUtfall("SØKER_ER_UNDER_18_ÅR", "SØKER_ER_OVER_18_ÅR", "Søker er under 18 år."),
                PersistertUtfall("SØKER_ER_MYNDIG", "SØKER_HAR_IKKE_VERGE", "Søker er myndig."),
                PersistertUtfall("SØKER_ER_UNDER_VERGEMÅL", "SØKER_HAR_IKKE_VERGE", "Søker er under vergemål."),
                PersistertUtfall(
                    "SØKER_MOTTAR_IKKE_LØPENDE_UTVIDET",
                    "SØKER_MOTTAR_IKKE_LØPENDE_UTVIDET",
                    "Søker mottar ikke utvidet barnetrygd.",
                ),
                PersistertUtfall(
                    "SØKER_MOTTAR_LØPENDE_UTVIDET",
                    "SØKER_MOTTAR_IKKE_LØPENDE_UTVIDET",
                    "Søker mottar utvidet barnetrygd.",
                ),
                PersistertUtfall(
                    "SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD",
                    "SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD",
                    "Søker har ikke løpende EØS-barnetrygd",
                ),
                PersistertUtfall(
                    "SØKER_HAR_LØPENDE_EØS_BARNETRYGD",
                    "SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD",
                    "Søker har EØS-barnetrygd",
                ),
                PersistertUtfall(
                    "SØKER_OPPFYLLER_IKKE_VILKÅR_FOR_UTVIDET_BARNETRYGD",
                    "SØKER_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR",
                    "Søker oppfyller ikke vilkår for utvidet barnetrygd",
                ),
                PersistertUtfall(
                    "SØKER_OPPFYLLER_VILKÅR_FOR_UTVIDET_BARNETRYGD",
                    "SØKER_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR",
                    "Søker oppfyller vilkår for utvidet barnetrygd",
                ),
                PersistertUtfall(
                    "UTBETALES_IKKE_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED",
                    "UTBETALES_IKKE_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED",
                    "Det utbetales ikke barnetrygd for barnet til annen mottaker i inneværende måned",
                ),
                PersistertUtfall(
                    "UTBETALES_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED",
                    "UTBETALES_IKKE_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED",
                    "Det utbetales barnetrygd for barnet til annen mottaker i inneværende måned",
                ),
                PersistertUtfall(
                    "SØKER_HAR_IKKE_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN",
                    "SØKER_HAR_IKKE_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN",
                    "Søker har ikke krysset på EØS-spørsmål i søknaden",
                ),
                PersistertUtfall(
                    "SØKER_HAR_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN",
                    "SØKER_HAR_IKKE_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN",
                    "Søker har krysset på EØS-spørsmål i søknaden",
                ),
                PersistertUtfall(
                    "SØKER_HAR_IKKE_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN",
                    "SØKER_HAR_IKKE_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN",
                    "Søker har ikke krysset for delt bosted for noen av barna i søknaden",
                ),
                PersistertUtfall(
                    "SØKER_HAR_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN",
                    "SØKER_HAR_IKKE_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN",
                    "Søker har krysset for delt bosted for minst ett av barna i søknaden",
                ),
                PersistertUtfall(
                    "SØKER_HAR_IKKE_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN",
                    "SØKER_HAR_IKKE_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN",
                    "Søker har ikke krysset for at noen av barna er i fosterhjem eller beredskapshjem i søknaden",
                ),
                PersistertUtfall(
                    "SØKER_HAR_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN",
                    "SØKER_HAR_IKKE_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN",
                    "Søker har krysset for at minst ett av barna er i fosterhjem eller beredskapshjem i søknaden",
                ),
                PersistertUtfall(
                    "SØKNADEN_INNEHOLDER_IKKE_VEDLEGG",
                    "SØKNADEN_INNEHOLDER_IKKE_VEDLEGG",
                    "Søknaden inneholder ikke vedlegg",
                ),
                PersistertUtfall(
                    "SØKNADEN_INNEHOLDER_VEDLEGG",
                    "SØKNADEN_INNEHOLDER_IKKE_VEDLEGG",
                    "Søknaden inneholder vedlegg",
                ),
            )

        // Act
        val utfall =
            Filtreringsregel.Identifikator.entries
                .flatMap { listOf(it.oppfylt, it.ikkeOppfylt) }
                .map { PersistertUtfall(it.hentNavn(), it.hentIdentifikator(), it.hentBeskrivelse()) }

        // Assert
        assertThat(utfall).containsExactlyInAnyOrderElementsOf(persisterteUtfall)
    }

    private data class PersistertUtfall(
        val navn: String,
        val identifikator: String,
        val beskrivelse: String,
    )
}
