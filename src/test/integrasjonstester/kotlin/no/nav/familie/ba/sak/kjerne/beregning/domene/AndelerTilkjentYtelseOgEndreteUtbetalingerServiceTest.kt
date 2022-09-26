package no.nav.familie.ba.sak.kjerne.beregning.domene

import io.mockk.every
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.TilkjentYtelseTestController
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingTestController
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class AndelerTilkjentYtelseOgEndreteUtbetalingerServiceTest : AbstractSpringIntegrationTest() {

    @Autowired
    lateinit var endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository

    @Autowired
    lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @Autowired
    lateinit var vilkårsvurderingTestController: VilkårsvurderingTestController

    @Autowired
    lateinit var tilkjentYtelseTestController: TilkjentYtelseTestController

    @Autowired
    lateinit var featureToggleService: FeatureToggleService

    @Autowired
    lateinit var andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService

    @BeforeEach
    fun init() {
    }

    @Test
    fun `Skal gjenopprette DB-koblinger mellom andeler og endringer når sikkerhetsnettet er på`() {
        // Skru PÅ virtuell kobling mellom andeler og endringer
        every { featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER) } returns true
        // Skru PÅ sikkerhetsnettet som skaper DB-koblinger fra virtuelle koblinger (false betyr MED sikkerhetsnett)
        every { featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER_UTEN_SIKKERHETSNETT) } returns false

        // Opprett tilkjent ytelse med 3 andeler tilkjent ytelse, hver koblet til en endret utbetaling i databasen
        val behandlingId = opprettTillkjentYtelseMedDeltBosted()

        val andelerTilkjentYtelseMedEndreteUtbetalinger = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        val endretUtbetalingAndelMedAndelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId)

        // Sjekk at virtuelle toveis-koblinger finnes
        andelerTilkjentYtelseMedEndreteUtbetalinger
            .also { assertEquals(3, it.size) }
            .forEach { assertEquals(1, it.endreteUtbetalinger.size) }
        assertEquals(3, endretUtbetalingAndelMedAndelerTilkjentYtelse.size)

        endretUtbetalingAndelMedAndelerTilkjentYtelse
            .also { assertEquals(3, it.size) }
            .forEach { assertEquals(1, it.andelerTilkjentYtelse.size) }

        // Sjekk at toveis-koblinger finnes i databasen
        andelerTilkjentYtelseMedEndreteUtbetalinger
            .map { it.andel }
            .also { assertEquals(3, it.size) }
            .forEach {
                assertEquals(1, it.endretUtbetalingAndeler.size)
            }
        endretUtbetalingAndelMedAndelerTilkjentYtelse
            .map { it.endretUtbetalingAndel }
            .also { assertEquals(3, it.size) }
            .forEach {
                assertEquals(1, it.andelTilkjentYtelser.size)
            }

        // Slett koblingene fra andeler til endringer
        andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)
            .map { it.andel.also { it.endretUtbetalingAndeler.clear() } }
            .also { andelTilkjentYtelseRepository.saveAllAndFlush(it) }

        // Sjekk at koblingene fra endringer til andeler har blitt slettet ved cascade
        endretUtbetalingAndelRepository.findByBehandlingId(behandlingId)
            .also { assertEquals(3, it.size) }
            .forEach { assertEquals(0, it.andelTilkjentYtelser.size) }

        // Trigg sikkerhetsnettet ved å hente ut andeler på nytt
        andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        // Sjekk at db-koblingene fra endringer til andeler har blitt opprettet ved cascade
        endretUtbetalingAndelRepository.findByBehandlingId(behandlingId)
            .also { assertEquals(3, it.size) }
            .forEach { assertEquals(1, it.andelTilkjentYtelser.size) }
    }

    @Test
    fun `Virtuelle koblinger mellom andeler og endringer skal fungere uten sikkerhetsnett når frikobling er PÅ`() {
        // Skru PÅ virtuell kobling mellom andeler og endringer
        every { featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER) } returns true
        // Skru AV sikkerhetsnettet som skaper DB-koblinger fra virtuelle koblinger (true betyr UTEN sikkerhetsnett)
        every { featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER_UTEN_SIKKERHETSNETT) } returns true

        // Opprett tilkjent ytelse med 3 andeler tilkjent ytelse, hver koblet til en endret utbetaling i databasen
        val behandlingId = opprettTillkjentYtelseMedDeltBosted()

        // Slett DB-koblingene fra andeler til endringer. Vil slette koblinger fra endringer til andeler med cascade
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .map { it.also { it.endretUtbetalingAndeler.clear() } }
            .also { andelTilkjentYtelseRepository.saveAllAndFlush(it) }

        val andelerTilkjentYtelseMedEndreteUtbetalinger = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        val endretUtbetalingAndelMedAndelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId)

        // Sjekk at virtuelle toveis-koblinger finnes
        andelerTilkjentYtelseMedEndreteUtbetalinger
            .also { assertEquals(3, it.size) }
            .forEach { assertEquals(1, it.endreteUtbetalinger.size) }
        assertEquals(3, endretUtbetalingAndelMedAndelerTilkjentYtelse.size)

        endretUtbetalingAndelMedAndelerTilkjentYtelse
            .also { assertEquals(3, it.size) }
            .forEach { assertEquals(1, it.andelerTilkjentYtelse.size) }

        // Sjekk at toveis-koblinger IKKE finnes i databasen
        andelerTilkjentYtelseMedEndreteUtbetalinger
            .map { it.andel }
            .also { assertEquals(3, it.size) }
            .forEach {
                assertEquals(0, it.endretUtbetalingAndeler.size)
            }
        endretUtbetalingAndelMedAndelerTilkjentYtelse
            .map { it.endretUtbetalingAndel }
            .also { assertEquals(3, it.size) }
            .forEach {
                assertEquals(0, it.andelTilkjentYtelser.size)
            }
    }

    @Test
    fun `Når frikobling er AV, vil DB-koblingene brukes`() {
        // Skru AV virtuell kobling mellom andeler og endringer
        every { featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER) } returns false
        // Likegyldig om sikkerhetsnett er av eller på. Setter det til PÅ for å vise at det ikke påvirker ting  (false betyr MED sikkerhetsnett)
        every { featureToggleService.isEnabled(FeatureToggleConfig.BRUK_FRIKOBLEDE_ANDELER_OG_ENDRINGER_UTEN_SIKKERHETSNETT) } returns false

        // Opprett tilkjent ytelse med 3 andeler tilkjent ytelse, hver koblet til en endret utbetaling i databasen
        val behandlingId = opprettTillkjentYtelseMedDeltBosted()

        // Slett DB-koblingene fra andeler til endringer. Vil slette koblinger fra endringer til andeler med cascade
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
            .map { it.also { it.endretUtbetalingAndeler.clear() } }
            .also { andelTilkjentYtelseRepository.saveAllAndFlush(it) }

        val andelerTilkjentYtelseMedEndreteUtbetalinger = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandlingId)

        val endretUtbetalingAndelMedAndelerTilkjentYtelse = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandlingId)

        // Sjekk at toveis-koblinger IKKE finnes i databasen
        andelerTilkjentYtelseMedEndreteUtbetalinger
            .map { it.andel }
            .also { assertEquals(3, it.size) }
            .forEach {
                assertEquals(0, it.endretUtbetalingAndeler.size)
            }
        endretUtbetalingAndelMedAndelerTilkjentYtelse
            .map { it.endretUtbetalingAndel }
            .also { assertEquals(3, it.size) }
            .forEach {
                assertEquals(0, it.andelTilkjentYtelser.size)
            }

        // Sjekk at virtuelle toveis-koblinger ikke finnes (de er de samme som DB-koblingene fordi frikobling er AV)
        andelerTilkjentYtelseMedEndreteUtbetalinger
            .also { assertEquals(3, it.size) }
            .forEach { assertEquals(0, it.endreteUtbetalinger.size) }

        endretUtbetalingAndelMedAndelerTilkjentYtelse
            .also { assertEquals(3, it.size) }
            .forEach { assertEquals(0, it.andelerTilkjentYtelse.size) }
    }

    private fun opprettTillkjentYtelseMedDeltBosted(): Long {
        val søkerStartdato = 1.jan(2020).tilLocalDate()
        val barnStartdato = 2.jan(2020).tilLocalDate()

        val vilkårsvurderingRequest = mapOf(
            søkerStartdato to mapOf(
                Vilkår.BOSATT_I_RIKET /*    */ to "++++++++++++++++",
                Vilkår.LOVLIG_OPPHOLD /*    */ to "++++++++++++++++"
            ),
            barnStartdato to mapOf(
                Vilkår.UNDER_18_ÅR /*       */ to "++++++++++++++++",
                Vilkår.GIFT_PARTNERSKAP /*  */ to "++++++++++++++++",
                Vilkår.BOSATT_I_RIKET /*    */ to "++++++++++++++++",
                Vilkår.LOVLIG_OPPHOLD /*    */ to "++++++++++++++++",
                Vilkår.BOR_MED_SØKER /*     */ to "++++++++++++++++"
            )
        )

        val deltBosteRequest = mapOf(
            barnStartdato /*                */ to " ////00000011111"
        )

        val behandlingId =
            vilkårsvurderingTestController.opprettBehandlingMedVilkårsvurdering(vilkårsvurderingRequest)
                .body?.data!!.behandlingId

        tilkjentYtelseTestController
            .oppdaterEndretUtebetalingAndeler(behandlingId, deltBosteRequest)
        return behandlingId
    }
}
