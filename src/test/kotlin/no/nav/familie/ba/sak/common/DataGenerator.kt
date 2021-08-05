package no.nav.familie.ba.sak.common

import io.mockk.mockk
import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestPerson
import no.nav.familie.ba.sak.ekstern.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksbegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedBegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.tilRestPerson
import no.nav.familie.ba.sak.integrasjoner.økonomi.sats
import no.nav.familie.ba.sak.kjerne.fødselshendelse.filtreringsregler.FiltreringsreglerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakPerson
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
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
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Utbetalingsperiode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

val fødselsnummerGenerator = FoedselsnummerGenerator()

fun randomFnr(): String = fødselsnummerGenerator.foedselsnummer().asString
fun randomAktørId(): AktørId = AktørId(Random.nextLong(1000_000_000_000, 31_121_299_99999).toString())

private var gjeldendeVedtakId: Long = abs(Random.nextLong(10000000))
private var gjeldendeVedtakBegrunnelseId: Long = abs(Random.nextLong(10000000))
private var gjeldendeBehandlingId: Long = abs(Random.nextLong(10000000))
private var gjeldendePersonId: Long = abs(Random.nextLong(10000000))
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

fun defaultFagsak() = Fagsak(1).also {
    it.søkerIdenter =
            setOf(FagsakPerson(fagsak = it, personIdent = PersonIdent(randomFnr()), opprettetTidspunkt = LocalDateTime.now()))
}

fun lagBehandling(fagsak: Fagsak = defaultFagsak(),
                  behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
                  behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                  årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
                  automatiskOpprettelse: Boolean = false,
                  førsteSteg: StegType = FØRSTE_STEG
) = Behandling(id = nesteBehandlingId(),
               fagsak = fagsak,
               skalBehandlesAutomatisk = automatiskOpprettelse,
               type = behandlingType,
               kategori = behandlingKategori,
               underkategori = BehandlingUnderkategori.ORDINÆR,
               opprettetÅrsak = årsak).also {
    it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, førsteSteg))
}

fun tilfeldigPerson(
        fødselsdato: LocalDate = LocalDate.now(),
        personType: PersonType = PersonType.BARN,
        kjønn: Kjønn = Kjønn.MANN,
        personIdent: PersonIdent = PersonIdent(randomFnr())
) = Person(
        id = nestePersonId(),
        aktørId = randomAktørId(),
        personIdent = personIdent,
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = kjønn
).apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }


fun tilfeldigSøker(
        fødselsdato: LocalDate = LocalDate.now(),
        personType: PersonType = PersonType.SØKER,
        kjønn: Kjønn = Kjønn.MANN,
        personIdent: PersonIdent = PersonIdent(randomFnr())
) = Person(
        id = nestePersonId(),
        aktørId = randomAktørId(),
        personIdent = personIdent,
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = kjønn
).apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = this)) }

fun lagVedtakBegrunnesle(
        vedtak: Vedtak = lagVedtak(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        vedtakBegrunnelse: VedtakBegrunnelseSpesifikasjon,
        brevBegrunnelse: String? = null,
): VedtakBegrunnelse =
        VedtakBegrunnelse(
                id = nesteVedtakBegrunnelseId(),
                vedtak = vedtak,
                fom = fom,
                tom = tom,
                begrunnelse = vedtakBegrunnelse,
                brevBegrunnelse = brevBegrunnelse,
        )


fun lagVedtak(
        behandling: Behandling = lagBehandling(),
        vedtakBegrunnelser: MutableSet<VedtakBegrunnelse> = mutableSetOf(),
) =
        Vedtak(
                id = nesteVedtakId(),
                behandling = behandling,
                vedtaksdato = LocalDateTime.now(),
                vedtakBegrunnelser = vedtakBegrunnelser,
        )

fun lagAndelTilkjentYtelse(fom: String,
                           tom: String,
                           ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
                           beløp: Int = sats(ytelseType),
                           behandling: Behandling = lagBehandling(),
                           person: Person = tilfeldigPerson(),
                           periodeIdOffset: Long? = null,
                           forrigeperiodeIdOffset: Long? = null,
                           tilkjentYtelse: TilkjentYtelse? = null): AndelTilkjentYtelse {

    return AndelTilkjentYtelse(
            personIdent = person.personIdent.ident,
            behandlingId = behandling.id,
            tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
            beløp = beløp,
            stønadFom = årMnd(fom),
            stønadTom = årMnd(tom),
            type = ytelseType,
            periodeOffset = periodeIdOffset,
            forrigePeriodeOffset = forrigeperiodeIdOffset
    )
}

fun lagAndelTilkjentYtelseUtvidet(fom: String,
                                  tom: String,
                                  ytelseType: YtelseType,
                                  beløp: Int = sats(ytelseType),
                                  behandling: Behandling = lagBehandling(),
                                  person: Person = tilfeldigSøker(),
                                  periodeIdOffset: Long? = null,
                                  forrigeperiodeIdOffset: Long? = null,
                                  tilkjentYtelse: TilkjentYtelse? = null): AndelTilkjentYtelse {

    return AndelTilkjentYtelse(
            personIdent = person.personIdent.ident,
            behandlingId = behandling.id,
            tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
            beløp = beløp,
            stønadFom = årMnd(fom),
            stønadTom = årMnd(tom),
            type = ytelseType,
            periodeOffset = periodeIdOffset,
            forrigePeriodeOffset = forrigeperiodeIdOffset
    )
}


fun lagInitiellTilkjentYtelse(behandling: Behandling = lagBehandling()): TilkjentYtelse {
    return TilkjentYtelse(behandling = behandling, opprettetDato = LocalDate.now(), endretDato = LocalDate.now())
}

fun lagTestPersonopplysningGrunnlag(behandlingId: Long,
                                    vararg personer: Person): PersonopplysningGrunnlag {

    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)

    personopplysningGrunnlag.personer.addAll(
            personer.map { it.copy(personopplysningGrunnlag = personopplysningGrunnlag) }
    )
    return personopplysningGrunnlag
}

fun lagTestPersonopplysningGrunnlag(behandlingId: Long,
                                    søkerPersonIdent: String,
                                    barnasIdenter: List<String>,
                                    barnFødselsdato: LocalDate = LocalDate.of(2019, 1, 1)): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)
    val bostedsadresse = GrMatrikkeladresse(matrikkelId = null, bruksenhetsnummer = "H301", tilleggsnavn = "navn",
                                            postnummer = "0202", kommunenummer = "2231")

    val søker = Person(
            aktørId = randomAktørId(),
            personIdent = PersonIdent(søkerPersonIdent),
            type = PersonType.SØKER,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = LocalDate.of(2019, 1, 1),
            navn = "",
            kjønn = Kjønn.KVINNE,
    ).also { søker ->
        søker.statsborgerskap = listOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = søker))
        søker.bostedsadresser = mutableListOf(bostedsadresse.apply { person = søker })
        søker.sivilstander = listOf(GrSivilstand(type = SIVILSTAND.GIFT,
                                                 person = søker))
    }
    personopplysningGrunnlag.personer.add(søker)

    barnasIdenter.map {
        personopplysningGrunnlag.personer.add(Person(aktørId = randomAktørId(),
                                                     personIdent = PersonIdent(it),
                                                     type = PersonType.BARN,
                                                     personopplysningGrunnlag = personopplysningGrunnlag,
                                                     fødselsdato = barnFødselsdato,
                                                     navn = "",
                                                     kjønn = Kjønn.MANN).also { barn ->
            barn.statsborgerskap = listOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = barn))
            barn.bostedsadresser = mutableListOf(bostedsadresse.apply { person = barn })
            barn.sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UGIFT,
                                                    person = barn))
        })
    }
    return personopplysningGrunnlag
}

fun dato(s: String) = LocalDate.parse(s)
fun årMnd(s: String) = YearMonth.parse(s)

fun nyOrdinærBehandling(søkersIdent: String, årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD): NyBehandling = NyBehandling(
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

fun lagSøknadDTO(søkerIdent: String, barnasIdenter: List<String>): SøknadDTO {
    return SøknadDTO(
            underkategori = BehandlingUnderkategori.ORDINÆR,
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

fun lagPersonResultaterForSøkerOgToBarn(vilkårsvurdering: Vilkårsvurdering,
                                        søkerFnr: String,
                                        barn1Fnr: String,
                                        barn2Fnr: String,
                                        stønadFom: LocalDate,
                                        stønadTom: LocalDate,
                                        erDeltBosted: Boolean = false): Set<PersonResultat> {
    return setOf(
            lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                              fnr = søkerFnr,
                              resultat = Resultat.OPPFYLT,
                              periodeFom = stønadFom,
                              periodeTom = stønadTom,
                              lagFullstendigVilkårResultat = true,
                              personType = PersonType.SØKER
            ),
            lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
                              fnr = barn1Fnr,
                              resultat = Resultat.OPPFYLT,
                              periodeFom = stønadFom,
                              periodeTom = stønadTom,
                              lagFullstendigVilkårResultat = true,
                              personType = PersonType.BARN,
                              erDeltBosted = erDeltBosted
            ),
            lagPersonResultat(vilkårsvurdering = vilkårsvurdering,
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

fun lagPersonResultat(vilkårsvurdering: Vilkårsvurdering,
                      fnr: String,
                      resultat: Resultat,
                      periodeFom: LocalDate?,
                      periodeTom: LocalDate?,
                      lagFullstendigVilkårResultat: Boolean = false,
                      personType: PersonType = PersonType.BARN,
                      vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
                      erDeltBosted: Boolean = false): PersonResultat {
    val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            personIdent = fnr)

    if (lagFullstendigVilkårResultat) {
        personResultat.setSortedVilkårResultater(
                Vilkår.hentVilkårFor(personType).map {
                    VilkårResultat(personResultat = personResultat,
                                   periodeFom = periodeFom,
                                   periodeTom = periodeTom,
                                   vilkårType = it,
                                   resultat = resultat,
                                   begrunnelse = "",
                                   behandlingId = vilkårsvurdering.behandling.id,
                                   erDeltBosted = erDeltBosted && it == Vilkår.BOR_MED_SØKER)
                }.toSet())
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

fun lagVilkårsvurdering(søkerFnr: String,
                        behandling: Behandling,
                        resultat: Resultat,
                        søkerPeriodeFom: LocalDate? = LocalDate.now().minusMonths(1),
                        søkerPeriodeTom: LocalDate? = LocalDate.now().plusYears(2)): Vilkårsvurdering {
    val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling
    )
    val personResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            personIdent = søkerFnr)
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
    personResultat.andreVurderinger.add(AnnenVurdering(personResultat = personResultat,
                                                       resultat = resultat,
                                                       type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                                                       begrunnelse = null))

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
    val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
    val behandling = stegService.håndterNyBehandling(NyBehandling(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            søkersIdent = søkerFnr,
            barnasIdenter = barnasIdenter,
    ))

    val behandlingEtterPersongrunnlagSteg =
            stegService.håndterSøknad(behandling = behandling,
                                      restRegistrerSøknad = RestRegistrerSøknad(
                                              søknad = lagSøknadDTO(søkerIdent = søkerFnr,
                                                                    barnasIdenter = barnasIdenter),
                                              bekreftEndringerViaFrontend = true))

    if (tilSteg == StegType.REGISTRERE_PERSONGRUNNLAG || tilSteg == StegType.REGISTRERE_SØKNAD)
        return behandlingEtterPersongrunnlagSteg

    val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)!!
    persongrunnlagService.hentAktiv(behandlingId = behandling.id)!!.barna.forEach { barn ->
        vurderVilkårsvurderingTilInnvilget(vilkårsvurdering, barn)
    }
    vilkårsvurderingService.oppdater(vilkårsvurdering)

    val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg)

    vedtakService.leggTilVedtakBegrunnelse(
            RestPostVedtakBegrunnelse(
                    fom = LocalDate.parse("2020-02-01"),
                    tom = LocalDate.parse("2025-02-01"),
                    vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR),
            fagsakId = fagsak.id)
    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg

    val behandlingEtterVurderTilbakekrevingSteg = stegService.håndterVurderTilbakekreving(
            behandlingEtterVilkårsvurderingSteg,
            RestTilbakekreving(valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                               begrunnelse = "Begrunnelse")
    )

    leggTilBegrunnelsePåVedtaksperiodeIBehandling(
            behandling = behandlingEtterVurderTilbakekrevingSteg,
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
    )

    if (tilSteg == StegType.VURDER_TILBAKEKREVING) return behandlingEtterVurderTilbakekrevingSteg

    val behandlingEtterSendTilBeslutter = stegService.håndterSendTilBeslutter(behandlingEtterVurderTilbakekrevingSteg, "1234")
    if (tilSteg == StegType.SEND_TIL_BESLUTTER) return behandlingEtterSendTilBeslutter

    val behandlingEtterBeslutteVedtak =
            stegService.håndterBeslutningForVedtak(behandlingEtterSendTilBeslutter,
                                                   RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT))
    if (tilSteg == StegType.BESLUTTE_VEDTAK) return behandlingEtterBeslutteVedtak


    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
    val behandlingEtterIverksetteVedtak =
            stegService.håndterIverksettMotØkonomi(behandlingEtterBeslutteVedtak, IverksettingTaskDTO(
                    behandlingsId = behandlingEtterBeslutteVedtak.id,
                    vedtaksId = vedtak!!.id,
                    saksbehandlerId = "System",
                    personIdent = søkerFnr
            ))
    if (tilSteg == StegType.IVERKSETT_MOT_OPPDRAG) return behandlingEtterIverksetteVedtak

    val behandlingEtterStatusFraOppdrag =
            stegService.håndterStatusFraØkonomi(behandlingEtterIverksetteVedtak, StatusFraOppdragMedTask(
                    statusFraOppdragDTO = StatusFraOppdragDTO(fagsystem = FAGSYSTEM,
                                                              personIdent = søkerFnr,
                                                              behandlingsId = behandlingEtterIverksetteVedtak.id,
                                                              vedtaksId = vedtak.id),
                    task = Task.nyTask(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = "")
            ))
    if (tilSteg == StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) return behandlingEtterStatusFraOppdrag

    val behandlingEtterIverksetteMotTilbake =
            stegService.håndterIverksettMotFamilieTilbake(behandlingEtterStatusFraOppdrag, Properties())
    if (tilSteg == StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) return behandlingEtterIverksetteMotTilbake

    val behandlingEtterJournalførtVedtak =
            stegService.håndterJournalførVedtaksbrev(behandlingEtterIverksetteMotTilbake, JournalførVedtaksbrevDTO(
                    vedtakId = vedtak.id,
                    task = Task.nyTask(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
            ))
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak


    val behandlingEtterDistribuertVedtak =
            stegService.håndterDistribuerVedtaksbrev(behandlingEtterJournalførtVedtak,
                                                     DistribuerVedtaksbrevDTO(behandlingId = behandling.id,
                                                                              journalpostId = "1234",
                                                                              personIdent = søkerFnr))
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
        stegService: StegService,
        tilbakekrevingService: TilbakekrevingService
): Behandling {
    val behandling = stegService.håndterNyBehandling(NyBehandling(
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL,
            søkersIdent = søkerFnr,
            barnasIdenter = barnasIdenter,
    ))

    val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandling)
    vedtakService.leggTilVedtakBegrunnelse(
            RestPostVedtakBegrunnelse(
                    fom = LocalDate.parse("2020-02-01"),
                    tom = LocalDate.parse("2025-02-01"),
                    vedtakBegrunnelse = VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR),
            fagsakId = behandling.fagsak.id)
    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg

    val behandlingEtterSimuleringSteg = stegService.håndterVurderTilbakekreving(
            behandlingEtterVilkårsvurderingSteg,
            RestTilbakekreving(valg = Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING,
                               begrunnelse = "Begrunnelse")
    )
    if (tilSteg == StegType.VURDER_TILBAKEKREVING) return behandlingEtterSimuleringSteg

    val restTilbakekreving = opprettRestTilbakekreving()
    tilbakekrevingService.validerRestTilbakekreving(restTilbakekreving, behandlingEtterSimuleringSteg.id)
    tilbakekrevingService.lagreTilbakekreving(restTilbakekreving, behandlingEtterSimuleringSteg.id)

    val behandlingEtterSendTilBeslutter = stegService.håndterSendTilBeslutter(behandlingEtterSimuleringSteg, "1234")
    if (tilSteg == StegType.SEND_TIL_BESLUTTER) return behandlingEtterSendTilBeslutter

    val behandlingEtterBeslutteVedtak =
            stegService.håndterBeslutningForVedtak(behandlingEtterSendTilBeslutter,
                                                   RestBeslutningPåVedtak(beslutning = Beslutning.GODKJENT))
    if (tilSteg == StegType.BESLUTTE_VEDTAK) return behandlingEtterBeslutteVedtak


    val vedtak = vedtakService.hentAktivForBehandling(behandlingEtterBeslutteVedtak.id)
    val behandlingEtterIverksetteVedtak =
            stegService.håndterIverksettMotØkonomi(behandlingEtterBeslutteVedtak, IverksettingTaskDTO(
                    behandlingsId = behandlingEtterBeslutteVedtak.id,
                    vedtaksId = vedtak!!.id,
                    saksbehandlerId = "System",
                    personIdent = søkerFnr
            ))
    if (tilSteg == StegType.IVERKSETT_MOT_OPPDRAG) return behandlingEtterIverksetteVedtak

    val behandlingEtterStatusFraOppdrag =
            stegService.håndterStatusFraØkonomi(behandlingEtterIverksetteVedtak, StatusFraOppdragMedTask(
                    statusFraOppdragDTO = StatusFraOppdragDTO(fagsystem = FAGSYSTEM,
                                                              personIdent = søkerFnr,
                                                              behandlingsId = behandlingEtterIverksetteVedtak.id,
                                                              vedtaksId = vedtak.id),
                    task = Task.nyTask(type = StatusFraOppdragTask.TASK_STEP_TYPE, payload = "")
            ))
    if (tilSteg == StegType.VENTE_PÅ_STATUS_FRA_ØKONOMI) return behandlingEtterStatusFraOppdrag

    val behandlingEtterIverksetteMotTilbake =
            stegService.håndterIverksettMotFamilieTilbake(behandlingEtterStatusFraOppdrag, Properties())
    if (tilSteg == StegType.IVERKSETT_MOT_FAMILIE_TILBAKE) return behandlingEtterIverksetteMotTilbake

    val behandlingEtterJournalførtVedtak =
            stegService.håndterJournalførVedtaksbrev(behandlingEtterIverksetteMotTilbake, JournalførVedtaksbrevDTO(
                    vedtakId = vedtak.id,
                    task = Task.nyTask(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
            ))
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak


    val behandlingEtterDistribuertVedtak =
            stegService.håndterDistribuerVedtaksbrev(behandlingEtterJournalførtVedtak,
                                                     DistribuerVedtaksbrevDTO(behandlingId = behandling.id,
                                                                              journalpostId = "1234",
                                                                              personIdent = søkerFnr))
    if (tilSteg == StegType.DISTRIBUER_VEDTAKSBREV) return behandlingEtterDistribuertVedtak

    return stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)

}

fun opprettRestTilbakekreving(): RestTilbakekreving = RestTilbakekreving(
        valg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
        varsel = "Varsel",
        begrunnelse = "Begrunnelse",
)

/**
 * Dette er en funksjon for å få en automatisk førstegangsbehandling til en ønsket tilstand ved test.
 * Man sender inn steg man ønsker å komme til (tilSteg), personer på behandlingen (søkerFnr og barnasIdenter),
 * og serviceinstanser som brukes i testen.
 */
fun kjørStegprosessForAutomatiskFGB(
        tilSteg: StegType,
        søkerFnr: String,
        barnasIdenter: List<String>,
        filtreringsreglerService: FiltreringsreglerService,
        behandlingService: BehandlingService,
        persongrunnlagService: PersongrunnlagService,
        stegService: StegService
): Behandling {
    val nyBehandling = NyBehandlingHendelse(
            morsIdent = søkerFnr,
            barnasIdenter = barnasIdenter
    )
    val behandling = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)

    if (tilSteg == StegType.REGISTRERE_PERSONGRUNNLAG) return behandling

    filtreringsreglerService.kjørFiltreringsregler(nyBehandling,
                                                   behandling)

    val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
    stegService.evaluerVilkårForFødselshendelse(behandling, personopplysningGrunnlag)

    // TODO implementer resten av flyt
    return behandlingService.hent(behandlingId = behandling.id)
}

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
) =
        UtbetalingsperiodeDetalj(person, ytelseType, utbetaltPerMnd)

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
        fom: LocalDate = LocalDate.now().withDayOfMonth(1),
        tom: LocalDate = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
        type: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
        begrunnelser: MutableSet<Vedtaksbegrunnelse> = mutableSetOf(lagVedtaksbegrunnelse()),
        fritekster: MutableSet<VedtaksbegrunnelseFritekst> = mutableSetOf(),
) = VedtaksperiodeMedBegrunnelser(
        vedtak = vedtak,
        fom = fom,
        tom = tom,
        type = type,
        begrunnelser = begrunnelser,
        fritekster = fritekster,
)

fun leggTilBegrunnelsePåVedtaksperiodeIBehandling(
        behandling: Behandling,
        vedtakService: VedtakService,
        vedtaksperiodeService: VedtaksperiodeService,
) {
    val aktivtVedtak = vedtakService.hentAktivForBehandling(behandling.id)!!

    val perisisterteVedtaksperioder =
            vedtaksperiodeService.hentPersisterteVedtaksperioder(aktivtVedtak)

    vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(
            vedtaksperiodeId = perisisterteVedtaksperioder.first().id,
            restPutVedtaksperiodeMedBegrunnelse =
            RestPutVedtaksperiodeMedBegrunnelse(begrunnelser = listOf(
                    RestPutVedtaksbegrunnelse(
                            vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                    ))))
}
