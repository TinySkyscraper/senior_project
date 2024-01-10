1. `docker run --rm -d -p 50003:5432 -v "postgres-data:/var/lib/postgresql/data" --name auth_server tinyskyscraper/postgres_server_image:v1`

2. use `docker run --rm -d -p 50002:50002 -v "C:\Users\kraze\certs:/certs:ro" --name auth_api tinyskyscraper/auth_api_image:v2` to run server

<br>
