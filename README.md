# Biblioteca Don Quijote de la Mancha — Railway

Proyecto independiente de **frontend + backend Spring Boot + MySQL**. Guia detallada en [GUIA-RAILWAY.md](GUIA-RAILWAY.md).

- `backend/`: Java 21 / Spring Boot 3.5.7 / JPA. Dockerfile.
- `frontend/`: HTML/CSS/JS servido con Nginx. Dockerfile genera `config.js` en ejecucion.
- `database/schema-railway.sql`: esquema MySQL compatible con la base provisionada por Railway.
- `backend/src/main/resources/application-railway.properties`: perfil para Railway.

## Local

Backend: `cd backend && mvn spring-boot:run`

Frontend: `cd frontend && python -m http.server 5500`

Visitar `http://localhost:5500` y probar `http://localhost:8080/api/books`.

## Railway

Crear proyecto y tres servicios **MySQL**, **Backend**, **Frontend**. En Backend asignar Root Directory `/backend`, en Frontend `/frontend`. Railway detecta los Dockerfiles. Anadir variables segun `GUIA-RAILWAY.md`.

### Advertencia de seguridad

Backend **sin autenticacion**. Solo usar datos ficticios. No subir datos personales reales ni exponerlo publicamente para uso real sin Spring Security.
