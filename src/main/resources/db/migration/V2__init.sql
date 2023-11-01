create table prepare_instance_to_dispatch_entity
(
    source_application_instance_id    varchar(255) not null,
    archive_instance_id               varchar(255),
    source_application_integration_id varchar(255),
    primary key (source_application_instance_id)
);