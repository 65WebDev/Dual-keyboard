from pathlib import Path
from wordfreq import top_n_list
import re
root = Path("app/src/main/assets")
for lang, pattern in [("en", r"[a-z]{2,30}"), ("ru", r"[а-яё]{2,30}")]:
    path = root / f"swipe_{lang}.txt"
    seed = path.read_text(encoding="utf-8").split()
    words = list(dict.fromkeys(w.lower() for w in top_n_list(lang, 150000) + seed if re.fullmatch(pattern, w.lower())))
    assert len(words) >= 50000, (lang, len(words))
    path.write_text("\n".join(words) + "\n", encoding="utf-8")
    print(lang, len(words))
