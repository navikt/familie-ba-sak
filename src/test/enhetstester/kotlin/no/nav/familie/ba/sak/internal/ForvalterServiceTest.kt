package no.nav.familie.ba.sak.internal

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.random.Random

class ForvalterServiceTest {
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val økonomiService = mockk<ØkonomiService>()
    private val vedtakService = mockk<VedtakService>()
    private val beregningService = mockk<BeregningService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val tilkjentYtelseValideringService = mockk<TilkjentYtelseValideringService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()

    private val forvalterService =
        ForvalterService(
            økonomiService = økonomiService,
            vedtakService = vedtakService,
            beregningService = beregningService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            fagsakRepository = fagsakRepository,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            arbeidsfordelingService = arbeidsfordelingService,
            vilkårsvurderingService = vilkårsvurderingService,
            persongrunnlagService = persongrunnlagService,
            aktørIdRepository = mockk(relaxed = true),
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
    inner class EndreFagsakStatusTilOpprettetTest {
        @Test
        fun `skal endre fagsakstatus fra LØPENDE til OPPRETTET når alle behandlinger er henlagt og avsluttet`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LØPENDE)
            val henlagtBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE)

            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak
            every { behandlingHentOgPersisterService.hentBehandlinger(fagsak.id) } returns listOf(henlagtBehandling)
            every { fagsakRepository.save(any()) } returns fagsak

            // Act
            forvalterService.`endreFagsakStatusFraLøpendeTilOprettet`(fagsak.id)

            // Assert
            assertThat(fagsak.status).isEqualTo(FagsakStatus.OPPRETTET)
        }

        @Test
        fun `skal kaste feil når fagsak ikke har status løpende`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.AVSLUTTET)

            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            val exception =
                assertThrows<Feil> {
                    forvalterService.`endreFagsakStatusFraLøpendeTilOprettet`(fagsak.id)
                }
            assertThat(exception.message).contains("Fagsak ${fagsak.id} har status ${fagsak.status}. Kan bare endre fra LØPENDE til OPPRETTET.")
        }

        @Test
        fun `Skal kaste feil når det finnes vedtatte behandlinger`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LØPENDE)
            val vedtattBehandling =
                lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.INNVILGET)
            val henlagtBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, resultat = Behandlingsresultat.HENLAGT_AUTOMATISK_FØDSELSHENDELSE)

            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak
            every { behandlingHentOgPersisterService.hentBehandlinger(fagsak.id) } returns listOf(vedtattBehandling, henlagtBehandling)

            // Act & Assert
            val exception =
                assertThrows<Feil> {
                    forvalterService.`endreFagsakStatusFraLøpendeTilOprettet`(fagsak.id)
                }
            assertThat(exception.message).contains("Fagsak ${fagsak.id} har behandlinger som ikke er henlagt og status kan ikke endres.")
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
