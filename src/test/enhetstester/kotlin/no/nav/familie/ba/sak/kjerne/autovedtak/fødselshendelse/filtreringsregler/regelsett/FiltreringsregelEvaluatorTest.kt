package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.regelsett

import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.tilfeldigSøker
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.erOppfylt
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.Filtreringsregel
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFaktaFødselshendelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FILTRERINGSREGLER_FØDSELSHENDELSE
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsregelEvaluator

internal class FiltreringsregelEvaluatorTest {
    private val gyldigAktørId = randomAktør()
    private val filtreringsregelEvaluator = FiltreringsregelEvaluator()

    @Test
    fun `Regelevaluering skal resultere i Ja`() {
        // Arrange
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = mor,
                    barnaSomSkalVurderes = listOf(barnet),
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
        assertThat(evalueringer.erOppfylt()).isTrue
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor mottar utvidet barnetrygd`() {
        // Arrange
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = mor,
                    søkerMottarLøpendeUtvidet = true,
                    barnaSomSkalVurderes = listOf(barnet),
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
        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.Identifikator.MOR_MOTTAR_IKKE_LØPENDE_UTVIDET)
    }

    @Test
    fun `Regelevaluering skal gi resultat IKKE_OPPFYLT når mor har løpende EØS-barnetrygd`() {
        // Arrange
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = mor,
                    søkerMottarLøpendeUtvidet = false,
                    barnaSomSkalVurderes = listOf(barnet),
                    restenAvBarna = restenAvBarna,
                    søkerLever = true,
                    barnaLever = true,
                    søkerHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    søkerMottarEøsBarnetrygd = true,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.Identifikator.MOR_HAR_IKKE_LØPENDE_EØS_BARNETRYGD)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor er under 18 år`() {
        // Arrange
        val mor = tilfeldigPerson(LocalDate.now().minusYears(17)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = mor,
                    barnaSomSkalVurderes = listOf(barnet),
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
        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.Identifikator.MOR_ER_OVER_18_ÅR)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på mor`() {
        // Arrange
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = mor,
                    barnaSomSkalVurderes = listOf(barnet),
                    restenAvBarna = restenAvBarna,
                    søkerLever = false,
                    barnaLever = true,
                    søkerHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.Identifikator.MOR_LEVER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når det er registrert dødsfall på barnet`() {
        // Arrange
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = mor,
                    barnaSomSkalVurderes = listOf(barnet),
                    restenAvBarna = restenAvBarna,
                    søkerLever = true,
                    barnaLever = false,
                    søkerHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.Identifikator.BARN_LEVER)
    }

    @Test
    fun `Regelevaluering skal resultere i NEI når mor har verge`() {
        // Arrange
        val mor = tilfeldigPerson(LocalDate.now().minusYears(20)).copy(aktør = gyldigAktørId)
        val barnet = tilfeldigPerson(LocalDate.now()).copy(aktør = gyldigAktørId)
        val restenAvBarna: List<PersonInfo> = listOf()

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = mor,
                    barnaSomSkalVurderes = listOf(barnet),
                    restenAvBarna = restenAvBarna,
                    søkerLever = true,
                    barnaLever = true,
                    søkerHarVerge = true,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertThat(evalueringer.erOppfylt()).isFalse
        assertEnesteRegelMedResultatNei(evalueringer, Filtreringsregel.Identifikator.MOR_HAR_IKKE_VERGE)
    }

    @Test
    fun `Mor er under 18`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("2020-10-23"), aktør = lagAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2019-10-23"), aktør = lagAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
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
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.Identifikator.MOR_ER_OVER_18_ÅR)
    }

    @Test
    fun `Barn med mindre mellomrom enn 5mnd`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = lagAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2020-09-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
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
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.Identifikator.MER_ENN_5_MND_SIDEN_FORRIGE_BARN)
    }

    @Test
    fun `Mor lever ikke`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = lagAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    søkerLever = false,
                    barnaLever = true,
                    søkerHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.Identifikator.MOR_LEVER)
    }

    @Test
    fun `Barnet lever ikke`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = lagAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    søkerLever = true,
                    barnaLever = false,
                    søkerHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        // Assert
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.Identifikator.BARN_LEVER)
    }

    @Test
    fun `Mor har verge`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = lagAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    søkerLever = true,
                    barnaLever = true,
                    søkerHarVerge = true,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.Identifikator.MOR_HAR_IKKE_VERGE)
    }

    @Test
    fun `Mor er død og er under vergemål`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = lagAktør("21111777001"))
        val barn2PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(barn2PersonInfo),
                    søkerLever = false,
                    barnaLever = true,
                    søkerHarVerge = true,
                    erFagsakenMigrertEtterBarnFødt = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.Identifikator.MOR_LEVER)
    }

    @Test
    fun `Flere barn født`() {
        // Arrange
        val nyligFødselsdato = LocalDate.now().minusDays(2)
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = nyligFødselsdato, aktør = lagAktør("21111777001"))
        val barn2Person =
            tilfeldigPerson(fødselsdato = nyligFødselsdato, aktør = lagAktør("23128438785"))
        val barn3PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person, barn2Person),
                    restenAvBarna = listOf(barn3PersonInfo),
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
        Assertions.assertTrue(evalueringer.erOppfylt())
    }

    @Test
    fun `Mor har ugyldig fødselsnummer`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør("23236789111"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = lagAktør("21111777001"))
        val barn3PersonInfo = PersonInfo(fødselsdato = LocalDate.parse("2018-09-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(barn3PersonInfo),
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
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.Identifikator.MOR_GYLDIG_FNR)
    }

    @Test
    fun `Barn med ugyldig fødselsnummer`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"), aktør = lagAktør("23102000000"))
        val barn2Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2018-09-23"), aktør = lagAktør("23091823456"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person, barn2Person),
                    restenAvBarna = listOf(),
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
        assertIkkeOppfyltFiltreringsregel(evalueringer, Filtreringsregel.Identifikator.BARN_GYLDIG_FNR)
    }

    @Test
    fun `Fagsak migrert etter barn født`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-09-23"), aktør = lagAktør("23092023456"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    søkerLever = true,
                    barnaLever = true,
                    søkerHarVerge = false,
                    erFagsakenMigrertEtterBarnFødt = true,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    dagensDato = LocalDate.parse("2020-10-23"),
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        assertIkkeOppfyltFiltreringsregel(
            evalueringer,
            Filtreringsregel.Identifikator.FAGSAK_IKKE_MIGRERT_UT_AV_INFOTRYGD_ETTER_BARN_FØDT,
        )
    }

    @Test
    fun `Saken er godkjent fordi barnet er født i denne måneden`() {
        // Arrange
        val fødselsdatoIDenneMåned = LocalDate.now().withDayOfMonth(1)

        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"), aktør = lagAktør("04086226621"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = fødselsdatoIDenneMåned, aktør = lagAktør("23091823456"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(),
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
        Assertions.assertTrue(evalueringer.erOppfylt())
    }

    @Test
    fun `Skal returnere ikke oppfylt for regelevaluering når det allerede løper barnetrygd for barnet på annen forelder`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    søkerLever = true,
                    barnaLever = true,
                    søkerHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = true,
                    erFagsakenMigrertEtterBarnFødt = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )

        // Assert
        Assertions.assertTrue(!evalueringer.erOppfylt())
    }

    @Test
    fun `Skal returnere ikke oppfylt for regelevaluering når mor oppfyller vilkår for utvidet barnetrygd`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    søkerLever = true,
                    barnaLever = true,
                    søkerHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = true,
                    morHarIkkeOpphørtBarnetrygd = true,
                ),
            )
        // Assert
        assertThat(evalueringer.erOppfylt()).isFalse
    }

    @Test
    fun `Skal returnere oppfylt for regelevaluering når mor ikke oppfyller vilkår for utvidet barnetrygd`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(),
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
        assertThat(evalueringer.erOppfylt()).isTrue
    }

    @Test
    fun `Skal returnere ikke oppfylt for regelevaluering når mor har opphørt barnetrygd`() {
        // Arrange
        val søkerPerson =
            tilfeldigSøker(fødselsdato = LocalDate.parse("1962-10-23"))
        val barn1Person =
            tilfeldigPerson(fødselsdato = LocalDate.parse("2020-10-23"))

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_FØDSELSHENDELSE,
                FiltreringsreglerFaktaFødselshendelse(
                    søker = søkerPerson,
                    barnaSomSkalVurderes = listOf(barn1Person),
                    restenAvBarna = listOf(),
                    søkerLever = true,
                    barnaLever = true,
                    søkerHarVerge = false,
                    løperBarnetrygdForBarnetPåAnnenForelder = false,
                    erFagsakenMigrertEtterBarnFødt = false,
                    søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                    morHarIkkeOpphørtBarnetrygd = false,
                ),
            )

        // Assert
        assertThat(evalueringer.erOppfylt()).isFalse
    }

    private fun assertEnesteRegelMedResultatNei(
        evalueringer: List<Evaluering>,
        filtreringsRegel: Filtreringsregel.Identifikator,
    ) {
        assertThat(1).isEqualTo(evalueringer.filter { it.resultat == Resultat.IKKE_OPPFYLT }.size)
        assertThat(filtreringsRegel.name)
            .isEqualTo(evalueringer.filter { it.resultat == Resultat.IKKE_OPPFYLT }[0].identifikator)
    }

    fun assertIkkeOppfyltFiltreringsregel(
        evalueringer: List<Evaluering>,
        filtreringsregel: Filtreringsregel.Identifikator,
    ) {
        evalueringer.forEach {
            if (it.evalueringÅrsaker.first().hentIdentifikator() == filtreringsregel.name) {
                Assertions.assertEquals(Resultat.IKKE_OPPFYLT, it.resultat)
                return
            } else {
                Assertions.assertEquals(Resultat.OPPFYLT, it.resultat)
            }
        }
    }
}
