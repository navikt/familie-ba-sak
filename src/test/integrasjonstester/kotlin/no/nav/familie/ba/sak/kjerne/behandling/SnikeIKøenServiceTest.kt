package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.MockPersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagVedtak
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SnikeIKøenServiceTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired private val hentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val snikeIKøenService: SnikeIKøenService,
    @Autowired private val settPåVentService: SettPåVentService,
    @Autowired private val vedtakRepository: VedtakRepository,
    @Autowired private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    @Autowired private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository,
    @Autowired private val behandlingService: BehandlingService,
) : AbstractSpringIntegrationTest() {
    private fun opprettLøpendeFagsak(): Fagsak {
        val søkerFnr = leggTilPersonInfo(fødselsdato = randomSøkerFødselsdato())
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        return fagsakService.lagre(fagsak.copy(status = FagsakStatus.LØPENDE))
    }

    @ParameterizedTest
    @EnumSource(BehandlingStatus::class, names = ["UTREDES", "SATT_PÅ_VENT"], mode = EnumSource.Mode.INCLUDE)
    fun `skal kunne sette en behandling med status UTREDES eller SATT_PÅ_VENT på maskinell vent`(status: BehandlingStatus) {
        val fagsak = opprettLøpendeFagsak()
        val behandling = opprettBehandling(status = status, fagsak = fagsak)

        settAktivBehandlingTilPåMaskinellVent(behandling)

        val oppdatertBehandling = behandlingRepository.finnBehandling(behandling.id)
        assertThat(behandling.status).isEqualTo(status)
        assertThat(behandling.aktiv).isTrue()
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
        assertThat(oppdatertBehandling.aktiv).isFalse()
    }

    @Test
    fun `reaktivering av behandling skal sette status tilbake til UTREDES`() {
        val fagsak = opprettLøpendeFagsak()
        val behandling1 = opprettBehandling(fagsak = fagsak)
        settAktivBehandlingTilPåMaskinellVent(behandling1)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertBehandling = behandlingRepository.finnBehandling(behandling1.id)
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.UTREDES)
        assertThat(oppdatertBehandling.aktiv).isTrue()
    }

    @Test
    fun `reaktivering av behandling som er på vent skal sette status tilbake til SATT_PÅ_VENT`() {
        val fagsak = opprettLøpendeFagsak()
        val behandling1 = opprettBehandling(fagsak = fagsak)
        lagreArbeidsfordeling(behandling1)
        settPåVentService.settBehandlingPåVent(behandling1.id, LocalDate.now(), SettPåVentÅrsak.AVVENTER_DOKUMENTASJON)
        settAktivBehandlingTilPåMaskinellVent(behandling1)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertBehandling = behandlingRepository.finnBehandling(behandling1.id)
        assertThat(oppdatertBehandling.status).isEqualTo(BehandlingStatus.SATT_PÅ_VENT)
        assertThat(oppdatertBehandling.aktiv).isTrue()
    }

    @Test
    fun `siste behandlingen er den som er aktiv til at behandlingen som er satt på vent er aktivert på nytt`() {
        val fagsak = opprettLøpendeFagsak()
        opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false, fagsak = fagsak)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        validerSisteBehandling(fagsak = fagsak, behandling = behandling2)
        validerErAktivBehandling(behandling = behandling2)
    }

    @Test
    fun `behandling som er satt på vent blir aktivert, men ennå ikke iverksatt, og er då siste aktive behandlingen`() {
        val fagsak = opprettLøpendeFagsak()
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false, fagsak = fagsak)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        validerSisteBehandling(fagsak = fagsak, behandling = behandling2)
        validerErAktivBehandling(behandling1)
    }

    @Test
    fun `behandling som er satt på vent blir aktivert og iverksatt, og er då siste aktive behandlingen`() {
        val fagsak = opprettLøpendeFagsak()
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false, fagsak = fagsak)
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)
        lagUtbetalingsoppdragOgAvslutt(behandling1)

        validerSisteBehandling(behandling = behandling1, fagsak = fagsak)
        validerErAktivBehandling(behandling1)
    }

    @Test
    fun `reaktivering skal tilbakestille behandling på vent`() {
        val fagsak = opprettLøpendeFagsak()
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false, fagsak = fagsak)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling1.id, StegType.VILKÅRSVURDERING)
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling1.id, StegType.VURDER_TILBAKEKREVING)

        val vedtak = vedtakRepository.saveAndFlush(lagVedtak(id = 0, behandling = behandling1))
        vedtaksperiodeHentOgPersisterService.lagre(
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            ),
        )
        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        assertThat(vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtak.id)).isEmpty()
    }

    @Test
    fun `reaktivering skal tilbakestille til vilkårsvurdering kun dersom steget er lagt til på behandlingen`() {
        val fagsak = opprettLøpendeFagsak()
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false, fagsak = fagsak)
        lagreArbeidsfordeling(behandling1)

        val initielStegTilstand = StegType.REGISTRERE_SØKNAD
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling1.id, initielStegTilstand)

        val behandling2 = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = true, fagsak = fagsak)
        lagUtbetalingsoppdragOgAvslutt(behandling2)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertBehandling = behandlingRepository.finnBehandling(behandling1.id)
        assertThat(oppdatertBehandling.steg).isEqualTo(initielStegTilstand)

        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(oppdatertBehandling.id, StegType.VILKÅRSVURDERING)
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(oppdatertBehandling.id, StegType.BEHANDLINGSRESULTAT)

        val oppdatertBehandlingEtterNyeSteg = behandlingRepository.finnBehandling(behandling1.id)

        settBehandlingTilPåMaskinellVent(oppdatertBehandlingEtterNyeSteg)

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        val oppdatertStegTilstand = behandlingRepository.finnBehandling(behandling1.id).behandlingStegTilstand.last()

        assertThat(oppdatertStegTilstand.behandlingSteg).isEqualTo(StegType.VILKÅRSVURDERING)
        assertThat(oppdatertStegTilstand.behandlingStegStatus).isEqualTo(BehandlingStegStatus.IKKE_UTFØRT)
    }

    private fun settBehandlingTilPåMaskinellVent(oppdatertBehandling: Behandling) {
        oppdatertBehandling.aktiv = false
        oppdatertBehandling.status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT
        behandlingRepository.saveAndFlush(oppdatertBehandling)
    }

    @Test
    fun `skal ikke reaktivere noe hvis det ikke finnes en behandling som er på maskinell vent`() {
        val fagsak = opprettLøpendeFagsak()
        val behandling2 =
            opprettBehandling(
                status = BehandlingStatus.AVSLUTTET,
                aktiv = true,
                fagsak = fagsak,
            )

        assertThat(snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)).isFalse()
        assertThat(behandlingRepository.finnBehandling(behandling2.id).aktiv).isTrue()
    }

    @Test
    fun `skal kunne reaktivere en behandling selv om det ikke finnes en annen behandling som er aktiv, eks henlagt`() {
        val fagsak = opprettLøpendeFagsak()
        val behandling1 = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false, fagsak = fagsak)
        val behandling2 =
            opprettBehandling(
                status = BehandlingStatus.AVSLUTTET,
                aktiv = false,
                resultat = Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET,
                fagsak = fagsak,
            )

        snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandling2)

        assertThat(behandlingRepository.finnBehandling(behandling1.id).aktiv).isTrue()
        assertThat(behandlingRepository.finnBehandling(behandling2.id).aktiv).isFalse()
    }

    @Nested
    inner class ValideringAvSettPåVent {
        @Test
        fun `kan ikke sette en inaktiv behandling på vent`() {
            val fagsak = opprettLøpendeFagsak()
            val behandling = opprettBehandling(aktiv = false, fagsak = fagsak)

            assertThatThrownBy {
                settAktivBehandlingTilPåMaskinellVent(behandling)
            }.hasMessageContaining("er ikke aktiv")
        }

        @ParameterizedTest
        @EnumSource(BehandlingStatus::class, names = ["UTREDES", "SATT_PÅ_VENT"], mode = EnumSource.Mode.EXCLUDE)
        fun `kan ikke sette en behandling på vent med annen status enn UTREDES eller SATT_PÅ_VENT`(status: BehandlingStatus) {
            val fagsak = opprettLøpendeFagsak()
            val behandling = opprettBehandling(status = status, fagsak = fagsak)

            assertThatThrownBy {
                settAktivBehandlingTilPåMaskinellVent(behandling)
            }.hasMessageContaining("kan ikke settes på maskinell vent då status")
        }
    }

    @Nested
    inner class ValideringAvReaktiverBehandling {
        @Suppress("UNUSED_VARIABLE")
        @Test
        fun `skal feile når åpen behandling er aktiv`() {
            val fagsak = opprettLøpendeFagsak()
            val behandlingPåVent = opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = true, fagsak = fagsak)
            val behandlingSomSnekIKøen = opprettBehandling(status = BehandlingStatus.AVSLUTTET, aktiv = false, fagsak = fagsak)

            assertThatThrownBy { snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomSnekIKøen) }
                .hasMessageContaining("Åpen behandling har feil tilstand")
        }

        @Test
        fun `skal feile når behandling som snek i køen har status satt på vent`() {
            val fagsak = opprettLøpendeFagsak()
            opprettBehandling(status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT, aktiv = false, fagsak = fagsak)
            val behandlingSomSnekIKøen = opprettBehandling(status = BehandlingStatus.UTREDES, aktiv = true, fagsak = fagsak)

            assertThatThrownBy { snikeIKøenService.reaktiverBehandlingPåMaskinellVent(behandlingSomSnekIKøen) }
                .hasMessageContaining("er ikke avsluttet")
        }
    }

    private fun settAktivBehandlingTilPåMaskinellVent(behandling: Behandling) {
        snikeIKøenService.settAktivBehandlingPåMaskinellVent(behandling.id, SettPåMaskinellVentÅrsak.SATSENDRING)
    }

    private fun validerSisteBehandling(
        fagsak: Fagsak,
        behandling: Behandling,
    ) {
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)!!.id).isEqualTo(behandling.id)
        assertThat(behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker()).contains(behandling.id)
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)!!.id).isEqualTo(behandling.id)

        assertThat(hentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id)!!.id).isEqualTo(behandling.id)
        assertThat(hentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id)!!.id).isEqualTo(behandling.id)
        assertThat(
            hentOgPersisterService.hentSisteBehandlingSomErSendtTilØkonomiPerFagsak(setOf(fagsak.id)).single().id,
        ).isEqualTo(behandling.id)
    }

    private fun validerErAktivBehandling(behandling: Behandling) {
        assertThat(hentOgPersisterService.finnAktivForFagsak(behandling.fagsak.id)!!.id)
            .isEqualTo(behandling.id)
    }

    private fun lagUtbetalingsoppdragOgAvslutt(behandling: Behandling) {
        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling, utbetalingsoppdrag = "utbetalingsoppdrag")
        tilkjentYtelseRepository.saveAndFlush(tilkjentYtelse)
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandling.id, StegType.BEHANDLING_AVSLUTTET)
        behandlingRepository.finnBehandling(behandling.id).let { behandlingFraDb ->
            behandlingFraDb.status = BehandlingStatus.AVSLUTTET
            behandlingRepository.saveAndFlush(behandlingFraDb)
        }
    }

    private fun opprettBehandling(
        fagsak: Fagsak,
        status: BehandlingStatus = BehandlingStatus.UTREDES,
        resultat: Behandlingsresultat = Behandlingsresultat.INNVILGET,
        aktiv: Boolean = true,
    ): Behandling {
        val behandling =
            Behandling(
                fagsak = fagsak,
                opprettetÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                type = BehandlingType.REVURDERING,
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                status = status,
                aktiv = aktiv,
                resultat = resultat,
            ).initBehandlingStegTilstand()
        return behandlingRepository.saveAndFlush(behandling)
    }

    private fun lagreArbeidsfordeling(behandling1: Behandling) {
        val arbeidsfordelingPåBehandling =
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling1.id,
                behandlendeEnhetId = "4820",
                behandlendeEnhetNavn = "Enhet",
            )
        arbeidsfordelingPåBehandlingRepository.saveAndFlush(arbeidsfordelingPåBehandling)
    }
}
