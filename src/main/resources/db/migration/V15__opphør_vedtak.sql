alter table VEDTAK add column forrige_vedtak_id bigint references VEDTAK default null;
alter table VEDTAK add column opphør_dato TIMESTAMP(3) default null;