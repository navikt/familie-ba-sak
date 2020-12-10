package no.nav.familie.ba.sak.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface BehandlingRepository : JpaRepository<Behandling, Long> {

    @Query(value = "SELECT b FROM Behandling b WHERE b.id = :behandlingId")
    fun finnBehandling(behandlingId: Long): Behandling

    @Query(value = "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId")
    fun finnBehandlinger(fagsakId: Long): List<Behandling>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.aktiv = true")
    fun findByFagsakAndAktiv(fagsakId: Long): Behandling?


    /* Denne henter først siste iverksatte behandling på en løpende fagsak.
     * Finner så alle perioder på siste iverksatte behandling
     * Finner deretter første behandling en periode oppstod i, som er det som skal avstemmes
     */
    @Query(value = """with sisteIverksatteBehandlingFraLøpendeFagsak as (
                            select f.id as fagsakId, max(b.id) as behandlingId
                            from behandling b
                                   inner join fagsak f on f.id = b.fk_fagsak_id
                                   inner join tilkjent_ytelse ty on b.id = ty.fk_behandling_id
                            where f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                            GROUP BY fagsakId)
                        
                        select distinct andel_tilkjent_ytelse.kilde_behandling_id
                        from andel_tilkjent_ytelse inner join sisteIverksatteBehandlingFraLøpendeFagsak 
                            on andel_tilkjent_ytelse.fk_behandling_id = sisteIverksatteBehandlingFraLøpendeFagsak.behandlingId""",
           nativeQuery = true)
    fun finnBehandlingerMedLøpendeAndel(): List<Long>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.status = 'AVSLUTTET'")
    fun findByFagsakAndAvsluttet(fagsakId: Long): List<Behandling>

    /*
    select * from behandling where aktiv=true and id in (
select id from gr_personopplysninger where aktiv=true and id in (
select fk_gr_personopplysninger_id from po_person where foedselsdato between '2016-11-01' and '2016-11-30')) ;
     */
    @Query(value = """SELECT b FROM Behandling b WHERE b.id in (
                            SELECT pg.behandlingId FROM PersonopplysningGrunnlag pg WHERE pg.aktiv=true AND pg.id IN (
                                SELECT p.personopplysningGrunnlag FROM Person p WHERE p.fødselsdato BETWEEN :fom AND :tom
                            )
                        )""")
    fun finnBehandlingerMedPersonerMedFødselsdatoInnenfor(fom: LocalDate, tom: LocalDate)
}