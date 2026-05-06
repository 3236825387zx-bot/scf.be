# Local Run Guide

## Ports

- `scf-gateway`: `8090`
- `scf-isc`: `8081`
- `scf-wms`: `8082`
- `scf-lgs`: `8083`
- `scf-oms`: `8084`

## Start Order

1. Start `scf-isc`
2. Start `scf-wms`
3. Start `scf-lgs`
4. Start `scf-oms`
5. Start `scf-gateway`

## One-Click Scripts

Run the following commands from the project root directory (`scf`):

- Start all services:
  `powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1`
- Stop all services:
  `powershell -ExecutionPolicy Bypass -File .\scripts\stop-all.ps1`
- Start all services without rebuilding jars:
  `powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1 -SkipBuild`
- Restart by stopping old processes first:
  `powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1 -StopExisting`

Notes:

- `start-all.ps1` writes logs to `.logs\*.out.log` and `.logs\*.err.log`
- It writes the actual jar path and `LastWriteTime` used for startup to `.logs\startup-manifest.log`
- It records started process ids in `.logs\service-pids.json`
- Recommended flow for local debugging: `.\scripts\stop-all.ps1` -> `.\mvnw.cmd -DskipTests package` -> `.\scripts\start-all.ps1 -StopExisting`
- `-SkipBuild` is only for the case where you have already rebuilt and explicitly want to reuse the current jars
- If a local `mysql` client is available, `start-all.ps1` will validate required tables before service startup
- Use `-StopExisting` on startup if you want it to clear existing listeners on `8081/8082/8083/8084/8090` before relaunching

## Frontend Access

- Preferred gateway base URL: `http://localhost:8090`
- OMS workspace: `GET http://localhost:8090/api/scf/oms/orders`
- OMS dashboard: `GET http://localhost:8090/api/scf/oms/dashboard`
- Order submit: `POST http://localhost:8090/api/oms/order/submit`
- WMS task list: `GET http://localhost:8090/api/wms/outbound/tasks`
- WMS task detail: `GET http://localhost:8090/api/wms/outbound/tasks/{taskNo}`
- WMS create outbound task: `POST http://localhost:8090/api/wms/outbound/create`
- WMS pick task: `POST http://localhost:8090/api/wms/outbound/tasks/{taskNo}/pick`
- WMS ship task: `POST http://localhost:8090/api/wms/outbound/tasks/{taskNo}/ship`
- WMS dashboard: `GET http://localhost:8090/api/wms/outbound/dashboard`

## Notes

- All services allow CORS from local frontend origins through wildcard rules.
- `scf-oms` calls `scf-isc`, `scf-wms`, and `scf-lgs` through local HTTP endpoints configured in `application.yml`.
- LGS logistics gateway APIs:
  `GET http://localhost:8090/api/lgs/providers`
  `GET http://localhost:8090/api/lgs/parcels`
  `POST http://localhost:8090/api/lgs/parcels/deliver`
  `POST http://localhost:8090/api/lgs/parcels/{parcelNo}/sign`
  `POST http://localhost:8090/api/lgs/parcels/{parcelNo}/tracking`
  `GET http://localhost:8090/api/lgs/dashboard`
- `scf-wms` now persists outbound task headers, task items, and task logs in dedicated WMS tables seeded by `db/init/scf_seed.sql`.
- The repo currently does not include a frontend project; use the gateway URL above as the frontend API base.
- Default database connection points to `scf_oms` on `localhost:3306`. Override with `SCF_DB_URL`, `SCF_DB_USERNAME`, and `SCF_DB_PASSWORD` if needed.
- If your frontend dev server runs on `8080`, point its API proxy or base URL to `http://localhost:8090`.

