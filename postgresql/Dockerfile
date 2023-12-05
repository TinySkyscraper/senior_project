FROM postgres:latest

ENV POSTGRES_USER=Crypto
ENV POSTGRES_PASSWORD=crypto
ENV POSTGRES_DB=access_control

EXPOSE 5432

ADD ./create_tables.sql /docker-entrypoint-initdb.d/