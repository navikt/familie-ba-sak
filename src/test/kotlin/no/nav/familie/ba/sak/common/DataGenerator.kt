package no.nav.familie.ba.sak.common

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.NyBehandlingDto
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.fagsak.Fagsak
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPerson
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.behandling.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.behandling.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.behandling.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.steg.*
import no.nav.familie.ba.sak.behandling.vedtak.*
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.task.DistribuerVedtaksbrevDTO
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import no.nav.familie.ba.sak.task.StatusFraOppdragTask
import no.nav.familie.ba.sak.task.dto.FAGSYSTEM
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.task.dto.StatusFraOppdragDTO
import no.nav.familie.ba.sak.økonomi.sats
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.familie.prosessering.domene.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.abs
import kotlin.random.Random

fun randomFnr(): String = Random.nextLong(10_000_000_000, 31_121_299_999).toString()
fun randomAktørId(): AktørId = AktørId(Random.nextLong(1000_000_000_000, 31_121_299_99999).toString())

private var gjeldendeVedtakId: Long = abs(Random.nextLong(10000000))
private var gjeldendeUtbetalingBegrunnelseId: Long = abs(Random.nextLong(10000000))
private var gjeldendeBehandlingId: Long = abs(Random.nextLong(10000000))
private var gjeldendePersonId: Long = abs(Random.nextLong(10000000))
private val id_inkrement = 50

fun nesteVedtakId(): Long {
    gjeldendeVedtakId += id_inkrement
    return gjeldendeVedtakId
}

fun nesteUtbetalingBegrunnelseId(): Long {
    gjeldendeUtbetalingBegrunnelseId += id_inkrement
    return gjeldendeUtbetalingBegrunnelseId
}

fun nesteBehandlingId(): Long {
    gjeldendeBehandlingId += id_inkrement
    return gjeldendeBehandlingId
}

fun nestePersonId(): Long {
    gjeldendePersonId += id_inkrement
    return gjeldendePersonId
}

val defaultFagsak = Fagsak(1).also {
    it.søkerIdenter =
            setOf(FagsakPerson(fagsak = it, personIdent = PersonIdent(randomFnr()), opprettetTidspunkt = LocalDateTime.now()))
}

fun lagBehandling(fagsak: Fagsak = defaultFagsak,
                  behandlingKategori: BehandlingKategori = BehandlingKategori.NASJONAL,
                  behandlingType: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                  årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
                  automatiskOpprettelse: Boolean = false
) = Behandling(id = nesteBehandlingId(),
               fagsak = fagsak,
               skalBehandlesAutomatisk = automatiskOpprettelse,
               type = behandlingType,
               kategori = behandlingKategori,
               underkategori = BehandlingUnderkategori.ORDINÆR,
               opprettetÅrsak = årsak).also {
    it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, FØRSTE_STEG))
}

fun tilfeldigPerson(fødselsdato: LocalDate = LocalDate.now(),
                    personType: PersonType = PersonType.BARN,
                    kjønn: Kjønn = Kjønn.MANN) = Person(
        id = nestePersonId(),
        aktørId = randomAktørId(),
        personIdent = PersonIdent(randomFnr()),
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = kjønn,
        sivilstand = SIVILSTAND.UGIFT
)

fun tilfeldigSøker(fødselsdato: LocalDate = LocalDate.now(),
                   personType: PersonType = PersonType.SØKER,
                   kjønn: Kjønn = Kjønn.MANN) = Person(
        id = nestePersonId(),
        aktørId = randomAktørId(),
        personIdent = PersonIdent(randomFnr()),
        fødselsdato = fødselsdato,
        type = personType,
        personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
        navn = "",
        kjønn = kjønn,
        sivilstand = SIVILSTAND.UGIFT
)

fun lagUtbetalingBegrunnesle(
        vedtak: Vedtak = lagVedtak(),
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        begrunnelseType: VedtakBegrunnelseType? = null,
        vedtakBegrunnelse: VedtakBegrunnelse? = null,
        brevBegrunnelse: String? = null,
): UtbetalingBegrunnelse =
        UtbetalingBegrunnelse(
                id = nesteUtbetalingBegrunnelseId(),
                vedtak = vedtak,
                fom = fom,
                tom = tom,
                begrunnelseType = begrunnelseType,
                vedtakBegrunnelse = vedtakBegrunnelse,
                brevBegrunnelse = brevBegrunnelse,
        )


fun lagVedtak(
        behandling: Behandling = lagBehandling(),
        opphørsdato: LocalDate? = null,
        utbetalingBegrunnelser: MutableSet<UtbetalingBegrunnelse> = mutableSetOf(),
) =
        Vedtak(
                id = nesteVedtakId(),
                behandling = behandling,
                vedtaksdato = LocalDateTime.now(),
                opphørsdato = opphørsdato,
                utbetalingBegrunnelser = utbetalingBegrunnelser,
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

    val søker = Person(aktørId = randomAktørId(),
                       personIdent = PersonIdent(søkerPersonIdent),
                       type = PersonType.SØKER,
                       personopplysningGrunnlag = personopplysningGrunnlag,
                       fødselsdato = LocalDate.of(2019, 1, 1),
                       navn = "",
                       kjønn = Kjønn.KVINNE,
                       bostedsadresse = bostedsadresse,
                       sivilstand = SIVILSTAND.GIFT
    ).apply { statsborgerskap = listOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = this)) }
    personopplysningGrunnlag.personer.add(søker)

    barnasIdenter.map {
        personopplysningGrunnlag.personer.add(Person(aktørId = randomAktørId(),
                                                     personIdent = PersonIdent(it),
                                                     type = PersonType.BARN,
                                                     personopplysningGrunnlag = personopplysningGrunnlag,
                                                     fødselsdato = barnFødselsdato,
                                                     navn = "",
                                                     kjønn = Kjønn.MANN,
                                                     bostedsadresse = bostedsadresse,
                                                     sivilstand = SIVILSTAND.UGIFT).apply {
            statsborgerskap = listOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = this))
        })
    }
    return personopplysningGrunnlag
}

fun dato(s: String) = LocalDate.parse(s)
fun årMnd(s: String) = YearMonth.parse(s)

fun nyOrdinærBehandling(søkersIdent: String): NyBehandlingDto = NyBehandlingDto(
        søkersIdent = søkersIdent,
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR
)

fun nyRevurdering(søkersIdent: String): NyBehandlingDto = NyBehandlingDto(
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

fun lagPersonResultaterForSøkerOgToBarn(behandlingResultat: BehandlingResultat,
                                        søkerFnr: String,
                                        barn1Fnr: String,
                                        barn2Fnr: String,
                                        stønadFom: LocalDate,
                                        stønadTom: LocalDate): Set<PersonResultat> {
    return setOf(
            lagPersonResultat(behandlingResultat = behandlingResultat,
                              fnr = søkerFnr,
                              resultat = Resultat.OPPFYLT,
                              periodeFom = stønadFom,
                              periodeTom = stønadTom,
                              lagFullstendigVilkårResultat = true,
                              personType = PersonType.SØKER
            ),
            lagPersonResultat(behandlingResultat = behandlingResultat,
                              fnr = barn1Fnr,
                              resultat = Resultat.OPPFYLT,
                              periodeFom = stønadFom,
                              periodeTom = stønadTom,
                              lagFullstendigVilkårResultat = true,
                              personType = PersonType.BARN
            ),
            lagPersonResultat(behandlingResultat = behandlingResultat,
                              fnr = barn2Fnr,
                              resultat = Resultat.OPPFYLT,
                              periodeFom = stønadFom,
                              periodeTom = stønadTom,
                              lagFullstendigVilkårResultat = true,
                              personType = PersonType.BARN
            )
    )
}

fun lagPersonResultat(behandlingResultat: BehandlingResultat,
                      fnr: String,
                      resultat: Resultat,
                      periodeFom: LocalDate?,
                      periodeTom: LocalDate?,
                      lagFullstendigVilkårResultat: Boolean = false,
                      personType: PersonType = PersonType.BARN,
                      vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET): PersonResultat {
    val personResultat = PersonResultat(
            behandlingResultat = behandlingResultat,
            personIdent = fnr)

    if (lagFullstendigVilkårResultat) {
        personResultat.setVilkårResultater(
                Vilkår.hentVilkårFor(personType).map {
                    VilkårResultat(personResultat = personResultat,
                                   periodeFom = periodeFom,
                                   periodeTom = periodeTom,
                                   vilkårType = it,
                                   resultat = resultat,
                                   begrunnelse = "",
                                   behandlingId = behandlingResultat.behandling.id,
                                   regelInput = null,
                                   regelOutput = null)
                }.toSet())
    } else {
        personResultat.setVilkårResultater(
                setOf(VilkårResultat(personResultat = personResultat,
                                     periodeFom = periodeFom,
                                     periodeTom = periodeTom,
                                     vilkårType = vilkårType,
                                     resultat = resultat,
                                     begrunnelse = "",
                                     behandlingId = behandlingResultat.behandling.id,
                                     regelInput = null,
                                     regelOutput = null))
        )
    }
    return personResultat
}

fun vurderBehandlingResultatTilInnvilget(behandlingResultat: BehandlingResultat, barn: Person) {
    behandlingResultat.personResultater.forEach { personResultat ->
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

fun lagBehandlingResultat(søkerFnr: String,
                          behandling: Behandling,
                          resultat: Resultat,
                          søkerPeriodeFom: LocalDate? = LocalDate.now().minusMonths(1),
                          søkerPeriodeTom: LocalDate? = LocalDate.now().plusYears(2)): BehandlingResultat {
    val behandlingResultat = BehandlingResultat(
            behandling = behandling
    )
    val personResultat = PersonResultat(
            behandlingResultat = behandlingResultat,
            personIdent = søkerFnr)
    personResultat.setVilkårResultater(
            setOf(VilkårResultat(personResultat = personResultat,
                                 vilkårType = Vilkår.BOSATT_I_RIKET,
                                 resultat = resultat,
                                 periodeFom = søkerPeriodeFom,
                                 periodeTom = søkerPeriodeTom,
                                 begrunnelse = "",
                                 behandlingId = behandling.id,
                                 regelInput = null,
                                 regelOutput = null),
                  VilkårResultat(personResultat = personResultat,
                                 vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                 resultat = resultat,
                                 periodeFom = søkerPeriodeFom,
                                 periodeTom = søkerPeriodeTom,
                                 begrunnelse = "",
                                 behandlingId = behandling.id,
                                 regelInput = null,
                                 regelOutput = null))
    )
    behandlingResultat.personResultater = setOf(personResultat)
    return behandlingResultat
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
        behandlingService: BehandlingService,
        vedtakService: VedtakService,
        persongrunnlagService: PersongrunnlagService,
        behandlingResultatService: BehandlingResultatService,
        stegService: StegService
): Behandling {
    val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
    val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

    val behandlingEtterPersongrunnlagSteg = stegService.håndterSøknad(behandling = behandling,
                                                                      restRegistrerSøknad = RestRegistrerSøknad(
                                                                              søknad = lagSøknadDTO(søkerIdent = søkerFnr,
                                                                                                    barnasIdenter = barnasIdenter),
                                                                              bekreftEndringerViaFrontend = true))
    if (tilSteg == StegType.REGISTRERE_PERSONGRUNNLAG) return behandlingEtterPersongrunnlagSteg

    val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId = behandling.id)!!
    persongrunnlagService.hentAktiv(behandlingId = behandling.id)!!.barna.forEach { barn ->
        vurderBehandlingResultatTilInnvilget(behandlingResultat, barn)
    }
    behandlingResultatService.oppdater(behandlingResultat)

    val behandlingEtterVilkårsvurderingSteg = stegService.håndterVilkårsvurdering(behandlingEtterPersongrunnlagSteg)
    if (tilSteg == StegType.VILKÅRSVURDERING) return behandlingEtterVilkårsvurderingSteg


    val behandlingEtterSendTilBeslutter = stegService.håndterSendTilBeslutter(behandlingEtterVilkårsvurderingSteg, "1234")
    if (tilSteg == StegType.SEND_TIL_BESLUTTER) return behandlingEtterSendTilBeslutter


    val behandlingEtterBeslutteVedtak = stegService.håndterBeslutningForVedtak(behandlingEtterSendTilBeslutter,
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


    val behandlingEtterJournalførtVedtak =
            stegService.håndterJournalførVedtaksbrev(behandlingEtterStatusFraOppdrag, JournalførVedtaksbrevDTO(
                    vedtakId = vedtak.id,
                    task = Task.nyTask(type = JournalførVedtaksbrevTask.TASK_STEP_TYPE, payload = "")
            ))
    if (tilSteg == StegType.JOURNALFØR_VEDTAKSBREV) return behandlingEtterJournalførtVedtak


    val behandlingEtterDistribuertVedtak = stegService.håndterDistribuerVedtaksbrev(behandlingEtterJournalførtVedtak,
                                                                                    DistribuerVedtaksbrevDTO(behandlingId = behandling.id,
                                                                                                             journalpostId = "1234",
                                                                                                             personIdent = søkerFnr))
    if (tilSteg == StegType.DISTRIBUER_VEDTAKSBREV) return behandlingEtterDistribuertVedtak

    return stegService.håndterFerdigstillBehandling(behandlingEtterDistribuertVedtak)
}


