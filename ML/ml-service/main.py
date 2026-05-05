from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dataset_builder import router as dataset_router
from dso1 import router as dso1_router
from dso2 import router as dso2_router
from dso3 import router as dso3_router
from dso4 import router as dso4_router

app = FastAPI(title="ESM ML Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(dataset_router, prefix="/ml")
app.include_router(dso1_router,    prefix="/ml/dso1")
app.include_router(dso2_router,    prefix="/ml/dso2")
app.include_router(dso3_router,    prefix="/ml/dso3")
app.include_router(dso4_router,    prefix="/ml/dso4")


@app.get("/ml/health")
def health():
    return {"status": "ok", "service": "ml-service"}
