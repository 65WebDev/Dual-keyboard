from pathlib import Path
from wordfreq import top_n_list
import re
# Filter inflected forms and common compounds, not ordinary manual typing.
RU = re.compile(r"(?:[хx][уy][йиеяёю]|п[иеё]зд|[её]б(?:а[нтл]|у[чт]|л[яи]|н[уы]|ош|ищ)|бля(?:д|т|ха|$)|мудак|мудил|гандон|пид[аое]р|пед[ео]р|залуп|дроч|сучк|сука$|суки$|суку$|срать|сра[нлч]|говн|дерьм|шлюх|ублюд|долбо[её]б|мраз|нахер|похер|хер(?:ня|ов|$))")
EN = re.compile(r"(?:fuck|shit|bitch|cunt|motherfuck|asshole|dickhead|cock sucker|cocksuck|bullshit|bastard|whore|slut|pussy|twat|wanker|nigger|faggot|douchebag)")
EN_EXACT = {"ass","arse","dick","cock","damn","crap","bollocks","prick","douche","piss","pissed","pissing","pisses","bitches","bitchy"}
def clean(word, lang):
    if lang == "ru":
        return not RU.search(word)
    return word not in EN_EXACT and not EN.search(word)

root = Path("app/src/main/assets")
for lang, pattern in [("en", r"[a-z]{2,30}"), ("ru", r"[а-яё]{2,30}")]:
    path = root / f"swipe_{lang}.txt"
    seed = path.read_text(encoding="utf-8").split()
    words = list(dict.fromkeys(w.lower() for w in top_n_list(lang, 150000) + seed if re.fullmatch(pattern, w.lower()) and clean(w.lower(), lang)))
    assert len(words) >= 50000, (lang, len(words))
    path.write_text("\n".join(words) + "\n", encoding="utf-8")
    print(lang, len(words))
