package no.nav.familie.ba.sak.common

import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.dataGenerator.vedtak.lagVedtaksbegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårRolle
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.brev.hentBrevtype
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene.RestVedtaksbegrunnelse
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
import no.nav.familie.ba.sak.task.DistribuerDokumentDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
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

fun randomFnr(): String = fødselsnummerGenerator.foedselsnummer().asString
fun randomPersonident(aktør: Aktør, fnr: String = randomFnr()): Personident =
    Personident(fødselsnummer = fnr, aktør = aktør)

fun randomAktørId(fnr: String = randomFnr()): Aktør =
    Aktør(Random.nextLong(1000_000_000_000, 31_121_299_99999).toString()).also {
        it.personidenter.add(
            randomPersonident(it, fnr)
        )
    }

private var gjeldendeVedtakId: Long = abs(Random.nextLong(10000000))
private var gjeldendeVedtakBegrunnelseId: Long = abs(Random.nextLong(10000000))
private var gjeldendeBehandlingId: Long = abs(Random.nextLong(10000000))
private var gjeldendePersonId: Long = abs(Random.nextLong(10000000))
private var gjeldendeUtvidetVedtaksperiodeId: Long = abs(Random.nextLong(10000000))
private const val ID_INKREMENT = 50

fun nesteVedtakId(): Long {
    gjeldendeVedtakId += ID_INKREMENT
    return gjeldendeVedtakId
}

fun nesteVedtakBegrunnelseId(): Long {
    gjeldendeVedtakBegrunnelseId += ID_INKREMENT
    return gjeldendeVedtakBegrunnelseId
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

fun defaultFagsak(aktør: Aktør = tilAktør(randomFnr())) = Fagsak(
    1, aktør = aktør
)

fun lagBehandling(
    fagsak: Fagsak = defaultFagsak(),
    behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    skalBehandlesAutomatisk: Boolean = false,
    førsteSteg: StegType = FØRSTE_STEG,
    resultat: Behandlingsresultat = Behandlingsresultat.IKKE_VURDERT,
    underkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR
) =
    Behandling(
        id = nesteBehandlingId(),
        fagsak = fagsak,
        skalBehandlesAutomatisk = skalBehandlesAutomatisk,
        type = behandlingType,
        kategori = behandlingKategori,
        underkategori = underkategori,
        opprettetÅrsak = årsak,
        resultat = resultat
    ).also {
        it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, førsteSteg))
    }

fun tilfeldigPerson(
    fødselsdato: LocalDate = LocalDate.now(),
    personType: PersonType = PersonType.BARN,
    kjønn: Kjønn = Kjønn.MANN,
    aktør: Aktør = randomAktørId(),
) =
    Person(
        id = nestePersonId(),
        aktør = aktør,
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = kjønn,
        målform = Målform.NB
    ).apply { sivilstander = mutableListOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }

fun tilfeldigSøker(
    fødselsdato: LocalDate = LocalDate.now(),
    personType: PersonType = PersonType.SØKER,
    kjønn: Kjønn = Kjønn.MANN,
    aktør: Aktør = randomAktørId(),
) =
    Person(
        id = nestePersonId(),
        aktør = aktør,
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = kjønn,
        målform = Målform.NB
    ).apply { sivilstander = mutableListOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }

fun lagVedtak(behandling: Behandling = lagBehandling(), stønadBrevPdF: ByteArray? = null) =
    Vedtak(
        id = nesteVedtakId(),
        behandling = behandling,
        vedtaksdato = LocalDateTime.now(),
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
    endretUtbetalingAndeler: List<EndretUtbetalingAndel> = emptyList()
): AndelTilkjentYtelse {

    return AndelTilkjentYtelse(
        aktør = aktør,
        behandlingId = behandling.id,
        tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
        kalkulertUtbetalingsbeløp = beløp,
        stønadFom = fom,
        stønadTom = tom,
        type = ytelseType,
        periodeOffset = periodeIdOffset,
        forrigePeriodeOffset = forrigeperiodeIdOffset,
        sats = beløp,
        prosent = prosent,
        endretUtbetalingAndeler = endretUtbetalingAndeler.toMutableList()
    )
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
    tilkjentYtelse: TilkjentYtelse? = null
): AndelTilkjentYtelse {

    return AndelTilkjentYtelse(
        aktør = person.aktør,
        behandlingId = behandling.id,
        tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
        kalkulertUtbetalingsbeløp = beløp,
        stønadFom = årMnd(fom),
        stønadTom = årMnd(tom),
        type = ytelseType,
        periodeOffset = periodeIdOffset,
        forrigePeriodeOffset = forrigeperiodeIdOffset,
        sats = beløp,
        prosent = BigDecimal(100)
    )
}

fun lagInitiellTilkjentYtelse(behandling: Behandling = lagBehandling()): TilkjentYtelse {
    return TilkjentYtelse(behandling = behandling, opprettetDato = LocalDate.now(), endretDato = LocalDate.now())
}

fun lagTestPersonopplysningGrunnlag(
    behandlingId: Long,
    vararg personer: Person
): PersonopplysningGrunnlag {

    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)

    personopplysningGrunnlag.personer.addAll(
        personer.map { it.copy(personopplysningGrunnlag = personopplysningGrunnlag) }
    )
    return personopplysningGrunnlag
}

fun lagTestPersonopplysningGrunnlag(
    behandlingId: Long,
    søkerPersonIdent: String,
    barnasIdenter: List<String>,
    barnFødselsdato: LocalDate = LocalDate.of(2019, 1, 1),
    søkerAktør: Aktør = tilAktør(søkerPersonIdent).also {
        it.personidenter.add(
            Personident(
                fødselsnummer = søkerPersonIdent,
                aktør = it,
                aktiv = søkerPersonIdent == it.personidenter.first().fødselsnummer
            )
        )
    },
    barnAktør: List<Aktør> = barnasIdenter.map { fødselsnummer ->
        tilAktør(fødselsnummer).also {
            it.personidenter.add(
                Personident(
                    fødselsnummer = fødselsnummer,
                    aktør = it,
                    aktiv = fødselsnummer == it.personidenter.first().fødselsnummer
                )
            )
        }
    },
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)
    val bostedsadresse = GrMatrikkeladresse(
        matrikkelId = null, bruksenhetsnummer = "H301", tilleggsnavn = "navn",
        postnummer = "0202", kommunenummer = "2231"
    )

    val søker = Person(
        aktør = søkerAktør,
        type = PersonType.SØKER,
        personopplysningGrunnlag = personopplysningGrunnlag,
        fødselsdato = LocalDate.of(2019, 1, 1),
        navn = "",
        kjønn = Kjønn.KVINNE,
    ).also { søker ->
        søker.statsborgerskap =
            mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = søker))
        søker.bostedsadresser = mutableListOf(bostedsadresse.apply { person = søker })
        søker.sivilstander = mutableListOf(
            GrSivilstand(
                type = SIVILSTAND.GIFT,
                person = søker
            )
        )
    }
    personopplysningGrunnlag.personer.add(søker)

    barnAktør.map {
        personopplysningGrunnlag.personer.add(
            Person(
                aktør = it,
                type = PersonType.BARN,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = barnFødselsdato,
                navn = "",
                kjønn = Kjønn.MANN
            ).also { barn ->
                barn.statsborgerskap =
                    mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = barn))
                barn.bostedsadresser = mutableListOf(bostedsadresse.apply { person = barn })
                barn.sivilstander = mutableListOf(
                    GrSivilstand(
                        type = SIVILSTAND.UGIFT,
                        person = barn
                    )
                )
            }
        )
    }
    return personopplysningGrunnlag
}

fun dato(s: String) = LocalDate.parse(s)
fun årMnd(s: String) = YearMonth.parse(s)

fun nyOrdinærBehandling(søkersIdent: String, årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD): NyBehandling =
    NyBehandling(
        søkersIdent = søkersIdent,
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR,
        behandlingÅrsak = årsak,
        søknadMottattDato = if (årsak == BehandlingÅrsak.SØKNAD) LocalDate.now() else null
    )

fun nyRevurdering(søkersIdent: String): NyBehandling = NyBehandling(
    søkersIdent = søkersIdent,
    behandlingType = BehandlingType.REVURDERING,
    kategori = BehandlingKategori.NASJONAL,
    underkategori = BehandlingUnderkategori.ORDINÆR,
    søknadMottattDato = LocalDate.now()
)

fun lagSøknadDTO(
    søkerIdent: String,
    barnasIdenter: List<String>,
    underkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR
): SøknadDTO {
    return SøknadDTO(
        underkategori = underkategori,
        søkerMedOpplysninger = SøkerMedOpplysninger(
            ident = søkerIdent
        ),
        barnaMedOpplysninger = barnasIdenter.map {
            BarnMedOpplysninger(
                ident = it
            )
        },
        endringAvOpplysningerBegrunnelse = ""
    )
}

fun lagPersonResultaterForSøkerOgToBarn(
    vilkårsvurdering: Vilkårsvurdering,
    søkerAktør: Aktør,
    barn1Aktør: Aktør,
    barn2Aktør: Aktør,
    stønadFom: LocalDate,
    stønadTom: LocalDate,
    erDeltBosted: Boolean = false
): Set<PersonResultat> {
    return setOf(
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerAktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER
        ),
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn1Aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = erDeltBosted
        ),
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barn2Aktør,
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = erDeltBosted
        )
    )
}

fun lagPersonResultat(
    vilkårsvurdering: Vilkårsvurdering,
    aktør: Aktør,
    resultat: Resultat,
    periodeFom: LocalDate?,
    periodeTom: LocalDate?,
    lagFullstendigVilkårResultat: Boolean = false,
    personType: PersonType = PersonType.BARN,
    vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
    erDeltBosted: Boolean = false
): PersonResultat {
    val personResultat = PersonResultat(
        vilkårsvurdering = vilkårsvurdering,
        aktør = aktør
    )

    if (lagFullstendigVilkårResultat) {
        personResultat.setSortedVilkårResultater(
            Vilkår.hentVilkårFor(personType).map {
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = periodeFom,
                    periodeTom = periodeTom,
                    vilkårType = it,
                    resultat = resultat,
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id,
                    utdypendeVilkårsvurderinger = listOfNotNull(
                        if (erDeltBosted && it == Vilkår.BOR_MED_SØKER) UtdypendeVilkårsvurdering.DELT_BOSTED else null
                    )
                )
            }.toSet()
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
                    behandlingId = vilkårsvurdering.behandling.id
                )
            )
        )
    }
    return personResultat
}

fun vurderVilkårsvurderingTilInnvilget(vilkårsvurdering: Vilkårsvurdering, barn: Person) {
    vilkårsvurdering.personResultater.filter { it.aktør == barn.aktør }.forEach { personResultat ->
        personResultat.vilkårResultater.forEach {
            if (it.vilkårType == Vilkår.UNDER_18_ÅR) {
                it.resultat = Resultat.OPPFYLT
                it.periodeFom = barn.fødselsdato
                it.periodeTom = barn.fødselsdato.plusYears(18)
            } else {
                it.resultat = Resultat.OPPFYLT
                it.periodeFom = LocalDate.now()
            }
        }
    }
}

fun lagVilkårsvurdering(
    søkerAktør: Aktør,
    behandling: Behandling,
    resultat: Resultat,
    søkerPeriodeFom: LocalDate? = LocalDate.now().minusMonths(1),
    søkerPeriodeTom: LocalDate? = LocalDate.now().plusYears(2)
): Vilkårsvurdering {
    val vilkårsvurdering = Vilkårsvurdering(
        behandling = behandling
    )
    val personResultat = PersonResultat(
        vilkårsvurdering = vilkårsvurdering,
        aktør = søkerAktør
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
                behandlingId = behandling.id
            ),
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
                behandlingId = behandling.id
            )
        )
    )
    personResultat.andreVurderinger.add(
        AnnenVurdering(
            personResultat = personResultat,
            resultat = resultat,
            type = AnnenVurderingType.OPPLYSNINGSPLIKT,
            begrunnelse = null
        )
    )

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
    søkerFnr: String = randomFnr(),
    barnasIdenter: List<String> = listOf(ClientMocks.barnFnr[0]),
    fagsakService: FagsakService,
    vedtakService: VedtakService,
    persongrunnlagService: PersongrunnlagService,
    vilkårsvurderingService: VilkårsvurderingService,
    stegService: StegService,
    vedtaksperiodeService: VedtaksperiodeService,
    behandlingUnderkategori: BehandlingUnderkategori = BehandlingUnderkategori.ORDINÆR
): Behandling {
    fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
    val behandling = stegService.håndterNyBehandling(
        NyBehandling(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = behandlingUnderkategori,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            søkersIdent = søkerFnr,
            barnasIdenter = barnasIdenter,
            søknadMottattDato = LocalDate.now()
        )
    )

    val behandlingEtterPersongrunnlagSteg =
        stegService.håndterSøknad(
            behandling = behandling,
            restRegistrerSøknad = RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = søkerFnr,
                    barnasIdenter = barnasIdenter,
                    underkategori = behandlingUnderkategori
                ),
                bekreftEndringerViaFrontend = true
            )
        )

    if (tilSteg == StegType.REGISTRERE_PERSONGRUNNLAG || tilSteg == StegType.REGISTRERE_SØKNAD)
        return behandlingEtterPersongrunnlagSteg

    val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
    persongrunnlagService.hentAktivThrows(behandlingId = behandling.id).personer.forEach { barn ->
        vurderVilkårsvurderingTilInnvilget(vilkårsvurdering, barn)
    }
    vilkårsvurderingService.oppdater(vilkårsvurdering)

    val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg)

    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg

    val behandlingEtterBehandlingsresultat = stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)

    if (tilSteg == StegType.BEHANDLINGSRESULTAT) return behandlingEtterBehandlingsresultat

    val behandlingEtterVurderTilbakekrevingSteg = stegService.håndterVurderTilbakekreving(
        behandlingEtterBehandlingsresultat,
        RestTilbakekreving(
            valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
            begrunnelse = "Begrunnelse"
        )
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
            RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT)
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
                personIdent = behandlingEtterBeslutteVedtak.fagsak.aktør.aktivFødselsnummer()
            )
        )
    if (tilSteg == StegType.IVERKSETT_MOT_OPPDRAG) return behandlingEtterIverksetteVedtak

    val behandlingEtterStatusFraOppdrag =
        stegService.håndterStatusFraØkonomi(
            behandlingEtterIverksetteVedtak,
            StatusFraOppdragMedTask(
                statusFraOppdragDTO = StatusFraOppdragDTO(
                    fagsystem = FAGSYSTEM,
                    personIdent = søkerFnr,
                    aktørId = behandlingEtterIverksetteVedtak.fagsak.aktør.aktivFødselsnummer(),
                    behandlingsId = behandlingEtterIverksetteVedtak.id,
                    vedtaksId = vedtak.id
                ),
                task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = "")
            )
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
                task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
            )
        )
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak

    val behandlingEtterDistribuertVedtak =
        stegService.håndterDistribuerVedtaksbrev(
            behandlingEtterJournalførtVedtak,
            DistribuerDokumentDTO(
                behandlingId = behandlingEtterJournalførtVedtak.id,
                journalpostId = "1234",
                personIdent = søkerFnr,
                brevmal = hentBrevtype(
                    behandlingEtterJournalførtVedtak
                ),
                erManueltSendt = false
            )
        )
    if (tilSteg == StegType.DISTRIBUER_VEDTAKSBREV) return behandlingEtterDistribuertVedtak

    return stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
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
    stegService: StegService
): Behandling {
    val behandling = stegService.håndterNyBehandling(
        NyBehandling(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
            søkersIdent = søkerFnr,
            barnasIdenter = barnasIdenter,
        )
    )

    val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandling)

    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg

    val behandlingEtterBehandlingsresultat = stegService.håndterBehandlingsresultat(behandlingEtterVilkårsvurderingSteg)

    if (tilSteg == StegType.BEHANDLINGSRESULTAT) return behandlingEtterBehandlingsresultat

    val behandlingEtterSimuleringSteg = stegService.håndterVurderTilbakekreving(
        behandlingEtterBehandlingsresultat,
        if (behandlingEtterBehandlingsresultat.resultat != Behandlingsresultat.FORTSATT_INNVILGET) RestTilbakekreving(
            valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
            begrunnelse = "Begrunnelse"
        ) else null
    )
    if (tilSteg == StegType.VURDER_TILBAKEKREVING) return behandlingEtterSimuleringSteg

    val behandlingEtterSendTilBeslutter = stegService.håndterSendTilBeslutter(behandlingEtterSimuleringSteg, "1234")
    if (tilSteg == StegType.SEND_TIL_BESLUTTER) return behandlingEtterSendTilBeslutter

    val behandlingEtterBeslutteVedtak =
        stegService.håndterBeslutningForVedtak(
            behandlingEtterSendTilBeslutter,
            RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT)
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
            )
        )
    if (tilSteg == StegType.IVERKSETT_MOT_OPPDRAG) return behandlingEtterIverksetteVedtak

    val behandlingEtterStatusFraOppdrag =
        stegService.håndterStatusFraØkonomi(
            behandlingEtterIverksetteVedtak,
            StatusFraOppdragMedTask(
                statusFraOppdragDTO = StatusFraOppdragDTO(
                    fagsystem = FAGSYSTEM,
                    personIdent = søkerFnr,
                    aktørId = behandlingEtterIverksetteVedtak.fagsak.aktør.aktørId,
                    behandlingsId = behandlingEtterIverksetteVedtak.id,
                    vedtaksId = vedtak.id
                ),
                task = Task(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = "")
            )
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
                task = Task(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
            )
        )
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak

    val behandlingEtterDistribuertVedtak =
        stegService.håndterDistribuerVedtaksbrev(
            behandlingEtterJournalførtVedtak,
            DistribuerDokumentDTO(
                behandlingId = behandling.id,
                journalpostId = "1234",
                personIdent = søkerFnr,
                brevmal = hentBrevtype(behandling),
                erManueltSendt = false
            )
        )
    if (tilSteg == StegType.DISTRIBUER_VEDTAKSBREV) return behandlingEtterDistribuertVedtak

    return stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
}

fun opprettRestTilbakekreving(): RestTilbakekreving = RestTilbakekreving(
    valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
    varsel = "Varsel",
    begrunnelse = "Begrunnelse",
)

fun lagUtbetalingsperiode(
    periodeFom: LocalDate = LocalDate.now().withDayOfMonth(1),
    periodeTom: LocalDate = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
    utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj>,
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
    prosent: BigDecimal = BigDecimal.valueOf(100)
) = UtbetalingsperiodeDetalj(person, ytelseType, utbetaltPerMnd, false, prosent)

fun lagVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak = lagVedtak(),
    fom: LocalDate? = LocalDate.now().withDayOfMonth(1),
    tom: LocalDate? = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    type: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
    begrunnelser: MutableSet<Vedtaksbegrunnelse> = mutableSetOf(lagVedtaksbegrunnelse()),
    fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf(),
) = VedtaksperiodeMedBegrunnelser(
    vedtak = vedtak,
    fom = fom,
    tom = tom,
    type = type,
    begrunnelser = begrunnelser,
    fritekster = fritekster,
)

fun lagRestVedtaksbegrunnelse(
    standardbegrunnelse: Standardbegrunnelse =
        Standardbegrunnelse.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
    vedtakBegrunnelseType: VedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET,
) = RestVedtaksbegrunnelse(
    standardbegrunnelse = standardbegrunnelse,
    vedtakBegrunnelseType = vedtakBegrunnelseType,
    vedtakBegrunnelseSpesifikasjon = standardbegrunnelse
)

fun lagUtvidetVedtaksperiodeMedBegrunnelser(
    id: Long = nesteUtvidetVedtaksperiodeId(),
    fom: LocalDate? = LocalDate.now().withDayOfMonth(1),
    tom: LocalDate? = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    type: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
    begrunnelser: List<RestVedtaksbegrunnelse> = listOf(lagRestVedtaksbegrunnelse()),
    fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf(),
    utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
) = UtvidetVedtaksperiodeMedBegrunnelser(
    id = id,
    fom = fom,
    tom = tom,
    type = type,
    begrunnelser = begrunnelser,
    fritekster = fritekster.map { it.fritekst },
    utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer
)

fun leggTilBegrunnelsePåVedtaksperiodeIBehandling(
    behandling: Behandling,
    vedtakService: VedtakService,
    vedtaksperiodeService: VedtaksperiodeService,
) {
    val aktivtVedtak = vedtakService.hentAktivForBehandling(behandling.id)!!

    val perisisterteVedtaksperioder =
        vedtaksperiodeService.hentPersisterteVedtaksperioder(aktivtVedtak)

    vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId = perisisterteVedtaksperioder.first().id,
        standardbegrunnelserFraFrontend = listOf(
            Standardbegrunnelse.INNVILGET_BOSATT_I_RIKTET,
        )
    )
}

fun lagVilkårResultat(
    vilkår: Vilkår,
    vilkårRegelverk: Regelverk? = null,
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    behandlingId: Long = 0
) = VilkårResultat(
    personResultat = null,
    vilkårType = vilkår,
    resultat = Resultat.OPPFYLT,
    periodeFom = fom?.toLocalDate(),
    periodeTom = tom?.toLocalDate(),
    begrunnelse = "",
    behandlingId = behandlingId,
    vurderesEtter = vilkårRegelverk
)

fun lagVilkårResultat(
    personResultat: PersonResultat? = null,
    vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
    resultat: Resultat = Resultat.OPPFYLT,
    periodeFom: LocalDate = LocalDate.of(2009, 12, 24),
    periodeTom: LocalDate? = LocalDate.of(2010, 1, 31),
    begrunnelse: String = "",
    behandlingId: Long = lagBehandling().id,
    utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList()
) = VilkårResultat(
    personResultat = personResultat,
    vilkårType = vilkårType,
    resultat = resultat,
    periodeFom = periodeFom,
    periodeTom = periodeTom,
    begrunnelse = begrunnelse,
    behandlingId = behandlingId,
    utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger
)

val guttenBarnesenFødselsdato = LocalDate.now().withDayOfMonth(10).minusYears(6)

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
    standardbegrunnelser: List<Standardbegrunnelse> = emptyList(),
    andelTilkjentYtelser: MutableList<AndelTilkjentYtelse> = mutableListOf()
) =
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
        standardbegrunnelser = standardbegrunnelser,
        andelTilkjentYtelser = andelTilkjentYtelser
    )

fun lagPerson(
    personIdent: PersonIdent = PersonIdent(randomFnr()),
    aktør: Aktør = tilAktør(personIdent.ident),
    type: PersonType = PersonType.SØKER,
    personopplysningGrunnlag: PersonopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
    fødselsdato: LocalDate = LocalDate.now().minusYears(19),
    kjønn: Kjønn = Kjønn.KVINNE
) = Person(
    aktør = aktør,
    type = type,
    personopplysningGrunnlag = personopplysningGrunnlag,
    fødselsdato = fødselsdato,
    navn = type.name,
    kjønn = kjønn
)

fun lagRestSanityBegrunnelse(
    apiNavn: String? = "",
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
    endretUtbetalingsperiodeDeltBostedTriggere: List<String>? = emptyList(),
    endretUtbetalingsperiodeTriggere: List<String>? = emptyList(),
): RestSanityBegrunnelse = RestSanityBegrunnelse(
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
    endretUtbetalingsperiodeDeltBostedTriggere = endretUtbetalingsperiodeDeltBostedTriggere,
    endretUtbetalingsperiodeTriggere = endretUtbetalingsperiodeTriggere,
)

fun lagSanityBegrunnelse(
    apiNavn: String? = "",
    navnISystem: String = "",
    vilkaar: List<SanityVilkår>? = null,
    rolle: List<VilkårRolle> = emptyList(),
    lovligOppholdTriggere: List<VilkårTrigger>? = null,
    bosattIRiketTriggere: List<VilkårTrigger>? = null,
    giftPartnerskapTriggere: List<VilkårTrigger>? = null,
    borMedSokerTriggere: List<VilkårTrigger>? = null,
    ovrigeTriggere: List<ØvrigTrigger>? = null,
    endringsaarsaker: List<Årsak>? = null,
    hjemler: List<String> = emptyList(),
    endretUtbetalingsperiodeDeltBostedTriggere: List<EndretUtbetalingsperiodeDeltBostedTriggere>? = null,
    endretUtbetalingsperiodeTriggere: List<EndretUtbetalingsperiodeTrigger>? = null,
): SanityBegrunnelse = SanityBegrunnelse(
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
    endretUtbetalingsperiodeDeltBostedTriggere = endretUtbetalingsperiodeDeltBostedTriggere,
    endretUtbetalingsperiodeTriggere = endretUtbetalingsperiodeTriggere,
)

fun lagTriggesAv(
    vilkår: Set<Vilkår> = emptySet(),
    personTyper: Set<PersonType> = setOf(PersonType.BARN, PersonType.SØKER),
    personerManglerOpplysninger: Boolean = false,
    satsendring: Boolean = false,
    barnMedSeksårsdag: Boolean = false,
    vurderingAnnetGrunnlag: Boolean = false,
    medlemskap: Boolean = false,
    deltbosted: Boolean = false,
    valgbar: Boolean = true,
    endringsaarsaker: Set<Årsak> = emptySet(),
    etterEndretUtbetaling: Boolean = false,
    endretUtbetalingSkalUtbetales: Boolean = false,
    småbarnstillegg: Boolean = false
): TriggesAv = TriggesAv(
    vilkår = vilkår,
    personTyper = personTyper,
    personerManglerOpplysninger = personerManglerOpplysninger,
    satsendring = satsendring,
    barnMedSeksårsdag = barnMedSeksårsdag,
    vurderingAnnetGrunnlag = vurderingAnnetGrunnlag,
    medlemskap = medlemskap,
    deltbosted = deltbosted,
    valgbar = valgbar,
    endringsaarsaker = endringsaarsaker,
    etterEndretUtbetaling = etterEndretUtbetaling,
    endretUtbetalingSkalUtbetales = endretUtbetalingSkalUtbetales,
    småbarnstillegg = småbarnstillegg,
)
