create table points(
	nodeid varchar(100) not null,
	`timestamp` bigint unsigned not null,
	msg text,
	latitude float,
	longitude float,
	llconf int,
	battery int,
	steps int,
	screen int,
	distance float,
	safe int,
	added bigint unsigned not null
);
 
alter table points add primary key(nodeid, `timestamp`);