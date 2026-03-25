from __future__ import annotations

from fastapi import APIRouter, Query

from src.db.sqlite_client import query_correlation_matrix, query_impact_factors
from src.models.schemas import CorrelationMatrix, FactorRankItem

router = APIRouter(prefix="/api/impact")


@router.get("/factors", response_model=list[FactorRankItem])
def get_factors(warehouseCode: str = Query(...)):
    rows = query_impact_factors(warehouseCode)
    return [
        FactorRankItem(
            rank=r["rank"],
            factorName=r["factor_name"],
            correlation=r["correlation"],
            description=r["description"],
        )
        for r in rows
    ]


@router.get("/correlation", response_model=CorrelationMatrix)
def get_correlation(warehouseCode: str = Query(...)):
    data = query_correlation_matrix(warehouseCode)
    if not data:
        return CorrelationMatrix(factors=[], matrix=[])
    return CorrelationMatrix(factors=data["factors"], matrix=data["matrix"])
