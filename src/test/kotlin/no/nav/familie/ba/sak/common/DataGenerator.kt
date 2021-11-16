package no.nav.familie.ba.sak.common

import io.mockk.mockk
import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.dokument.domene.RestSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.hentBrevtype
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPerson
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.kjerne.steg.JournalførVedtaksbrevDTO
import no.nav.familie.ba.sak.kjerne.steg.StatusFraOppdragMedTask
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
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
fun randomAktørId(): AktørId = AktørId(Random.nextLong(1000_000_000_000, 31_121_299_99999).toString())

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

fun defaultFagsak() = Fagsak(1).also {
    it.søkerIdenter =
        setOf(
            FagsakPerson(
                fagsak = it,
                personIdent = PersonIdent(randomFnr()),
                opprettetTidspunkt = LocalDateTime.now()
            )
        )
}

fun lagBehandling(
    fagsak: Fagsak = defaultFagsak(),
    behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    automatiskOpprettelse: Boolean = false,
    førsteSteg: StegType = FØRSTE_STEG
) =
    Behandling(
        id = nesteBehandlingId(),
        fagsak = fagsak,
        skalBehandlesAutomatisk = automatiskOpprettelse,
        type = behandlingType,
        kategori = behandlingKategori,
        underkategori = BehandlingUnderkategori.ORDINÆR,
        opprettetÅrsak = årsak
    ).also {
        it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, førsteSteg))
    }

fun tilfeldigPerson(
    fødselsdato: LocalDate = LocalDate.now(),
    personType: PersonType = PersonType.BARN,
    kjønn: Kjønn = Kjønn.MANN,
    personIdent: PersonIdent = PersonIdent(randomFnr())
) =
    Person(
        id = nestePersonId(),
        aktørId = randomAktørId(),
        personIdent = personIdent,
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = kjønn,
        målform = Målform.NB
    ).apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }

fun tilfeldigSøker(
    fødselsdato: LocalDate = LocalDate.now(),
    personType: PersonType = PersonType.SØKER,
    kjønn: Kjønn = Kjønn.MANN,
    personIdent: PersonIdent = PersonIdent(randomFnr())
) =
    Person(
        id = nestePersonId(),
        aktørId = randomAktørId(),
        personIdent = personIdent,
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = kjønn,
        målform = Målform.NB
    ).apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }

fun lagVedtak(behandling: Behandling = lagBehandling()) =
    Vedtak(
        id = nesteVedtakId(),
        behandling = behandling,
        vedtaksdato = LocalDateTime.now()
    )

fun lagAndelTilkjentYtelse(
    fom: YearMonth,
    tom: YearMonth,
    ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    beløp: Int = sats(ytelseType),
    behandling: Behandling = lagBehandling(),
    person: Person = tilfeldigPerson(),
    periodeIdOffset: Long? = null,
    forrigeperiodeIdOffset: Long? = null,
    tilkjentYtelse: TilkjentYtelse? = null,
    prosent: BigDecimal = BigDecimal(100),
    endretUtbetalingAndeler: List<EndretUtbetalingAndel> = emptyList()
): AndelTilkjentYtelse {

    return AndelTilkjentYtelse(
        personIdent = person.personIdent.ident,
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
        personIdent = person.personIdent.ident,
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
    barnFødselsdato: LocalDate = LocalDate.of(2019, 1, 1)
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)
    val bostedsadresse = GrMatrikkeladresse(
        matrikkelId = null, bruksenhetsnummer = "H301", tilleggsnavn = "navn",
        postnummer = "0202", kommunenummer = "2231"
    )

    val søker = Person(
        aktørId = randomAktørId(),
        personIdent = PersonIdent(søkerPersonIdent),
        type = PersonType.SØKER,
        personopplysningGrunnlag = personopplysningGrunnlag,
        fødselsdato = LocalDate.of(2019, 1, 1),
        navn = "",
        kjønn = Kjønn.KVINNE,
    ).also { søker ->
        søker.statsborgerskap =
            listOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = søker))
        søker.bostedsadresser = mutableListOf(bostedsadresse.apply { person = søker })
        søker.sivilstander = listOf(
            GrSivilstand(
                type = SIVILSTAND.GIFT,
                person = søker
            )
        )
    }
    personopplysningGrunnlag.personer.add(søker)

    barnasIdenter.map {
        personopplysningGrunnlag.personer.add(
            Person(
                aktørId = randomAktørId(),
                personIdent = PersonIdent(it),
                type = PersonType.BARN,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = barnFødselsdato,
                navn = "",
                kjønn = Kjønn.MANN
            ).also { barn ->
                barn.statsborgerskap =
                    listOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = barn))
                barn.bostedsadresser = mutableListOf(bostedsadresse.apply { person = barn })
                barn.sivilstander = listOf(
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
        behandlingÅrsak = årsak
    )

fun nyRevurdering(søkersIdent: String): NyBehandling = NyBehandling(
    søkersIdent = søkersIdent,
    behandlingType = BehandlingType.REVURDERING,
    kategori = BehandlingKategori.NASJONAL,
    underkategori = BehandlingUnderkategori.ORDINÆR
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
    søkerFnr: String,
    barn1Fnr: String,
    barn2Fnr: String,
    stønadFom: LocalDate,
    stønadTom: LocalDate,
    erDeltBosted: Boolean = false
): Set<PersonResultat> {
    return setOf(
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            fnr = søkerFnr,
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER
        ),
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            fnr = barn1Fnr,
            resultat = Resultat.OPPFYLT,
            periodeFom = stønadFom,
            periodeTom = stønadTom,
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = erDeltBosted
        ),
        lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            fnr = barn2Fnr,
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
    fnr: String,
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
        personIdent = fnr
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
                    erDeltBosted = erDeltBosted && it == Vilkår.BOR_MED_SØKER
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
    vilkårsvurdering.personResultater.forEach { personResultat ->
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
    søkerFnr: String,
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
        personIdent = søkerFnr
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
    søkerFnr: String,
    barnasIdenter: List<String>,
    fagsakService: FagsakService,
    vedtakService: VedtakService,
    persongrunnlagService: PersongrunnlagService,
    vilkårsvurderingService: VilkårsvurderingService,
    stegService: StegService,
    vedtaksperiodeService: VedtaksperiodeService,
): Behandling {
    fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
    val behandling = stegService.håndterNyBehandling(
        NyBehandling(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            søkersIdent = søkerFnr,
            barnasIdenter = barnasIdenter,
        )
    )

    val behandlingEtterPersongrunnlagSteg =
        stegService.håndterSøknad(
            behandling = behandling,
            restRegistrerSøknad = RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = søkerFnr,
                    barnasIdenter = barnasIdenter
                ),
                bekreftEndringerViaFrontend = true
            )
        )

    if (tilSteg == StegType.REGISTRERE_PERSONGRUNNLAG || tilSteg == StegType.REGISTRERE_SØKNAD)
        return behandlingEtterPersongrunnlagSteg

    val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
    persongrunnlagService.hentAktiv(behandlingId = behandling.id)!!.barna.forEach { barn ->
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
                personIdent = søkerFnr
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
        if (behandlingEtterBehandlingsresultat.resultat != BehandlingResultat.FORTSATT_INNVILGET) RestTilbakekreving(
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
                personIdent = søkerFnr
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

fun lagVedtaksbegrunnelse(
    vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon =
        VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
    personIdenter: List<String> = listOf(tilfeldigPerson().personIdent.ident),
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser = mockk()
) = Vedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
    vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
    personIdenter = personIdenter,
)

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
    vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon =
        VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_SØKER_OG_BARN_BOSATT_I_RIKET,
    vedtakBegrunnelseType: VedtakBegrunnelseType = VedtakBegrunnelseType.FORTSATT_INNVILGET,
    personIdenter: List<String> = listOf(tilfeldigPerson().personIdent.ident),
) = RestVedtaksbegrunnelse(
    vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
    vedtakBegrunnelseType = vedtakBegrunnelseType,
    personIdenter = personIdenter,
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
            VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
        )
    )
}

fun lagVilkårResultat(
    personResultat: PersonResultat,
    vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
    resultat: Resultat = Resultat.OPPFYLT,
    periodeFom: LocalDate = LocalDate.of(2009, 12, 24),
    periodeTom: LocalDate = LocalDate.of(2010, 1, 31),
    begrunnelse: String = "",
    behandlingId: Long = lagBehandling().id,
    erMedlemskapVurdert: Boolean = false,
    utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList()
) = VilkårResultat(
    personResultat = personResultat,
    vilkårType = vilkårType,
    resultat = resultat,
    periodeFom = periodeFom,
    periodeTom = periodeTom,
    begrunnelse = begrunnelse,
    behandlingId = behandlingId,
    erMedlemskapVurdert = erMedlemskapVurdert,
    utdypendeVilkårsvurderinger = (
        utdypendeVilkårsvurderinger + listOfNotNull(if (erMedlemskapVurdert) UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP else null)
        ).distinct()
)

val guttenBarnesenFødselsdato = LocalDate.now().withDayOfMonth(10).minusYears(6)

fun lagEndretUtbetalingAndel(
    id: Long = 0,
    behandlingId: Long = 0,
    person: Person,
    prosent: BigDecimal = BigDecimal.valueOf(100),
    fom: YearMonth = YearMonth.now().minusMonths(1),
    tom: YearMonth = YearMonth.now(),
    årsak: Årsak = Årsak.DELT_BOSTED,
    avtaletidspunktDeltBosted: LocalDate = LocalDate.now().minusMonths(1),
    søknadstidspunkt: LocalDate = LocalDate.now().minusMonths(1),
    vedtakBegrunnelseSpesifikasjoner: List<VedtakBegrunnelseSpesifikasjon> = emptyList(),
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
        vedtakBegrunnelseSpesifikasjoner = vedtakBegrunnelseSpesifikasjoner,
        andelTilkjentYtelser = andelTilkjentYtelser
    )

fun lagPerson(
    aktørId: AktørId = randomAktørId(),
    personIdent: PersonIdent = PersonIdent(randomFnr()),
    type: PersonType = PersonType.SØKER,
    personopplysningGrunnlag: PersonopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
    fødselsdato: LocalDate = LocalDate.now().minusYears(19),
    kjønn: Kjønn = Kjønn.KVINNE
) = Person(
    aktørId = aktørId,
    personIdent = personIdent,
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
