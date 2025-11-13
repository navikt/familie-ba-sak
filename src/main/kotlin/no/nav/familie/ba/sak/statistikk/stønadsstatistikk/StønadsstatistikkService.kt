package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.filtrerGjeldendeNå
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.eksterne.kontrakter.BehandlingTypeV2
import no.nav.familie.eksterne.kontrakter.BehandlingÅrsakV2
import no.nav.familie.eksterne.kontrakter.FagsakType
import no.nav.familie.eksterne.kontrakter.KategoriV2
import no.nav.familie.eksterne.kontrakter.Kompetanse
import no.nav.familie.eksterne.kontrakter.KompetanseAktivitet
import no.nav.familie.eksterne.kontrakter.KompetanseResultat
import no.nav.familie.eksterne.kontrakter.PersonDVHV2
import no.nav.familie.eksterne.kontrakter.UtbetalingsDetaljDVHV2
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVHV2
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
import no.nav.familie.eksterne.kontrakter.YtelseType.FINNMARKSTILLEGG
import no.nav.familie.eksterne.kontrakter.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.eksterne.kontrakter.YtelseType.SMÅBARNSTILLEGG
import no.nav.familie.eksterne.kontrakter.YtelseType.SVALBARDTILLEGG
import no.nav.familie.eksterne.kontrakter.YtelseType.UTVIDET_BARNETRYGD
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class StønadsstatistikkService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val vedtakService: VedtakService,
    private val personopplysningerService: PersonopplysningerService,
    private val vedtakRepository: VedtakRepository,
    private val kompetanseService: KompetanseService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val featureToggleService: FeatureToggleService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun hentVedtakV2(behandlingId: Long): VedtakDVHV2 {
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId)
        val behandling = vedtak?.behandling ?: behandlingHentOgPersisterService.hent(behandlingId)
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId)
        // DVH ønsker tidspunkt med klokkeslett

        var datoVedtak = vedtak?.vedtaksdato

        if (datoVedtak == null) {
            datoVedtak = vedtakRepository.finnVedtakForBehandling(behandlingId).singleOrNull()?.vedtaksdato
                ?: throw Feil("Fant ikke vedtaksdato for behandling $behandlingId")
        }

        val tidspunktVedtak = datoVedtak
        val sisteIverksatteBehandlingId =
            (tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id) != null)
                .let { behandlingHarUtbetalingsoppdrag ->
                    if (behandlingHarUtbetalingsoppdrag) {
                        null
                    } else {
                        behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)?.id.toString()
                    }
                }

        return VedtakDVHV2(
            fagsakId = behandling.fagsak.id.toString(),
            fagsakType = FagsakType.valueOf(behandling.fagsak.type.name),
            behandlingsId = behandlingId.toString(),
            sisteIverksatteBehandlingId = sisteIverksatteBehandlingId,
            tidspunktVedtak = tidspunktVedtak.atZone(TIMEZONE),
            personV2 = hentSøkerV2(persongrunnlag),
            // TODO implementere støtte for dette
            ensligForsørger = utledEnsligForsørger(behandlingId),
            kategoriV2 = KategoriV2.valueOf(behandling.kategori.name),
            underkategoriV2 = null,
            behandlingTypeV2 = BehandlingTypeV2.valueOf(behandling.type.name),
            utbetalingsperioderV2 = hentUtbetalingsperioderTilDatavarehus(behandling, persongrunnlag),
            funksjonellId = UUID.randomUUID().toString(),
            kompetanseperioder = hentKompetanse(BehandlingId(behandlingId)),
            behandlingÅrsakV2 = BehandlingÅrsakV2.valueOf(behandling.opprettetÅrsak.name),
        )
    }

    private fun hentKompetanse(behandlingId: BehandlingId): List<Kompetanse> {
        val kompetanser = kompetanseService.hentKompetanser(behandlingId)

        return kompetanser.filter { it.resultat != null }.map { kompetanse ->
            Kompetanse(
                barnsIdenter = kompetanse.barnAktører.map { aktør -> aktør.aktivFødselsnummer() },
                annenForeldersAktivitet =
                    if (kompetanse.annenForeldersAktivitet != null) {
                        KompetanseAktivitet.valueOf(
                            kompetanse.annenForeldersAktivitet.name,
                        )
                    } else {
                        null
                    },
                annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland,
                barnetsBostedsland = kompetanse.barnetsBostedsland,
                fom = kompetanse.fom!!,
                tom = kompetanse.tom,
                resultat = KompetanseResultat.valueOf(kompetanse.resultat!!.name),
                sokersaktivitet = if (kompetanse.søkersAktivitet != null) KompetanseAktivitet.valueOf(kompetanse.søkersAktivitet.name) else null,
                sokersAktivitetsland = kompetanse.søkersAktivitetsland,
            )
        }
    }

    private fun hentSøkerV2(persongrunnlag: PersonopplysningGrunnlag): PersonDVHV2 {
        val søker = persongrunnlag.søker
        return lagPersonDVHV2(søker)
    }

    fun hentUtbetalingsperioderTilDatavarehus(
        behandling: Behandling,
        persongrunnlag: PersonopplysningGrunnlag,
    ): List<UtbetalingsperiodeDVHV2> {
        val andelerMedEndringer =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        if (andelerMedEndringer.isEmpty()) return emptyList()

        val utbetalingsPerioder =
            andelerMedEndringer
                .map { it.andel }
                .tilTidslinjerPerAktørOgType()
                .values
                .kombiner { it }
        val søkerOgBarn = persongrunnlag.søkerOgBarn

        return utbetalingsPerioder
            .tilPerioderIkkeNull()
            .map { periode ->
                mapTilUtbetalingsperiodeV2(
                    fom = periode.fom?.førsteDagIInneværendeMåned() ?: throw Feil("Fra og med-dato kan ikke være null"),
                    tom = periode.tom?.sisteDagIMåned() ?: throw Feil("Til og med-dato kan ikke være null"),
                    andelerForSegment = periode.verdi,
                    behandling = behandling,
                    søkerOgBarn = søkerOgBarn,
                )
            }
    }

    private fun utledEnsligForsørger(behandlingId: Long): Boolean {
        val andelerTilkjentYtelse =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)
        if (andelerTilkjentYtelse.isEmpty()) {
            return false
        }

        return andelerTilkjentYtelse.find { it.type == YtelseType.UTVIDET_BARNETRYGD } != null
    }

    private fun mapTilUtbetalingsperiodeV2(
        fom: LocalDate,
        tom: LocalDate,
        andelerForSegment: Iterable<AndelTilkjentYtelse>,
        behandling: Behandling,
        søkerOgBarn: List<Person>,
    ): UtbetalingsperiodeDVHV2 =
        UtbetalingsperiodeDVHV2(
            hjemmel = "Ikke implementert",
            stønadFom = fom,
            stønadTom = tom,
            utbetaltPerMnd = andelerForSegment.sumOf { it.kalkulertUtbetalingsbeløp },
            utbetalingsDetaljer =
                andelerForSegment.filter { it.erAndelSomSkalSendesTilOppdrag() }.map { andel ->
                    val personForAndel =
                        søkerOgBarn.find { person -> andel.aktør == person.aktør }
                            ?: throw Feil("Fant ikke personopplysningsgrunnlag for andel")
                    UtbetalingsDetaljDVHV2(
                        person =
                            lagPersonDVHV2(
                                personForAndel,
                                andel.prosent.intValueExact(),
                            ),
                        klassekode = andel.type.klassifisering,
                        ytelseType =
                            when (andel.type) {
                                YtelseType.ORDINÆR_BARNETRYGD -> ORDINÆR_BARNETRYGD
                                YtelseType.UTVIDET_BARNETRYGD -> UTVIDET_BARNETRYGD
                                YtelseType.SMÅBARNSTILLEGG -> SMÅBARNSTILLEGG
                                YtelseType.FINNMARKSTILLEGG -> FINNMARKSTILLEGG
                                YtelseType.SVALBARDTILLEGG -> SVALBARDTILLEGG
                            },
                        utbetaltPrMnd = andel.kalkulertUtbetalingsbeløp,
                        delytelseId = if (andel.periodeOffset != null) behandling.fagsak.id.toString() + andel.periodeOffset else null,
                    )
                },
        )

    private fun lagPersonDVHV2(
        person: Person,
        delingsProsentYtelse: Int = 0,
    ): PersonDVHV2 =
        PersonDVHV2(
            rolle = person.type.name,
            statsborgerskap = hentStatsborgerskap(person),
            bostedsland = hentLandkode(person),
            delingsprosentYtelse = if (delingsProsentYtelse == 50) delingsProsentYtelse else 0,
            personIdent = person.aktør.aktivFødselsnummer(),
        )

    private fun hentStatsborgerskap(person: Person): List<String> =
        if (person.statsborgerskap.isNotEmpty()) {
            person.statsborgerskap.filtrerGjeldendeNå().map { it.landkode }
        } else {
            listOf(personopplysningerService.hentGjeldendeStatsborgerskap(person.aktør).land)
        }

    private fun hentLandkode(person: Person): String =
        if (person.bostedsadresser.isNotEmpty()) {
            "NO"
        } else if (personopplysningerService.hentPersoninfoEnkel(person.aktør).bostedsadresser.isNotEmpty()) {
            "NO"
        } else {
            val landKode = personopplysningerService.hentLandkodeAlpha2UtenlandskBostedsadresse(person.aktør)

            if (landKode == PersonopplysningerService.UKJENT_LANDKODE) {
                logger.info("Sender landkode ukjent til DVH")
                secureLogger.info("Ukjent land sendt til DVH for person ${person.aktør.aktivFødselsnummer()}")
            }
            landKode
        }

    companion object {
        private val logger = LoggerFactory.getLogger(StønadsstatistikkService::class.java)
        private val TIMEZONE = ZoneId.of("Europe/Paris")
    }
}
