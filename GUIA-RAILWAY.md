# Guia de despliegue en Railway — Biblioteca Don Quijote

## Antes de comenzar
La aplicacion funciona con Java 21 y Spring Boot 3.5.7. El backend tiene API REST en `/api`. Separamos dos aplicaciones con Dockerfile. El servicio MySQL en Railway se provisiona desde la plantilla.

## Paso 1: subir a GitHub
Crea un repositorio, descomprime este ZIP en su raiz y haz push del contenido que incluye `backend/`, `frontend/`, `database/`.

## Paso 2: crear proyecto y MySQL
En https://railway.com/new crea un proyecto. Anade una base de datos MySQL desde `+ New -> Database -> MySQL`. Renombra el servicio EXACTAMENTE **MySQL**. Railway provisiona MYSQLHOST, MYSQLPORT, MYSQLUSER, MYSQLPASSWORD, MYSQLDATABASE. **NO crees otra base**: usa el esquema que Railway crea.

## Paso 3: backend
En el mismo proyecto, `+ New -> GitHub Repo`, elige tu repositorio, renombra el servicio exactamente **Backend**.
En `Backend > Settings > Source > Root Directory` establece `/backend`.
Railway detecta `backend/Dockerfile` y usa Java 21.
En `Backend > Variables`, agrega (usa variable references, no introduzcas claves manualmente):

```env
SPRING_PROFILES_ACTIVE=railway
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?connectionTimeZone=UTC
DB_USER=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
APP_DEMO_DATA_ENABLED=false
JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=65.0 -Duser.timezone=America/Lima
```

Aplica los cambios (`Deploy` o `Redeploy` segun el panel). En `Backend > Settings > Networking -> Public Networking` haz `Generate Domain`.
Visita `https://<dominio-backend>/api/books`. Debe devolver JSON, incluso lista vacia.

El perfil `railway` crea automaticamente las cinco tablas con `CREATE TABLE IF NOT EXISTS` antes de que Hibernate las valide. El SQL esta en `database/schema-railway.sql` y empaquetado dentro del JAR. No hace falta importarlo a mano para una base nueva.

## Paso 4: frontend
En el mismo proyecto, `+ New -> GitHub Repo` otra vez, selecciona el MISMO repositorio y llama al servicio **Frontend**.
En `Frontend > Settings > Source > Root Directory` establece `/frontend`; Railway detecta `frontend/Dockerfile`.
En `Frontend > Variables` crea:

```env
API_BASE_URL=https://${{Backend.RAILWAY_PUBLIC_DOMAIN}}
```

**No agregues `/api`** al final. `app.js` ya lo adjunta. Esta variable genera `config.js` en cada arranque del contenedor Nginx.
Genera dominio publico del servicio Frontend desde `Settings > Networking`.

## Paso 5: CORS y prueba
En `Backend > Variables`, agrega:

```env
APP_CORS_ALLOWED_ORIGINS=https://${{Frontend.RAILWAY_PUBLIC_DOMAIN}}
```

Aplica cambios y verifica el frontend en `https://<dominio-frontend>`.
Comprueba registro de libros, lectores ficticios, prestamos, devoluciones, multas y recompensas.

## Paso 6: costos
Verifica el credito disponible en `Usage` y define un `Hard limit` de computo en tu workspace si tu plan permite la opcion. Un laboratorio con 3 servicios activos puede agotar los creditos de Free.

## Problemas frecuentes
- Backend con 500 o falla al iniciar: mirar `Backend > Deployments > View Logs`; revisa las referencias de variables del servicio MySQL.
- `Unknown database`: verifica que no hayas usado el antiguo `database/schema-mysql.sql` de Azure, que crea una base diferente. Usa la base MYSQLDATABASE de Railway.
- `Schema-validation: missing table`: confirma `SPRING_PROFILES_ACTIVE=railway` y el archivo `schema-railway.sql` dentro del JAR.
- `CORS blocked`: verifica el dominio EXACTO en APP_CORS_ALLOWED_ORIGINS, sin `/` final.
- `502`: Java no arranco, el puerto no esta correcto, o el servicio se quedo sin RAM. Revisa logs.
- `config.js` muestra localhost: frontend requiere `API_BASE_URL` definido en Railway y redeploy.
- Si el proxy MySQL se habilita para conexiones externas, pueden generarse costos; no es necesario para el despliegue normal.

## Seguridad y estado actual
No cuenta con autenticacion: **NO** introduzcas DNI u otros datos personales reales. Usa registros ficticios. El frontend es publico y la API expone operaciones de escritura; CORS NO es autenticacion. Para produccion implementar Spring Security, control de roles, gestion de secretos, pruebas, migraciones versionadas y respaldos.

## Fuentes oficiales
- https://docs.railway.com/deployments/monorepo
- https://docs.railway.com/databases/mysql
- https://docs.railway.com/variables
- https://docs.railway.com/builds/dockerfiles
- https://docs.railway.com/networking/public-networking
- https://docs.railway.com/pricing/free-trial
