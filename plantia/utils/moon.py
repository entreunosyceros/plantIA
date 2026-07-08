from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime, timezone
from math import floor


@dataclass(frozen=True)
class MoonInfo:
    date_iso: str
    phase_name: str
    emoji: str
    age_days: float
    illumination_pct: int


_SYNODIC_MONTH = 29.530588853  # days
# Reference new moon: 2000-01-06 18:14 UTC (commonly used epoch)
_REF_NEW_MOON = datetime(2000, 1, 6, 18, 14, tzinfo=timezone.utc)


def _moon_age_days(d: date) -> float:
    dt = datetime(d.year, d.month, d.day, 12, 0, tzinfo=timezone.utc)
    delta_days = (dt - _REF_NEW_MOON).total_seconds() / 86400.0
    return delta_days % _SYNODIC_MONTH


def _illumination_from_age(age: float) -> int:
    # Simple approximation: illumination based on distance from new/full
    # 0 at new, 100 at full (age ~= 14.77)
    x = abs(age - (_SYNODIC_MONTH / 2)) / (_SYNODIC_MONTH / 2)
    illum = int(round((1 - x) * 100))
    return max(0, min(100, illum))


def _phase_name_and_emoji(age: float) -> tuple[str, str]:
    # 8-phase split
    idx = int(floor((age / _SYNODIC_MONTH) * 8)) % 8
    phases = [
        ("Luna nueva", "🌑"),
        ("Luna creciente", "🌒"),
        ("Cuarto creciente", "🌓"),
        ("Gibosa creciente", "🌔"),
        ("Luna llena", "🌕"),
        ("Gibosa menguante", "🌖"),
        ("Cuarto menguante", "🌗"),
        ("Luna menguante", "🌘"),
    ]
    return phases[idx]


def moon_info(today: date | None = None) -> MoonInfo:
    d = today or date.today()
    age = _moon_age_days(d)
    phase_name, emoji = _phase_name_and_emoji(age)
    illum = _illumination_from_age(age)
    return MoonInfo(
        date_iso=d.isoformat(),
        phase_name=phase_name,
        emoji=emoji,
        age_days=age,
        illumination_pct=illum,
    )

