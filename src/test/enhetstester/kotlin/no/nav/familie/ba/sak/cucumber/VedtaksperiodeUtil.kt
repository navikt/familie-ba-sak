package no.nav.familie.ba.sak.cucumber

import io.cucumber.datatable.DataTable
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.tilddMMyyyy
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.cucumber.domeneparser.BrevPeriodeParser
import no.nav.familie.ba.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ba.sak.cucumber.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ba.sak.cucumber.domeneparser.VedtaksperiodeMedBegrunnelserParser
import no.nav.familie.ba.sak.cucumber.domeneparser.parseBigDecimal
import no.nav.familie.ba.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseEnumListe
import no.nav.familie.ba.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseList
import no.nav.familie.ba.sak.cucumber.domeneparser.parseLong
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriBigDecimal
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriBoolean
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriEnum
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriLong
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriString
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriStringList
import no.nav.familie.ba.sak.cucumber.domeneparser.parseValgfriÅrMåned
import no.nav.familie.ba.sak.cucumber.mock.CucumberMock
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVedtakMedId
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenTidligerePerioder
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.konverterBeløpTilMånedlig
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Vurderingsform
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.forrigebehandling.EndringIUtbetalingUtil
import no.nav.familie.ba.sak.kjerne.grunnlag.overgangsstønad.splittOgSlåSammen
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.lagDødsfall
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.tilVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.random.Random

fun Map<Long, Behandling>.finnBehandling(behandlingId: Long) = this[behandlingId] ?: throw Feil("Finner ikke behandling med id $behandlingId")

fun Map<Long, PersonopplysningGrunnlag>.finnPersonGrunnlagForBehandling(behandlingId: Long): PersonopplysningGrunnlag = this[behandlingId] ?: throw Feil("Finner ikke persongrunnlag for behandling med id $behandlingId")

fun lagFagsaker(dataTable: DataTable) =
    dataTable
        .asMaps()
        .map { rad ->
            Fagsak(
                id = parseLong(Domenebegrep.FAGSAK_ID, rad),
                type = parseValgfriEnum<FagsakType>(Domenebegrep.FAGSAK_TYPE, rad) ?: FagsakType.NORMAL,
                aktør = randomAktør(),
                status = parseValgfriEnum<FagsakStatus>(Domenebegrep.STATUS, rad) ?: FagsakStatus.OPPRETTET,
                institusjon = Institusjon(orgNummer = "", tssEksternId = ""),
            )
        }.associateBy { it.id }
        .toMutableMap()

fun lagVedtak(
    dataTable: DataTable,
    behandlinger: MutableMap<Long, Behandling>,
    behandlingTilForrigeBehandling: MutableMap<Long, Long?>,
    vedtaksListe: MutableList<Vedtak>,
    fagsaker: MutableMap<Long, Fagsak>,
) {
    behandlinger.putAll(
        lagBehandlinger(dataTable, fagsaker).associateBy { it.id },
    )
    behandlingTilForrigeBehandling.putAll(
        dataTable.asMaps().associate { rad ->
            parseLong(Domenebegrep.BEHANDLING_ID, rad) to parseValgfriLong(Domenebegrep.FORRIGE_BEHANDLING_ID, rad)
        },
    )
    vedtaksListe.addAll(
        dataTable
            .groupByBehandlingId()
            .map {
                lagVedtakMedId(behandlinger[it.key] ?: throw Feil("Finner ikke behandling"))
            },
    )
}

fun lagBehandlinger(
    dataTable: DataTable,
    fagsaker: MutableMap<Long, Fagsak>,
): List<Behandling> =
    dataTable.asMaps().map { rad ->
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
        val fagsakId = parseValgfriLong(Domenebegrep.FAGSAK_ID, rad)
        val fagsak =
            fagsaker.getOrDefault(fagsakId, defaultFagsak()).also {
                if (it.id !in fagsaker.keys) {
                    fagsaker[it.id] = it
                }
            }

        val behandlingÅrsak = parseValgfriEnum<BehandlingÅrsak>(Domenebegrep.BEHANDLINGSÅRSAK, rad)
        val behandlingResultat = parseValgfriEnum<Behandlingsresultat>(Domenebegrep.BEHANDLINGSRESULTAT, rad)
        val skalBehandlesAutomatisk = parseValgfriBoolean(Domenebegrep.SKAL_BEHANLDES_AUTOMATISK, rad) ?: false
        val behandlingKategori =
            parseValgfriEnum<BehandlingKategori>(Domenebegrep.BEHANDLINGSKATEGORI, rad)
                ?: BehandlingKategori.NASJONAL
        val status = parseValgfriEnum<BehandlingStatus>(Domenebegrep.BEHANDLINGSSTATUS, rad)
        val behandlingstype = parseValgfriEnum<BehandlingType>(Domenebegrep.BEHANDLINGSTYPE, rad) ?: BehandlingType.FØRSTEGANGSBEHANDLING
        val behandlingssteg = parseValgfriEnum<StegType>(Domenebegrep.BEHANDLINGSSTEG, rad) ?: StegType.REGISTRERE_PERSONGRUNNLAG
        val underkategori = parseValgfriEnum<BehandlingUnderkategori>(Domenebegrep.UNDERKATEGORI, rad) ?: BehandlingUnderkategori.ORDINÆR
        val endretTidspunkt = parseValgfriDato(Domenebegrep.ENDRET_TIDSPUNKT, rad) ?: LocalDate.now()

        lagBehandlingUtenId(
            endretTidspunkt = endretTidspunkt.atStartOfDay(),
            fagsak = fagsak,
            årsak = behandlingÅrsak ?: BehandlingÅrsak.SØKNAD,
            resultat = behandlingResultat ?: Behandlingsresultat.IKKE_VURDERT,
            skalBehandlesAutomatisk = skalBehandlesAutomatisk,
            behandlingKategori = behandlingKategori,
            status = status ?: BehandlingStatus.UTREDES,
            behandlingType = behandlingstype,
            førsteSteg = behandlingssteg,
            underkategori = underkategori,
        ).copy(id = behandlingId).also {
            it.endretTidspunkt = endretTidspunkt.atStartOfDay()
        }
    }

fun lagVilkårsvurdering(
    persongrunnlagForBehandling: PersonopplysningGrunnlag,
    behandling: Behandling,
): Vilkårsvurdering {
    val vilkårsvurdering =
        lagVilkårsvurdering(søkerAktør = behandling.fagsak.aktør, behandling = behandling, resultat = Resultat.OPPFYLT)
    val personResultater =
        persongrunnlagForBehandling.personer
            .map { person ->
                lagPersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    person = person,
                    resultat = Resultat.IKKE_VURDERT,
                    personType = person.type,
                    lagFullstendigVilkårResultat = true,
                    periodeFom = null,
                    periodeTom = null,
                )
            }.toSet()
    vilkårsvurdering.personResultater = personResultater
    return vilkårsvurdering
}

fun leggTilVilkårResultatPåPersonResultat(
    personResultatForBehandling: Set<PersonResultat>,
    vilkårResultaterPerPerson: Map<String, List<MutableMap<String, String>>>,
    behandlingId: Long,
) = personResultatForBehandling
    .map { personResultat ->
        personResultat.apply {
            vilkårResultaterPerPerson[aktør.aktørId]?.let { vilkårResultaterForPerson ->
                vilkårResultater.clear()
                val nyeVilkårResultater = parseVilkårResultaterForAktør(vilkårResultaterForPerson, behandlingId, personResultat)
                vilkårResultater.addAll(nyeVilkårResultater)
            }
        }
    }.toSet()

fun parseVilkårResultaterForAktør(
    vilkårResultatRaderForAktør: List<MutableMap<String, String>>,
    behandlingId: Long,
    personResultat: PersonResultat? = null,
): Set<VilkårResultat> =
    vilkårResultatRaderForAktør
        .flatMap { rad ->
            val vilkårForÉnRad =
                parseEnumListe<Vilkår>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.VILKÅR,
                    rad,
                )

            val utdypendeVilkårsvurderingForÉnRad =
                parseEnumListe<UtdypendeVilkårsvurdering>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.UTDYPENDE_VILKÅR,
                    rad,
                )

            val vurderesEtterForEnRad =
                parseValgfriEnum<Regelverk>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.VURDERES_ETTER,
                    rad,
                ) ?: Regelverk.NASJONALE_REGLER

            val vilkårResultaterForÉnRad =
                vilkårForÉnRad.map { vilkår ->
                    VilkårResultat(
                        sistEndretIBehandlingId = behandlingId,
                        personResultat = personResultat,
                        vilkårType = vilkår,
                        resultat =
                            parseEnum(
                                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.RESULTAT,
                                rad,
                            ),
                        periodeFom = parseValgfriDato(Domenebegrep.FRA_DATO, rad),
                        periodeTom = parseValgfriDato(Domenebegrep.TIL_DATO, rad),
                        erEksplisittAvslagPåSøknad =
                            parseValgfriBoolean(
                                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.ER_EKSPLISITT_AVSLAG,
                                rad,
                            ),
                        begrunnelse = "",
                        utdypendeVilkårsvurderinger = utdypendeVilkårsvurderingForÉnRad,
                        vurderesEtter = vurderesEtterForEnRad,
                        standardbegrunnelser = hentStandardBegrunnelser(rad),
                    )
                }
            vilkårResultaterForÉnRad
        }.toSet()

private fun hentStandardBegrunnelser(rad: MutableMap<String, String>): List<IVedtakBegrunnelse> {
    val standardbegrunnelser: List<IVedtakBegrunnelse> =
        try {
            parseEnumListe<Standardbegrunnelse>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.STANDARDBEGRUNNELSER,
                rad,
            )
        } catch (_: Exception) {
            parseEnumListe<EØSStandardbegrunnelse>(
                VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.STANDARDBEGRUNNELSER,
                rad,
            )
        }

    return standardbegrunnelser
}

fun lagKompetanser(
    nyeKompetanserPerBarn: MutableList<MutableMap<String, String>>,
    personopplysningGrunnlag: Map<Long, PersonopplysningGrunnlag>,
) = nyeKompetanserPerBarn
    .map { rad ->
        val aktørerForKompetanse = VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe(rad)
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
        Kompetanse(
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            barnAktører =
                personopplysningGrunnlag
                    .finnPersonGrunnlagForBehandling(behandlingId)
                    .personer
                    .filter { aktørerForKompetanse.contains(it.aktør.aktørId) }
                    .map { it.aktør }
                    .toSet(),
            søkersAktivitet =
                parseValgfriEnum<KompetanseAktivitet>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITET,
                    rad,
                )
                    ?: KompetanseAktivitet.ARBEIDER,
            annenForeldersAktivitet =
                parseValgfriEnum<KompetanseAktivitet>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITET,
                    rad,
                )
                    ?: KompetanseAktivitet.I_ARBEID,
            søkersAktivitetsland =
                parseValgfriString(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.SØKERS_AKTIVITETSLAND,
                    rad,
                )?.also { validerErLandkode(it) } ?: "PL",
            annenForeldersAktivitetsland =
                parseValgfriString(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.ANNEN_FORELDERS_AKTIVITETSLAND,
                    rad,
                )?.also { validerErLandkode(it) } ?: "NO",
            barnetsBostedsland =
                parseValgfriString(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.BARNETS_BOSTEDSLAND,
                    rad,
                )?.also { validerErLandkode(it) } ?: "NO",
            resultat =
                parseEnum<KompetanseResultat>(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepKompetanse.RESULTAT,
                    rad,
                ),
        ).also { it.behandlingId = behandlingId }
    }.groupBy { it.behandlingId }
    .toMutableMap()

fun lagValutakurs(
    nyeValutakursPerBarn: MutableList<MutableMap<String, String>>,
    personopplysningGrunnlag: Map<Long, PersonopplysningGrunnlag>,
) = nyeValutakursPerBarn
    .map { rad ->
        val aktørerForValutakurs = VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe(rad)
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)

        Valutakurs(
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            barnAktører =
                personopplysningGrunnlag
                    .finnPersonGrunnlagForBehandling(behandlingId)
                    .personer
                    .filter { aktørerForValutakurs.contains(it.aktør.aktørId) }
                    .map { it.aktør }
                    .toSet(),
            valutakursdato = parseValgfriDato(VedtaksperiodeMedBegrunnelserParser.DomenebegrepValutakurs.VALUTAKURSDATO, rad),
            valutakode =
                parseValgfriString(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepValutakurs.VALUTA_KODE,
                    rad,
                ),
            kurs = parseValgfriBigDecimal(VedtaksperiodeMedBegrunnelserParser.DomenebegrepValutakurs.KURS, rad),
            vurderingsform = parseValgfriEnum<Vurderingsform>(VedtaksperiodeMedBegrunnelserParser.DomenebegrepValutakurs.VURDERINGSFORM, rad) ?: Vurderingsform.MANUELL,
        ).also { it.behandlingId = behandlingId }
    }.groupBy { it.behandlingId }
    .toMutableMap()

fun lagUtenlandskperiodeBeløp(
    nyeUtenlandskPeriodebeløpPerBarn: MutableList<MutableMap<String, String>>,
    personopplysningGrunnlag: Map<Long, PersonopplysningGrunnlag>,
) = nyeUtenlandskPeriodebeløpPerBarn
    .map { rad ->
        val aktørerForValutakurs = VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe(rad)
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)

        val intervall = parseValgfriEnum<Intervall>(VedtaksperiodeMedBegrunnelserParser.DomenebegrepUtenlandskPeriodebeløp.INTERVALL, rad) ?: Intervall.MÅNEDLIG
        val beløp = parseBigDecimal(VedtaksperiodeMedBegrunnelserParser.DomenebegrepUtenlandskPeriodebeløp.BELØP, rad)
        UtenlandskPeriodebeløp(
            fom = parseValgfriÅrMåned(Domenebegrep.FRA_MÅNED, rad),
            tom = parseValgfriÅrMåned(Domenebegrep.TIL_MÅNED, rad),
            barnAktører =
                personopplysningGrunnlag
                    .finnPersonGrunnlagForBehandling(behandlingId)
                    .personer
                    .filter { aktørerForValutakurs.contains(it.aktør.aktørId) }
                    .map { it.aktør }
                    .toSet(),
            beløp = beløp,
            valutakode =
                parseValgfriString(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepUtenlandskPeriodebeløp.VALUTA_KODE,
                    rad,
                ),
            intervall = intervall,
            utbetalingsland = parseValgfriString(VedtaksperiodeMedBegrunnelserParser.DomenebegrepUtenlandskPeriodebeløp.UTBETALINGSLAND, rad),
            kalkulertMånedligBeløp = intervall.konverterBeløpTilMånedlig(beløp),
        ).also { it.behandlingId = behandlingId }
    }.groupBy { it.behandlingId }
    .toMutableMap()

private fun validerErLandkode(it: String) {
    if (it.length != 2) {
        throw Feil("$it er ikke en landkode")
    }
}

fun lagEndredeUtbetalinger(
    nyeEndredeUtbetalingAndeler: MutableList<MutableMap<String, String>>,
    persongrunnlag: Map<Long, PersonopplysningGrunnlag>,
) = nyeEndredeUtbetalingAndeler
    .map { rad ->
        val aktørIder = VedtaksperiodeMedBegrunnelserParser.parseAktørIdListe(rad)
        val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
        EndretUtbetalingAndel(
            behandlingId = behandlingId,
            fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)?.toYearMonth(),
            tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)?.toYearMonth(),
            personer =
                persongrunnlag
                    .finnPersonGrunnlagForBehandling(behandlingId)
                    .personer
                    .filter { it.aktør.aktørId in aktørIder }
                    .toMutableSet(),
            prosent =
                parseValgfriLong(
                    VedtaksperiodeMedBegrunnelserParser.DomenebegrepEndretUtbetaling.PROSENT,
                    rad,
                )?.toBigDecimal() ?: BigDecimal.valueOf(100),
            årsak =
                parseValgfriEnum<Årsak>(VedtaksperiodeMedBegrunnelserParser.DomenebegrepEndretUtbetaling.ÅRSAK, rad)
                    ?: Årsak.ALLEREDE_UTBETALT,
            søknadstidspunkt = parseValgfriDato(Domenebegrep.SØKNADSTIDSPUNKT, rad) ?: LocalDate.now(),
            begrunnelse = "Fordi at...",
            avtaletidspunktDeltBosted =
                parseValgfriDato(
                    BrevPeriodeParser.DomenebegrepBrevBegrunnelse.AVTALETIDSPUNKT_DELT_BOSTED,
                    rad,
                ),
        )
    }.groupBy { it.behandlingId }
    .toMutableMap()

fun lagPersonGrunnlag(dataTable: DataTable): Map<Long, PersonopplysningGrunnlag> =
    dataTable
        .asMaps()
        .map { rad ->
            val behandlingsIder = parseList(Domenebegrep.BEHANDLING_ID, rad)
            behandlingsIder.map { id ->
                id to
                    tilfeldigPerson(
                        personType =
                            parseEnum(
                                VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.PERSON_TYPE,
                                rad,
                            ),
                        fødselsdato =
                            parseDato(
                                VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.FØDSELSDATO,
                                rad,
                            ),
                        aktør = randomAktør().copy(aktørId = VedtaksperiodeMedBegrunnelserParser.parseAktørId(rad)),
                    ).also { person ->
                        parseValgfriDato(
                            VedtaksperiodeMedBegrunnelserParser.DomenebegrepPersongrunnlag.DØDSFALLDATO,
                            rad,
                        )?.let { person.dødsfall = lagDødsfall(person = person, dødsfallDato = it) }
                    }
            }
        }.flatten()
        .groupBy({ it.first }, { it.second })
        .map { (behandlingId, personer) ->
            PersonopplysningGrunnlag(
                behandlingId = behandlingId,
                personer = personer.toMutableSet(),
            )
        }.associateBy { it.behandlingId }

fun lagTilkjentYtelse(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    dataTable: DataTable,
    behandlinger: MutableMap<Long, Behandling>,
    personGrunnlag: Map<Long, PersonopplysningGrunnlag>,
    vedtaksliste: List<Vedtak>,
    behandlingTilForrigeBehandling: MutableMap<Long, Long?>,
): MutableMap<Long, TilkjentYtelse> {
    val tilkjenteYtelser =
        dataTable
            .asMaps()
            .map { rad ->
                val aktørId = VedtaksperiodeMedBegrunnelserParser.parseAktørId(rad)
                val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
                val beløp = parseInt(VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.BELØP, rad)
                val differanseberegnetBeløp = parseValgfriInt(VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.DIFFERANSEBEREGNET_BELØP, rad)
                val sats =
                    parseValgfriInt(
                        VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.SATS,
                        rad,
                    ) ?: beløp

                lagAndelTilkjentYtelse(
                    id = Random.nextLong(),
                    fom = parseDato(Domenebegrep.FRA_DATO, rad).toYearMonth(),
                    tom = parseDato(Domenebegrep.TIL_DATO, rad).toYearMonth(),
                    behandling = behandlinger.finnBehandling(behandlingId),
                    person = personGrunnlag.finnPersonGrunnlagForBehandling(behandlingId).personer.find { aktørId == it.aktør.aktørId }!!,
                    beløp = beløp,
                    ytelseType =
                        parseValgfriEnum<YtelseType>(
                            VedtaksperiodeMedBegrunnelserParser.DomenebegrepAndelTilkjentYtelse.YTELSE_TYPE,
                            rad,
                        ) ?: YtelseType.ORDINÆR_BARNETRYGD,
                    prosent =
                        parseValgfriLong(
                            VedtaksperiodeMedBegrunnelserParser.DomenebegrepEndretUtbetaling.PROSENT,
                            rad,
                        )?.toBigDecimal() ?: BigDecimal(100),
                    sats = sats,
                    differanseberegnetPeriodebeløp = differanseberegnetBeløp,
                    nasjonaltPeriodebeløp = sats,
                    beløpUtenEndretUtbetaling = sats,
                )
            }.groupBy {
                it.behandlingId
            }.mapValues { (behandlingId, andeler) ->
                TilkjentYtelse(
                    behandling = behandlinger.finnBehandling(behandlingId),
                    andelerTilkjentYtelse = andeler.toMutableSet(),
                    opprettetDato = LocalDate.now(),
                    endretDato = LocalDate.now(),
                )
            }

    tilkjenteYtelser.forEach { tilkjentYtelse ->
        val behandling = tilkjentYtelse.value.behandling
        val nåværendeAndeler = tilkjentYtelse.value.andelerTilkjentYtelse.toList()
        val forrigeTilkjentYtelse = tilkjenteYtelser.values.find { it.behandling.id == behandlingTilForrigeBehandling[behandling.id] }
        val forrigeAndeler = forrigeTilkjentYtelse?.andelerTilkjentYtelse?.toList() ?: emptyList()
        if (
            behandling.status == BehandlingStatus.AVSLUTTET &&
            (skalIverksettesMotOppdrag(nåværendeAndeler, forrigeAndeler) || behandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD)
        ) {
            val vedtak = vedtaksliste.single { it.behandling.id == tilkjentYtelse.value.behandling.id && it.aktiv }
            tilkjentYtelse.value.oppdaterMedUtbetalingsoppdrag(dataFraCucumber, vedtak)
        }
    }

    return tilkjenteYtelser.toMutableMap()
}

private fun skalIverksettesMotOppdrag(
    nåværendeAndeler: List<AndelTilkjentYtelse>,
    forrigeAndeler: List<AndelTilkjentYtelse>,
): Boolean =
    EndringIUtbetalingUtil
        .lagEndringIUtbetalingTidslinje(nåværendeAndeler, forrigeAndeler)
        .tilPerioder()
        .any { it.verdi == true }

private fun TilkjentYtelse.oppdaterMedUtbetalingsoppdrag(
    dataFraCucumber: VedtaksperioderOgBegrunnelserStepDefinition,
    vedtak: Vedtak,
) {
    if (this.andelerTilkjentYtelse.none { it.erAndelSomSkalSendesTilOppdrag() }) {
        return
    }
    val mock = CucumberMock(dataFraCucumber, behandling.id)
    val beregnetUtbetalingsoppdrag =
        mock.utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
            saksbehandlerId = "saksbehandlerId",
            vedtak = vedtak,
            tilkjentYtelse = this,
            erSimulering = false,
        )
    mock.oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(this, beregnetUtbetalingsoppdrag)
}

fun lagOvergangsstønad(
    dataTable: DataTable,
    persongrunnlag: Map<Long, PersonopplysningGrunnlag>,
    tidligereBehandlinger: Map<Long, Long?>,
    dagensDato: LocalDate,
): Map<Long, List<InternPeriodeOvergangsstønad>> {
    val overgangsstønadPeriodePåBehandlinger =
        dataTable
            .asMaps()
            .groupBy({ rad -> parseLong(Domenebegrep.BEHANDLING_ID, rad) }, { rad ->
                val behandlingId = parseLong(Domenebegrep.BEHANDLING_ID, rad)
                val aktørId = VedtaksperiodeMedBegrunnelserParser.parseAktørId(rad)

                InternPeriodeOvergangsstønad(
                    fomDato = parseDato(Domenebegrep.FRA_DATO, rad),
                    tomDato = parseDato(Domenebegrep.TIL_DATO, rad),
                    personIdent =
                        persongrunnlag[behandlingId]!!
                            .personer
                            .single { it.aktør.aktørId == aktørId }
                            .aktør
                            .aktivFødselsnummer(),
                )
            })

    return overgangsstønadPeriodePåBehandlinger.mapValues { (behandlingId, overgangsstønad) ->
        overgangsstønad.splittOgSlåSammen(
            overgangsstønadPeriodePåBehandlinger[tidligereBehandlinger[behandlingId]]?.slåSammenTidligerePerioder(
                dagensDato,
            ) ?: emptyList(),
            dagensDato,
        )
    }
}

fun leggBegrunnelserIVedtaksperiodene(
    dataTable: DataTable,
    vedtaksperioder: List<UtvidetVedtaksperiodeMedBegrunnelser>,
    vedtak: Vedtak,
) = dataTable.asMaps().map { rad ->
    val fom = parseValgfriDato(Domenebegrep.FRA_DATO, rad)
    val tom = parseValgfriDato(Domenebegrep.TIL_DATO, rad)

    val vedtaksperiode =
        vedtaksperioder.find { it.fom == fom && it.tom == tom }
            ?: throw Feil(
                "Ingen vedtaksperioder med Fom=$fom og Tom=$tom. " +
                    "Vedtaksperiodene var ${vedtaksperioder.map { "\n${it.fom?.tilddMMyyyy()} til ${it.tom?.tilddMMyyyy()}" }}",
            )
    val vedtaksperiodeMedBegrunnelser =
        vedtaksperiode.tilVedtaksperiodeMedBegrunnelser(
            vedtak,
        )

    val standardbegrunnelser =
        parseEnumListe<Standardbegrunnelse>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.STANDARDBEGRUNNELSER,
            rad,
        ).map {
            Vedtaksbegrunnelse(
                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                standardbegrunnelse = it,
            )
        }.toMutableSet()
    val eøsBegrunnelser =
        parseEnumListe<EØSStandardbegrunnelse>(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.EØSBEGRUNNELSER,
            rad,
        ).map { EØSBegrunnelse(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser, begrunnelse = it) }
            .toMutableSet()
    val fritekster =
        parseValgfriStringList(
            VedtaksperiodeMedBegrunnelserParser.DomenebegrepVedtaksperiodeMedBegrunnelser.FRITEKSTER,
            rad,
        ).map {
            VedtaksbegrunnelseFritekst(
                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                fritekst = it,
            )
        }.toMutableList()

    vedtaksperiodeMedBegrunnelser
        .copy(begrunnelser = standardbegrunnelser, eøsBegrunnelser = eøsBegrunnelser, fritekster = fritekster)
}
