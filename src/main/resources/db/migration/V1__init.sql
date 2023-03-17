create table instance_to_dispatch_entity
(
    source_application_instance_id varchar(255) not null,
    class_type                     varchar(255),
    instance_to_dispatch           TEXT,
    uri                            varchar(255),
    primary key (source_application_instance_id)
);