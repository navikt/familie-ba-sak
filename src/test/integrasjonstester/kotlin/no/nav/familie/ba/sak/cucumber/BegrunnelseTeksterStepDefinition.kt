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
import no.nav.familie.ba.sak.cucumber.domeneparser.BrevBegrunnelseParser.mapBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.parseAktørId
import no.nav.familie.ba.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ba.sak.cucumber.mock.CucumberMock
import no.nav.familie.ba.sak.cucumber.mock.mockAutovedtakMånedligValutajusteringService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.brev.LANDKODER
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.GrunnlagForBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent.lagBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.RestSanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriode
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfall
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseMedData
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.utledEndringstidspunkt
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtakBegrunnelseProdusent.hentGyldigeBegrunnelserForPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.BehandlingsGrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent.genererVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate

val sanityBegrunnelserMock = SanityBegrunnelseMock.hentSanityBegrunnelserMock()
val sanityEØSBegrunnelserMock = SanityBegrunnelseMock.hentSanityEØSBegrunnelserMock()

@Suppress("ktlint:standard:function-naming")
class BegrunnelseTeksterStepDefinition {
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
    var endredeUtbetalinger = mutableMapOf<Long, List<EndretUtbetalingAndel>>()
    var tilkjenteYtelser = mutableMapOf<Long, TilkjentYtelse>()
    var overstyrteEndringstidspunkt = mutableMapOf<Long, LocalDate>()
    var overgangsstønader = mutableMapOf<Long, List<InternPeriodeOvergangsstønad>>()
    var totrinnskontroller = mutableMapOf<Long, Totrinnskontroll>()
    var dagensDato: LocalDate = LocalDate.now()

    var gjeldendeBehandlingId: Long? = null

    var utvidetVedtaksperiodeMedBegrunnelser = listOf<UtvidetVedtaksperiodeMedBegrunnelser>()

    var målform: Målform = Målform.NB
    var søknadstidspunkt: LocalDate? = null

    /**
     * Mulige verdier: | FagsakId | Fagsaktype | Status |
     */
    @Gitt("følgende fagsaker for begrunnelse")
    fun `følgende fagsaker for begrunnelse`(dataTable: DataTable) {
        fagsaker = lagFagsaker(dataTable)
    }

    /**
     * Mulige felter:
     * | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Behandlingsstatus |
     */
    @Gitt("følgende behandling")
    fun `følgende behandling`(dataTable: DataTable) {
        lagVedtak(
            dataTable = dataTable,
            behandlinger = behandlinger,
            behandlingTilForrigeBehandling = behandlingTilForrigeBehandling,
            vedtaksListe = vedtaksliste,
            fagsaker = fagsaker,
        )
    }

    /**
     * Mulige verdier: | BehandlingId |  AktørId | Persontype | Fødselsdato |
     */
    @Og("følgende persongrunnlag for begrunnelse")
    fun `følgende persongrunnlag for begrunnelse`(dataTable: DataTable) {
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

    @Og("følgende dagens dato {}")
    fun `følgende dagens dato`(dagensDatoString: String) {
        dagensDato = parseDato(dagensDatoString)
    }

    @Og("lag personresultater for begrunnelse for behandling {}")
    fun `lag personresultater for begrunnelse`(behandlingId: Long) {
        val persongrunnlagForBehandling = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId)
        val behandling = behandlinger.finnBehandling(behandlingId)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlagForBehandling, behandling)
        vilkårsvurderinger[behandlingId] = vilkårsvurdering
    }

    /**
     * Mulige verdier: | AktørId | Vilkår | Utdypende vilkår | Fra dato | Til dato | Resultat | Er eksplisitt avslag | Vurderes etter |
     */
    @Og("legg til nye vilkårresultater for begrunnelse for behandling {}")
    fun `legg til nye vilkårresultater for behandling`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val vilkårResultaterPerPerson =
            dataTable.asMaps().groupBy { parseAktørId(it) }
        val personResultatForBehandling =
            vilkårsvurderinger[behandlingId]?.personResultater
                ?: error("Finner ikke personresultater for behandling med id $behandlingId")

        vilkårsvurderinger[behandlingId]?.personResultater =
            leggTilVilkårResultatPåPersonResultat(personResultatForBehandling, vilkårResultaterPerPerson, behandlingId)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | Resultat | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
     */
    @Og("med kompetanser for begrunnelse")
    fun `med kompetanser for begrunnelse`(dataTable: DataTable) {
        val nyeKompetanserPerBarn = dataTable.asMaps()
        kompetanser = lagKompetanser(nyeKompetanserPerBarn, persongrunnlag)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | BehandlingId | Valutakursdato | Valuta kode | Kurs
     */

    @Og("med valutakurs for begrunnelse")
    fun `med valutakurs for begrunnelse`(dataTable: DataTable) {
        val nyeValutakursPerBarn = dataTable.asMaps()
        valutakurs = lagValutakurs(nyeValutakursPerBarn, persongrunnlag)
    }

    /**
     * Mulige felt:
     * | AktørId | Fra dato | Til dato | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland
     */
    @Og("med utenlandsk periodebeløp for begrunnelse")
    fun `med utenlandsk periodebeløp for begrunnelse`(dataTable: DataTable) {
        val nyeUtenlandskPeriodebeløpPerBarn = dataTable.asMaps()
        utenlandskPeriodebeløp = lagUtenlandskperiodeBeløp(nyeUtenlandskPeriodebeløpPerBarn, persongrunnlag)
    }

    /**
     * Mulige verdier: | AktørId | Fra dato | Til dato | BehandlingId |  Årsak | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
     */
    @Og("med endrede utbetalinger for begrunnelse")
    fun `med endrede utbetalinger for begrunnelse`(dataTable: DataTable) {
        val nyeEndredeUtbetalingAndeler = dataTable.asMaps()
        endredeUtbetalinger = lagEndredeUtbetalinger(nyeEndredeUtbetalingAndeler, persongrunnlag)
    }

    /**
     * Mulige verdier: | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |
     */
    @Og("med andeler tilkjent ytelse for begrunnelse")
    fun `med andeler tilkjent ytelse for begrunnelse`(dataTable: DataTable) {
        tilkjenteYtelser = lagTilkjentYtelse(dataTable = dataTable, behandlinger = behandlinger, personGrunnlag = persongrunnlag, vedtaksliste = vedtaksliste)
    }

    /**
     * Mulige verdier: | BehandlingId | AktørId | Fra dato | Til dato |
     */
    @Og("med overgangsstønad for begrunnelse")
    fun `med overgangsstønad for begrunnelse`(dataTable: DataTable) {
        overgangsstønader =
            lagOvergangsstønad(
                dataTable = dataTable,
                persongrunnlag = persongrunnlag,
                tidligereBehandlinger = behandlingTilForrigeBehandling,
                dagensDato = dagensDato,
            ).toMutableMap()
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
        gjeldendeBehandlingId = behandlingId

        val vedtak = vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: error("Finner ikke vedtak")

        vedtak.behandling.overstyrtEndringstidspunkt = overstyrteEndringstidspunkt[behandlingId]

        val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]

        val grunnlagForBegrunnelser = hentGrunnlagForBegrunnelser(behandlingId, vedtak, forrigeBehandlingId)

        vedtaksperioderMedBegrunnelser =
            genererVedtaksperioder(
                vedtak = vedtak,
                grunnlagForVedtakPerioder = grunnlagForBegrunnelser.behandlingsGrunnlagForVedtaksperioder,
                grunnlagForVedtakPerioderForrigeBehandling = grunnlagForBegrunnelser.behandlingsGrunnlagForVedtaksperioderForrigeBehandling,
                nåDato = dagensDato,
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
                personResultater = vilkårsvurderinger[behandlingId]?.personResultater ?: error("Finner ikke personresultater"),
                behandling = vedtak.behandling,
                kompetanser = kompetanser[behandlingId] ?: emptyList(),
                endredeUtbetalinger = endredeUtbetalinger[behandlingId] ?: emptyList(),
                andelerTilkjentYtelse = tilkjenteYtelser[behandlingId]?.andelerTilkjentYtelse?.toList() ?: emptyList(),
                perioderOvergangsstønad = overgangsstønader[behandlingId] ?: emptyList(),
                uregistrerteBarn = emptyList(),
                utenlandskPeriodebeløp = utenlandskPeriodebeløp[behandlingId] ?: emptyList(),
                valutakurs = valutakurs[behandlingId] ?: emptyList(),
            )

        val grunnlagForVedtaksperiodeForrigeBehandling =
            forrigeBehandlingId?.let {
                val forrigeVedtak =
                    vedtaksliste.find { it.behandling.id == forrigeBehandlingId && it.aktiv } ?: error("Finner ikke vedtak")
                BehandlingsGrunnlagForVedtaksperioder(
                    persongrunnlag = persongrunnlag.finnPersonGrunnlagForBehandling(forrigeBehandlingId),
                    personResultater = vilkårsvurderinger[forrigeBehandlingId]?.personResultater ?: error("Finner ikke personresultater"),
                    behandling = forrigeVedtak.behandling,
                    kompetanser = kompetanser[forrigeBehandlingId] ?: emptyList(),
                    endredeUtbetalinger = endredeUtbetalinger[forrigeBehandlingId] ?: emptyList(),
                    andelerTilkjentYtelse = tilkjenteYtelser[forrigeBehandlingId]?.andelerTilkjentYtelse?.toList() ?: emptyList(),
                    perioderOvergangsstønad = overgangsstønader[forrigeBehandlingId] ?: emptyList(),
                    uregistrerteBarn = emptyList(),
                    utenlandskPeriodebeløp = utenlandskPeriodebeløp[forrigeBehandlingId] ?: emptyList(),
                    valutakurs = valutakurs[forrigeBehandlingId] ?: emptyList(),
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
    @Så("forvent følgende brevbegrunnelser for behandling {} i periode {} til {}")
    fun `forvent følgende brevbegrunnelser for behandling i periode`(
        behandlingId: Long,
        periodeFom: String,
        periodeTom: String,
        dataTable: DataTable,
    ) {
        val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]
        val vedtak = vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: error("Finner ikke vedtak")
        val grunnlagForBegrunnelse = hentGrunnlagForBegrunnelser(behandlingId, vedtak, forrigeBehandlingId)

        val vedtaksperiodeMedBegrunnelser =
            vedtaksperioderMedBegrunnelser.find {
                it.fom == parseNullableDato(periodeFom) && it.tom == parseNullableDato(periodeTom)
            } ?: throw Feil(
                "Forventet å finne en vedtaksperiode med Fom: $periodeFom og Tom: $periodeTom. \n" +
                    "Faktiske vedtaksperioder var \n${
                        vedtaksperioderMedBegrunnelser.joinToString("\n") {
                            "   Fom: ${it.fom}, Tom: ${it.tom}"
                        }
                    }",
            )

        val faktiskeBegrunnelser: List<BegrunnelseMedData> =
            vedtaksperiodeMedBegrunnelser
                .lagBrevPeriode(grunnlagForBegrunnelse, LANDKODER)!!
                .begrunnelser
                .filterIsInstance<BegrunnelseMedData>()

        val forvendtedeBegrunnelser = parseBegrunnelser(dataTable)

        assertThat(faktiskeBegrunnelser.sortedBy { it.apiNavn })
            .usingRecursiveComparison()
            .ignoringFields("vedtakBegrunnelseType")
            .isEqualTo(forvendtedeBegrunnelser.sortedBy { it.apiNavn })
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
        val vedtak = vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: error("Finner ikke vedtak")
        val grunnlagForBegrunnelse = hentGrunnlagForBegrunnelser(behandlingId, vedtak, forrigeBehandlingId)

        val faktiskeBrevperioder: List<BrevPeriode> =
            vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.mapNotNull {
                it.lagBrevPeriode(grunnlagForBegrunnelse, LANDKODER)
            }

        val forvendtedeBrevperioder = parseBrevPerioder(dataTable)

        assertThat(faktiskeBrevperioder)
            .usingRecursiveComparison()
            .ignoringFields("begrunnelser")
            .isEqualTo(forvendtedeBrevperioder)
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
                .map({ rad ->
                    InternPeriodeOvergangsstønad(
                        fomDato = parseDato(Domenebegrep.FRA_DATO, rad),
                        tomDato = parseDato(Domenebegrep.TIL_DATO, rad),
                        personIdent = fagsak.aktør.aktivFødselsnummer(),
                    )
                })

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
     * Mulige verdier: | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |
     */
    @Så("forvent følgende andeler tilkjent ytelse for behandling {}")
    fun `med andeler tilkjent ytelse`(
        behandlingId: Long,
        dataTable: DataTable,
    ) {
        val beregnetTilkjentYtelse =
            tilkjenteYtelser[behandlingId]
                ?.andelerTilkjentYtelse
                ?.toList()!!
                .sortedWith(compareBy({ it.aktør.aktørId }, { it.stønadFom }, { it.stønadTom }))

        val forventedeAndeler =
            lagTilkjentYtelse(dataTable = dataTable, behandlinger = behandlinger, personGrunnlag = persongrunnlag, vedtaksliste = vedtaksliste)[behandlingId]!!
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
                    val dødsfallDato = parseValgfriDato(VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.DØDSFALLDATO, rad) ?: error("Dødsfallsdato må være satt")

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
    fun `med andeler tilkjent ytelse`(
        dataTable: DataTable,
    ) {
        val forventedeBehandlinger = lagBehandlinger(dataTable, fagsaker)
        forventedeBehandlinger.forEach {
            assertThat(behandlinger[it.id].toString()).isEqualTo(it.toString())
        }
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
            .ignoringFieldsMatchingRegexes(".*endretTidspunkt", ".*opprettetTidspunkt", ".*id")
            .isEqualTo(forventedeValutakurser[behandlingId]!!.sortedBy { it.valutakursdato })
    }

    @Så("forvent at endringstidspunktet er {} for behandling {}")
    fun `forvent at endringstidspunktet er for behandling`(
        forventetEndringstidspunktString: String,
        behandlingId: Long,
    ) {
        val vedtak = vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: error("Finner ikke vedtak")
        val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]
        val grunnlagForBegrunnelser = hentGrunnlagForBegrunnelser(behandlingId, vedtak, forrigeBehandlingId)

        val faktiskEndringstidspunkt =
            utledEndringstidspunkt(
                behandlingsGrunnlagForVedtaksperioder = grunnlagForBegrunnelser.behandlingsGrunnlagForVedtaksperioder,
                behandlingsGrunnlagForVedtaksperioderForrigeBehandling = grunnlagForBegrunnelser.behandlingsGrunnlagForVedtaksperioderForrigeBehandling,
            )

        val forventetEndringstidspunkt = parseNullableDato(forventetEndringstidspunktString) ?: error("Så forvent følgende endringstidspunkt {} forventer en dato")

        assertThat(faktiskEndringstidspunkt).isEqualTo(forventetEndringstidspunkt)
    }

    @Når("vi oppdaterer valutakursene for beslutter på behandling {}")
    fun `vi oppdaterer valutakursene for beslutter på behandling`(
        behandlingId: Long,
    ) {
        val mock =
            CucumberMock(
                dataFraCucumber = this,
                nyBehanldingId = behandlingId,
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
                nyBehanldingId = behandlingId,
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
                nyBehanldingId = behandlingId,
            )

        mock.automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(BehandlingId(behandlingId))
    }
}

data class SammenlignbarBegrunnelse(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val inkluderteStandardBegrunnelser: Set<IVedtakBegrunnelse>,
    val ekskluderteStandardBegrunnelser: Set<IVedtakBegrunnelse> = emptySet<IVedtakBegrunnelse>(),
)

private object SanityBegrunnelseMock {
    // For å laste ned begrunnelsene kjør scriptet "src/test/resources/oppdater-sanity-mock.sh" eller
    // se https://familie-brev.sanity.studio/ba-brev/vision med query fra SanityQueries.kt.
    fun hentSanityBegrunnelserMock(): Map<Standardbegrunnelse, SanityBegrunnelse> {
        val restSanityBegrunnelserJson =
            this::class.java.getResource("/no/nav/familie/ba/sak/cucumber/gyldigeBegrunnelser/restSanityBegrunnelser")!!

        val restSanityBegrunnelser =
            objectMapper
                .readValue(restSanityBegrunnelserJson, Array<RestSanityBegrunnelse>::class.java)
                .toList()

        val enumPåApiNavn = Standardbegrunnelse.entries.associateBy { it.sanityApiNavn }
        val sanityBegrunnelser = restSanityBegrunnelser.mapNotNull { it.tilSanityBegrunnelse() }

        return sanityBegrunnelser
            .mapNotNull {
                val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                if (begrunnelseEnum == null) {
                    null
                } else {
                    begrunnelseEnum to it
                }
            }.toMap()
    }

    fun hentSanityEØSBegrunnelserMock(): Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse> {
        val restSanityEØSBegrunnelserJson =
            this::class.java.getResource("/no/nav/familie/ba/sak/cucumber/gyldigeBegrunnelser/restSanityEØSBegrunnelser")!!

        val restSanityEØSBegrunnelser =
            objectMapper
                .readValue(
                    restSanityEØSBegrunnelserJson,
                    Array<RestSanityEØSBegrunnelse>::class.java,
                ).toList()

        val enumPåApiNavn = EØSStandardbegrunnelse.entries.associateBy { it.sanityApiNavn }
        val sanityEØSBegrunnelser = restSanityEØSBegrunnelser.mapNotNull { it.tilSanityEØSBegrunnelse() }

        return sanityEØSBegrunnelser
            .mapNotNull {
                val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                if (begrunnelseEnum == null) {
                    null
                } else {
                    begrunnelseEnum to it
                }
            }.toMap()
    }
}
