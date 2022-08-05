package no.nav.familie.ba.sak.kjerne.endretutbetaling

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.ekstern.restDomene.RestEndretUtbetalingAndel
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.Ressurs
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

class EndretUtbetalingAndelControllerTest(
    @Autowired
    private val endretUtbetalingAndelController: EndretUtbetalingAndelController,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,

    @Autowired
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,

    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,

    @Autowired
    private val transactionManager: PlatformTransactionManager
) : AbstractSpringIntegrationTest() {

    @Test
    @Tag("integration")
    fun `ved endring av periode blir tilknyttet andeler-flagget oppdatert`() {
        val barnFnr = randomFnr()
        val fnr = randomFnr()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(fnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        settOppForÅEndreUtbetalingAndel(barnFnr, behandling, fagsak, fnr)

        val endretUtbetalingAndel =
            endretUtbetalingAndelController.lagreEndretUtbetalingAndelOgOppdaterTilkjentYtelse(behandling.id)

        val response = TransactionTemplate(transactionManager).execute {
            oppdaterEndretAndel(
                behandling,
                endretUtbetalingAndel,
                barnFnr
            )
        }
        assertTrue(response?.body?.data?.endretUtbetalingAndeler?.first()?.erTilknyttetAndeler!!)
    }

    private fun settOppForÅEndreUtbetalingAndel(
        barnFnr: String,
        behandling: Behandling,
        fagsak: Fagsak,
        fnr: String
    ) {
        val stønadFom = LocalDate.of(2015, Month.JANUARY, 1)
        val barnAktør = personidentService.hentOgLagreAktør(barnFnr, true)

        vilkårsvurderingService.lagreNyOgDeaktiverGammel(
            vilkårsvurdering = lagVilkårsvurdering(
                behandling,
                fagsak.aktør,
                barnAktør,
                stønadFom = stønadFom,
                stønadTom = stønadFom.plusYears(17)
            )
        )

        personopplysningGrunnlagRepository.save(
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                fnr,
                listOf(barnFnr),
                barnFødselsdato = stønadFom,
                søkerAktør = fagsak.aktør,
                barnAktør = listOf(barnAktør)
            )
        )

        behandlingService.opprettOgInitierNyttVedtakForBehandling(behandling = behandling)

        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = stønadFom,
            endretDato = LocalDate.now(),
            stønadFom = stønadFom.toYearMonth()
        ).also { tilkjentYtelseRepository.save(it) }

        andelTilkjentYtelseRepository.save(
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                tilkjentYtelse = tilkjentYtelse,
                aktør = barnAktør,
                kalkulertUtbetalingsbeløp = 1054,
                nasjonaltPeriodebeløp = 1054,
                sats = 123,
                stønadFom = YearMonth.of(2015, Month.JUNE),
                stønadTom = YearMonth.now().plusYears(5),
                prosent = BigDecimal(2)
            )
        )
    }

    fun oppdaterEndretAndel(
        behandling: Behandling,
        endretUtbetalingAndel: ResponseEntity<Ressurs<RestUtvidetBehandling>>,
        barnFnr: String
    ) = endretUtbetalingAndelController.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(
        behandling.id,
        endretUtbetalingAndel.body?.data?.endretUtbetalingAndeler?.first()?.id!!,
        restEndretUtbetalingAndel = RestEndretUtbetalingAndel(
            id = 3L,
            fom = YearMonth.of(2015, 6),
            tom = YearMonth.of(2018, 6),
            erTilknyttetAndeler = false,
            avtaletidspunktDeltBosted = null,
            begrunnelse = "test",
            personIdent = barnFnr,
            prosent = BigDecimal.ZERO,
            årsak = Årsak.ETTERBETALING_3ÅR,
            søknadstidspunkt = LocalDate.now()
        )
    )

    private fun lagVilkårsvurdering(
        behandling: Behandling,
        søkerAktør: Aktør,
        barnAktør: Aktør,
        stønadFom: LocalDate,
        stønadTom: LocalDate
    ): Vilkårsvurdering {
        val vilkårsvurdering =
            Vilkårsvurdering(behandling = behandling)
        vilkårsvurdering.personResultater = setOf(
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
                aktør = barnAktør,
                resultat = Resultat.OPPFYLT,
                periodeFom = stønadFom,
                periodeTom = stønadTom,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN
            )
        )
        return vilkårsvurdering
    }
}
