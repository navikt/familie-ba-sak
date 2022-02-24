alter table andel_tilkjent_ytelse
    add COLUMN vurdert_etter varchar;

update andel_tilkjent_ytelse
set vurdert_etter = 'NASJONALE_REGLER'
where type != 'EØS';

update andel_tilkjent_ytelse
set vurdert_etter = 'EØS_FORORDNINGEN'
where type = 'EØS';