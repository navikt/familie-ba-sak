package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import lagSvarFraEcbMock
import mockAutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.tilddMMyyyy
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.cucumber.domeneparser.BrevBegrunnelseParser.mapBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.mapForventetVedtaksperioderMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.parseAktørId
import no.nav.familie.ba.sak.cucumber.domeneparser.parseAdresser
import no.nav.familie.ba.sak.cucumber.domeneparser.parseBoolean
import no.nav.familie.ba.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseLong
import no.nav.familie.ba.sak.cucumber.domeneparser.parseString
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ba.sak.cucumber.mock.CucumberMock
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockAutovedtakFinnmarkstilleggService
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockAutovedtakSvalbardtilleggService
import no.nav.familie.ba.sak.cucumber.mock.komponentMocks.mockFeatureToggleService
import no.nav.familie.ba.sak.cucumber.mock.mockAutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.FinnmarkstilleggData
import no.nav.familie.ba.sak.kjerne.autovedtak.SvalbardtilleggData
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.brev.LANDKODER
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent.lagBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfall
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseMedData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.tilbakekrevingsvedtakmotregning.TilbakekrevingsvedtakMotregning
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.endringstidspunkt.utledEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentGyldigeBegrunnelserForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.genererVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sanity.SanityData
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate

val sanityBegrunnelserMock = SanityData.hentSanityBegrunnelserMap()
val sanityEØSBegrunnelserMock = SanityData.hentSanityEØSBegrunnelserMap()

@Suppress("ktlint:standard:function-naming")
class VedtaksperioderOgBegrunnelserStepDefinition {
    var fagsaker: MutableMap<Long, Fagsak> = mutableMapOf()
    var behandlinger = mutableMapOf<Long, Behandling>()
    var behandlingTilForrigeBehandling = mutableMapOf<Long, Long?>()
    var vedtaksliste = mutableListOf<Vedtak>()
    var persongrunnlag = mutableMapOf<Long, PersonopplysningGrunnlag>()
    var vilkårsvurderinger = mutableMapOf<Long, Vilkårsvurdering>()
    var vedtaksperioderMedBegrunnelser = listOf<VedtaksperiodeMedBegrunnelser>()
    var kompetanser = mutableMapOf<Long, List<Kompetanse>>()
    var valutakurs = mutableMapOf<Long, List<Valutakurs>>()
    var utenlandskPeriodebeløp = mutableMapOf<Long, List<UtenlandskPeriodebeløp>>()
    var tilbakekrevingsvedtakMotregning = mutableMapOf<Long, TilbakekrevingsvedtakMotregning>()
    var endredeUtbetalinger = mutableMapOf<Long, List<EndretUtbetalingAndel>>()
    var tilkjenteYtelser = mutableMapOf<Long, TilkjentYtelse>()
    var overstyrteEndringstidspunkt = mutableMapOf<Long, LocalDate>()
    var overgangsstønader = mutableMapOf<Long, List<InternPeriodeOvergangsstønad>>()
    var totrinnskontroller = mutableMapOf<Long, Totrinnskontroll>()
    var uregistrerteBarn = listOf<BarnMedOpplysninger>()
    var dagensDato: LocalDate = LocalDate.now()
    var toggles = mapOf<Long, Map<String, Boolean>>()
    var adresser = mutableMapOf<String, PdlAdresserPerson>()

    var utvidetVedtaksperiodeMedBegrunnelser = listOf<UtvidetVedtaksperiodeMedBegrunnelser>()

    var målform: Målform = Målform.NB
    var søknadstidspunkt: LocalDate? = null
    var personerFremstiltKravFor = mapOf<Long, List<Aktør>>()

    /**
     * Mulige verdier: | FagsakId | Fagsaktype | Status |
     */
    @Gitt("følgende fagsaker")
    fun `følgende fagsaker`(dataTable: DataTable) {
        fagsaker = lagFagsaker(dataTable)
    }

    /**
     * Mulige felter:
     * | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Behandlingsstatus |
     */
    @Gitt("følgende behandlinger")
    fun `følgende behandlinger`(dataTable: DataTable) {
        lagVedtak(
            dataTable = dataTable,
            behandlinger = behandlinger,
            behandlingTilForrigeBehandling = behandlingTilForrigeBehandling,
            vedtaksListe = vedtaksliste,
            fagsaker = fagsaker,
        )
    }

    @Og("med følgende feature toggles")
    fun følgendeFeatureToggles(dataTable: DataTable) {
        toggles =
            dataTable
                .groupByBehandlingId()
                .mapValues {
                    it.value.associate { rad ->
                        val featureToggleId = parseString(Domenebegrep.FEATURE_TOGGLE_ID, rad)
                        val featureToggleVerdi = parseBoolean(Domenebegrep.ER_FEATURE_TOGGLE_TOGGLET_PÅ, rad)
                        featureToggleId to featureToggleVerdi
                    }
                }
    }

    /**
     * Mulige verdier: | BehandlingId |  AktørId | Persontype | Fødselsdato |
     */
    @Og("følgende persongrunnlag")
    fun `følgende persongrunnlag`(dataTable: DataTable) {
        val personGrunnlagMap = lagPersonGrunnlag(dataTable)
        persongrunnlag.putAll(personGrunnlagMap)

        fagsaker =
            fagsaker
                .mapValues { (_, fagsak) ->
                    val behandlingerPåFagsak = behandlinger.values.filter { it.fagsak.id == fagsak.id }
                    val søkerAktør = persongrunnlag[behandlingerPåFagsak.first().id]!!.søker.aktør
                    fagsak.copy(aktør = søkerAktør)
                }.toMutableMap()

        behandlinger =
            behandlinger
                .mapValues { (_, behandling) ->
                    behandling.copy(fagsak = fagsaker[behandling.fagsak.id]!!)
                }.toMutableMap()
    }

    @Og("dagens dato er {}")
    fun `dagens dato er`(dagensDatoString: String) {
        dagensDato = parseDato(dagensDatoString)
    }

    /**
     * Mulige verdier: | BehandlingId | AktørId |
     */
    @Og("med personer fremstilt krav for")
    fun `med personer fremstilt krav for`(dataTable: DataTable) {
        personerFremstiltKravFor =
            dataTable
                .asMaps()
                .map { rad ->
                    val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
                    val person = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId).personer.find { parseAktørId(rad) == it.aktør.aktørId } ?: throw Feil("Person fremstilt krav for finnes ikke i persongrunnlag")
                    Pair(behandlingId, person.aktør)
                }.groupBy({ it.first }, { it.second })
    }

    @Og("lag personresultater for behandling {}")
    fun `lag personresultater for begrunnelse`(behandlingId: Long) {
        val persongrunnlagForBehandling = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId)
        val behandling = behandlinger.finnBehandling(behandlingId)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlagForBehandling, behandling)
        vilkårsvurderinger[behandlingId] = vilkårsvurdering
    }

    /**
     * Mulige verdier: | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Vurderes etter |
     */
    @Og("legg til nye vilkårresultater for behandling {}")
    fun `legg til nye vilkårresultater for behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val vilkårResultaterPerPerson =
            dataTable.asMaps().groupBy { parseAktørId(it) }
        val personResultatForBehandling =
            vilkårsvurderinger[behandlingId]?.personResultater
                ?: throw Feil("Finner ikke personresultater for behandling med id $behandlingId")

        vilkårsvurderinger[behandlingId]?.personResultater =
            leggTilVilkårResultatPåPersonResultat(personResultatForBehandling, vilkårResultaterPerPerson, behandlingId)
    }

    @Og("fyll ut vikårresultater for behandling {} fra dato {}")
    fun `fyll ut vikårresultater for behandling fra dato`(
        behandlingId: Long,
        fraDato: String? = null,
    ) {
        CucumberMock(
            this,
            behandlingId,
        ).testVerktøyService.oppdaterVilkårUtenFomTilFødselsdato(behandlingId, parseValgfriDato(fraDato))
    }

    @Og("kopier vilkårresultater fra behandling {} til behandling {}")
    fun `kopier vilkårresultater fra behandling til behandling`(
        fraBehandlingId: Long,
        tilBehandlingId: Long,
    ) {
        val personResultatForFraBehandling =
            vilkårsvurderinger[fraBehandlingId]?.personResultater
                ?: throw Feil("Finner ikke personresultater for behandling med id $fraBehandlingId")

        val vilkårsvurderingTilBehandling =
            vilkårsvurderinger[tilBehandlingId]
                ?: throw Feil("Finner ikke vilkårsvurdering for behandling med id $tilBehandlingId")

        vilkårsvurderinger[tilBehandlingId]?.personResultater =
            personResultatForFraBehandling
                .map { it.apply { vilkårsvurdering = vilkårsvurderingTilBehandling } }
                .toSet()
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | Resultat | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
     */
    @Og("med kompetanser")
    fun `med kompetanser`(dataTable: DataTable) {
        val nyeKompetanserPerBarn = dataTable.asMaps()
        kompetanser = lagKompetanser(nyeKompetanserPerBarn, persongrunnlag)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | BehandlingId | Valutakursdato | Valuta kode | Kurs
     */

    @Og("med valutakurser")
    fun `med valutakurser`(dataTable: DataTable) {
        val nyeValutakursPerBarn = dataTable.asMaps()
        valutakurs = lagValutakurs(nyeValutakursPerBarn, persongrunnlag)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland
     */
    @Og("med utenlandsk periodebeløp")
    fun `med utenlandsk periodebeløp`(dataTable: DataTable) {
        val nyeUtenlandskPeriodebeløpPerBarn = dataTable.asMaps()
        utenlandskPeriodebeløp = lagUtenlandskperiodeBeløp(nyeUtenlandskPeriodebeløpPerBarn, persongrunnlag)
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | BehandlingId |  Årsak | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
     */
    @Og("med endrede utbetalinger")
    fun `med endrede utbetalinger`(dataTable: DataTable) {
        val nyeEndredeUtbetalingAndeler = dataTable.asMaps()
        endredeUtbetalinger = lagEndredeUtbetalinger(nyeEndredeUtbetalingAndeler, persongrunnlag)
    }

    /**
     * Mulige verdier: | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |
     */
    @Og("med andeler tilkjent ytelse")
    fun `med andeler tilkjent ytelse`(dataTable: DataTable) {
        tilkjenteYtelser =
            lagTilkjentYtelse(
                dataFraCucumber = this,
                dataTable = dataTable,
                behandlinger = behandlinger,
                personGrunnlag = persongrunnlag,
                vedtaksliste = vedtaksliste,
                behandlingTilForrigeBehandling = behandlingTilForrigeBehandling,
            )
    }

    /**
     * Mulige verdier: | BehandlingId | AktørId | Fra dato | Til dato |
     */
    @Og("med overgangsstønad")
    fun `med overgangsstønad`(dataTable: DataTable) {
        overgangsstønader =
            lagOvergangsstønad(
                dataTable = dataTable,
                persongrunnlag = persongrunnlag,
                tidligereBehandlinger = behandlingTilForrigeBehandling,
                dagensDato = dagensDato,
            ).toMutableMap()
    }

    @Og("med uregistrerte barn")
    fun `med uregistrerte barn`() {
        uregistrerteBarn = listOf(BarnMedOpplysninger(ident = ""))
    }

    /**
     * Mulige verdier: | Fra dato | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
     */
    @Og("når disse begrunnelsene er valgt for behandling {}")
    fun `når disse begrunnelsene er valgt for behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val vedtaksperioder = genererVedtaksperioderForBehandling(behandlingId)

        vedtaksperioderMedBegrunnelser =
            leggBegrunnelserIVedtaksperiodene(
                dataTable,
                vedtaksperioder,
                vedtaksliste.single { it.behandling.id == behandlingId },
            )
    }

    @Når("vedtaksperiodene genereres for behandling {}")
    fun `generer vedtaksperioder for `(behandlingId: Long) {
        utvidetVedtaksperiodeMedBegrunnelser = genererVedtaksperioderForBehandling(behandlingId)
    }

    private fun genererVedtaksperioderForBehandling(behandlingId: Long): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vedtak =
            vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: throw Feil("Finner ikke vedtak")

        vedtak.behandling.overstyrtEndringstidspunkt = overstyrteEndringstidspunkt[behandlingId]

        val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]

        val grunnlagForBegrunnelser = hentGrunnlagForBegrunnelser(behandlingId, vedtak, forrigeBehandlingId)

        vedtaksperioderMedBegrunnelser =
            genererVedtaksperioder(
                vedtak = vedtak,
                grunnlagForVedtaksperioder = grunnlagForBegrunnelser.behandlingsGrunnlagForVedtaksperioder,
                grunnlagForVedtaksperioderForrigeBehandling = grunnlagForBegrunnelser.behandlingsGrunnlagForVedtaksperioderForrigeBehandling,
                nåDato = dagensDato,
                featureToggleService = mockFeatureToggleService(),
            )

        val utvidedeVedtaksperioderMedBegrunnelser =
            vedtaksperioderMedBegrunnelser.map {
                it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                    personopplysningGrunnlag = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId),
                    andelerTilkjentYtelse =
                        tilkjenteYtelser[behandlingId]?.andelerTilkjentYtelse?.map {
                            AndelTilkjentYtelseMedEndreteUtbetalinger(
                                it,
                                endredeUtbetalinger[behandlingId] ?: emptySet(),
                            )
                        } ?: emptyList(),
                )
            }

        return utvidedeVedtaksperioderMedBegrunnelser.map {
            it.copy(
                gyldigeBegrunnelser =
                    it
                        .tilVedtaksperiodeMedBegrunnelser(vedtak)
                        .hentGyldigeBegrunnelserForPeriode(grunnlagForBegrunnelser)
                        .toList(),
            )
        }
    }

    private fun hentGrunnlagForBegrunnelser(
        behandlingId: Long,
        vedtak: Vedtak,
        forrigeBehandlingId: Long?,
    ): GrunnlagForBegrunnelse {
        val grunnlagForVedtaksperiode =
            BehandlingsGrunnlagForVedtaksperioder(
                persongrunnlag = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId),
                personResultater =
                    vilkårsvurderinger[behandlingId]?.personResultater
                        ?: throw Feil("Finner ikke personresultater"),
                behandling = vedtak.behandling,
                kompetanser = kompetanser[behandlingId] ?: emptyList(),
                endredeUtbetalinger = endredeUtbetalinger[behandlingId] ?: emptyList(),
                andelerTilkjentYtelse = tilkjenteYtelser[behandlingId]?.andelerTilkjentYtelse?.toList() ?: emptyList(),
                perioderOvergangsstønad = overgangsstønader[behandlingId] ?: emptyList(),
                uregistrerteBarn = uregistrerteBarn,
                utenlandskPeriodebeløp = utenlandskPeriodebeløp[behandlingId] ?: emptyList(),
                valutakurs = valutakurs[behandlingId] ?: emptyList(),
                personerFremstiltKravFor = personerFremstiltKravFor[behandlingId] ?: emptyList(),
            )

        val grunnlagForVedtaksperiodeForrigeBehandling =
            forrigeBehandlingId?.let {
                val forrigeVedtak =
                    vedtaksliste.find { it.behandling.id == forrigeBehandlingId && it.aktiv }
                        ?: throw Feil("Finner ikke vedtak")
                BehandlingsGrunnlagForVedtaksperioder(
                    persongrunnlag = persongrunnlag.finnPersonGrunnlagForBehandling(forrigeBehandlingId),
                    personResultater =
                        vilkårsvurderinger[forrigeBehandlingId]?.personResultater
                            ?: throw Feil("Finner ikke personresultater"),
                    behandling = forrigeVedtak.behandling,
                    kompetanser = kompetanser[forrigeBehandlingId] ?: emptyList(),
                    endredeUtbetalinger = endredeUtbetalinger[forrigeBehandlingId] ?: emptyList(),
                    andelerTilkjentYtelse = tilkjenteYtelser[forrigeBehandlingId]?.andelerTilkjentYtelse?.toList() ?: emptyList(),
                    perioderOvergangsstønad = overgangsstønader[forrigeBehandlingId] ?: emptyList(),
                    uregistrerteBarn = uregistrerteBarn,
                    utenlandskPeriodebeløp = utenlandskPeriodebeløp[forrigeBehandlingId] ?: emptyList(),
                    valutakurs = valutakurs[forrigeBehandlingId] ?: emptyList(),
                    personerFremstiltKravFor = personerFremstiltKravFor[forrigeBehandlingId] ?: emptyList(),
                )
            }

        val grunnlagForBegrunnelse =
            GrunnlagForBegrunnelse(
                behandlingsGrunnlagForVedtaksperioder = grunnlagForVedtaksperiode,
                behandlingsGrunnlagForVedtaksperioderForrigeBehandling = grunnlagForVedtaksperiodeForrigeBehandling,
                sanityBegrunnelser = sanityBegrunnelserMock,
                sanityEØSBegrunnelser = sanityEØSBegrunnelserMock,
                nåDato = dagensDato,
            )
        return grunnlagForBegrunnelse
    }

    /**
     * Mulige verdier: | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Regelverk Ugyldige begrunnelser | Ugyldige begrunnelser |
     */
    @Så("forvent at følgende begrunnelser er gyldige")
    fun `forvent at følgende begrunnelser er gyldige`(dataTable: DataTable) {
        val forventedeStandardBegrunnelser = mapBegrunnelser(dataTable).toSet()

        forventedeStandardBegrunnelser.forEach { forventet ->
            val faktisk =
                utvidetVedtaksperiodeMedBegrunnelser.find { it.fom == forventet.fom && it.tom == forventet.tom }
                    ?: throw Feil(
                        "Forventet å finne en vedtaksperiode med  \n" +
                                "   Fom: ${forventet.fom?.tilddMMyyyy()} og Tom: ${forventet.tom?.tilddMMyyyy()}. \n" +
                                "Faktiske vedtaksperioder var \n${
                                    utvidetVedtaksperiodeMedBegrunnelser.joinToString("\n") {
                                        "   Fom: ${it.fom?.tilddMMyyyy()}, Tom: ${it.tom?.tilddMMyyyy()}"
                                    }
                                }",
                    )
            assertThat(faktisk.type)
                .`as`("For periode: ${forventet.fom} til ${forventet.tom}")
                .isEqualTo(forventet.type)
            assertThat(faktisk.gyldigeBegrunnelser)
                .`as`("For periode: ${forventet.fom} til ${forventet.tom}")
                .containsAll(forventet.inkluderteStandardBegrunnelser)

            if (faktisk.gyldigeBegrunnelser.isNotEmpty() && forventet.ekskluderteStandardBegrunnelser.isNotEmpty()) {
                assertThat(faktisk.gyldigeBegrunnelser).doesNotContainAnyElementsOf(forventet.ekskluderteStandardBegrunnelser)
            }
        }
    }

    /**
     * Mulige verdier: | Begrunnelse | Type | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Avtale tidspunkt delt bosted | Søkers rett til utvidet |
     */
    @Så("forvent følgende brevbegrunnelser i rekkefølge for behandling {} i periode {} til {}")
    fun `forvent følgende brevbegrunnelser i rekkefølge for behandling i periode`(
        behandlingId: Long,
        periodeFom: String,
        periodeTom: String,
        dataTable: DataTable,
    ) {
        val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]
        val vedtak =
            vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: throw Feil("Finner ikke vedtak")
        val grunnlagForBegrunnelse = hentGrunnlagForBegrunnelser(behandlingId, vedtak, forrigeBehandlingId)

        val vedtaksperiodeMedBegrunnelser =
            vedtaksperioderMedBegrunnelser.filter {
                it.fom == parseNullableDato(periodeFom) && it.tom == parseNullableDato(periodeTom)
            }

        if (vedtaksperioderMedBegrunnelser.isEmpty()) {
            throw Feil(
                "Forventet å finne en vedtaksperiode med Fom: $periodeFom og Tom: $periodeTom. \n" +
                        "Faktiske vedtaksperioder var \n${
                            vedtaksperioderMedBegrunnelser.joinToString("\n") {
                                "   Fom: ${it.fom}, Tom: ${it.tom}"
                            }
                        }",
            )
        }

        val faktiskeBegrunnelser =
            vedtaksperiodeMedBegrunnelser.flatMap { vedtaksperiodeMedBegrunnelser ->
                vedtaksperiodeMedBegrunnelser
                    .lagBrevPeriode(grunnlagForBegrunnelse, LANDKODER, mockFeatureToggleService().isEnabled(FeatureToggle.SKAL_BRUKE_NYTT_FELT_I_EØS_BEGRUNNELSE_DATA_MED_KOMPETANSE))!!
                    .begrunnelser
                    .filterIsInstance<BegrunnelseMedData>()
            }

        val forvendtedeBegrunnelser = parseBegrunnelser(dataTable)

        assertThat(faktiskeBegrunnelser)
            .usingRecursiveComparison()
            .ignoringFields("vedtakBegrunnelseType")
            .isEqualTo(forvendtedeBegrunnelser)
    }

    /**
     * Mulige verdier: | Brevperiodetype | Fra dato | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
     */
    @Så("forvent følgende brevperioder for behandling {}")
    fun `forvent følgende brevperioder for behandling i periode`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]
        val vedtak =
            vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: throw Feil("Finner ikke vedtak")
        val grunnlagForBegrunnelse = hentGrunnlagForBegrunnelser(behandlingId, vedtak, forrigeBehandlingId)

        val faktiskeBrevperioder: List<BrevPeriode> =
            vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.mapNotNull {
                it.lagBrevPeriode(grunnlagForBegrunnelse, LANDKODER, mockFeatureToggleService().isEnabled(FeatureToggle.SKAL_BRUKE_NYTT_FELT_I_EØS_BEGRUNNELSE_DATA_MED_KOMPETANSE))
            }

        val forvendtedeBrevperioder = parseBrevPerioder(dataTable)

        assertThat(faktiskeBrevperioder)
            .usingRecursiveComparison()
            .ignoringFields("begrunnelser")
            .isEqualTo(forvendtedeBrevperioder)
    }

    @Så("forvent følgende aktører på behandling {}")
    fun `forvent følgende aktører på behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val forventedeAktører = dataTable.asMaps().map { Aktør(aktørId = parseAktørId(it)) }.sortedBy { it.aktørId }
        val faktiskeAktører =
            persongrunnlag
                .finnPersonGrunnlagForBehandling(behandlingId)
                .personer
                .map { it.aktør }
                .sortedBy { it.aktørId }

        assertThat(faktiskeAktører).isEqualTo(forventedeAktører)
    }

    /**
     * Mulige verdier: | Fra dato | Til dato |
     */
    @Når("vi lager automatisk behandling med id {} på fagsak {} på grunn av nye overgangsstønadsperioder")
    fun `kjør behandling småbarnstillegg på fagsak med behandlingsid`(
        småbarnstilleggBehandlingId: Long,
        fagsakId: Long,
        dataTable: DataTable,
    ) {
        val fagsak = fagsaker[fagsakId]!!
        val internePerioderOvergangsstønad =
            dataTable
                .asMaps()
                .map { rad ->
                    InternPeriodeOvergangsstønad(
                        fomDato = parseDato(Domenebegrep.FRA_DATO, rad),
                        tomDato = parseDato(Domenebegrep.TIL_DATO, rad),
                        personIdent = fagsak.aktør.aktivFødselsnummer(),
                    )
                }

        mockAutovedtakSmåbarnstilleggService(
            dataFraCucumber = this,
            fagsak = fagsak,
            internPeriodeOvergangsstønadNyBehandling = internePerioderOvergangsstønad,
            småbarnstilleggBehandlingId = småbarnstilleggBehandlingId,
        ).kjørBehandlingSmåbarnstillegg(
            mottakersAktør = fagsak.aktør,
            aktør = fagsak.aktør,
        )
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | Bostedskommune | Adressetype |
     *
     * Adressetype kan være "Bostedsadresse" eller "Delt bosted"
     */
    @Og("med adressekommuner")
    fun `med adressekommuner`(dataTable: DataTable) {
        val identTilAdresser = parseAdresser(dataTable, persongrunnlag)
        adresser.putAll(parseAdresser(dataTable, persongrunnlag))
        persongrunnlag.forEach {
            it.value.personer.forEach { person ->
                person.bostedsadresser =
                    (identTilAdresser[person.aktør.personidenter.first().fødselsnummer]?.bostedsadresse?.map { bostedsadresse ->
                        GrBostedsadresse.fraBostedsadresse(bostedsadresse, person)
                    } ?: emptyList()).toMutableList()
            }

        }

        @Når("vi lager automatisk behandling med id {} på fagsak {} på grunn av finnmarkstillegg")
        fun `kjør behandling finnmarkstillegg på fagsak med behandlingsid`(
            finnmarkstilleggBehandlingId: Long,
            fagsakId: Long,
        ) {
            mockAutovedtakFinnmarkstilleggService(
                dataFraCucumber = this,
                fagsakId = fagsakId,
                nyBehanldingId = finnmarkstilleggBehandlingId,
            ).kjørBehandling(FinnmarkstilleggData(fagsakId))
        }

        @Når("vi lager automatisk behandling med id {} på fagsak {} på grunn av svalbardtillegg")
        fun `kjør behandling svalbardtillegg på fagsak med behandlingsid`(
            svalbardtilleggBehandlingId: Long,
            fagsakId: Long,
        ) {
            mockAutovedtakSvalbardtilleggService(
                dataFraCucumber = this,
                fagsakId = fagsakId,
                nyBehanldingId = svalbardtilleggBehandlingId,
            ).kjørBehandling(SvalbardtilleggData(fagsakId))
        }

        @Så("forvent at brevmal {} er brukt for behandling {}")
        fun `forvent følgende brevmal for behandling`(
            forventetBrevmal: Brevmal,
            behandlingId: Long,
        ) {
            val behandling = behandlinger.finnBehandling(behandlingId)
            val faktiskBrevmal = CucumberMock(this, behandlingId).brevmalService.hentBrevmal(behandling)

            assertThat(faktiskBrevmal).isEqualTo(forventetBrevmal)
        }

        @Så("forvent følgende vilkårresultater for behandling {}")
        fun `forvent følgende vilkårresultater for behandling`(
            behandlingId: Long,
            dataTable: DataTable,
        ) {
            val forventedeVilkårResultaterPerAktør =
                dataTable
                    .asMaps()
                    .groupBy { parseAktørId(it) }
                    .mapValues { (aktørId, vilkårResultatRaderForAktør) ->
                        parseVilkårResultaterForAktør(
                            vilkårResultatRaderForAktør = vilkårResultatRaderForAktør,
                            behandlingId = behandlingId,
                            personResultat = vilkårsvurderinger[behandlingId]!!.personResultater.first { it.aktør.aktørId == aktørId },
                        )
                    }

            val faktiskeVilkårResultaterPerAktør =
                vilkårsvurderinger[behandlingId]!!
                    .personResultater
                    .associate { it.aktør.aktørId to it.vilkårResultater }

            assertThat(faktiskeVilkårResultaterPerAktør)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes("id", ".*opprettetTidspunkt", ".*endretTidspunkt", ".*begrunnelse", ".*erAutomatiskVurdert", ".*erOpprinneligPreutfylt")
                .ignoringCollectionOrder()
                .isEqualTo(forventedeVilkårResultaterPerAktør)
        }

        /**
         * Mulige verdier: | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |
         */
        @Så("forvent følgende andeler tilkjent ytelse for behandling {}")
        fun `forvent andeler tilkjent ytelse`(
            behandlingId: Long,
            dataTable: DataTable,
        ) {
            val beregnetTilkjentYtelse =
                tilkjenteYtelser[behandlingId]
                    ?.andelerTilkjentYtelse
                    ?.toList()!!
                    .sortedWith(compareBy({ it.aktør.aktørId }, { it.stønadFom }, { it.stønadTom }))

            val forventedeAndeler =
                lagTilkjentYtelse(
                    dataFraCucumber = this,
                    dataTable = dataTable,
                    behandlinger = behandlinger,
                    personGrunnlag = persongrunnlag,
                    vedtaksliste = vedtaksliste,
                    behandlingTilForrigeBehandling = behandlingTilForrigeBehandling,
                )[behandlingId]!!
                    .andelerTilkjentYtelse
                    .sortedWith(compareBy({ it.aktør.aktørId }, { it.stønadFom }, { it.stønadTom }))

            assertThat(beregnetTilkjentYtelse)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*endretTidspunkt", ".*opprettetTidspunkt", ".*kildeBehandlingId", ".*tilkjentYtelse", ".*id", ".*forrigePeriodeOffset", ".*periodeOffset")
                .isEqualTo(forventedeAndeler)
        }

        /**
         * Mulige verdier: | AktørId | Dødsfalldato |
         */
        @Og("med dødsfall")
        fun `med dødsfall`(
            dataTable: DataTable,
        ) {
            val aktørTilDødsfall: Map<String, LocalDate> =
                dataTable
                    .asMaps()
                    .map { rad ->
                        val aktørId = parseAktørId(rad)
                        val dødsfallDato =
                            parseValgfriDato(
                                VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.DØDSFALLDATO,
                                rad,
                            ) ?: throw Feil("Dødsfallsdato må være satt")

                        aktørId to dødsfallDato
                    }.toMap()

            aktørTilDødsfall.forEach { (aktørId, dødsfallDato) ->
                persongrunnlag.values.flatMap { it.personer }.filter { it.aktør.aktørId == aktørId }.forEach {
                    it.dødsfall =
                        lagDødsfall(
                            person = it,
                            dødsfallDato = dødsfallDato,
                        )
                }
            }
        }

        /**
         * | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Behandlingsstatus | Behandlingssteg | Underkategori |
         */
        @Så("forvent disse behandlingene")
        fun `forvent disse behandlingene`(
            dataTable: DataTable,
        ) {
            val forventedeBehandlinger = lagBehandlinger(dataTable, fagsaker)
            forventedeBehandlinger.forEach {
                assertThat(behandlinger[it.id].toString()).isEqualTo(it.toString())
            }
        }

        @Så("forvent nøyaktig disse behandlingene for fagsak {}")
        fun `forvent nøyaktig disse behandlingene for fagsak`(
            fagsakId: Long,
            dataTable: DataTable,
        ) {
            val forventedeBehandlinger = lagBehandlinger(dataTable, fagsaker).map { it.toString() }
            val behandlingerPåFagsak = behandlinger.filter { it.value.fagsak.id == fagsakId }.map { it.value.toString() }
            assertThat(behandlingerPåFagsak).containsExactlyInAnyOrder(*forventedeBehandlinger.toTypedArray())
        }

        /**
         * | Valuta kode | Valutakursdato | Kurs |
         */
        @Når("vi lager automatisk behandling med id {} på fagsak {} på grunn av automatisk valutajustering og har følgende valutakurser")
        fun `kjør automatisk valutajustering med behandlingsid på fagsak `(
            nyBehandling: Long,
            fagsakId: Long,
            dataTable: DataTable,
        ) {
            val fagsak = fagsaker[fagsakId]!!

            val svarFraEcbMock = lagSvarFraEcbMock(dataTable)

            mockAutovedtakMånedligValutajusteringService(
                dataFraCucumber = this,
                fagsak = fagsak,
                nyBehanldingId = nyBehandling,
                svarFraEcbMock = svarFraEcbMock,
            ).utførMånedligValutajustering(fagsakId = fagsakId, måned = dagensDato.toYearMonth())
        }

        @Så("forvent følgende valutakurser for behandling {}")
        fun `forvent følgende valutakurser for behandling`(
            behandlingId: Long,
            dataTable: DataTable,
        ) {
            val forventedeValutakurser = lagValutakurs(dataTable.asMaps(), persongrunnlag)

            assertThat(valutakurs[behandlingId]!!.sortedBy { it.valutakursdato })
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*endretTidspunkt", ".*opprettetTidspunkt", ".*id", ".*personidenter")
                .isEqualTo(forventedeValutakurser[behandlingId]!!.sortedBy { it.valutakursdato })
        }

        @Så("forvent at endringstidspunktet er {} for behandling {}")
        fun `forvent at endringstidspunktet er for behandling`(
            forventetEndringstidspunktString: String,
            behandlingId: Long,
        ) {
            val vedtak =
                vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: throw Feil("Finner ikke vedtak")
            val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]
            val grunnlagForBegrunnelser = hentGrunnlagForBegrunnelser(behandlingId, vedtak, forrigeBehandlingId)

            val faktiskEndringstidspunkt =
                utledEndringstidspunkt(
                    behandlingsGrunnlagForVedtaksperioder = grunnlagForBegrunnelser.behandlingsGrunnlagForVedtaksperioder,
                    behandlingsGrunnlagForVedtaksperioderForrigeBehandling = grunnlagForBegrunnelser.behandlingsGrunnlagForVedtaksperioderForrigeBehandling,
                    featureToggleService = mockFeatureToggleService(),
                )

            val forventetEndringstidspunkt =
                parseNullableDato(forventetEndringstidspunktString)
                    ?: throw Feil("Så forvent følgende endringstidspunkt {} forventer en dato")

            assertThat(faktiskEndringstidspunkt).isEqualTo(forventetEndringstidspunkt)
        }

        @Når("vi oppdaterer valutakursene for beslutter på behandling {}")
        fun `vi oppdaterer valutakursene for beslutter på behandling`(
            behandlingId: Long,
        ) {
            val mock =
                CucumberMock(
                    dataFraCucumber = this,
                    nyBehandlingId = behandlingId,
                )

            mock.automatiskOppdaterValutakursService.oppdaterValutakurserOgSimulering(BehandlingId(behandlingId))
        }

        /**
         * Mulige felt:
         * | AktørId | Fra dato | Til dato | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
         */
        @Når("vi legger til utenlandsk periodebeløp for behandling {}")
        fun `når vi legger til upb på behandling`(
            behandlingId: Long,
            dataTable: DataTable,
        ) {
            val utenlandskPeriodebeløp = lagUtenlandskperiodeBeløp(dataTable.asMaps(), persongrunnlag)[behandlingId]!!

            val mock =
                CucumberMock(
                    dataFraCucumber = this,
                    nyBehandlingId = behandlingId,
                    forrigeBehandling = null,
                )

            mock.utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(BehandlingId(behandlingId), utenlandskPeriodebeløp.single())
        }

        @Når("vi automatisk oppdaterer valutakurser for behandling {}")
        fun `når vi automatisk oppdaterer valutakurser for behandling`(
            behandlingId: Long,
        ) {
            val mock =
                CucumberMock(
                    dataFraCucumber = this,
                    nyBehandlingId = behandlingId,
                )

            mock.automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(BehandlingId(behandlingId))
        }

        @Og("med overstyrt endringstidspunkt {} for behandling {}")
        fun settEndringstidspunkt(
            endringstidspunkt: String,
            behandlingId: Long,
        ) {
            overstyrteEndringstidspunkt[behandlingId] = parseDato(endringstidspunkt)
        }

        @Så("forvent følgende vedtaksperioder for behandling {}")
        fun `forvent følgende vedtaksperioder for behandling`(
            behandlingId: Long,
            dataTable: DataTable,
        ) {
            val forventedeVedtaksperioder =
                mapForventetVedtaksperioderMedBegrunnelser(
                    dataTable = dataTable,
                    vedtak =
                        vedtaksliste.find { it.behandling.id == behandlingId }
                            ?: throw Feil("Fant ingen vedtak for behandling $behandlingId"),
                )
            val vedtaksperioderComparator = compareBy<VedtaksperiodeMedBegrunnelser>({ it.type }, { it.fom }, { it.tom })
            assertThat(vedtaksperioderMedBegrunnelser.sortedWith(vedtaksperioderComparator))
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*endretTidspunkt", ".*opprettetTidspunkt")
                .isEqualTo(forventedeVedtaksperioder.sortedWith(vedtaksperioderComparator))
        }

        @Og("kopier kompetanser fra behandling {} til behandling {}")
        fun `kopier kompetanser fra behandling til behandling`(
            fraBehandlingId: Long,
            tilBehandlingId: Long,
        ) {
            kompetanser[tilBehandlingId] = kompetanser[fraBehandlingId]
                ?: throw Feil("Finner ikke kompetanser for behandling med id $fraBehandlingId")
        }

        @Og("kopier utenlandsk periodebeløp fra behandling {} til behandling {}")
        fun `kopier utenlandsk periodebeløp fra behandling til behandling`(
            fraBehandlingId: Long,
            tilBehandlingId: Long,
        ) {
            utenlandskPeriodebeløp[tilBehandlingId] = utenlandskPeriodebeløp[fraBehandlingId]
                ?: throw Feil("Finner ikke utenlandsk periodebeløp for behandling med id $fraBehandlingId")
        }

        @Når("vi utfører vilkårsvurderingssteget for behandling {}")
        fun `vi utfører vilkårsvurderingssteg for behandling`(
            behandlingId: Long,
        ) {
            val mock =
                CucumberMock(
                    dataFraCucumber = this,
                    nyBehandlingId = behandlingId,
                )

            mock.stegService.håndterVilkårsvurdering(behandlinger[behandlingId]!!)
        }

        @Og("når behandlingsresultatet er utledet for behandling {}")
        fun `når behandlingsresultatet er utledet for behehandling`(
            behandlingId: Long,
        ) {
            val mock =
                CucumberMock(
                    dataFraCucumber = this,
                    nyBehandlingId = behandlingId,
                )

            val behandling = behandlinger[behandlingId]!!

            val behandlingsresultat = mock.behandlingsresultatService.utledBehandlingsresultat(behandlingId)

            behandlinger[behandlingId] = behandling.copy(resultat = behandlingsresultat)
        }

        @Så("forvent at behandlingsresultatet er {} på behandling {}")
        fun `forvent følgende behandlingsresultat på behandling`(
            forventetBehandlingsresultat: Behandlingsresultat,
            behandlingId: Long,
        ) {
            val faktiskResultat = behandlinger[behandlingId]!!.resultat
            assertThat(faktiskResultat).isEqualTo(forventetBehandlingsresultat)
        }
    }

    data class SammenlignbarBegrunnelse(
        val fom: LocalDate?,
        val tom: LocalDate?,
        val type: Vedtaksperiodetype,
        val inkluderteStandardBegrunnelser: Set<IVedtakBegrunnelse>,
        val ekskluderteStandardBegrunnelser: Set<IVedtakBegrunnelse> = emptySet<IVedtakBegrunnelse>(),
    )
