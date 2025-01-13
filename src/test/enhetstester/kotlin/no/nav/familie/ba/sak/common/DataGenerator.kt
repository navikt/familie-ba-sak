package no.nav.familie.ba.sak.common

import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.datagenerator.vedtak.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestInstitusjon
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.initStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Tema
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårRolle
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.BarnetsBostedsland
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ba.sak.kjerne.brev.mottaker.MottakerType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ba.sak.kjerne.eøs.vilkårsvurdering.VilkårRegelverkResultat
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.domene.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.domene.EØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøs
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.gjelderAlltidFraBarnetsFødselsdato
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Properties
import kotlin.math.abs
import kotlin.random.Random

val fødselsnummerGenerator = FoedselsnummerGenerator()

fun randomFnr(foedselsdato: LocalDate? = null): String =
    fødselsnummerGenerator
        .foedselsnummer(
            foedselsdato ?: (20..70).random().årSiden.minusDays((1..364).random().toLong()),
        ).asString

fun randomBarnFnr(alder: Int? = null): String =
    randomFnr(
        (alder ?: (1..16).random()).årSiden.minusDays((1..364).random().toLong()),
    )

fun randomPersonident(
    aktør: Aktør,
    fnr: String = randomFnr(),
): Personident = Personident(fødselsnummer = fnr, aktør = aktør)

fun randomAktør(fnr: String = randomFnr()): Aktør =
    Aktør(Random.nextLong(1000_000_000_000, 31_121_299_99999).toString()).also {
        it.personidenter.add(
            randomPersonident(it, fnr),
        )
    }

private var gjeldendeVedtakId: Long = abs(Random.nextLong(10000000))
private var gjeldendeBehandlingId: Long = abs(Random.nextLong(10000000))
private var gjeldendePersonId: Long = abs(Random.nextLong(10000000))
private var gjeldendeUtvidetVedtaksperiodeId: Long = abs(Random.nextLong(10000000))
private const val ID_INKREMENT = 50

fun nesteVedtakId(): Long {
    gjeldendeVedtakId += ID_INKREMENT
    return gjeldendeVedtakId
}

fun setGjeldendeBehandlingID(id: Long) {
    gjeldendeBehandlingId = id
}

fun nesteBehandlingId(): Long {
    gjeldendeBehandlingId += ID_INKREMENT
    return gjeldendeBehandlingId
}

fun nestePersonId(): Long {
    gjeldendePersonId += ID_INKREMENT
    return gjeldendePersonId
}

fun nesteUtvidetVedtaksperiodeId(): Long {
    gjeldendeUtvidetVedtaksperiodeId += ID_INKREMENT
    return gjeldendeUtvidetVedtaksperiodeId
}

fun defaultFagsak(aktør: Aktør = tilAktør(randomFnr())) =
    Fagsak(
        0,
        aktør = aktør,
    )

fun lagFagsak(
    id: Long = 0,
    aktør: Aktør = tilAktør(randomFnr()),
    institusjon: Institusjon? = null,
    status: FagsakStatus = FagsakStatus.OPPRETTET,
    type: FagsakType = FagsakType.NORMAL,
    arkivert: Boolean = false,
) = Fagsak(
    id = id,
    aktør = aktør,
    institusjon = institusjon,
    status = status,
    type = type,
    arkivert = arkivert,
)

fun lagInstitusjon(
    id: Long = 0L,
    orgNummer: String = "123456789",
    tssEksternId: String? = "tssEksternId",
) = Institusjon(
    id = id,
    orgNummer = orgNummer,
    tssEksternId = tssEksternId,
)

fun lagBehandling(
    fagsak: Fagsak = defaultFagsak(),
    behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    skalBehandlesAutomatisk: Boolean = false,
    førsteSteg: StegType = FØRSTE_STEG,
    resultat: Behandlingsresultat = Behandlingsresultat.IKKE_VURDERT,
    underkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
    status: BehandlingStatus = initStatus(),
    aktivertTid: LocalDateTime = LocalDateTime.now(),
    id: Long = 0,
    endretTidspunkt: LocalDateTime = LocalDateTime.now(),
    aktiv: Boolean = true,
) = Behandling(
    id = id,
    fagsak = fagsak,
    skalBehandlesAutomatisk = skalBehandlesAutomatisk,
    type = behandlingType,
    kategori = behandlingKategori,
    underkategori = underkategori,
    opprettetÅrsak = årsak,
    resultat = resultat,
    status = status,
    aktivertTidspunkt = aktivertTid,
    aktiv = aktiv,
).also {
    it.endretTidspunkt = endretTidspunkt
    val tidligereSteg =
        StegType.entries
            .filter { it.rekkefølge < førsteSteg.rekkefølge }
            .filter { it != StegType.HENLEGG_BEHANDLING }

    tidligereSteg.forEach { steg ->
        it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, steg, behandlingStegStatus = BehandlingStegStatus.UTFØRT))
    }

    it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, førsteSteg))
}

fun tilfeldigPerson(
    fødselsdato: LocalDate = LocalDate.now(),
    personType: PersonType = PersonType.BARN,
    kjønn: Kjønn = Kjønn.MANN,
    aktør: Aktør = randomAktør(),
    personId: Long = nestePersonId(),
    dødsfall: Dødsfall? = null,
) = Person(
    id = personId,
    aktør = aktør,
    fødselsdato = fødselsdato,
    type = personType,
    personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
    navn = "",
    kjønn = kjønn,
    målform = Målform.NB,
    dødsfall = dødsfall,
).apply { sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.UGIFT, person = this)) }

fun Person.tilPersonEnkel() = PersonEnkel(this.type, this.aktør, this.fødselsdato, this.dødsfall?.dødsfallDato, this.målform)

fun tilfeldigSøker(
    fødselsdato: LocalDate = LocalDate.now(),
    personType: PersonType = PersonType.SØKER,
    kjønn: Kjønn = Kjønn.MANN,
    aktør: Aktør = randomAktør(),
) = Person(
    id = nestePersonId(),
    aktør = aktør,
    fødselsdato = fødselsdato,
    type = personType,
    personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
    navn = "",
    kjønn = kjønn,
    målform = Målform.NB,
).apply { sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.UGIFT, person = this)) }

fun lagVedtak(
    behandling: Behandling = lagBehandling(),
    stønadBrevPdF: ByteArray? = null,
    vedtaksdato: LocalDateTime? = LocalDateTime.now(),
) = Vedtak(
    id = 0,
    behandling = behandling,
    vedtaksdato = vedtaksdato,
    stønadBrevPdF = stønadBrevPdF,
)

fun lagAndelTilkjentYtelse(
    fom: YearMonth,
    tom: YearMonth,
    ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    beløp: Int = sats(ytelseType),
    behandling: Behandling = lagBehandling(),
    person: Person = tilfeldigPerson(),
    aktør: Aktør = person.aktør,
    periodeIdOffset: Long? = null,
    forrigeperiodeIdOffset: Long? = null,
    tilkjentYtelse: TilkjentYtelse? = null,
    prosent: BigDecimal = BigDecimal(100),
    kildeBehandlingId: Long? = behandling.id,
    differanseberegnetPeriodebeløp: Int? = null,
    id: Long = 0,
    sats: Int = sats(ytelseType),
    kalkulertUtbetalingsbeløp: Int? = null,
    nasjonaltPeriodebeløp: Int = beløp,
): AndelTilkjentYtelse =
    AndelTilkjentYtelse(
        id = id,
        aktør = aktør,
        behandlingId = behandling.id,
        tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
        kalkulertUtbetalingsbeløp = if (kalkulertUtbetalingsbeløp == null) beløp else kalkulertUtbetalingsbeløp,
        nasjonaltPeriodebeløp = nasjonaltPeriodebeløp,
        stønadFom = fom,
        stønadTom = tom,
        type = ytelseType,
        periodeOffset = periodeIdOffset,
        forrigePeriodeOffset = forrigeperiodeIdOffset,
        sats = sats,
        prosent = prosent,
        kildeBehandlingId = kildeBehandlingId,
        differanseberegnetPeriodebeløp = differanseberegnetPeriodebeløp,
    )

fun lagAndelTilkjentYtelseMedEndreteUtbetalinger(
    fom: YearMonth,
    tom: YearMonth,
    ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    beløp: Int = sats(ytelseType),
    behandling: Behandling = lagBehandling(),
    person: Person = tilfeldigPerson(),
    aktør: Aktør = person.aktør,
    periodeIdOffset: Long? = null,
    forrigeperiodeIdOffset: Long? = null,
    tilkjentYtelse: TilkjentYtelse? = null,
    prosent: BigDecimal = BigDecimal(100),
    endretUtbetalingAndeler: List<EndretUtbetalingAndel> = emptyList(),
    differanseberegnetPeriodebeløp: Int? = null,
    sats: Int = beløp,
): AndelTilkjentYtelseMedEndreteUtbetalinger {
    val aty =
        AndelTilkjentYtelse(
            aktør = aktør,
            behandlingId = behandling.id,
            tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
            kalkulertUtbetalingsbeløp = beløp,
            nasjonaltPeriodebeløp = beløp,
            stønadFom = fom,
            stønadTom = tom,
            type = ytelseType,
            periodeOffset = periodeIdOffset,
            forrigePeriodeOffset = forrigeperiodeIdOffset,
            sats = sats,
            prosent = prosent,
            differanseberegnetPeriodebeløp = differanseberegnetPeriodebeløp,
        )

    return AndelTilkjentYtelseMedEndreteUtbetalinger(aty, endretUtbetalingAndeler)
}

fun lagAndelTilkjentYtelseUtvidet(
    fom: String,
    tom: String,
    ytelseType: YtelseType,
    beløp: Int = sats(ytelseType),
    behandling: Behandling = lagBehandling(),
    person: Person = tilfeldigSøker(),
    periodeIdOffset: Long? = null,
    forrigeperiodeIdOffset: Long? = null,
    tilkjentYtelse: TilkjentYtelse? = null,
): AndelTilkjentYtelse =
    AndelTilkjentYtelse(
        aktør = person.aktør,
        behandlingId = behandling.id,
        tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
        kalkulertUtbetalingsbeløp = beløp,
        nasjonaltPeriodebeløp = beløp,
        stønadFom = årMnd(fom),
        stønadTom = årMnd(tom),
        type = ytelseType,
        periodeOffset = periodeIdOffset,
        forrigePeriodeOffset = forrigeperiodeIdOffset,
        sats = beløp,
        prosent = BigDecimal(100),
    )

fun lagTilkjentYtelse(
    behandling: Behandling = lagBehandling(),
    stønadFom: YearMonth? = YearMonth.now(),
    stønadTom: YearMonth? = YearMonth.now(),
    opphørFom: YearMonth? = YearMonth.now(),
    opprettetDato: LocalDate = LocalDate.now(),
    endretDato: LocalDate = LocalDate.now(),
    utbetalingsoppdrag: String? = null,
    lagAndelerTilkjentYtelse: (tilkjentYtelse: TilkjentYtelse) -> Set<AndelTilkjentYtelse> = {
        emptySet()
    },
): TilkjentYtelse {
    val andelerTilkjentYtelse = mutableSetOf<AndelTilkjentYtelse>()
    val tilkjentYtelse =
        TilkjentYtelse(
            behandling = behandling,
            stønadFom = stønadFom,
            stønadTom = stønadTom,
            opphørFom = opphørFom,
            opprettetDato = opprettetDato,
            endretDato = endretDato,
            utbetalingsoppdrag = utbetalingsoppdrag,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
        )
    tilkjentYtelse.andelerTilkjentYtelse.addAll(lagAndelerTilkjentYtelse(tilkjentYtelse))
    return tilkjentYtelse
}

fun lagInitiellTilkjentYtelse(
    behandling: Behandling = lagBehandling(),
    utbetalingsoppdrag: String? = null,
): TilkjentYtelse =
    TilkjentYtelse(
        behandling = behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now(),
        utbetalingsoppdrag = utbetalingsoppdrag,
    )

fun lagTestPersonopplysningGrunnlag(
    behandlingId: Long,
    vararg personer: Person,
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)

    personopplysningGrunnlag.personer.addAll(
        personer.map { it.copy(personopplysningGrunnlag = personopplysningGrunnlag) },
    )
    return personopplysningGrunnlag
}

fun lagTestPersonopplysningGrunnlag(
    behandlingId: Long,
    søkerPersonIdent: String,
    barnasIdenter: List<String>,
    barnasFødselsdatoer: List<LocalDate> = barnasIdenter.map { LocalDate.of(2019, 1, 1) },
    søkerFødselsdato: LocalDate = LocalDate.of(1987, 1, 1),
    søkerAktør: Aktør =
        tilAktør(søkerPersonIdent).also {
            it.personidenter.add(
                Personident(
                    fødselsnummer = søkerPersonIdent,
                    aktør = it,
                    aktiv = søkerPersonIdent == it.personidenter.first().fødselsnummer,
                ),
            )
        },
    barnAktør: List<Aktør> =
        barnasIdenter.map { fødselsnummer ->
            tilAktør(fødselsnummer).also {
                it.personidenter.add(
                    Personident(
                        fødselsnummer = fødselsnummer,
                        aktør = it,
                        aktiv = fødselsnummer == it.personidenter.first().fødselsnummer,
                    ),
                )
            }
        },
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)
    val bostedsadresse =
        GrMatrikkeladresse(
            matrikkelId = null,
            bruksenhetsnummer = "H301",
            tilleggsnavn = "navn",
            postnummer = "0202",
            kommunenummer = "2231",
        )

    val søker =
        Person(
            aktør = søkerAktør,
            type = PersonType.SØKER,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = søkerFødselsdato,
            navn = "",
            kjønn = Kjønn.KVINNE,
        ).also { søker ->
            søker.statsborgerskap =
                mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = søker))
            søker.bostedsadresser = mutableListOf(bostedsadresse.apply { person = søker })
            søker.sivilstander =
                mutableListOf(
                    GrSivilstand(
                        type = SIVILSTANDTYPE.GIFT,
                        person = søker,
                    ),
                )
        }
    personopplysningGrunnlag.personer.add(søker)

    barnAktør.mapIndexed { index, aktør ->
        personopplysningGrunnlag.personer.add(
            Person(
                aktør = aktør,
                type = PersonType.BARN,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = barnasFødselsdatoer.get(index),
                navn = "",
                kjønn = Kjønn.MANN,
            ).also { barn ->
                barn.statsborgerskap =
                    mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = barn))
                barn.bostedsadresser = mutableListOf(bostedsadresse.apply { person = barn })
                barn.sivilstander =
                    mutableListOf(
                        GrSivilstand(
                            type = SIVILSTANDTYPE.UGIFT,
                            person = barn,
                        ),
                    )
            },
        )
    }
    return personopplysningGrunnlag
}

fun PersonopplysningGrunnlag.tilPersonEnkelSøkerOgBarn() = this.søkerOgBarn.map { it.tilPersonEnkel() }

fun dato(s: String) = LocalDate.parse(s)

fun årMnd(s: String) = YearMonth.parse(s)

fun nyOrdinærBehandling(
    søkersIdent: String,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    fagsakId: Long,
): NyBehandling =
    NyBehandling(
        søkersIdent = søkersIdent,
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR,
        behandlingÅrsak = årsak,
        søknadMottattDato = if (årsak == BehandlingÅrsak.SØKNAD) LocalDate.now() else null,
        fagsakId = fagsakId,
    )

fun nyRevurdering(
    søkersIdent: String,
    fagsakId: Long,
): NyBehandling =
    NyBehandling(
        søkersIdent = søkersIdent,
        behandlingType = BehandlingType.REVURDERING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR,
        søknadMottattDato = LocalDate.now(),
        fagsakId = fagsakId,
    )

fun lagSøknadDTO(
    søkerIdent: String,
    barnasIdenter: List<String>,
    underkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
): SøknadDTO =
    SøknadDTO(
        underkategori = underkategori.tilDto(),
        søkerMedOpplysninger =
            SøkerMedOpplysninger(
                ident = søkerIdent,
            ),
        barnaMedOpplysninger =
            barnasIdenter.map {
                BarnMedOpplysninger(
                    ident = it,
                )
            },
        endringAvOpplysningerBegrunnelse = "",
    )

fun lagPersonResultaterForSøkerOgToBarn(
    vilkårsvurdering: Vilkårsvurdering,
    søkerAktør: Aktør,
    barn1Aktør: Aktør,
    barn2Aktør: Aktør,
    stønadFom: LocalDate,
    stønadTom: LocalDate,
    erDeltBosted: Boolean = false,
): Set<PersonResultat> =
    setOf(
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person = lagPerson(type = PersonType.SØKER, aktør = søkerAktør),
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
        ),
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person =
                lagPerson(
                    type = PersonType.BARN,
                    aktør = barn1Aktør,
                    fødselsdato = stønadFom,
                ),
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = erDeltBosted,
        ),
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person = lagPerson(type = PersonType.BARN, aktør = barn2Aktør, fødselsdato = stønadFom),
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = erDeltBosted,
        ),
    )

fun lagPersonResultat(
    vilkårsvurdering: Vilkårsvurdering,
    person: Person,
    resultat: Resultat,
    periodeFom: LocalDate?,
    periodeTom: LocalDate?,
    lagFullstendigVilkårResultat: Boolean = false,
    personType: PersonType = PersonType.BARN,
    vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
    erDeltBosted: Boolean = false,
    erDeltBostedSkalIkkeDeles: Boolean = false,
    erEksplisittAvslagPåSøknad: Boolean? = null,
): PersonResultat {
    val personResultat =
        PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = person.aktør,
        )

    if (lagFullstendigVilkårResultat) {
        personResultat.setSortedVilkårResultater(
            Vilkår
                .hentVilkårFor(
                    personType = personType,
                    fagsakType = FagsakType.NORMAL,
                    behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
                ).map {
                    VilkårResultat(
                        personResultat = personResultat,
                        periodeFom = if (it.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else periodeFom,
                        periodeTom = periodeTom,
                        vilkårType = it,
                        resultat = resultat,
                        begrunnelse = "",
                        sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                        utdypendeVilkårsvurderinger =
                            listOfNotNull(
                                when {
                                    erDeltBosted && it == Vilkår.BOR_MED_SØKER -> UtdypendeVilkårsvurdering.DELT_BOSTED
                                    erDeltBostedSkalIkkeDeles && it == Vilkår.BOR_MED_SØKER -> UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES
                                    else -> null
                                },
                            ),
                        erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
                    )
                }.toSet(),
        )
    } else {
        personResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = periodeFom,
                    periodeTom = periodeTom,
                    vilkårType = vilkårType,
                    resultat = resultat,
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                    erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
                ),
            ),
        )
    }
    return personResultat
}

fun vurderVilkårsvurderingTilInnvilget(
    vilkårsvurdering: Vilkårsvurdering,
    barn: Person,
    innvilgetFom: LocalDate? = null,
) {
    vilkårsvurdering.personResultater.filter { it.aktør == barn.aktør }.forEach { personResultat ->
        personResultat.vilkårResultater.forEach {
            if (it.vilkårType == Vilkår.UNDER_18_ÅR) {
                it.resultat = Resultat.OPPFYLT
                it.periodeFom = barn.fødselsdato
                it.periodeTom = barn.fødselsdato.plusYears(18)
            } else {
                it.resultat = Resultat.OPPFYLT
                it.periodeFom = innvilgetFom ?: LocalDate.now()
            }
        }
    }
}

fun lagVilkårsvurdering(
    id: Long = 0L,
    behandling: Behandling = lagBehandling(),
    aktiv: Boolean = true,
    lagPersonResultater: (vilkårsvurdering: Vilkårsvurdering) -> Set<PersonResultat> = {
        setOf(
            lagPersonResultat(
                vilkårsvurdering = it,
                aktør = behandling.fagsak.aktør,
            ),
        )
    },
): Vilkårsvurdering {
    val vilkårsvurdering =
        Vilkårsvurdering(
            id = id,
            behandling = behandling,
            aktiv = aktiv,
        )
    vilkårsvurdering.personResultater = lagPersonResultater(vilkårsvurdering)
    return vilkårsvurdering
}

fun lagPersonResultat(
    id: Long = 0L,
    vilkårsvurdering: Vilkårsvurdering,
    aktør: Aktør = randomAktør(),
    lagVilkårResultater: (personResultat: PersonResultat) -> Set<VilkårResultat> = {
        setOf(
            lagVilkårResultat(
                behandlingId = vilkårsvurdering.behandling.id,
                personResultat = it,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(1),
                periodeTom = LocalDate.now().plusYears(2),
                begrunnelse = "",
            ),
            lagVilkårResultat(
                behandlingId = vilkårsvurdering.behandling.id,
                personResultat = it,
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(1),
                periodeTom = LocalDate.now().plusYears(2),
                begrunnelse = "",
            ),
        )
    },
    lagAnnenVurderinger: (personResultat: PersonResultat) -> Set<AnnenVurdering> = {
        setOf(
            lagAnnenVurdering(
                personResultat = it,
            ),
        )
    },
): PersonResultat {
    val personResultat =
        PersonResultat(
            id = id,
            vilkårsvurdering = vilkårsvurdering,
            aktør = aktør,
        )
    personResultat.setSortedVilkårResultater(lagVilkårResultater(personResultat))
    personResultat.andreVurderinger.addAll(lagAnnenVurderinger(personResultat))
    return personResultat
}

fun lagAnnenVurdering(
    id: Long = 0L,
    personResultat: PersonResultat,
    resultat: Resultat = Resultat.OPPFYLT,
    type: AnnenVurderingType = AnnenVurderingType.OPPLYSNINGSPLIKT,
    begrunnelse: String? = null,
): AnnenVurdering =
    AnnenVurdering(
        id = id,
        personResultat = personResultat,
        resultat = resultat,
        type = type,
        begrunnelse = begrunnelse,
    )

fun lagRefusjonEøs(
    behandlingId: Long = 0L,
    fom: LocalDate = LocalDate.now().minusMonths(1),
    tom: LocalDate = LocalDate.now().plusMonths(1),
    refusjonsbeløp: Int = 0,
    land: String = "NO",
    refusjonAvklart: Boolean = true,
    id: Long = 0L,
): RefusjonEøs =
    RefusjonEøs(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        refusjonsbeløp = refusjonsbeløp,
        land = land,
        refusjonAvklart = refusjonAvklart,
        id = id,
    )

fun lagVilkårsvurdering(
    søkerAktør: Aktør,
    behandling: Behandling,
    resultat: Resultat,
    søkerPeriodeFom: LocalDate? = LocalDate.now().minusMonths(1),
    søkerPeriodeTom: LocalDate? = LocalDate.now().plusYears(2),
    medAndreVurderinger: Boolean = true,
): Vilkårsvurdering {
    val vilkårsvurdering =
        Vilkårsvurdering(
            behandling = behandling,
        )
    val personResultat =
        PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerAktør,
        )
    personResultat.setSortedVilkårResultater(
        setOf(
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
                sistEndretIBehandlingId = behandling.id,
            ),
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
                sistEndretIBehandlingId = behandling.id,
            ),
        ),
    )
    if (medAndreVurderinger) {
        personResultat.andreVurderinger.add(
            AnnenVurdering(
                personResultat = personResultat,
                resultat = resultat,
                type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                begrunnelse = null,
            ),
        )
    }

    vilkårsvurdering.personResultater = setOf(personResultat)
    return vilkårsvurdering
}

/**
 * Dette er en funksjon for å få en førstegangsbehandling til en ønsket tilstand ved test.
 * Man sender inn steg man ønsker å komme til (tilSteg), personer på behandlingen (søkerFnr og barnasIdenter),
 * og serviceinstanser som brukes i testen.
 */
fun kjørStegprosessForFGB(
    tilSteg: StegType,
    barnasIdenter: List<String>,
    søkerFnr: String = randomFnr(),
    fagsakService: FagsakService,
    vedtakService: VedtakService,
    persongrunnlagService: PersongrunnlagService,
    vilkårsvurderingService: VilkårsvurderingService,
    stegService: StegService,
    vedtaksperiodeService: VedtaksperiodeService,
    behandlingUnderkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
    institusjon: RestInstitusjon? = null,
    brevmalService: BrevmalService,
    behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    vilkårInnvilgetFom: LocalDate? = null,
): Behandling {
    val fagsakType = utledFagsaktype(institusjon)
    val fagsak =
        fagsakService.hentEllerOpprettFagsakForPersonIdent(
            fødselsnummer = søkerFnr,
            institusjon = institusjon,
            fagsakType = fagsakType,
        )
    val behandling =
        stegService.håndterNyBehandling(
            NyBehandling(
                kategori = behandlingKategori,
                underkategori = behandlingUnderkategori,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                søknadMottattDato = LocalDate.now(),
                fagsakId = fagsak.id,
            ),
        )

    val behandlingEtterPersongrunnlagSteg =
        stegService.håndterSøknad(
            behandling = behandling,
            restRegistrerSøknad =
                RestRegistrerSøknad(
                    søknad =
                        lagSøknadDTO(
                            søkerIdent = søkerFnr,
                            barnasIdenter = barnasIdenter,
                            underkategori = behandlingUnderkategori,
                        ),
                    bekreftEndringerViaFrontend = true,
                ),
        )

    if (tilSteg == StegType.REGISTRERE_PERSONGRUNNLAG || tilSteg == StegType.REGISTRERE_SØKNAD) {
        return behandlingEtterPersongrunnlagSteg
    }

    val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
    persongrunnlagService.hentAktivThrows(behandlingId = behandling.id).personer.forEach { barn ->
        vurderVilkårsvurderingTilInnvilget(vilkårsvurdering, barn, vilkårInnvilgetFom)
    }
    vilkårsvurderingService.oppdater(vilkårsvurdering)

    val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg)

    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg

    val behandlingEtterBehandlingsresultat = stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)

    if (tilSteg == StegType.BEHANDLINGSRESULTAT) return behandlingEtterBehandlingsresultat

    val behandlingEtterVurderTilbakekrevingSteg =
        stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultat,
            RestTilbakekreving(
                valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                begrunnelse = "Begrunnelse",
            ),
        )

    leggTilBegrunnelsePåVedtaksperiodeIBehandling(
        behandling = behandlingEtterVurderTilbakekrevingSteg,
        vedtakService = vedtakService,
        vedtaksperiodeService = vedtaksperiodeService,
    )

    if (tilSteg == StegType.VURDER_TILBAKEKREVING) return behandlingEtterVurderTilbakekrevingSteg

    val behandlingEtterSendTilBeslutter =
        stegService.håndterSendTilBeslutter(behandlingEtterVurderTilbakekrevingSteg, "1234")
    if (tilSteg == StegType.SEND_TIL_BESLUTTER) return behandlingEtterSendTilBeslutter

    val behandlingEtterBeslutteVedtak =
        stegService.håndterBeslutningForVedtak(
            behandlingEtterSendTilBeslutter,
            RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT),
        )
    if (tilSteg == StegType.BESLUTTE_VEDTAK) return behandlingEtterBeslutteVedtak

    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
    val behandlingEtterIverksetteVedtak =
        stegService.håndterIverksettMotØkonomi(
            behandlingEtterBeslutteVedtak,
            IverksettingTaskDTO(
                behandlingsId = behandlingEtterBeslutteVedtak.id,
                vedtaksId = vedtak!!.id,
                saksbehandlerId = "System",
                personIdent = behandlingEtterBeslutteVedtak.fagsak.aktør.aktivFødselsnummer(),
            ),
        )
    if (tilSteg == StegType.IVERKSETT_MOT_OPPDRAG) return behandlingEtterIverksetteVedtak

    val behandlingEtterStatusFraOppdrag =
        stegService.håndterStatusFraØkonomi(
            behandlingEtterIverksetteVedtak,
            StatusFraOppdragMedTask(
                statusFraOppdragDTO =
                    StatusFraOppdragDTO(
                        fagsystem = FAGSYSTEM,
                        personIdent = søkerFnr,
                        aktørId = behandlingEtterIverksetteVedtak.fagsak.aktør.aktivFødselsnummer(),
                        behandlingsId = behandlingEtterIverksetteVedtak.id,
                        vedtaksId = vedtak.id,
                    ),
                task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = ""),
            ),
        )
    if (tilSteg == StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) return behandlingEtterStatusFraOppdrag

    val behandlingEtterIverksetteMotTilbake =
        stegService.håndterIverksettMotFamilieTilbake(behandlingEtterStatusFraOppdrag, Properties())
    if (tilSteg == StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) return behandlingEtterIverksetteMotTilbake

    val behandlingEtterJournalførtVedtak =
        stegService.håndterJournalførVedtaksbrev(
            behandlingEtterIverksetteMotTilbake,
            JournalførVedtaksbrevDTO(
                vedtakId = vedtak.id,
                task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = ""),
            ),
        )
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak

    val behandlingEtterDistribuertVedtak =
        stegService.håndterDistribuerVedtaksbrev(
            behandlingEtterJournalførtVedtak,
            DistribuerDokumentDTO(
                behandlingId = behandlingEtterJournalførtVedtak.id,
                journalpostId = "1234",
                fagsakId = behandlingEtterJournalførtVedtak.fagsak.id,
                brevmal =
                    brevmalService.hentBrevmal(
                        behandlingEtterJournalførtVedtak,
                    ),
                erManueltSendt = false,
            ),
        )
    if (tilSteg == StegType.DISTRIBUER_VEDTAKSBREV) return behandlingEtterDistribuertVedtak

    return stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
}

private fun utledFagsaktype(
    institusjon: RestInstitusjon?,
): FagsakType =
    if (institusjon != null) {
        FagsakType.INSTITUSJON
    } else {
        FagsakType.NORMAL
    }

/**
 * Dette er en funksjon for å få en førstegangsbehandling til en ønsket tilstand ved test.
 * Man sender inn steg man ønsker å komme til (tilSteg), personer på behandlingen (søkerFnr og barnasIdenter),
 * og serviceinstanser som brukes i testen.
 */
fun kjørStegprosessForRevurderingÅrligKontroll(
    tilSteg: StegType,
    søkerFnr: String,
    barnasIdenter: List<String>,
    vedtakService: VedtakService,
    stegService: StegService,
    fagsakId: Long,
    brevmalService: BrevmalService,
    vedtaksperiodeService: VedtaksperiodeService,
): Behandling {
    val behandling =
        stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
                søkersIdent = søkerFnr,
                barnasIdenter = barnasIdenter,
                fagsakId = fagsakId,
            ),
        )

    val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandling)

    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg

    val behandlingEtterBehandlingsresultat = stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)

    if (tilSteg == StegType.BEHANDLINGSRESULTAT) return behandlingEtterBehandlingsresultat

    val behandlingEtterSimuleringSteg =
        stegService.håndterVurderTilbakekreving(
            behandlingEtterBehandlingsresultat,
            if (behandlingEtterBehandlingsresultat.resultat != Behandlingsresultat.FORTSATT_INNVILGET) {
                RestTilbakekreving(
                    valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                    begrunnelse = "Begrunnelse",
                )
            } else {
                null
            },
        )
    if (tilSteg == StegType.VURDER_TILBAKEKREVING) return behandlingEtterSimuleringSteg

    leggTilBegrunnelsePåVedtaksperiodeIBehandling(
        behandling = behandlingEtterSimuleringSteg,
        vedtakService = vedtakService,
        vedtaksperiodeService = vedtaksperiodeService,
    )

    val behandlingEtterSendTilBeslutter = stegService.håndterSendTilBeslutter(behandlingEtterSimuleringSteg, "1234")
    if (tilSteg == StegType.SEND_TIL_BESLUTTER) return behandlingEtterSendTilBeslutter

    val behandlingEtterBeslutteVedtak =
        stegService.håndterBeslutningForVedtak(
            behandlingEtterSendTilBeslutter,
            RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT),
        )
    if (tilSteg == StegType.BESLUTTE_VEDTAK) return behandlingEtterBeslutteVedtak

    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
    val behandlingEtterIverksetteVedtak =
        stegService.håndterIverksettMotØkonomi(
            behandlingEtterBeslutteVedtak,
            IverksettingTaskDTO(
                behandlingsId = behandlingEtterBeslutteVedtak.id,
                vedtaksId = vedtak!!.id,
                saksbehandlerId = "System",
                personIdent = behandlingEtterBeslutteVedtak.fagsak.aktør.aktivFødselsnummer(),
            ),
        )
    if (tilSteg == StegType.IVERKSETT_MOT_OPPDRAG) return behandlingEtterIverksetteVedtak

    val behandlingEtterStatusFraOppdrag =
        stegService.håndterStatusFraØkonomi(
            behandlingEtterIverksetteVedtak,
            StatusFraOppdragMedTask(
                statusFraOppdragDTO =
                    StatusFraOppdragDTO(
                        fagsystem = FAGSYSTEM,
                        personIdent = søkerFnr,
                        aktørId = behandlingEtterIverksetteVedtak.fagsak.aktør.aktørId,
                        behandlingsId = behandlingEtterIverksetteVedtak.id,
                        vedtaksId = vedtak.id,
                    ),
                task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = ""),
            ),
        )
    if (tilSteg == StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) return behandlingEtterStatusFraOppdrag

    val behandlingEtterIverksetteMotTilbake =
        stegService.håndterIverksettMotFamilieTilbake(behandlingEtterStatusFraOppdrag, Properties())
    if (tilSteg == StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) return behandlingEtterIverksetteMotTilbake

    val behandlingEtterJournalførtVedtak =
        stegService.håndterJournalførVedtaksbrev(
            behandlingEtterIverksetteMotTilbake,
            JournalførVedtaksbrevDTO(
                vedtakId = vedtak.id,
                task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = ""),
            ),
        )
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak

    val behandlingEtterDistribuertVedtak =
        stegService.håndterDistribuerVedtaksbrev(
            behandlingEtterJournalførtVedtak,
            DistribuerDokumentDTO(
                behandlingId = behandling.id,
                journalpostId = "1234",
                fagsakId = behandling.fagsak.id,
                brevmal = brevmalService.hentBrevmal(behandling),
                erManueltSendt = false,
            ),
        )
    if (tilSteg == StegType.DISTRIBUER_VEDTAKSBREV) return behandlingEtterDistribuertVedtak

    return stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
}

fun opprettRestTilbakekreving(): RestTilbakekreving =
    RestTilbakekreving(
        valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
        varsel = "Varsel",
        begrunnelse = "Begrunnelse",
    )

fun lagUtbetalingsperiode(
    periodeFom: LocalDate = LocalDate.now().withDayOfMonth(1),
    periodeTom: LocalDate = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
    utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
    ytelseTyper: List<YtelseType> = listOf(YtelseType.ORDINÆR_BARNETRYGD),
    antallBarn: Int = 1,
    utbetaltPerMnd: Int = sats(YtelseType.ORDINÆR_BARNETRYGD),
) = Utbetalingsperiode(
    periodeFom,
    periodeTom,
    vedtaksperiodetype,
    utbetalingsperiodeDetaljer,
    ytelseTyper,
    antallBarn,
    utbetaltPerMnd,
)

fun lagUtbetalingsperiodeDetalj(
    person: RestPerson = tilfeldigSøker().tilRestPerson(),
    ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    utbetaltPerMnd: Int = sats(YtelseType.ORDINÆR_BARNETRYGD),
    prosent: BigDecimal = BigDecimal.valueOf(100),
) = UtbetalingsperiodeDetalj(
    person = person,
    ytelseType = ytelseType,
    utbetaltPerMnd = utbetaltPerMnd,
    erPåvirketAvEndring = false,
    endringsårsak = null,
    prosent = prosent,
)

fun lagVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak = lagVedtak(),
    fom: LocalDate? = LocalDate.now().withDayOfMonth(1),
    tom: LocalDate? = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    type: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
    begrunnelser: MutableSet<Vedtaksbegrunnelse> = mutableSetOf(lagVedtaksbegrunnelse()),
    eøsBegrunnelser: MutableSet<EØSBegrunnelse> = mutableSetOf(),
    fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf(),
) = VedtaksperiodeMedBegrunnelser(
    vedtak = vedtak,
    fom = fom,
    tom = tom,
    type = type,
    begrunnelser = begrunnelser,
    fritekster = fritekster,
    eøsBegrunnelser = eøsBegrunnelser,
)

fun lagUtvidetVedtaksperiodeMedBegrunnelser(
    id: Long = nesteUtvidetVedtaksperiodeId(),
    fom: LocalDate? = LocalDate.now().withDayOfMonth(1),
    tom: LocalDate? = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    type: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
    begrunnelser: List<Vedtaksbegrunnelse> = listOf(lagVedtaksbegrunnelse()),
    fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf(),
    utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
    eøsBegrunnelser: List<EØSBegrunnelse> = emptyList(),
) = UtvidetVedtaksperiodeMedBegrunnelser(
    id = id,
    fom = fom,
    tom = tom,
    type = type,
    begrunnelser = begrunnelser,
    fritekster = fritekster.map { it.fritekst },
    utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer,
    eøsBegrunnelser = eøsBegrunnelser,
)

fun leggTilBegrunnelsePåVedtaksperiodeIBehandling(
    behandling: Behandling,
    vedtakService: VedtakService,
    vedtaksperiodeService: VedtaksperiodeService,
) {
    val aktivtVedtak = vedtakService.hentAktivForBehandling(behandling.id)!!

    val perisisterteVedtaksperioder =
        vedtaksperiodeService.hentPersisterteVedtaksperioder(aktivtVedtak)

    if (behandling.resultat != Behandlingsresultat.FORTSATT_INNVILGET) {
        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = perisisterteVedtaksperioder.first { it.type == Vedtaksperiodetype.UTBETALING }.id,
            standardbegrunnelserFraFrontend =
                listOf(
                    Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                ),
            eøsStandardbegrunnelserFraFrontend = emptyList(),
        )
    } else {
        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = perisisterteVedtaksperioder.first().id,
            standardbegrunnelserFraFrontend =
                listOf(
                    Standardbegrunnelse.FORTSATT_INNVILGET_BARN_BOSATT_I_RIKET,
                ),
            eøsStandardbegrunnelserFraFrontend = emptyList(),
        )
    }
}

fun lagVilkårResultat(
    vilkår: Vilkår,
    vilkårRegelverk: Regelverk? = null,
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    behandlingId: Long = 0,
) = VilkårResultat(
    personResultat = null,
    vilkårType = vilkår,
    resultat = Resultat.OPPFYLT,
    periodeFom = fom?.toLocalDate(),
    periodeTom = tom?.toLocalDate(),
    begrunnelse = "",
    sistEndretIBehandlingId = behandlingId,
    vurderesEtter = vilkårRegelverk,
)

fun lagVilkårResultat(
    id: Long = 0L,
    personResultat: PersonResultat? = null,
    vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
    resultat: Resultat = Resultat.OPPFYLT,
    periodeFom: LocalDate? = LocalDate.of(2009, 12, 24),
    periodeTom: LocalDate? = LocalDate.of(2010, 1, 31),
    begrunnelse: String = "",
    behandlingId: Long = lagBehandling().id,
    utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
    erEksplisittAvslagPåSøknad: Boolean = false,
    standardbegrunnelser: List<IVedtakBegrunnelse> = emptyList(),
    vurderesEtter: Regelverk? = null,
) = VilkårResultat(
    id = id,
    personResultat = personResultat,
    vilkårType = vilkårType,
    resultat = resultat,
    periodeFom = periodeFom,
    periodeTom = periodeTom,
    begrunnelse = begrunnelse,
    sistEndretIBehandlingId = behandlingId,
    utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
    erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
    standardbegrunnelser = standardbegrunnelser,
    vurderesEtter = vurderesEtter,
)

val guttenBarnesenFødselsdato = LocalDate.now().withDayOfMonth(10).minusYears(6)

fun lagEndretUtbetalingAndel(
    behandlingId: Long,
    barn: Person,
    fom: YearMonth,
    tom: YearMonth,
    prosent: Int,
) = lagEndretUtbetalingAndel(
    behandlingId = behandlingId,
    person = barn,
    fom = fom,
    tom = tom,
    prosent = BigDecimal(prosent),
)

fun lagEndretUtbetalingAndel(
    id: Long = 0,
    behandlingId: Long = 0,
    person: Person,
    prosent: BigDecimal = BigDecimal.valueOf(100),
    fom: YearMonth = YearMonth.now().minusMonths(1),
    tom: YearMonth? = YearMonth.now(),
    årsak: Årsak = Årsak.DELT_BOSTED,
    avtaletidspunktDeltBosted: LocalDate = LocalDate.now().minusMonths(1),
    søknadstidspunkt: LocalDate = LocalDate.now().minusMonths(1),
) = EndretUtbetalingAndel(
    id = id,
    behandlingId = behandlingId,
    person = person,
    prosent = prosent,
    fom = fom,
    tom = tom,
    årsak = årsak,
    avtaletidspunktDeltBosted = avtaletidspunktDeltBosted,
    søknadstidspunkt = søknadstidspunkt,
    begrunnelse = "Test",
)

fun lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
    behandlingId: Long,
    barn: Person,
    fom: YearMonth,
    tom: YearMonth,
    prosent: Int,
) = lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
    behandlingId = behandlingId,
    person = barn,
    fom = fom,
    tom = tom,
    prosent = BigDecimal(prosent),
)

fun lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
    id: Long = 0,
    behandlingId: Long = 0,
    person: Person,
    prosent: BigDecimal = BigDecimal.valueOf(100),
    fom: YearMonth = YearMonth.now().minusMonths(1),
    tom: YearMonth? = YearMonth.now(),
    årsak: Årsak = Årsak.DELT_BOSTED,
    avtaletidspunktDeltBosted: LocalDate = LocalDate.now().minusMonths(1),
    søknadstidspunkt: LocalDate = LocalDate.now().minusMonths(1),
    andelTilkjentYtelser: MutableList<AndelTilkjentYtelse> = mutableListOf(),
): EndretUtbetalingAndelMedAndelerTilkjentYtelse {
    val eua =
        EndretUtbetalingAndel(
            id = id,
            behandlingId = behandlingId,
            person = person,
            prosent = prosent,
            fom = fom,
            tom = tom,
            årsak = årsak,
            avtaletidspunktDeltBosted = avtaletidspunktDeltBosted,
            søknadstidspunkt = søknadstidspunkt,
            begrunnelse = "Test",
        )

    return EndretUtbetalingAndelMedAndelerTilkjentYtelse(eua, andelTilkjentYtelser)
}

fun lagPerson(
    personIdent: PersonIdent = PersonIdent(randomFnr()),
    aktør: Aktør = tilAktør(personIdent.ident),
    type: PersonType = PersonType.SØKER,
    personopplysningGrunnlag: PersonopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
    fødselsdato: LocalDate = LocalDate.now().minusYears(19),
    kjønn: Kjønn = Kjønn.KVINNE,
    dødsfall: Dødsfall? = null,
    id: Long = 0,
) = Person(
    aktør = aktør,
    type = type,
    personopplysningGrunnlag = personopplysningGrunnlag,
    fødselsdato = fødselsdato,
    navn = type.name,
    kjønn = kjønn,
    dødsfall = dødsfall,
    id = id,
)

fun lagPersonEnkel(
    personType: PersonType,
    aktør: Aktør = randomAktør(),
    dødsfallDato: LocalDate? = null,
    fødselsdato: LocalDate =
        if (personType == PersonType.SØKER) {
            LocalDate.now().minusYears(34)
        } else {
            LocalDate.now().minusYears(4)
        },
    målform: Målform = Målform.NB,
): PersonEnkel =
    PersonEnkel(
        type = personType,
        aktør = aktør,
        dødsfallDato = dødsfallDato,
        fødselsdato = fødselsdato,
        målform = målform,
    )

fun lagRestSanityBegrunnelse(
    apiNavn: String = "",
    navnISystem: String = "",
    vilkaar: List<String>? = emptyList(),
    rolle: List<String>? = emptyList(),
    lovligOppholdTriggere: List<String>? = emptyList(),
    bosattIRiketTriggere: List<String>? = emptyList(),
    giftPartnerskapTriggere: List<String>? = emptyList(),
    borMedSokerTriggere: List<String>? = emptyList(),
    ovrigeTriggere: List<String>? = emptyList(),
    endringsaarsaker: List<String>? = emptyList(),
    hjemler: List<String> = emptyList(),
    hjemlerFolketrygdloven: List<String> = emptyList(),
    endretUtbetalingsperiodeDeltBostedTriggere: String = "",
    endretUtbetalingsperiodeTriggere: List<String>? = emptyList(),
    periodeResultatForPerson: String? = null,
    fagsakType: String? = null,
    regelverk: String? = null,
    brevPeriodeType: String? = null,
    begrunnelseTypeForPerson: String? = null,
    ikkeIBruk: Boolean? = false,
    stotterFritekst: Boolean? = false,
): RestSanityBegrunnelse =
    RestSanityBegrunnelse(
        apiNavn = apiNavn,
        navnISystem = navnISystem,
        vilkaar = vilkaar,
        rolle = rolle,
        lovligOppholdTriggere = lovligOppholdTriggere,
        bosattIRiketTriggere = bosattIRiketTriggere,
        giftPartnerskapTriggere = giftPartnerskapTriggere,
        borMedSokerTriggere = borMedSokerTriggere,
        ovrigeTriggere = ovrigeTriggere,
        endringsaarsaker = endringsaarsaker,
        hjemler = hjemler,
        hjemlerFolketrygdloven = hjemlerFolketrygdloven,
        endretUtbetalingsperiodeDeltBostedUtbetalingTrigger = endretUtbetalingsperiodeDeltBostedTriggere,
        endretUtbetalingsperiodeTriggere = endretUtbetalingsperiodeTriggere,
        periodeResultatForPerson = periodeResultatForPerson,
        fagsakType = fagsakType,
        regelverk = regelverk,
        brevPeriodeType = brevPeriodeType,
        begrunnelseTypeForPerson = begrunnelseTypeForPerson,
        ikkeIBruk = ikkeIBruk,
        stotterFritekst = stotterFritekst,
    )

fun lagSanityBegrunnelse(
    apiNavn: String = "",
    navnISystem: String = "",
    vilkår: Set<Vilkår> = emptySet(),
    rolle: List<VilkårRolle> = emptyList(),
    lovligOppholdTriggere: List<VilkårTrigger> = emptyList(),
    bosattIRiketTriggere: List<VilkårTrigger> = emptyList(),
    giftPartnerskapTriggere: List<VilkårTrigger> = emptyList(),
    borMedSokerTriggere: List<VilkårTrigger> = emptyList(),
    ovrigeTriggere: List<ØvrigTrigger> = emptyList(),
    endringsaarsaker: List<Årsak> = emptyList(),
    hjemler: List<String> = emptyList(),
    hjemlerFolketrygdloven: List<String> = emptyList(),
    endretUtbetalingsperiodeDeltBostedTriggere: EndretUtbetalingsperiodeDeltBostedTriggere? = null,
    endretUtbetalingsperiodeTriggere: List<EndretUtbetalingsperiodeTrigger> = emptyList(),
    resultat: SanityPeriodeResultat? = null,
    fagsakType: FagsakType? = null,
    periodeType: BrevPeriodeType? = null,
    begrunnelseTypeForPerson: VedtakBegrunnelseType? = null,
): SanityBegrunnelse =
    SanityBegrunnelse(
        apiNavn = apiNavn,
        navnISystem = navnISystem,
        vilkår = vilkår,
        rolle = rolle,
        lovligOppholdTriggere = lovligOppholdTriggere,
        bosattIRiketTriggere = bosattIRiketTriggere,
        giftPartnerskapTriggere = giftPartnerskapTriggere,
        borMedSokerTriggere = borMedSokerTriggere,
        øvrigeTriggere = ovrigeTriggere,
        endringsaarsaker = endringsaarsaker,
        hjemler = hjemler,
        hjemlerFolketrygdloven = hjemlerFolketrygdloven,
        endretUtbetalingsperiodeDeltBostedUtbetalingTrigger = endretUtbetalingsperiodeDeltBostedTriggere,
        endretUtbetalingsperiodeTriggere = endretUtbetalingsperiodeTriggere,
        periodeResultat = resultat,
        fagsakType = fagsakType,
        periodeType = periodeType,
        begrunnelseTypeForPerson = begrunnelseTypeForPerson,
    )

fun lagSanityEøsBegrunnelse(
    apiNavn: String = "",
    navnISystem: String = "",
    annenForeldersAktivitet: List<KompetanseAktivitet> = emptyList(),
    barnetsBostedsland: List<BarnetsBostedsland> = emptyList(),
    kompetanseResultat: List<KompetanseResultat> = emptyList(),
    hjemler: List<String> = emptyList(),
    hjemlerFolketrygdloven: List<String> = emptyList(),
    hjemlerEØSForordningen883: List<String> = emptyList(),
    hjemlerEØSForordningen987: List<String> = emptyList(),
    hjemlerSeperasjonsavtalenStorbritannina: List<String> = emptyList(),
    vilkår: List<Vilkår> = emptyList(),
    fagsakType: FagsakType? = null,
    tema: Tema? = null,
    periodeType: BrevPeriodeType? = null,
    valgbarhet: Valgbarhet? = null,
    ovrigeTriggere: List<ØvrigTrigger> = emptyList(),
): SanityEØSBegrunnelse =
    SanityEØSBegrunnelse(
        apiNavn = apiNavn,
        navnISystem = navnISystem,
        annenForeldersAktivitet = annenForeldersAktivitet,
        barnetsBostedsland = barnetsBostedsland,
        kompetanseResultat = kompetanseResultat,
        hjemler = hjemler,
        hjemlerFolketrygdloven = hjemlerFolketrygdloven,
        hjemlerEØSForordningen883 = hjemlerEØSForordningen883,
        hjemlerEØSForordningen987 = hjemlerEØSForordningen987,
        hjemlerSeperasjonsavtalenStorbritannina = hjemlerSeperasjonsavtalenStorbritannina,
        vilkår = vilkår.toSet(),
        fagsakType = fagsakType,
        tema = tema,
        periodeType = periodeType,
        valgbarhet = valgbarhet,
        øvrigeTriggere = ovrigeTriggere,
    )

fun oppfyltVilkår(
    vilkår: Vilkår,
    regelverk: Regelverk? = null,
) = VilkårRegelverkResultat(
    vilkår = vilkår,
    regelverkResultat =
        when (regelverk) {
            Regelverk.NASJONALE_REGLER -> RegelverkResultat.OPPFYLT_NASJONALE_REGLER
            Regelverk.EØS_FORORDNINGEN -> RegelverkResultat.OPPFYLT_EØS_FORORDNINGEN
            else -> RegelverkResultat.OPPFYLT_REGELVERK_IKKE_SATT
        },
)

fun ikkeOppfyltVilkår(vilkår: Vilkår) =
    VilkårRegelverkResultat(
        vilkår = vilkår,
        regelverkResultat = RegelverkResultat.IKKE_OPPFYLT,
    )

fun lagBrevmottakerDb(
    behandlingId: Long,
    type: MottakerType = MottakerType.FULLMEKTIG,
    navn: String = "Test Testesen",
    adresselinje1: String = "En adresse her",
    adresselinje2: String? = null,
    postnummer: String = "0661",
    poststed: String = "Oslo",
    landkode: String = "NO",
) = BrevmottakerDb(
    behandlingId = behandlingId,
    type = type,
    navn = navn,
    adresselinje1 = adresselinje1,
    adresselinje2 = adresselinje2,
    postnummer = postnummer,
    poststed = poststed,
    landkode = landkode,
)

val Number.årSiden: LocalDate get() = LocalDate.now().minusYears(this.toLong())

fun lagEØSBegrunnelse(
    id: Long = 0L,
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(),
    begrunnelse: EØSStandardbegrunnelse,
): EØSBegrunnelse =
    EØSBegrunnelse(
        id = id,
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        begrunnelse = begrunnelse,
    )
