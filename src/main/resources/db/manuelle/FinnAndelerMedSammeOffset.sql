select aty.periode_offset, b.id, count(1) from andel_tilkjent_ytelse aty
                                                   inner join behandling b on b.id = aty.fk_behandling_id
group by (b.id, aty.periode_offset)
having count(aty.periode_offset) > 1;