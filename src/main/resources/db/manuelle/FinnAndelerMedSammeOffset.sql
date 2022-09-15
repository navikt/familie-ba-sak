select aty.periode_offset, b.id as "Behandlingsid", b.opprettet_tid as "Behandling opprettet", count(1) from andel_tilkjent_ytelse aty
                                                   inner join behandling b on b.id = aty.fk_behandling_id
group by (b.id, aty.periode_offset, b.opprettet_tid)
having count(aty.periode_offset) > 1;