package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BehandlingsresultatUtilsTest {

    val søker = tilfeldigPerson()

    val barn1Ident = randomFnr()
    val defaultYtelseSluttForLøpende = inneværendeMåned().plusMonths(1)
    val defaultYtelseSluttForAvslått = TIDENES_MORGEN.toYearMonth()

    @Test
    fun `Skal kaste feil dersom det finnes uvurderte ytelsepersoner`() {
        val feil = assertThrows<Feil> {
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                    YtelsePerson(
                        personIdent = barn1Ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = listOf(KravOpprinnelse.TIDLIGERE),
                        resultater = setOf(YtelsePersonResultat.IKKE_VURDERT)
                    )
                )
            )
        }

        assertEquals("Minst én ytelseperson er ikke vurdert", feil.message)
    }

    @Test
    fun `Skal kaste feil dersom sammensetningen av resultater ikke er støttet`() {
        val feil = assertThrows<Feil> {
            BehandlingsresultatUtils.utledBehandlingsresultatBasertPåYtelsePersoner(
                listOf(
                    YtelsePerson(
                        personIdent = barn1Ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                        resultater = setOf(YtelsePersonResultat.ENDRET),
                        ytelseSlutt = defaultYtelseSluttForLøpende,
                    ),
                    YtelsePerson(
                        personIdent = barn1Ident,
                        ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                        kravOpprinnelse = listOf(KravOpprinnelse.INNEVÆRENDE),
                        resultater = setOf(YtelsePersonResultat.AVSLÅTT),
                        ytelseSlutt = defaultYtelseSluttForAvslått,
                    )
                )
            )
        }

        assertTrue(feil.message?.contains("Behandlingsresultatet er ikke støttet i løsningen")!!)
    }

    @Test
    fun `Kaster feil ved ugyldig resultat på førstegangsbehandling`() {
        val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

        setOf(
            BehandlingResultat.AVSLÅTT_OG_OPPHØRT,
            BehandlingResultat.ENDRET,
            BehandlingResultat.ENDRET_OG_OPPHØRT,
            BehandlingResultat.OPPHØRT,
            BehandlingResultat.FORTSATT_INNVILGET,
            BehandlingResultat.IKKE_VURDERT
        ).forEach {

            val feil = assertThrows<FunksjonellFeil> {
                BehandlingsresultatUtils.validerBehandlingsresultat(behandling, it)
            }
            assertTrue(feil.message?.contains("ugyldig") ?: false)
        }
    }

    @Test
    fun `Kaster feil ved ugyldig resultat på revurdering`() {
        val behandling = lagBehandling(behandlingType = BehandlingType.REVURDERING)

        val feil = assertThrows<FunksjonellFeil> {
            BehandlingsresultatUtils.validerBehandlingsresultat(behandling, BehandlingResultat.IKKE_VURDERT)
        }
        assertTrue(feil.message?.contains("ugyldig") ?: false)
    }
}
