# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table `authenticator` (`id` varchar(200) NOT NULL PRIMARY KEY,`email` VARCHAR(254) NOT NULL,`expiration_date` TIMESTAMP NOT NULL,`last_used` TIMESTAMP NOT NULL,`creation_date` TIMESTAMP NOT NULL);
create table `oauth1` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`token` VARCHAR(254) NOT NULL,`secret` VARCHAR(254) NOT NULL);
create table `oauth2` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`access_token` VARCHAR(254) NOT NULL,`token_type` VARCHAR(254),`expires_in` INTEGER,`refresh_token` VARCHAR(254));
create table `password` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`hasher` VARCHAR(254) NOT NULL,`password` VARCHAR(254) NOT NULL,`salt` VARCHAR(254));
create table `profile` (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,`provider_id` VARCHAR(254) NOT NULL,`email` VARCHAR(254) NOT NULL,`full_name` VARCHAR(254),`phone_number` VARCHAR(254),`auth_method` VARCHAR(254) NOT NULL,`avatar_url` VARCHAR(254),`oauth1_id` BIGINT,`oauth2_id` BIGINT,`password_id` BIGINT);
create index `profile_idx` on `profile` (`provider_id`,`email`);
create table `twilio_token` (`uuid` VARCHAR(254) NOT NULL PRIMARY KEY,`twilio_code` VARCHAR(254) NOT NULL,`full_name` VARCHAR(254) NOT NULL,`phone_number` VARCHAR(254) NOT NULL,`email` VARCHAR(254) NOT NULL,`password` VARCHAR(254) NOT NULL,`creation_time` TIMESTAMP NOT NULL,`expiration_time` TIMESTAMP NOT NULL);
create table `user` (`email` VARCHAR(254) NOT NULL PRIMARY KEY,`main_id` BIGINT NOT NULL);

# --- !Downs

drop table `user`;
drop table `twilio_token`;
drop table `profile`;
drop table `password`;
drop table `oauth2`;
drop table `oauth1`;
drop table `authenticator`;

