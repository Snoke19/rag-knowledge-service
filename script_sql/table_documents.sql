create table documents
(
    title          varchar(100) not null,
    content_type   varchar(64)  not null,
    size           integer      not null,
    storage_key    varchar(64)  not null
        constraint documents_storage_key_uq
            unique,
    id             bigint generated always as identity
        constraint documents_pk
            primary key,
    document_id    uuid         not null
        constraint documents_document_id_uq
            unique,
    status         varchar(20)  not null,
    content_sha256 varchar(64)
);

alter table documents
    owner to rag;