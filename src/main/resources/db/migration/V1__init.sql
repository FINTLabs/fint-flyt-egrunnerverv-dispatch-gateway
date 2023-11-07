create table instance_headers_entity
(
    source_application_instance_id    varchar(255) not null,
    archive_instance_id               varchar(255),
    source_application_integration_id varchar(255),
    primary key (source_application_instance_id)
);
create table instance_receipt_dispatch_entity
(
    source_application_instance_id varchar(255) not null,
    class_type                     varchar(255),
    instance_receipt               TEXT,
    uri                            varchar(255),
    primary key (source_application_instance_id)
);
