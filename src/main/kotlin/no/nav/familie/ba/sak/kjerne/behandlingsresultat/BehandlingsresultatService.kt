package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.BehandlingsresultatUtils.skalUtledeSøknadsresultatForBehandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class BehandlingsresultatService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val persongrunnlagService: PersongrunnlagService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingAndelHentOgPersisterService: EndretUtbetalingAndelHentOgPersisterService,
    private val kompetanseService: KompetanseService,
    private val clockProvider: ClockProvider,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
) {
    internal fun utledBehandlingsresultat(behandlingId: Long): Behandlingsresultat {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val forrigeBehandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = behandling.fagsak.id)

        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)

        val forrigeAndelerTilkjentYtelse = forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = it.id) } ?: emptyList()
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)

        val forrigeEndretUtbetalingAndeler = forrigeBehandling?.let { endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId = it.id) } ?: emptyList()
        val endretUtbetalingAndeler = endretUtbetalingAndelHentOgPersisterService.hentForBehandling(behandlingId = behandlingId)

        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = behandlingId)

        val personerIBehandling = persongrunnlagService.hentAktivThrows(behandlingId = behandling.id).personer.toSet()
        val personerIForrigeBehandling = forrigeBehandling?.let { persongrunnlagService.hentAktivThrows(behandlingId = forrigeBehandling.id).personer.toSet() } ?: emptySet()

        val personerFremstiltKravFor =
            søknadGrunnlagService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                forrigeBehandling = forrigeBehandling,
            )

        BehandlingsresultatValideringUtils.validerAtBarePersonerFremstiltKravForEllerSøkerHarFåttEksplisittAvslag(personerFremstiltKravFor = personerFremstiltKravFor, personResultater = vilkårsvurdering.personResultater)

        // 1 SØKNAD
        val søknadsresultat =
            if (skalUtledeSøknadsresultatForBehandling(behandling)) {
                BehandlingsresultatSøknadUtils.utledResultatPåSøknad(
                    nåværendeAndeler = andelerTilkjentYtelse,
                    forrigeAndeler = forrigeAndelerTilkjentYtelse,
                    endretUtbetalingAndeler = endretUtbetalingAndeler,
                    personerFremstiltKravFor = personerFremstiltKravFor,
                    nåværendePersonResultater = vilkårsvurdering.personResultater,
                    behandlingÅrsak = behandling.opprettetÅrsak,
                    finnesUregistrerteBarn = søknadGrunnlag?.hentUregistrerteBarn()?.isNotEmpty() ?: false,
                )
            } else {
                null
            }

        // 2 ENDRINGER
        val endringsresultat =
            if (forrigeBehandling != null) {
                val forrigeVilkårsvurdering = vilkårsvurderingService.hentAktivForBehandlingThrows(behandlingId = forrigeBehandling.id)
                val kompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(behandlingId))
                val forrigeKompetanser = kompetanseService.hentKompetanser(behandlingId = BehandlingId(forrigeBehandling.id))

                val utenlandskPeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(BehandlingId(behandlingId))
                val forrigeUtenlandskPeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(BehandlingId(forrigeBehandling.id))

                BehandlingsresultatEndringUtils.utledEndringsresultat(
                    nåværendeAndeler = andelerTilkjentYtelse,
                    forrigeAndeler = forrigeAndelerTilkjentYtelse,
                    nåværendeEndretAndeler = endretUtbetalingAndeler,
                    forrigeEndretAndeler = forrigeEndretUtbetalingAndeler,
                    nåværendePersonResultat = vilkårsvurdering.personResultater,
                    forrigePersonResultat = forrigeVilkårsvurdering.personResultater,
                    nåværendeKompetanser = kompetanser.toList(),
                    forrigeKompetanser = forrigeKompetanser.toList(),
                    nåværendeUtenlandskPeriodebeløp = utenlandskPeriodebeløp.toList(),
                    forrigeUtenlandskPeriodebeløp = forrigeUtenlandskPeriodebeløp.toList(),
                    personerFremstiltKravFor = personerFremstiltKravFor,
                    personerIBehandling = personerIBehandling,
                    personerIForrigeBehandling = personerIForrigeBehandling,
                    nåMåned = YearMonth.now(clockProvider.get()),
                )
            } else {
                Endringsresultat.INGEN_ENDRING
            }

        // 3 OPPHØR
        val opphørsresultat =
            BehandlingsresultatOpphørUtils.hentOpphørsresultatPåBehandling(
                nåværendeAndeler = andelerTilkjentYtelse,
                forrigeAndeler = forrigeAndelerTilkjentYtelse,
                nåværendeEndretAndeler = endretUtbetalingAndeler,
                forrigeEndretAndeler = forrigeEndretUtbetalingAndeler,
            )

        // KOMBINER
        val behandlingsresultat = BehandlingsresultatUtils.kombinerResultaterTilBehandlingsresultat(søknadsresultat, endringsresultat, opphørsresultat)

        return behandlingsresultat
    }
}
