package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Og
import io.cucumber.java.no.Så
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITET
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITETSLAND
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.BARNETS_BOSTEDSLAND
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.RESULTAT
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITET
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITETSLAND
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.mapForventetVedtaksperioderMedBegrunnelser
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.parseAktørId
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe
import no.nav.familie.ba.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnumListe
import no.nav.familie.ba.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseList
import no.nav.familie.ba.sak.cucumber.domeneparser.parseLong
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriBoolean
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriLong
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriString
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.SøkersAktivitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.GrunnlagForVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.produsent.genererVedtaksperioder
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.assertj.core.api.Assertions
import java.math.BigDecimal
import java.time.LocalDate

class VedtaksperiodeMedBegrunnelserStepDefinition {

    private var behandlinger = mapOf<Long, Behandling>()
    private var behandlingTilForrigeBehandling = mapOf<Long, Long?>()
    private var vedtaksliste = listOf<Vedtak>()
    private var persongrunnlag = mapOf<Long, PersonopplysningGrunnlag>()
    private var personResultater = mutableMapOf<Long, Set<PersonResultat>>()
    private var vedtaksperioderMedBegrunnelser = listOf<VedtaksperiodeMedBegrunnelser>()
    private var kompetanser = mutableMapOf<Long, List<Kompetanse>>()
    private var endredeUtbetalinger = mutableMapOf<Long, List<EndretUtbetalingAndel>>()
    private var andelerTilkjentYtelse = mutableMapOf<Long, List<AndelTilkjentYtelse>>()

    private var gjeldendeBehandlingId: Long? = null

    @Gitt("følgende vedtak")
    fun `følgende vedtak`(dataTable: DataTable) {
        genererVedtak(dataTable)
    }

    @Og("følgende persongrunnlag")
    fun `følgende persongrunnlag`(dataTable: DataTable) {
        lagPersonGrunnlag(dataTable)
    }

    @Og("lag personresultater for behandling {}")
    fun `lag personresultater`(behandlingId: Long) {
        personResultater[behandlingId] = persongrunnlagForBehandling(behandlingId).personer.map { person ->
            lagPersonResultat(
                vilkårsvurdering = lagVilkårsvurdering(person.aktør, finnBehandling(behandlingId), Resultat.OPPFYLT),
                person = person,
                resultat = Resultat.OPPFYLT,
                personType = person.type,
                lagFullstendigVilkårResultat = true,
                periodeFom = null,
                periodeTom = null,
            )
        }.toSet()
    }

    @Og("legg til nye vilkårresultater for behandling {}")
    fun `legg til nye vilkårresultater for behandling`(behandlingId: Long, dataTable: DataTable) {
        val nyeVilkårPerPerson = dataTable.asMaps().groupBy { parseAktørId(it) }
        val personResultatForBehandling =
            personResultater[behandlingId] ?: error("Finner ikke personresultater for behandling med id $behandlingId")
        personResultater[behandlingId] = personResultatForBehandling.map { personResultat ->
            val nyeVilkårForPerson = nyeVilkårPerPerson[personResultat.aktør.aktørId]

            val nyeVilkårResultater = tilVilkårResultater(nyeVilkårForPerson, behandlingId, personResultat)
            personResultat.vilkårResultater.addAll(nyeVilkårResultater)
            personResultat
        }.toSet()
    }

    @Og("med kompetanser")
    fun `med kompetanser`(dataTable: DataTable) {
        val nyeKompetanserPerBarn = dataTable.asMaps()
        kompetanser = nyeKompetanserPerBarn.map { rad ->
            val aktørerForKompetanse = parseAktørIdListe(rad)
            val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
            Kompetanse(
                fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
                tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
                barnAktører = persongrunnlagForBehandling(behandlingId).personer
                    .filter { aktørerForKompetanse.contains(it.aktør.aktørId) }
                    .map { it.aktør }
                    .toSet(),
                søkersAktivitet = parseValgfriEnum<SøkersAktivitet>(SØKERS_AKTIVITET, rad)
                    ?: SøkersAktivitet.ARBEIDER,
                annenForeldersAktivitet =
                parseValgfriEnum<AnnenForeldersAktivitet>(ANNEN_FORELDERS_AKTIVITET, rad)
                    ?: AnnenForeldersAktivitet.I_ARBEID,
                søkersAktivitetsland = parseValgfriString(SØKERS_AKTIVITETSLAND, rad) ?: "PL",
                annenForeldersAktivitetsland = parseValgfriString(ANNEN_FORELDERS_AKTIVITETSLAND, rad) ?: "NO",
                barnetsBostedsland = parseValgfriString(BARNETS_BOSTEDSLAND, rad) ?: "NO",
                resultat = parseEnum<KompetanseResultat>(RESULTAT, rad),
            ).also { it.behandlingId = behandlingId }
        }.groupBy { it.behandlingId }
            .toMutableMap()
    }

    @Og("med endrede utbetalinger")
    fun `med endrede utbetalinger`(dataTable: DataTable) {
        val nyeEndredeUtbetalingAndeler = dataTable.asMaps()
        endredeUtbetalinger = nyeEndredeUtbetalingAndeler.map { rad ->
            val aktørId = parseAktørId(rad)
            val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
            EndretUtbetalingAndel(
                behandlingId = behandlingId,
                fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
                tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
                person = persongrunnlagForBehandling(behandlingId).personer.find { aktørId == it.aktør.aktørId },
                prosent = BigDecimal.ZERO,
                årsak = Årsak.ALLEREDE_UTBETALT,
                søknadstidspunkt = LocalDate.now(),
                begrunnelse = "Fordi at...",
            )
        }.groupBy { it.behandlingId }
            .toMutableMap()
    }

    @Og("med andeler tilkjent ytelse")
    fun `med andeler tilkjent ytelse`(dataTable: DataTable) {
        val nyeAndelerTilkjentYtelse = dataTable.asMaps()
        andelerTilkjentYtelse = nyeAndelerTilkjentYtelse.map { rad ->
            val aktørId = parseAktørId(rad)
            val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
            lagAndelTilkjentYtelse(
                fom = parseDato(Domenebegrep.FRA_DATO, rad).toYearMonth(),
                tom = parseDato(Domenebegrep.TIL_DATO, rad).toYearMonth(),
                behandling = finnBehandling(behandlingId),
                person = persongrunnlagForBehandling(behandlingId).personer.find { aktørId == it.aktør.aktørId }!!,
                beløp = parseInt(DomenebegrepVedtaksperiodeMedBegrunnelser.BELØP, rad),
            )
        }.groupBy { it.behandlingId }
            .toMutableMap()
    }

    @Og("med overstyring av vilkår for behandling {}")
    fun overstyrPersonResultater(behandlingId: Long, dataTable: DataTable) {
        val overstyringerPerPerson = dataTable.asMaps().groupBy { parseAktørId(it) }
        val personResultatForBehandling =
            personResultater[behandlingId] ?: error("Finner ikke personresultater for behandling med id $behandlingId")
        personResultater[behandlingId] = personResultatForBehandling.map { personResultat ->
            val overstyringerForPerson = overstyringerPerPerson[personResultat.aktør.aktørId]

            personResultat.vilkårResultater.forEach { vilkårResultat ->
                oppdaterVilkårResultat(
                    vilkårResultat,
                    overstyringerForPerson,
                )
            }
            personResultat
        }.toSet()
    }

    @Når("vedtaksperioder med begrunnelser genereres for behandling {}")
    fun `generer vedtaksperiode med begrunnelse`(behandlingId: Long) {
        gjeldendeBehandlingId = behandlingId

        val vedtak = vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: error("Finner ikke vedtak")

        val grunnlagForVedtaksperiode = GrunnlagForVedtaksperioder(
            persongrunnlag = persongrunnlagForBehandling(behandlingId),
            personResultater = personResultater[behandlingId] ?: error("Finner ikke personresultater"),
            fagsakType = vedtak.behandling.fagsak.type,
            kompetanser = kompetanser[behandlingId] ?: emptyList(),
            endredeUtbetalinger = endredeUtbetalinger[behandlingId] ?: emptyList(),
            andelerTilkjentYtelse = andelerTilkjentYtelse[behandlingId] ?: emptyList(),
            perioderOvergangsstønad = emptyList(), // todo
        )
        val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]

        val grunnlagForVedtaksperiodeForrigeBehandling = forrigeBehandlingId?.let {
            val forrigeVedtak =
                vedtaksliste.find { it.behandling.id == forrigeBehandlingId && it.aktiv } ?: error("Finner ikke vedtak")
            GrunnlagForVedtaksperioder(
                persongrunnlag = persongrunnlagForBehandling(forrigeBehandlingId),
                personResultater = personResultater[forrigeBehandlingId] ?: error("Finner ikke personresultater"),
                fagsakType = forrigeVedtak.behandling.fagsak.type,
                kompetanser = kompetanser[forrigeBehandlingId] ?: emptyList(),
                endredeUtbetalinger = endredeUtbetalinger[forrigeBehandlingId] ?: emptyList(),
                andelerTilkjentYtelse = andelerTilkjentYtelse[forrigeBehandlingId] ?: emptyList(),
                perioderOvergangsstønad = emptyList(),
            )
        }

        vedtaksperioderMedBegrunnelser = genererVedtaksperioder(
            vedtak = vedtaksliste.find { it.behandling.id == behandlingId && it.aktiv } ?: error("Finner ikke vedtak"),
            grunnlagForVedtakPerioder = grunnlagForVedtaksperiode,
            grunnlagForVedtakPerioderForrigeBehandling = grunnlagForVedtaksperiodeForrigeBehandling,
            endringstidspunkt = TIDENES_MORGEN,
        )
    }

    @Så("forvent følgende vedtaksperioder med begrunnelser")
    fun `forvent følgende vedtaksperioder med begrunnelser`(dataTable: DataTable) {
        val forventedeVedtaksperioder = mapForventetVedtaksperioderMedBegrunnelser(
            dataTable = dataTable,
            vedtak = vedtaksliste.find { it.behandling.id == gjeldendeBehandlingId }
                ?: throw Feil("Fant ingen vedtak for behandling $gjeldendeBehandlingId"),
        )

        val vedtaksperioderComparator = compareBy<VedtaksperiodeMedBegrunnelser>({ it.type }, { it.fom }, { it.tom })
        Assertions.assertThat(vedtaksperioderMedBegrunnelser.sortedWith(vedtaksperioderComparator))
            .isEqualTo(forventedeVedtaksperioder.sortedWith(vedtaksperioderComparator))
    }

    private fun tilVilkårResultater(
        nyeVilkårForPerson: List<MutableMap<String, String>>?,
        behandlingId: Long,
        personResultat: PersonResultat,
    ) =
        nyeVilkårForPerson?.map {
            VilkårResultat(
                vilkårType = parseEnum(DomenebegrepVedtaksperiodeMedBegrunnelser.VILKÅR, it),
                resultat = parseEnum(DomenebegrepVedtaksperiodeMedBegrunnelser.RESULTAT, it),
                periodeFom = parseValgfriDato(Domenebegrep.FRA_DATO, it),
                periodeTom = parseValgfriDato(Domenebegrep.TIL_DATO, it),
                begrunnelse = "",
                behandlingId = behandlingId,
                personResultat = personResultat,
                erEksplisittAvslagPåSøknad = parseValgfriBoolean(
                    DomenebegrepVedtaksperiodeMedBegrunnelser.ER_EKSPLISITT_AVSLAG,
                    it,
                ),

            )
        } ?: emptyList()

    private fun oppdaterVilkårResultat(
        vilkårResultat: VilkårResultat,
        overstyringerForPerson: List<MutableMap<String, String>>?,
    ) {
        val overstyringForVilkår = overstyringerForPerson?.find {
            parseEnumListe<Vilkår>(
                DomenebegrepVedtaksperiodeMedBegrunnelser.VILKÅR,
                it,
            ).contains(vilkårResultat.vilkårType)
        }
        if (overstyringForVilkår != null) {
            vilkårResultat.resultat =
                parseEnum(DomenebegrepVedtaksperiodeMedBegrunnelser.RESULTAT, overstyringForVilkår)
            vilkårResultat.periodeFom = parseValgfriDato(Domenebegrep.FRA_DATO, overstyringForVilkår)
            vilkårResultat.periodeTom = parseValgfriDato(Domenebegrep.TIL_DATO, overstyringForVilkår)
            vilkårResultat.erEksplisittAvslagPåSøknad = parseValgfriBoolean(
                DomenebegrepVedtaksperiodeMedBegrunnelser.ER_EKSPLISITT_AVSLAG,
                overstyringForVilkår,
            )
        }
    }

    private fun genererVedtak(dataTable: DataTable) {
        val fagsak = defaultFagsak()
        behandlinger = dataTable.groupByBehandlingId()
            .map { lagBehandling(fagsak = fagsak).copy(id = it.key) }
            .associateBy { it.id }
        behandlingTilForrigeBehandling = dataTable.asMaps().associate { rad ->
            parseLong(Domenebegrep.BEHANDLING_ID, rad) to parseValgfriLong(Domenebegrep.FORRIGE_BEHANDLING_ID, rad)
        }
        vedtaksliste = dataTable.groupByBehandlingId()
            .map { lagVedtak(behandlinger[it.key] ?: error("Finner ikke behandling")) }
    }

    private fun lagPersonGrunnlag(dataTable: DataTable) {
        persongrunnlag = dataTable.asMaps().map { rad ->
            val behandlingsIder = parseList(Domenebegrep.BEHANDLING_ID, rad)
            behandlingsIder.map { id ->
                id to tilfeldigPerson(
                    personType = parseEnum(
                        DomenebegrepPersongrunnlag.PERSON_TYPE,
                        rad,
                    ),
                    fødselsdato = parseDato(DomenebegrepPersongrunnlag.FØDSELSDATO, rad),
                    aktør = randomAktør().copy(aktørId = parseAktørId(rad)),
                )
            }
        }.flatten()
            .groupBy({ it.first }, { it.second })
            .map { (behandlingId, personer) ->
                PersonopplysningGrunnlag(
                    behandlingId = behandlingId,
                    personer = personer.toMutableSet(),
                )
            }.associateBy { it.behandlingId }
    }

    private fun persongrunnlagForBehandling(behandlingId: Long): PersonopplysningGrunnlag =
        persongrunnlag[behandlingId] ?: error("Finner ikke persongrunnlag for behandling med id $behandlingId")

    private fun finnBehandling(behandlingId: Long) =
        behandlinger[behandlingId] ?: error("Finner ikke behandling med id $behandlingId")
}
