package no.nav.familie.ba.sak.internal

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.integrasjoner.økonomi.UtbetalingsTidslinjeService
import no.nav.familie.ba.sak.integrasjoner.økonomi.Utbetalingstidslinje
import no.nav.familie.ba.sak.integrasjoner.økonomi.lagUtbetalingsperiode
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.OppdaterTilkjentYtelseService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random
import no.nav.familie.ba.sak.common.Periode as CommonPeriode

class ForvalterServiceTest {
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val økonomiService = mockk<ØkonomiService>()
    private val vedtakService = mockk<VedtakService>()
    private val beregningService = mockk<BeregningService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val tilkjentYtelseValideringService = mockk<TilkjentYtelseValideringService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val utbetalingsTidslinjeService = mockk<UtbetalingsTidslinjeService>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val oppdaterTilkjentYtelseService = mockk<OppdaterTilkjentYtelseService>()

    private val forvalterService =
        ForvalterService(
            økonomiService = økonomiService,
            vedtakService = vedtakService,
            beregningService = beregningService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            fagsakRepository = fagsakRepository,
            behandlingRepository = behandlingRepository,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            arbeidsfordelingService = arbeidsfordelingService,
            vilkårsvurderingService = vilkårsvurderingService,
            persongrunnlagService = persongrunnlagService,
            utbetalingsTidslinjeService = utbetalingsTidslinjeService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            oppdaterTilkjentYtelseService = oppdaterTilkjentYtelseService,
        )

    @Test
    fun `Skal endre periodeFom på vilkårresultat når den er før fødselsdato på person`() {
        val behandling = lagBehandling(status = BehandlingStatus.AVSLUTTET)

        val barnFødselsdato = LocalDate.now().minusYears(1).withDayOfMonth(15)
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = barnFødselsdato)

        val vilkårsvurderingSomSkalFlushes = slot<Vilkårsvurdering>()
        every { vilkårsvurderingService.oppdater(capture(vilkårsvurderingSomSkalFlushes)) } answers {
            if (vilkårsvurderingSomSkalFlushes.isCaptured) {
                vilkårsvurderingSomSkalFlushes.captured
            } else {
                throw Feil("Noe gikk feil ved capturing av vilkårsvurdering.")
            }
        }

        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(any()) } returns
            listOf(
                personTilPersonEnkel(barn),
                personTilPersonEnkel(søker),
            )

        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = behandling,
                søkerAktør = søker.aktør,
                resultat = Resultat.OPPFYLT,
            )
        val barnResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        barnResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.førsteDagIInneværendeMåned(), periodeTom = barn.fødselsdato.plusMonths(3), vilkårType = Vilkår.BOSATT_I_RIKET),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.plusMonths(4), vilkårType = Vilkår.BOSATT_I_RIKET),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.UNDER_18_ÅR),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, periodeTom = barnFødselsdato.plusMonths(2), vilkårType = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.plusMonths(3), vilkårType = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.LOVLIG_OPPHOLD),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.GIFT_PARTNERSKAP),
            ),
        )
        vilkårsvurdering.personResultater += barnResultat

        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns vilkårsvurdering

        val vilkårsvurderingEndret = forvalterService.settFomPåVilkårTilPersonsFødselsdato(behandling.id)
        vilkårsvurderingEndret.personResultater
            .singleOrNull { !it.erSøkersResultater() }
            ?.vilkårResultater
            ?.forEach { vilkårResultat ->
                assertTrue(vilkårResultat.periodeFom?.isSameOrAfter(barn.fødselsdato) ?: false)
            }

        val vilkårResultaterMedFomEtterBarnsFødselsDato =
            vilkårsvurderingEndret.personResultater
                .singleOrNull { !it.erSøkersResultater() }
                ?.vilkårResultater
                ?.filter { it.periodeFom?.isAfter(barn.fødselsdato) ?: false }
        assertThat(vilkårResultaterMedFomEtterBarnsFødselsDato?.size)
            .isEqualTo(2)
            .`as`("Vilkårresultater med fom etter barns fødselsdato har også blitt endret: $vilkårResultaterMedFomEtterBarnsFødselsDato")
    }

    @Test
    fun `Skal kaste feil når vi har etterfølgende perioder som begge begynner før fødselsdato`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.AVSLUTTET

        val barnFødselsdato = LocalDate.now().minusYears(1).withDayOfMonth(15)
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = barnFødselsdato)

        val vilkårsvurderingSomSkalFlushes = slot<Vilkårsvurdering>()
        every { vilkårsvurderingService.oppdater(capture(vilkårsvurderingSomSkalFlushes)) } answers {
            if (vilkårsvurderingSomSkalFlushes.isCaptured) {
                vilkårsvurderingSomSkalFlushes.captured
            } else {
                throw Feil("Noe gikk feil ved capturing av vilkårsvurdring.")
            }
        }

        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(any()) } returns
            listOf(
                personTilPersonEnkel(barn),
                personTilPersonEnkel(søker),
            )

        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = behandling,
                søkerAktør = søker.aktør,
                resultat = Resultat.OPPFYLT,
            )
        val barnResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        barnResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    barnResultat = barnResultat,
                    periodeFom = barn.fødselsdato.førsteDagIInneværendeMåned(),
                    periodeTom = barn.fødselsdato.minusDays(5),
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                ),
                lagVilkårResultat(
                    barnResultat = barnResultat,
                    periodeFom = barn.fødselsdato.minusDays(4),
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                ),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.UNDER_18_ÅR),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.LOVLIG_OPPHOLD),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.GIFT_PARTNERSKAP),
            ),
        )
        vilkårsvurdering.personResultater += barnResultat

        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns vilkårsvurdering

        assertThrows<Feil> {
            forvalterService.settFomPåVilkårTilPersonsFødselsdato(behandling.id)
        }
    }

    @Test
    fun `Skal kaste feil når vilkårResultat begynner tidligere måned enn fødselsdato`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.AVSLUTTET

        val barnFødselsdato = LocalDate.now().minusYears(1).withDayOfMonth(15)
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = barnFødselsdato)

        val vilkårsvurderingSomSkalFlushes = slot<Vilkårsvurdering>()
        every { vilkårsvurderingService.oppdater(capture(vilkårsvurderingSomSkalFlushes)) } answers {
            if (vilkårsvurderingSomSkalFlushes.isCaptured) {
                vilkårsvurderingSomSkalFlushes.captured
            } else {
                throw Feil("Noe gikk feil ved capturing av vilkårsvurdring.")
            }
        }

        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(any()) } returns
            listOf(
                personTilPersonEnkel(barn),
                personTilPersonEnkel(søker),
            )

        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = behandling,
                søkerAktør = søker.aktør,
                resultat = Resultat.OPPFYLT,
            )
        val barnResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        barnResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.minusMonths(2), vilkårType = Vilkår.BOSATT_I_RIKET),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.UNDER_18_ÅR),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.LOVLIG_OPPHOLD),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.GIFT_PARTNERSKAP),
            ),
        )
        vilkårsvurdering.personResultater += barnResultat

        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns vilkårsvurdering

        assertThrows<Feil> {
            forvalterService.settFomPåVilkårTilPersonsFødselsdato(behandling.id)
        }
    }

    @Test
    fun `Skal ikke endre på fom på under-18-vilkår`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.AVSLUTTET

        val barnFødselsdato = LocalDate.now().minusYears(1).withDayOfMonth(15)
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = barnFødselsdato)

        val vilkårsvurderingSomSkalFlushes = slot<Vilkårsvurdering>()
        every { vilkårsvurderingService.oppdater(capture(vilkårsvurderingSomSkalFlushes)) } answers {
            if (vilkårsvurderingSomSkalFlushes.isCaptured) {
                vilkårsvurderingSomSkalFlushes.captured
            } else {
                throw Feil("Noe gikk feil ved capturing av vilkårsvurdring.")
            }
        }

        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(any()) } returns
            listOf(
                personTilPersonEnkel(barn),
                personTilPersonEnkel(søker),
            )

        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = behandling,
                søkerAktør = søker.aktør,
                resultat = Resultat.OPPFYLT,
            )
        val barnResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        barnResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.førsteDagIInneværendeMåned(), vilkårType = Vilkår.BOSATT_I_RIKET),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.førsteDagIInneværendeMåned(), vilkårType = Vilkår.UNDER_18_ÅR),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.LOVLIG_OPPHOLD),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.GIFT_PARTNERSKAP),
            ),
        )
        vilkårsvurdering.personResultater += barnResultat

        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns vilkårsvurdering

        val vilkårsvurderingEndret = forvalterService.settFomPåVilkårTilPersonsFødselsdato(behandling.id)
        vilkårsvurderingEndret.personResultater
            .singleOrNull { !it.erSøkersResultater() }
            ?.vilkårResultater
            ?.forEach { vilkårResultat ->
                if (vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR) {
                    assertThat(vilkårResultat.periodeFom == barn.fødselsdato.førsteDagIInneværendeMåned())
                } else {
                    assertTrue(vilkårResultat.periodeFom?.isSameOrAfter(barn.fødselsdato) ?: false)
                }
            }
    }

    @Nested
    inner class FinnOgPatchAndelerTilkjentYtelseIFagsakerMedAvvik {
        @Test
        fun `skal finne og korrigere avvik i periodeId, forrigePeriodeId og kildeBehandlingId dersom dryRun er satt til false`() {
            // Arrange
            val fagsak = lagFagsak()
            val behandling = lagBehandling(fagsak = fagsak)
            val sisteIverksatteBehandling = lagBehandling(fagsak = fagsak)
            val barn = lagPerson(type = PersonType.BARN)

            // Perioder med utbetalinger i fagsak
            val periode1 =
                CommonPeriode(fom = YearMonth.of(2022, 7).førsteDagIInneværendeMåned(), tom = YearMonth.of(2024, 6).sisteDagIInneværendeMåned())
            val periode2 =
                CommonPeriode(fom = YearMonth.of(2024, 7).førsteDagIInneværendeMåned(), tom = YearMonth.of(2026, 2).sisteDagIInneværendeMåned())
            val periode3 =
                CommonPeriode(fom = YearMonth.of(2026, 3).førsteDagIInneværendeMåned(), tom = YearMonth.of(2035, 2).sisteDagIInneværendeMåned())

            // Andeler i sisteIversatteBehandling med feil periodeId, forrigePeriodeId og kildeBehandlingsId for de 2 siste andelene
            val andelTilkjentYtelse1 = lagAndelTilkjentYtelse(id = 1, fom = periode1.fom.toYearMonth(), tom = periode1.tom.toYearMonth(), aktør = barn.aktør, periodeIdOffset = 0, forrigeperiodeIdOffset = null, kildeBehandlingId = behandling.id)
            val andelTilkjentYtelse2 = lagAndelTilkjentYtelse(id = 2, fom = periode2.fom.toYearMonth(), tom = periode2.tom.toYearMonth(), aktør = barn.aktør, periodeIdOffset = 1, forrigeperiodeIdOffset = 0, kildeBehandlingId = behandling.id)
            val andelTilkjentYtelse3 = lagAndelTilkjentYtelse(id = 3, fom = periode3.fom.toYearMonth(), tom = periode3.tom.toYearMonth(), aktør = barn.aktør, periodeIdOffset = 2, forrigeperiodeIdOffset = 1, kildeBehandlingId = behandling.id)

            // Tilkjent ytelse i sisteIverksatteBehandling
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = sisteIverksatteBehandling, lagAndelerTilkjentYtelse = {
                    setOf(
                        andelTilkjentYtelse1,
                        andelTilkjentYtelse2,
                        andelTilkjentYtelse3,
                    )
                })

            // Alle utbetalingsperioder sendt til Oppdrag
            val utbetalingsperiode1 =
                lagUtbetalingsperiode(
                    fom = periode1.fom,
                    tom = periode1.tom,
                    periodeId = 0,
                    forrigePeriodeId = null,
                    behandlingId = behandling.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal(1766),
                    opphør = null,
                )

            val utbetalingsperiode2 =
                lagUtbetalingsperiode(
                    fom = periode2.fom,
                    tom = periode2.tom,
                    periodeId = 1,
                    forrigePeriodeId = 0,
                    behandlingId = behandling.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal(1766),
                    opphør = null,
                )

            val utbetalingsperiode3 =
                lagUtbetalingsperiode(
                    fom = periode3.fom,
                    tom = periode3.tom,
                    periodeId = 2,
                    forrigePeriodeId = 1,
                    behandlingId = behandling.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal(1766),
                    opphør = null,
                )

            // Perioder oppdatert i sisteIverksatteBehandling
            val utbetalingsperiode4 =
                lagUtbetalingsperiode(
                    fom = periode2.fom,
                    tom = periode2.tom,
                    periodeId = 3,
                    forrigePeriodeId = 2,
                    behandlingId = sisteIverksatteBehandling.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal(1766),
                    opphør = null,
                )

            val utbetalingsperiode5 =
                lagUtbetalingsperiode(
                    fom = periode3.fom,
                    tom = periode3.tom,
                    periodeId = 4,
                    forrigePeriodeId = 3,
                    behandlingId = sisteIverksatteBehandling.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal(1766),
                    opphør = null,
                )

            // Utbetalingstidslinjer i fagsak
            val utbetalingstidslinjer: List<Utbetalingstidslinje> =
                listOf(
                    Utbetalingstidslinje(
                        utbetalingsperioder = setOf(utbetalingsperiode1, utbetalingsperiode2, utbetalingsperiode3, utbetalingsperiode4, utbetalingsperiode5),
                        tidslinje =
                            listOf(
                                Periode(
                                    verdi = utbetalingsperiode1,
                                    fom = periode1.fom,
                                    tom = periode1.tom,
                                ),
                                Periode(
                                    verdi = utbetalingsperiode4,
                                    fom = periode2.fom,
                                    tom = periode2.tom,
                                ),
                                Periode(
                                    verdi = utbetalingsperiode5,
                                    fom = periode3.fom,
                                    tom = periode3.tom,
                                ),
                            ).tilTidslinje(),
                    ),
                )

            val fagsaker = setOf(fagsak.id)
            val korrigerAndelerFraOgMedDato = LocalDate.of(2025, 2, 1)

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsak.id) } returns sisteIverksatteBehandling
            every { tilkjentYtelseRepository.findByBehandling(behandlingId = sisteIverksatteBehandling.id) } returns tilkjentYtelse
            every { utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsak.id) } returns utbetalingstidslinjer
            every { oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedKorrigerteAndeler(any(), any()) } just Runs

            // Act
            val fagsakerMedTilhørendeAndelTilkjentYtelseKorreksjon =
                forvalterService.finnOgPatchAndelerTilkjentYtelseIFagsakerMedAvvik(
                    fagsaker = fagsaker,
                    korrigerAndelerFraOgMedDato = korrigerAndelerFraOgMedDato,
                    dryRun = false,
                )

            // Assert
            assertThat(fagsakerMedTilhørendeAndelTilkjentYtelseKorreksjon).hasSize(1)
            verify(exactly = 1) { oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedKorrigerteAndeler(any(), any()) }

            val andelTilkjentYtelseKorreksjonerForFagsak = fagsakerMedTilhørendeAndelTilkjentYtelseKorreksjon.first()
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.first).isEqualTo(fagsak.id)
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.second).hasSize(2)
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.second?.map { it.korrigertAndel.periodeId }).containsExactlyInAnyOrder(3, 4)
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.second?.map { it.korrigertAndel.forrigePeriodeId }).containsExactlyInAnyOrder(2, 3)
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.second?.map { it.korrigertAndel.kildeBehandlingId }?.toSet()).containsExactlyInAnyOrder(sisteIverksatteBehandling.id)
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.second?.map { it.andelMedFeil.id }).containsExactlyInAnyOrder(andelTilkjentYtelse2.id, andelTilkjentYtelse3.id)
        }

        @Test
        fun `skal finne avvik i periodeId, forrigePeriodeId og kildeBehandlingId dersom dryRun er satt til true`() {
            // Arrange
            val fagsak = lagFagsak()
            val behandling = lagBehandling(fagsak = fagsak)
            val sisteIverksatteBehandling = lagBehandling(fagsak = fagsak)
            val barn = lagPerson(type = PersonType.BARN)

            // Perioder med utbetalinger i fagsak
            val periode1 =
                CommonPeriode(fom = YearMonth.of(2022, 7).førsteDagIInneværendeMåned(), tom = YearMonth.of(2024, 6).sisteDagIInneværendeMåned())
            val periode2 =
                CommonPeriode(fom = YearMonth.of(2024, 7).førsteDagIInneværendeMåned(), tom = YearMonth.of(2026, 2).sisteDagIInneværendeMåned())
            val periode3 =
                CommonPeriode(fom = YearMonth.of(2026, 3).førsteDagIInneværendeMåned(), tom = YearMonth.of(2035, 2).sisteDagIInneværendeMåned())

            // Andeler i sisteIversatteBehandling med feil periodeId, forrigePeriodeId og kildeBehandlingsId for de 2 siste andelene
            val andelTilkjentYtelse1 = lagAndelTilkjentYtelse(id = 1, fom = periode1.fom.toYearMonth(), tom = periode1.tom.toYearMonth(), aktør = barn.aktør, periodeIdOffset = 0, forrigeperiodeIdOffset = null, kildeBehandlingId = behandling.id)
            val andelTilkjentYtelse2 = lagAndelTilkjentYtelse(id = 2, fom = periode2.fom.toYearMonth(), tom = periode2.tom.toYearMonth(), aktør = barn.aktør, periodeIdOffset = 1, forrigeperiodeIdOffset = 0, kildeBehandlingId = behandling.id)
            val andelTilkjentYtelse3 = lagAndelTilkjentYtelse(id = 3, fom = periode3.fom.toYearMonth(), tom = periode3.tom.toYearMonth(), aktør = barn.aktør, periodeIdOffset = 2, forrigeperiodeIdOffset = 1, kildeBehandlingId = behandling.id)

            // Tilkjent ytelse i sisteIverksatteBehandling
            val tilkjentYtelse =
                lagTilkjentYtelse(behandling = sisteIverksatteBehandling, lagAndelerTilkjentYtelse = {
                    setOf(
                        andelTilkjentYtelse1,
                        andelTilkjentYtelse2,
                        andelTilkjentYtelse3,
                    )
                })

            // Alle utbetalingsperioder sendt til Oppdrag
            val utbetalingsperiode1 =
                lagUtbetalingsperiode(
                    fom = periode1.fom,
                    tom = periode1.tom,
                    periodeId = 0,
                    forrigePeriodeId = null,
                    behandlingId = behandling.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal(1766),
                    opphør = null,
                )

            val utbetalingsperiode2 =
                lagUtbetalingsperiode(
                    fom = periode2.fom,
                    tom = periode2.tom,
                    periodeId = 1,
                    forrigePeriodeId = 0,
                    behandlingId = behandling.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal(1766),
                    opphør = null,
                )

            val utbetalingsperiode3 =
                lagUtbetalingsperiode(
                    fom = periode3.fom,
                    tom = periode3.tom,
                    periodeId = 2,
                    forrigePeriodeId = 1,
                    behandlingId = behandling.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal(1766),
                    opphør = null,
                )

            // Perioder oppdatert i sisteIverksatteBehandling
            val utbetalingsperiode4 =
                lagUtbetalingsperiode(
                    fom = periode2.fom,
                    tom = periode2.tom,
                    periodeId = 3,
                    forrigePeriodeId = 2,
                    behandlingId = sisteIverksatteBehandling.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal(1766),
                    opphør = null,
                )

            val utbetalingsperiode5 =
                lagUtbetalingsperiode(
                    fom = periode3.fom,
                    tom = periode3.tom,
                    periodeId = 4,
                    forrigePeriodeId = 3,
                    behandlingId = sisteIverksatteBehandling.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal(1766),
                    opphør = null,
                )

            // Utbetalingstidslinjer i fagsak
            val utbetalingstidslinjer: List<Utbetalingstidslinje> =
                listOf(
                    Utbetalingstidslinje(
                        utbetalingsperioder = setOf(utbetalingsperiode1, utbetalingsperiode2, utbetalingsperiode3, utbetalingsperiode4, utbetalingsperiode5),
                        tidslinje =
                            listOf(
                                Periode(
                                    verdi = utbetalingsperiode1,
                                    fom = periode1.fom,
                                    tom = periode1.tom,
                                ),
                                Periode(
                                    verdi = utbetalingsperiode4,
                                    fom = periode2.fom,
                                    tom = periode2.tom,
                                ),
                                Periode(
                                    verdi = utbetalingsperiode5,
                                    fom = periode3.fom,
                                    tom = periode3.tom,
                                ),
                            ).tilTidslinje(),
                    ),
                )

            val fagsaker = setOf(fagsak.id)
            val korrigerAndelerFraOgMedDato = LocalDate.of(2025, 2, 1)

            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsakId = fagsak.id) } returns sisteIverksatteBehandling
            every { tilkjentYtelseRepository.findByBehandling(behandlingId = sisteIverksatteBehandling.id) } returns tilkjentYtelse
            every { utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsak.id) } returns utbetalingstidslinjer

            // Act
            val fagsakerMedTilhørendeAndelTilkjentYtelseKorreksjon =
                forvalterService.finnOgPatchAndelerTilkjentYtelseIFagsakerMedAvvik(
                    fagsaker = fagsaker,
                    korrigerAndelerFraOgMedDato = korrigerAndelerFraOgMedDato,
                    dryRun = true,
                )

            // Assert
            assertThat(fagsakerMedTilhørendeAndelTilkjentYtelseKorreksjon).hasSize(1)
            verify(exactly = 0) { oppdaterTilkjentYtelseService.oppdaterTilkjentYtelseMedKorrigerteAndeler(any(), any()) }

            val andelTilkjentYtelseKorreksjonerForFagsak = fagsakerMedTilhørendeAndelTilkjentYtelseKorreksjon.first()
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.first).isEqualTo(fagsak.id)
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.second).hasSize(2)
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.second?.map { it.korrigertAndel.periodeId }).containsExactlyInAnyOrder(3, 4)
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.second?.map { it.korrigertAndel.forrigePeriodeId }).containsExactlyInAnyOrder(2, 3)
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.second?.map { it.korrigertAndel.kildeBehandlingId }?.toSet()).containsExactlyInAnyOrder(sisteIverksatteBehandling.id)
            assertThat(andelTilkjentYtelseKorreksjonerForFagsak.second?.map { it.andelMedFeil.id }).containsExactlyInAnyOrder(andelTilkjentYtelse2.id, andelTilkjentYtelse3.id)
        }
    }

    private fun personTilPersonEnkel(barn: Person) =
        PersonEnkel(
            type = PersonType.BARN,
            aktør = barn.aktør,
            fødselsdato = barn.fødselsdato,
            dødsfallDato = null,
            målform = Målform.NB,
        )

    private fun lagVilkårResultat(
        vilkårType: Vilkår,
        barnResultat: PersonResultat,
        periodeFom: LocalDate,
        periodeTom: LocalDate? =
            if (vilkårType == Vilkår.UNDER_18_ÅR) {
                periodeFom.plusYears(18).minusMonths(1)
            } else {
                null
            },
    ): VilkårResultat =
        VilkårResultat(
            personResultat = barnResultat,
            vilkårType = vilkårType,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            begrunnelse = "",
            sistEndretIBehandlingId = Random.nextLong(),
        )
}
