package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser
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
import java.math.BigDecimal
import java.time.LocalDate

fun Map<Long, Behandling>.finnBehandling(behandlingId: Long) =
    this[behandlingId] ?: error("Finner ikke behandling med id $behandlingId")

fun Map<Long, PersonopplysningGrunnlag>.finnPersonGrunnlagForBehandling(behandlingId: Long): PersonopplysningGrunnlag =
    this[behandlingId] ?: error("Finner ikke persongrunnlag for behandling med id $behandlingId")

fun List<MutableMap<String, String>>?.tilVilkårResultater(
    behandlingId: Long,
    personResultat: PersonResultat,
) =
    this?.map {
        VilkårResultat(
            vilkårType = parseEnum(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.VILKÅR,
                it,
            ),
            resultat = parseEnum(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.RESULTAT,
                it,
            ),
            periodeFom = parseValgfriDato(Domenebegrep.FRA_DATO, it),
            periodeTom = parseValgfriDato(Domenebegrep.TIL_DATO, it),
            begrunnelse = "",
            behandlingId = behandlingId,
            personResultat = personResultat,
            erEksplisittAvslagPåSøknad = parseValgfriBoolean(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.ER_EKSPLISITT_AVSLAG,
                it,
            ),
        )
    } ?: emptyList()

fun lagVedtak(
    dataTable: DataTable,
    behandlinger: MutableMap<Long, Behandling>,
    behandlingTilForrigeBehandling: MutableMap<Long, Long?>,
    vedtaksListe: MutableList<Vedtak>,
) {
    val fagsak = defaultFagsak()
    behandlinger.putAll(
        dataTable.groupByBehandlingId()
            .map { lagBehandling(fagsak = fagsak).copy(id = it.key) }
            .associateBy { it.id },
    )
    behandlingTilForrigeBehandling.putAll(
        dataTable.asMaps().associate { rad ->
            parseLong(Domenebegrep.BEHANDLING_ID, rad) to parseValgfriLong(Domenebegrep.FORRIGE_BEHANDLING_ID, rad)
        },
    )
    vedtaksListe.addAll(
        dataTable.groupByBehandlingId()
            .map { no.nav.familie.ba.sak.common.lagVedtak(behandlinger[it.key] ?: error("Finner ikke behandling")) },
    )
}

fun lagPersonresultater(
    persongrunnlagForBehandling: PersonopplysningGrunnlag,
    behandling: Behandling,
) = persongrunnlagForBehandling.personer.map { person ->
    lagPersonResultat(
        vilkårsvurdering = lagVilkårsvurdering(person.aktør, behandling, Resultat.OPPFYLT),
        person = person,
        resultat = Resultat.OPPFYLT,
        personType = person.type,
        lagFullstendigVilkårResultat = true,
        periodeFom = null,
        periodeTom = null,
    )
}.toSet()

fun leggTilVilkårResultatPåPersonResultat(
    personResultatForBehandling: Set<PersonResultat>,
    vilkårResultaterPerPerson: Map<String, List<MutableMap<String, String>>>,
    behandlingId: Long,
) = personResultatForBehandling.map { personResultat ->
    personResultat.vilkårResultater.clear()

    vilkårResultaterPerPerson[personResultat.aktør.aktørId]?.forEach { rad ->
        val vilkårResultaterForÉnRad = parseEnumListe<Vilkår>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.VILKÅR,
            rad,
        ).map { vilkår ->
            VilkårResultat(
                behandlingId = behandlingId,
                personResultat = personResultat,
                vilkårType = vilkår,
                resultat = parseEnum(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.RESULTAT,
                    rad,
                ),
                periodeFom = parseValgfriDato(Domenebegrep.FRA_DATO, rad),
                periodeTom = parseValgfriDato(Domenebegrep.TIL_DATO, rad),
                erEksplisittAvslagPåSøknad = parseValgfriBoolean(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.ER_EKSPLISITT_AVSLAG,
                    rad,
                ),
                begrunnelse = "",
            )
        }
        personResultat.vilkårResultater.addAll(vilkårResultaterForÉnRad)
    }
    personResultat
}.toSet()

fun lagKompetanser(
    nyeKompetanserPerBarn: MutableList<MutableMap<String, String>>,
    personopplysningGrunnlag: Map<Long, PersonopplysningGrunnlag>,
) =
    nyeKompetanserPerBarn.map { rad ->
        val aktørerForKompetanse = VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe(rad)
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
        Kompetanse(
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            barnAktører = personopplysningGrunnlag.finnPersonGrunnlagForBehandling(behandlingId).personer
                .filter { aktørerForKompetanse.contains(it.aktør.aktørId) }
                .map { it.aktør }
                .toSet(),
            søkersAktivitet = parseValgfriEnum<SøkersAktivitet>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITET,
                rad,
            )
                ?: SøkersAktivitet.ARBEIDER,
            annenForeldersAktivitet =
            parseValgfriEnum<AnnenForeldersAktivitet>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITET,
                rad,
            )
                ?: AnnenForeldersAktivitet.I_ARBEID,
            søkersAktivitetsland = parseValgfriString(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITETSLAND,
                rad,
            ) ?: "PL",
            annenForeldersAktivitetsland = parseValgfriString(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITETSLAND,
                rad,
            ) ?: "NO",
            barnetsBostedsland = parseValgfriString(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.BARNETS_BOSTEDSLAND,
                rad,
            ) ?: "NO",
            resultat = parseEnum<KompetanseResultat>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.RESULTAT,
                rad,
            ),
        ).also { it.behandlingId = behandlingId }
    }.groupBy { it.behandlingId }
        .toMutableMap()

fun lagEndredeUtbetalinger(
    nyeEndredeUtbetalingAndeler: MutableList<MutableMap<String, String>>,
    persongrunnlag: Map<Long, PersonopplysningGrunnlag>,
) =
    nyeEndredeUtbetalingAndeler.map { rad ->
        val aktørId = VedtaksperiodeMedBegrunnelserParser.parseAktørId(rad)
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
        EndretUtbetalingAndel(
            behandlingId = behandlingId,
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            person = persongrunnlag.finnPersonGrunnlagForBehandling(behandlingId).personer.find { aktørId == it.aktør.aktørId },
            prosent = BigDecimal.ZERO,
            årsak = Årsak.ALLEREDE_UTBETALT,
            søknadstidspunkt = LocalDate.now(),
            begrunnelse = "Fordi at...",
        )
    }.groupBy { it.behandlingId }
        .toMutableMap()

fun lagPersonGrunnlag(dataTable: DataTable): Map<Long, PersonopplysningGrunnlag> {
    return dataTable.asMaps().map { rad ->
        val behandlingsIder = parseList(Domenebegrep.BEHANDLING_ID, rad)
        behandlingsIder.map { id ->
            id to tilfeldigPerson(
                personType = parseEnum(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.PERSON_TYPE,
                    rad,
                ),
                fødselsdato = parseDato(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.FØDSELSDATO,
                    rad,
                ),
                aktør = randomAktør().copy(aktørId = VedtaksperiodeMedBegrunnelserParser.parseAktørId(rad)),
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

fun lagAndelerTilkjentYtelse(
    dataTable: DataTable,
    behandlinger: MutableMap<Long, Behandling>,
    personGrunnlag: Map<Long, PersonopplysningGrunnlag>,
) = dataTable.asMaps().map { rad ->
    val aktørId = VedtaksperiodeMedBegrunnelserParser.parseAktørId(rad)
    val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
    lagAndelTilkjentYtelse(
        fom = parseDato(Domenebegrep.FRA_DATO, rad).toYearMonth(),
        tom = parseDato(Domenebegrep.TIL_DATO, rad).toYearMonth(),
        behandling = behandlinger.finnBehandling(behandlingId),
        person = personGrunnlag.finnPersonGrunnlagForBehandling(behandlingId).personer.find { aktørId == it.aktør.aktørId }!!,
        beløp = parseInt(VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.BELØP, rad),
    )
}.groupBy { it.behandlingId }
    .toMutableMap()

fun lagVedtaksPerioder(
    behandlingId: Long,
    vedtaksListe: List<Vedtak>,
    behandlingTilForrigeBehandling: MutableMap<Long, Long?>,
    personGrunnlag: Map<Long, PersonopplysningGrunnlag>,
    personResultater: Map<Long, Set<PersonResultat>>,
    kompetanser: Map<Long, List<Kompetanse>>,
    endredeUtbetalinger: Map<Long, List<EndretUtbetalingAndel>>,
    andelerTilkjentYtelse: Map<Long, List<AndelTilkjentYtelse>>,
    endringstidspunkt: Map<Long, LocalDate?>,
): List<VedtaksperiodeMedBegrunnelser> {
    val vedtak = vedtaksListe.find { it.behandling.id == behandlingId && it.aktiv }
        ?: error("Finner ikke vedtak")
    val grunnlagForVedtaksperiode = GrunnlagForVedtaksperioder(
        persongrunnlag = personGrunnlag.finnPersonGrunnlagForBehandling(behandlingId),
        personResultater = personResultater[behandlingId] ?: error("Finner ikke personresultater"),
        fagsakType = vedtak.behandling.fagsak.type,
        kompetanser = kompetanser[behandlingId] ?: emptyList(),
        endredeUtbetalinger = endredeUtbetalinger[behandlingId] ?: emptyList(),
        andelerTilkjentYtelse = andelerTilkjentYtelse[behandlingId] ?: emptyList(),
        perioderOvergangsstønad = emptyList(),
    )

    val forrigeBehandlingId = behandlingTilForrigeBehandling[behandlingId]

    val grunnlagForVedtaksperiodeForrigeBehandling = forrigeBehandlingId?.let {
        val forrigeVedtak = vedtaksListe.find { it.behandling.id == forrigeBehandlingId && it.aktiv }
            ?: error("Finner ikke vedtak")
        GrunnlagForVedtaksperioder(
            persongrunnlag = personGrunnlag.finnPersonGrunnlagForBehandling(forrigeBehandlingId),
            personResultater = personResultater[forrigeBehandlingId] ?: error("Finner ikke personresultater"),
            fagsakType = forrigeVedtak.behandling.fagsak.type,
            kompetanser = kompetanser[forrigeBehandlingId] ?: emptyList(),
            endredeUtbetalinger = endredeUtbetalinger[forrigeBehandlingId] ?: emptyList(),
            andelerTilkjentYtelse = andelerTilkjentYtelse[forrigeBehandlingId] ?: emptyList(),
            perioderOvergangsstønad = emptyList(),
        )
    }

    return genererVedtaksperioder(
        vedtak = vedtak,
        grunnlagForVedtakPerioder = grunnlagForVedtaksperiode,
        grunnlagForVedtakPerioderForrigeBehandling = grunnlagForVedtaksperiodeForrigeBehandling,
        endringstidspunkt = endringstidspunkt[behandlingId] ?: TIDENES_MORGEN,
    )
}
