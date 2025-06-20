from typing import Optional, Literal

from pydantic import BaseModel


class MovieEvent(BaseModel):
    eventType: Literal["ADD", "DELETE", "RATE"]
    userId: str
    movieId: int
    rating: Optional[int] = None


class SuggestionsRequest(BaseModel):
    userId: str
