package no.nav.familie.ba.sak.common

import lagBehandling
import lagVedtak
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
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
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
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.personident.Aktør
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
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import randomAktør
import randomFnr
import tilfeldigSøker
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Properties
import kotlin.math.abs
import kotlin.random.Random

private var gjeldendeUtvidetVedtaksperiodeId: Long = abs(Random.nextLong(10000000))
private const val ID_INKREMENT = 50

fun nesteUtvidetVedtaksperiodeId(): Long {
    gjeldendeUtvidetVedtaksperiodeId += ID_INKREMENT
    return gjeldendeUtvidetVedtaksperiodeId
}

fun dato(s: String) = LocalDate.parse(s)

fun årMnd(s: String) = YearMonth.parse(s)

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
