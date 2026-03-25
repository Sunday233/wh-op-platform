import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from src.db.mysql_client import close_pool, init_pool
from src.db.sqlite_client import init_db
from src.routers import baseline, health, impact
from src.tasks.scheduler import run_precompute, start_scheduler, stop_scheduler

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    init_pool()
    init_db()
    start_scheduler()
    run_precompute()
    yield
    # Shutdown
    stop_scheduler()
    close_pool()


app = FastAPI(title="WH-OP Analytics", version="0.1.0", lifespan=lifespan)

app.include_router(health.router)
app.include_router(impact.router)
app.include_router(baseline.router)
