from typing import Literal

from pydantic import BaseModel


class MovieEvent(BaseModel):
    eventType: Literal["ADD", "DELETE", "RATE"]
    userId: str
    movieId: int
    rating: int | None = None


class SuggestionsRequest(BaseModel):
    userId: str
