#!/usr/bin/env python3
"""
Generates the language files (English and Korean) from one place, so both languages always
have the same keys.

Run:  python3 tools/gen_lang.py
Output: src/main/resources/assets/tycoonrogue/lang/en_us.json and ko_kr.json

It also checks that every dialogue situation declared in Dialogue.java has all of its lines
in both languages, and stops with an error if one is missing.
"""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
LANG_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "tycoonrogue", "lang")
DIALOGUE_JAVA = os.path.join(ROOT, "src", "main", "java", "com", "ysh", "tycoonrogue", "entity", "Dialogue.java")

VARIANTS = ["plain", "shy", "picky", "chatty", "generous"]

# key -> (English, Korean)
T = {}


def add(key, en, ko):
    if key in T:
        raise SystemExit("duplicate key: " + key)
    T[key] = (en, ko)


# ---- basic names ----
add("entity.tycoonrogue.customer", "Customer", "손님")
add("item.tycoonrogue.customer_spawn_egg", "Customer Spawn Egg", "손님 생성 알")

for k, en, ko in [
    ("history", "History", "역사"), ("cars", "Cars", "자동차"), ("airplanes", "Airplanes", "비행기"),
    ("coffee", "Coffee", "커피"), ("cola", "Cola", "콜라"),
]:
    add("taste.tycoonrogue." + k, en, ko)

for k, en, ko in [
    ("farmer", "Farmer", "농부"), ("blacksmith", "Blacksmith", "대장장이"), ("chef", "Chef", "요리사"),
    ("barista", "Barista", "바리스타"), ("historian", "Historian", "역사학자"), ("mechanic", "Mechanic", "정비사"),
    ("pilot", "Pilot", "조종사"), ("miner", "Miner", "광부"), ("builder", "Builder", "건축가"),
    ("soda_vendor", "Soda Vendor", "음료 판매원"), ("raider", "Armed Raider", "무장 강도"), ("guard", "Guard", "경비원"),
]:
    add("job.tycoonrogue." + k, en, ko)

# ---- action bar / chat messages ----
add("message.tycoonrogue.order", "[%1$s] Wants %2$s x%3$s (pays %4$s emeralds)",
    "[%1$s] %2$s %3$s개 필요 (에메랄드 %4$s개 지급)")
add("message.tycoonrogue.order.done", "Order complete! +%s emeralds", "주문 완료! 에메랄드 +%s")
add("message.tycoonrogue.order.partial", "+%s emeralds, %s still needed", "에메랄드 +%s, %s개 더 필요")
add("message.tycoonrogue.no_order", "...", "...")
add("message.tycoonrogue.robber.stole", "That customer was a robber! He stole %s emeralds! Catch him!",
    "그 손님은 강도였다! 에메랄드 %s개를 훔쳐갔다! 잡아라!")
add("message.tycoonrogue.robber.empty", "That customer was a robber, but he found no emeralds on you.",
    "그 손님은 강도였지만, 훔칠 에메랄드가 없었다.")
add("message.tycoonrogue.scammer.escape",
    "That customer was a scammer! He took your goods and ran without paying! Catch him!",
    "그 손님은 사기꾼이었다! 물건만 챙기고 돈도 안 내고 도망친다! 잡아라!")
add("message.tycoonrogue.raid.alert", "WARNING: Armed raiders have been spotted near your factory!",
    "경고: 공장 근처에서 무장 강도가 발견됐다!")
add("message.tycoonrogue.raider.talk", "Don't move.", "움직이지 마.")
add("message.tycoonrogue.raider.stole", "The raiders grabbed %s emeralds!", "무장 강도가 에메랄드 %s개를 강탈했다!")
add("message.tycoonrogue.raider.ransack", "The raiders are ransacking your chests! Stop them!",
    "무장 강도가 상자를 털고 있다! 막아라!")
add("message.tycoonrogue.reputation", "Reputation %1$s (%2$s) - now %3$s", "평판 %1$s (%2$s) - 현재 %3$s")

add("reason.tycoonrogue.gun_threat", "pointed a gun at a customer", "손님에게 총을 겨눔")
add("reason.tycoonrogue.scammed", "got scammed", "사기를 당함")
add("reason.tycoonrogue.order_done", "order completed", "주문 완료")
add("reason.tycoonrogue.thief_caught", "caught a thief", "도둑을 잡음")

# ---- dialogue (speech bubbles) ----
# situation -> variant -> [(en, ko), ...]
D = {}


def dlg(situation, variant, *lines):
    D.setdefault(situation, {})[variant] = list(lines)


dlg("greeting", "plain", ("Hello there!", "안녕하세요!"), ("Good day.", "좋은 하루예요."))
dlg("greeting", "shy", ("Um... hi...", "저, 저기... 안녕하세요..."), ("S-sorry to bother you...", "죄, 죄송한데요..."))
dlg("greeting", "picky", ("Hmph. Let's see if this place is any good.", "흠. 여기 물건이 괜찮은지 볼까."),
    ("I hope the quality here is acceptable.", "품질이 기대 이상이면 좋겠군."))
dlg("greeting", "chatty", ("Oh hi! Lovely weather, isn't it? Anyway, listen!", "어머, 안녕하세요! 날씨 좋죠? 아, 그건 그렇고 들어보세요!"),
    ("Hey hey! I've heard so much about this place!", "안녕안녕! 여기 소문 정말 많이 들었어요!"))
dlg("greeting", "generous", ("What a wonderful place! I'm in a spending mood!", "여기 정말 멋지네요! 오늘은 아낌없이 쓸게요!"),
    ("Hello, friend! I'm happy to pay well today.", "반가워요! 오늘은 값을 후하게 쳐 드릴게요."))

dlg("thanks", "plain", ("Thanks a lot!", "정말 고마워요!"), ("Pleasure doing business.", "좋은 거래였어요."))
dlg("thanks", "shy", ("Th-thank you...", "고, 고맙습니다..."), ("That was... really nice of you.", "정말... 친절하시네요."))
dlg("thanks", "picky", ("Acceptable. Barely.", "그럭저럭이군. 겨우."), ("Not bad. I might come back.", "나쁘지 않군. 또 올지도."))
dlg("thanks", "chatty", ("Wow, thanks! I'm telling everyone about this!", "와, 고마워요! 다들한테 얘기할 거예요!"),
    ("Amazing! Wait till I post about this!", "대단해요! 얼른 SNS에 올려야겠어요!"))
dlg("thanks", "generous", ("Wonderful! Keep the extra!", "훌륭해요! 남는 건 가지세요!"),
    ("You've earned every emerald!", "에메랄드 한 알도 아깝지 않네요!"))

dlg("scared", "plain", ("Whoa! Put that away!", "으악! 그거 치워요!"), ("Please don't shoot!", "쏘지 마세요!"))
dlg("scared", "shy", ("Eeek!!", "꺄아악!!"), ("P-please... don't...", "제, 제발... 하지 마세요..."))
dlg("scared", "picky", ("This is outrageous! I'll never shop here again!", "이게 무슨 짓이야! 다시는 여기서 안 사!"),
    ("Unacceptable! I'm leaving a review!", "용납 못 해! 후기 남길 거야!"))
dlg("scared", "chatty", ("Help! Somebody! Everyone's gonna hear about this!", "살려줘요! 온 동네에 소문낼 거예요!"),
    ("Hands up?! I'm telling the whole town!", "손 들라고요?! 마을 전체에 말할 거예요!"))
dlg("scared", "generous", ("Take it easy! I'll pay whatever, just don't!", "진정해요! 얼마든지 줄 테니 제발!"),
    ("Calm down, friend! No need for that!", "진정해요, 친구! 그럴 필요 없어요!"))

dlg("complain", "plain", ("That's not what I ordered.", "그건 제가 주문한 게 아니에요."), ("Wrong item, I think.", "물건이 잘못된 것 같아요."))
dlg("complain", "shy", ("Um... I think that's the wrong one...", "저... 그거 아닌 것 같은데요..."),
    ("S-sorry, that's not it...", "죄, 죄송해요, 그게 아니에요..."))
dlg("complain", "picky", ("Are you blind? That's not what I asked for!", "눈이 없어요? 그게 아니라고요!"),
    ("Wrong. Try again.", "틀렸어요. 다시 가져와요."))
dlg("complain", "chatty", ("Haha, nope, not that one! I need exactly what I said!", "하하, 아니에요, 그거 말고요! 말한 그대로 필요해요!"),
    ("Oh dear, mixed up the order, did you?", "어머, 주문이 헷갈리셨나 봐요?"))
dlg("complain", "generous", ("No worries, but that's not quite it!", "괜찮아요, 근데 그건 아니에요!"),
    ("Nice try, friend, but I need the other thing!", "좋은 시도예요, 친구. 근데 다른 게 필요해요!"))

dlg("order", "plain", ("I'm looking for %2$s of %1$s. Can you supply them?", "%1$s %2$s개가 필요해요. 구해줄 수 있나요?"),
    ("Need %2$s %1$s, quickly if possible!", "%1$s %2$s개 급해요, 빨리 부탁해요!"),
    ("My shop is out of %1$s. %2$s should do it.", "가게에 %1$s이(가) 다 떨어졌어요. %2$s개면 돼요."))
dlg("partial", "plain", ("Good, now the rest please.", "좋아요, 이제 나머지도 부탁해요."),
    ("Getting there, keep them coming!", "거의 다 됐어요, 계속 가져와요!"))
dlg("gossip", "plain", ("Did you hear? Someone pulled a gun on a customer at that factory!", "들었어요? 저 공장에서 손님한테 총을 겨눴대요!"),
    ("I heard the owner scares customers. I'm staying away.", "사장이 손님을 겁준다던데. 난 안 갈래요."),
    ("Rumor has it there's trouble at that factory...", "저 공장에 무슨 문제가 있다는 소문이..."))
dlg("robber_reveal", "plain", ("Hand it over! This was a robbery all along!", "내놔! 처음부터 강도였다!"),
    ("Heh... thanks for the emeralds!", "헤헤... 에메랄드 고맙다!"))
dlg("scammer_reveal", "plain", ("Haha, sucker! Thanks for the free goods!", "하하, 호구네! 공짜 물건 고맙다!"),
    ("Pleasure doing business... for me!", "나한텐 좋은 거래였어!"))
dlg("raider_charge", "plain", ("Nobody move! This is a robbery!", "다들 움직이지 마! 강도다!"),
    ("Give me everything you've got!", "가진 거 다 내놔!"), ("Your emeralds are ours!", "네 에메랄드는 우리 거다!"))
dlg("raider_flee", "plain", ("Time to go!", "튀자!"), ("Fall back! We got the loot!", "후퇴! 전리품 챙겼다!"))
dlg("leaving", "plain", ("Well, I'd better get going.", "그럼 이만 가볼게요."), ("See you around!", "또 봐요!"),
    ("Time to head home.", "집에 가야겠네요."))

dlg("inspect", "plain", ("Let me take a look around...", "공장을 좀 둘러볼게요..."), ("Hmm, interesting setup.", "흠, 흥미로운 시설이네요."),
    ("So this is where it all happens.", "여기서 다 만드는군요."))
dlg("rating_high", "plain", ("Fantastic factory! Five stars!", "훌륭한 공장이에요! 별 다섯 개!"), ("Impressive! Here, a tip.", "인상적이에요! 팁 여기 있어요."))
dlg("rating_high", "shy", ("It's... really amazing. Thank you.", "정말... 멋져요. 고맙습니다."), ("I-I'll leave a good review...", "조, 좋은 후기 남길게요..."))
dlg("rating_high", "picky", ("I rarely say this, but well done.", "좀처럼 칭찬 안 하는데, 잘 만들었군."), ("Top quality. I'm surprised.", "최고 수준이군. 놀랍네."))
dlg("rating_high", "chatty", ("Wow wow wow! I'm telling EVERYONE about this place!", "우와 우와! 온 동네에 자랑할 거예요!"), ("Best factory ever! Posting right now!", "역대급 공장! 지금 바로 올릴게요!"))
dlg("rating_high", "generous", ("Splendid! Take a big tip!", "멋져요! 팁 두둑이 드릴게요!"), ("You deserve more than I can give!", "드리는 것보다 더 받아야 해요!"))
dlg("rating_mid", "plain", ("Not bad. Decent factory.", "나쁘지 않네요. 괜찮은 공장이에요."), ("It's okay. Room to grow.", "무난해요. 더 발전할 여지가 있어요."))
dlg("rating_low", "plain", ("Hmm... a bit small, isn't it?", "음... 좀 작은 거 아니에요?"), ("I expected more, honestly.", "솔직히 더 기대했어요."))
dlg("rating_low", "shy", ("Um... it's a little disappointing...", "저... 조금 아쉬워요..."), ("S-sorry, I expected more...", "죄, 죄송해요, 더 기대했는데..."))
dlg("rating_low", "picky", ("Pathetic. Call this a factory?", "한심하군. 이걸 공장이라고?"), ("One star. Do better.", "별 하나. 더 노력해."))
dlg("rating_low", "chatty", ("Oh dear, I have to tell people this isn't great...", "어머, 별로라고 사람들한테 말해야겠네요..."), ("Not what I heard! Gonna post about it!", "듣던 거랑 달라요! 후기 올릴 거예요!"))
dlg("rating_low", "generous", ("It's... fine. Keep trying, friend!", "음... 괜찮아요. 계속 힘내요, 친구!"), ("Needs work, but I believe in you!", "손볼 곳이 많지만 응원할게요!"))

dlg("streamer", "plain", ("Hi chat! Today we visit a factory!", "여러분 안녕! 오늘은 공장 방문 방송이에요!"),
    ("Smash that like button and look at these machines!", "구독과 좋아요! 이 기계들 좀 보세요!"),
    ("This will be a banger of a video.", "이거 대박 영상 되겠는데요."))

dlg("guard_hello", "plain", ("All quiet here, boss.", "여긴 이상 무예요, 사장님."), ("I'm keeping an eye out.", "제가 잘 지켜보고 있어요."),
    ("Nobody gets past me.", "아무도 못 지나가요."))
dlg("guard_spot", "plain", ("Hey! I know that face!", "어이! 저 얼굴 알아!"), ("Stop right there!", "거기 서!"),
    ("You're not fooling me!", "나는 못 속여!"))
dlg("guard_catch", "plain", ("Got him!", "잡았다!"), ("Case closed.", "상황 종료."))

dlg("color_talk", "plain", ("My favorite color is %s. Show me some!", "제가 제일 좋아하는 색은 %s이에요. 많이 보여주세요!"),
    ("I do love the color %s...", "%s, 정말 좋아하거든요..."), ("Anything %s makes me happy.", "%s이면 뭐든 기분이 좋아져요."))

dlg("belt_watch", "plain", ("Look at those goods rolling by on the belt!", "벨트 위로 물건이 쭉쭉 나오네요!"),
    ("I could watch this belt all day.", "이 벨트는 하루 종일 봐도 안 질리겠어요."), ("Nice steady flow.", "흐름이 아주 안정적이에요."))
dlg("wheel_look", "plain", ("These wheels are spinning nicely.", "바퀴가 아주 잘 돌아가네요."),
    ("Now that's a proper drivetrain.", "이거 제대로 된 구동계네요."), ("I'd love to take it for a spin.", "한번 몰아보고 싶어요."))
dlg("rocket_look", "plain", ("A rocket engine! Please stand back...", "로켓 엔진이네요! 좀 물러나 있을게요..."),
    ("Whoa, feel that heat!", "우와, 이 열기 좀 봐요!"), ("That thrust could lift a whole plane.", "이 추력이면 비행기도 띄우겠는데요."))
dlg("stop_idle", "plain", ("It's not even running...", "이거 돌아가지도 않잖아요..."),
    ("A machine that stands still makes no money.", "멈춰 있는 기계는 돈을 못 벌어요."), ("Hmm, nothing is moving here.", "음, 여기는 아무것도 안 움직이네요."))
dlg("flow_good", "plain", ("Goods keep coming out. Impressive!", "물건이 계속 나오네요. 대단해요!"),
    ("A factory that never stops. I like it.", "멈추지 않는 공장이라니, 마음에 들어요."))
dlg("flow_bad", "plain", ("It seems to have stopped producing...", "생산이 멈춘 것 같아요..."),
    ("Where are the goods coming out?", "물건은 어디서 나오는 거예요?"))

dlg("propeller_look", "plain", ("Look at that propeller spin!", "프로펠러가 씽씽 도네요!"),
    ("That bearing runs smooth. Ready for takeoff.", "베어링이 부드럽게 돌아가네요. 이륙 준비 완료!"), ("I can feel the wind from here.", "여기서도 바람이 느껴져요."))
dlg("sail_look", "plain", ("Those windmill wings are a fine airframe.", "저 풍차 날개, 기체로 딱이네요."),
    ("Light sails, strong lift. Clever.", "가벼운 날개에 양력도 좋겠어요. 영리해요."), ("Wings like these carry a whole airship.", "이런 날개면 비행선도 뜨겠어요."))
dlg("engine_look", "plain", ("The engine is humming nicely.", "엔진 소리가 좋네요."),
    ("A good engine, a good day.", "좋은 엔진이 있으면 좋은 하루죠."), ("Nice and warm. It is really running.", "따뜻하네요. 진짜 돌아가고 있어요."))
dlg("balloon_look", "plain", ("The burner is lit. The envelope is filling!", "버너가 켜졌네요. 기구가 부풀고 있어요!"),
    ("A hot air balloon, how lovely.", "열기구라니, 멋져요."), ("Up, up and away...", "두둥실 떠오르겠어요..."))

TASTE_CHAT = {
    "history": [("Did you know some of these machines look like ancient engineering?", "이 기계들, 고대 건축물 같지 않아요?"),
                ("I love anything with a story behind it.", "사연 있는 물건이 제일 좋아요.")],
    "cars": [("A good engine sound makes my day.", "엔진 소리만 들어도 기분이 좋아져요."),
             ("Do you build anything with wheels here?", "여기서 바퀴 달린 것도 만드나요?")],
    "airplanes": [("I dream of building a plane someday.", "언젠가 비행기를 만드는 게 꿈이에요."),
                  ("Anything that flies, I want it.", "하늘을 나는 건 뭐든 좋아요.")],
    "coffee": [("I can't think straight without coffee.", "커피 없인 생각이 안 돼요."),
               ("The smell of roasted beans... heaven.", "볶은 원두 냄새... 천국이에요.")],
    "cola": [("Nothing beats an ice-cold cola.", "얼음 든 콜라보다 좋은 건 없죠."),
             ("Is there something fizzy around here?", "여기 탄산 같은 거 없나요?")],
}
for taste, lines in TASTE_CHAT.items():
    dlg("taste_" + taste, "plain", *lines)


def check_dialogue_against_java():
    """Every Situation in Dialogue.java must have all its lines here, in every needed variant."""
    src = open(DIALOGUE_JAVA, encoding="utf-8").read()
    problems = []
    seen = set()
    for sid, personality, count in re.findall(r'\w+\("(\w+)",\s*(true|false),\s*(\d+)\)', src):
        seen.add(sid)
        variants = VARIANTS if personality == "true" else ["plain"]
        for v in variants:
            have = len(D.get(sid, {}).get(v, []))
            if have != int(count):
                problems.append("%s.%s: Java expects %s lines, script has %d" % (sid, v, count, have))
    for sid in D:
        if not sid.startswith("taste_") and sid not in seen:
            problems.append("%s: in script but not declared in Dialogue.java" % sid)
    for taste in TASTE_CHAT:
        if len(D["taste_" + taste]["plain"]) != 2:
            problems.append("taste_%s needs exactly 2 lines" % taste)
    if problems:
        raise SystemExit("Dialogue mismatch:\n  " + "\n  ".join(problems))


def main():
    check_dialogue_against_java()
    for sid, variants in D.items():
        for variant, lines in variants.items():
            for i, (en, ko) in enumerate(lines, 1):
                add("dialogue.tycoonrogue.%s.%s.%d" % (sid, variant, i), en, ko)
    for extra in EXTRA:
        add(*extra)
    os.makedirs(LANG_DIR, exist_ok=True)
    for idx, name in ((0, "en_us.json"), (1, "ko_kr.json")):
        data = {k: v[idx] for k, v in T.items()}
        with open(os.path.join(LANG_DIR, name), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("wrote %d keys per language" % len(T))


def _load_cards():
    import importlib.util
    spec = importlib.util.spec_from_file_location("gen_cards", os.path.join(os.path.dirname(os.path.abspath(__file__)), "gen_cards.py"))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


_cards = _load_cards()
_cards.write_json()

EXTRA = []

# rarity names, shown on cards
for _r, (_en, _ko) in _cards.RARITY_NAMES.items():
    EXTRA.append(("rarity.tycoonrogue." + _r, _en, _ko))

# card names and descriptions, generated from the same table as the card JSON files
for _cid, _rarity, _fx, _jobs, _tastes, (_nen, _nko), (_den, _dko) in _cards.CARDS:
    EXTRA.append(("card.tycoonrogue.%s.name" % _cid, _nen, _nko))
    EXTRA.append(("card.tycoonrogue.%s.desc" % _cid, _den, _dko))

# rounds, cards and HUD
EXTRA += [
    ("message.tycoonrogue.round.success", "Round %1$s cleared! Revenue %2$s / quota %3$s. Choose a card!",
     "라운드 %1$s 성공! 매출 %2$s / 목표 %3$s. 카드를 선택하세요!"),
    ("message.tycoonrogue.round.fail", "Round %1$s failed. Revenue %2$s / quota %3$s. Reputation %4$s.",
     "라운드 %1$s 실패. 매출 %2$s / 목표 %3$s. 평판 %4$s."),
    ("message.tycoonrogue.run.over", "Too many failed rounds in a row. The run is over and starts again from round 1.",
     "라운드를 연속으로 너무 많이 실패했습니다. 이번 런이 끝나고 1라운드부터 다시 시작합니다."),
    ("message.tycoonrogue.card.picked", "%1$s picked the card: %2$s", "%1$s 님이 카드를 선택했습니다: %2$s"),
    ("screen.tycoonrogue.cards.title", "Round %s cleared! Choose a card", "라운드 %s 성공! 카드를 선택하세요"),
    ("screen.tycoonrogue.cards.reroll", "Reroll (%s coins, %s left)", "다시 뽑기 (%s 코인, %s회 남음)"),
    ("message.tycoonrogue.reroll.poor", "You need %s coins to reroll.", "다시 뽑으려면 %s 코인이 필요해요."),
    ("message.tycoonrogue.deck.header", "Your deck: %s cards", "내 덱: 카드 %s장"),
    ("screen.tycoonrogue.cards.hint", "Click a card to take it", "카드를 클릭해서 선택"),
    ("screen.tycoonrogue.cards.picked", "Card chosen!", "카드 선택 완료!"),
    ("screen.tycoonrogue.deck", "Your Deck", "내 덱"),
    ("screen.tycoonrogue.deck.empty", "No cards yet. Clear a round to earn one.", "아직 카드가 없어요. 라운드를 클리어하면 받아요."),
    ("key.tycoonrogue.deck", "Open deck", "덱 열기"),
    ("key.categories.tycoonrogue", "Factory Tycoon Rogue", "팩토리 타이쿤 로그"),
    ("hud.tycoonrogue.round", "Round %s", "라운드 %s"),
    ("hud.tycoonrogue.revenue", "Revenue %1$s / %2$s", "매출 %1$s / %2$s"),
    ("hud.tycoonrogue.reputation", "Reputation %s", "평판 %s"),
    ("hud.tycoonrogue.cards", "Cards: %s", "카드: %s장"),
    ("hud.tycoonrogue.rumor", "Bad rumor: %s", "나쁜 소문: %s"),
]

# phone, counter, reviews and news
EXTRA += [
    ("item.tycoonrogue.phone", "Phone", "휴대폰"),
    ("block.tycoonrogue.tycoon_counter", "Tycoon Counter", "타이쿤 카운터"),
    ("reason.tycoonrogue.rating", "factory rating", "공장 평가"),
    ("reason.tycoonrogue.robbed", "got robbed", "강도를 당함"),
    ("reason.tycoonrogue.raided", "raided by armed robbers", "무장 강도에게 털림"),
    ("message.tycoonrogue.counter.header", "== Factory report ==", "== 공장 보고서 =="),
    ("message.tycoonrogue.counter.machines", "Machines: %1$s blocks, %2$s kinds. Scale %3$s/25, variety %4$s/10",
     "기계: %1$s개, %2$s종류. 규모 %3$s/25, 다양성 %4$s/10"),
    ("message.tycoonrogue.counter.output", "Revenue %1$s / %2$s. Output score %3$s/15",
     "매출 %1$s / %2$s. 생산 점수 %3$s/15"),
    ("reason.tycoonrogue.guard_caught", "a guard caught a criminal", "경비원이 범인을 잡음"),
    ("feed.tycoonrogue.news.guard_catch", "Factory guard collars a thief", "공장 경비원이 도둑을 현장에서 검거"),
    ("message.tycoonrogue.counter.history", "History: tiled roofs %1$s, painted eaves %2$s, libraries %3$s = %4$s/25 (history lovers only count buildings, not chest items)",
     "역사: 기와 지붕 %1$s, 단청 %2$s, 도서관 %3$s = %4$s/25 (역사 손님은 상자 속 물건이 아닌 건축물만 봐요)"),
    ("message.tycoonrogue.counter.showcase", "Showcase (put matching items in chests nearby): %s",
     "전시 (근처 상자에 취향 물건을 넣으세요): %s"),
    ("message.tycoonrogue.rated", "A %1$s rated your factory %2$s (score %3$s). %4$s showcase: %5$s/25",
     "%1$s 손님이 공장을 %2$s로 평가했어요 (점수 %3$s). %4$s 전시: %5$s/25"),
    ("item.tycoonrogue.hologram_guide", "Hologram Guide", "홀로그램 가이드"),
    ("block.tycoonrogue.supply_desk", "Supply Desk", "공급 상점"),
    ("block.tycoonrogue.display_stand", "Display Stand", "전시대"),
    ("shop.tycoonrogue.display_stand.name", "Display Stands x2", "전시대 2개"),
    ("shop.tycoonrogue.display_stand.desc", "A glass-domed pedestal. Right-click with an item to put it on show.", "유리 돔 받침대. 아이템을 들고 우클릭하면 전시해요."),
    ("screen.tycoonrogue.ledger", "Ledger", "장부"),
    ("screen.tycoonrogue.ledger.time", "Time left %1$s", "남은 시간 %1$s"),
    ("screen.tycoonrogue.ledger.sales", "Sales vs. quota", "매출 / 목표"),
    ("screen.tycoonrogue.ledger.pace_ok", "On pace", "순조로워요"),
    ("screen.tycoonrogue.ledger.pace_late", "Behind schedule", "목표보다 늦어요"),
    ("screen.tycoonrogue.ledger.left", "Still needed: %1$s", "남은 목표: %1$s"),
    ("screen.tycoonrogue.ledger.reputation", "Reputation", "평판"),
    ("key.tycoonrogue.ledger", "Open ledger", "장부 열기"),
    ("screen.tycoonrogue.shop", "Supply Desk", "공급 상점"),
    ("screen.tycoonrogue.shop.balance", "Money: %s emeralds", "보유: 에메랄드 %s개"),
    ("screen.tycoonrogue.shop.balance_coins", "Money: %s emeralds worth", "보유: 에메랄드 %s개 상당"),
    ("screen.tycoonrogue.shop.hint", "Spend your earnings. Click to buy.", "번 돈으로 구입하세요. 클릭하면 구매해요."),
    ("screen.tycoonrogue.shop.click", "Click to buy", "클릭해서 구매"),
    ("screen.tycoonrogue.shop.poor", "Not enough money", "돈이 부족해요"),
    ("message.tycoonrogue.shop.poor", "Not enough money.", "돈이 부족해요."),
    ("message.tycoonrogue.shop.bought", "Bought: %s", "구매 완료: %s"),
    ("message.tycoonrogue.hologram.on", "Hologram placed. Sneak + right-click to switch it off.", "홀로그램을 설치했어요. 웅크리고 우클릭하면 꺼요."),
    ("message.tycoonrogue.hologram.off", "Hologram off.", "홀로그램을 껐어요."),
    ("message.tycoonrogue.hologram.progress", "Hanok progress: %1$s / %2$s blocks", "한옥 진행도: %1$s / %2$s 블록"),
    ("message.tycoonrogue.hologram.done", "Hanok complete! History lovers will be impressed.", "한옥 완성! 역사 손님들이 감탄할 거예요."),
    ("hologram.tycoonrogue.title", "Hanok Hologram  %1$s/%2$s", "한옥 홀로그램  %1$s/%2$s"),
    ("hologram.tycoonrogue.step", "Step %1$s/%2$s: %3$s", "단계 %1$s/%2$s: %3$s"),
    ("hologram.tycoonrogue.banner", "Step %1$s/%2$s: build the %3$s", "%1$s/%2$s단계: %3$s 짓기"),
    ("hologram.tycoonrogue.complete", "Complete!", "완성!"),
    ("hologram.tycoonrogue.wrong", "Wrong block! Needs %1$s", "잘못된 블록! %1$s 필요"),
    ("hologram.tycoonrogue.hint", "Coloured fill = place here. Red = wrong block.", "색 칠해진 곳 = 여기에 놓기. 빨강 = 잘못된 블록."),
    ("hologram.tycoonrogue.layer.0", "stone platform", "돌 기단"),
    ("hologram.tycoonrogue.layer.1", "pillars (bottom)", "기둥 (아래)"),
    ("hologram.tycoonrogue.layer.2", "pillars (middle)", "기둥 (가운데)"),
    ("hologram.tycoonrogue.layer.3", "pillars (top)", "기둥 (위)"),
    ("hologram.tycoonrogue.layer.4", "painted dancheong beams", "단청 보"),
    ("hologram.tycoonrogue.layer.5", "wide eaves", "넓은 처마"),
    ("hologram.tycoonrogue.layer.6", "lower tiled roof", "아래 기와 지붕"),
    ("hologram.tycoonrogue.layer.7", "upper tiled roof", "위 기와 지붕"),
    ("hologram.tycoonrogue.layer.8", "roof ridge", "용마루"),
    ("shop.tycoonrogue.hologram_guide.name", "Hologram Guide", "홀로그램 가이드"),
    ("shop.tycoonrogue.hologram_guide.desc", "Shows a hanok pavilion as glowing boxes. Right-click a block to place it.", "한옥 정자를 빛나는 상자로 보여줘요. 블록을 우클릭해 설치해요."),
    ("shop.tycoonrogue.giwa_kit.name", "Giwa Roof Kit", "기와 지붕 재료"),
    ("shop.tycoonrogue.giwa_kit.desc", "64 tile stairs and 48 tile slabs for a tiled roof.", "기와 지붕용 타일 계단 64개, 반블록 48개."),
    ("shop.tycoonrogue.dancheong_kit.name", "Dancheong Kit", "단청 재료"),
    ("shop.tycoonrogue.dancheong_kit.desc", "Red, green, blue and yellow terracotta plus pillar logs.", "붉은·초록·파랑·노랑 테라코타와 기둥용 목재."),
    ("shop.tycoonrogue.pavilion_kit.name", "Full Hanok Pavilion Kit", "한옥 정자 전체 재료"),
    ("shop.tycoonrogue.pavilion_kit.desc", "Every block the hologram pavilion needs, in the exact amounts.", "홀로그램 정자에 필요한 모든 블록을 딱 맞게 담았어요."),
    ("shop.tycoonrogue.library_kit.name", "Library Kit", "도서관 재료"),
    ("shop.tycoonrogue.library_kit.desc", "Bookshelves, lecterns and map tables for history workshops.", "책장, 교탁, 지도 제작대. 역사 공방용."),
    ("shop.tycoonrogue.lantern_set.name", "Lantern Set", "등불 세트"),
    ("shop.tycoonrogue.lantern_set.desc", "8 lanterns and a bell for the finishing touch.", "마무리용 등불 8개와 종."),
    ("shop.tycoonrogue.flight_guide.name", "Flight Guide", "비행 가이드"),
    ("shop.tycoonrogue.flight_guide.desc", "A book on building flying machines with Create Aeronautics.", "Create Aeronautics로 비행체를 만드는 법을 알려주는 책."),
    ("shop.tycoonrogue.giwa_guide.name", "Giwa Guide", "기와 가이드"),
    ("shop.tycoonrogue.giwa_guide.desc", "How to build a real tiled hanok roof, and how it is scored.", "진짜 한옥 기와 지붕 짓는 법과 평가 방식."),
    ("shop.tycoonrogue.dancheong_guide.name", "Dancheong Guide", "단청 가이드"),
    ("shop.tycoonrogue.dancheong_guide.desc", "The five traditional colours and how to paint beams with blocks.", "오방색과 블록으로 들보를 칠하는 법."),
    ("shop.tycoonrogue.car_guide.name", "Car Guide", "자동차 가이드"),
    ("shop.tycoonrogue.car_guide.desc", "Build a drivable car with Offroad wheels and Simulated.", "Offroad 바퀴와 Simulated로 굴러가는 자동차 만들기."),
    ("shop.tycoonrogue.aero_kit.name", "Aircraft Parts Kit", "비행기 부품 키트"),
    ("shop.tycoonrogue.aero_kit.desc", "Propeller bearing, physics assembler, typewriter, engines, sails and more.", "프로펠러 베어링, 물리 조립기, 타자기, 엔진, 돛 등."),
    ("shop.tycoonrogue.car_kit.name", "Car Parts Kit", "자동차 부품 키트"),
    ("shop.tycoonrogue.car_kit.desc", "Wheel mounts, physics assembler, typewriter, engines and chassis.", "휠 마운트, 물리 조립기, 타자기, 엔진, 섀시."),
    ("book.tycoonrogue.giwa.1", "GIWA ROOFS\n\nA hanok roof is covered in curved clay tiles (giwa): concave female tiles and convex male tiles, ending in decorated round end tiles. Two common shapes: gable (matbae) and hip-and-gable (paljak).",
     "기와 지붕\n\n한옥 지붕은 휘어진 흙 기와(암키와·수키와)로 덮고, 끝은 무늬 있는 와당으로 마무리해요. 흔한 모양은 맞배지붕과 팔작지붕이에요."),
    ("book.tycoonrogue.giwa.2", "IN MINECRAFT\n\nUse dark roof blocks: deepslate tile, stone brick or blackstone stairs and slabs. Step each layer one block inward. Let the eaves stick out 1-2 blocks past the wall.",
     "마인크래프트에서\n\n어두운 지붕 블록(딥슬레이트 타일, 석재 벽돌, 블랙스톤의 계단과 반블록)을 쓰고, 층마다 한 칸씩 안쪽으로 줄이세요. 처마는 벽보다 1~2칸 내밀어요."),
    ("book.tycoonrogue.giwa.3", "THE CURVE\n\nThe famous upturned corners (chuyeo) come from raising the four corners one slab higher than the middle of each edge. Put upside-down stairs under the eaves as rafters (seokkaerae). Finish the ridge (yongmaru) with a row of slabs.",
     "처마 곡선\n\n하늘로 들린 처마 모서리(추녀)는 네 모서리를 변 가운데보다 반블록 높게 해서 만들어요. 처마 밑에는 뒤집은 계단으로 서까래를 만들고, 용마루는 반블록 한 줄로 마무리하세요."),
    ("book.tycoonrogue.giwa.4", "SCORING\n\nTile-roof stairs and slabs count: 6 blocks = 1 point, up to 12 points. History lovers need real buildings, not chest items. Buy the Hologram Guide to see a full pavilion.",
     "평가 방식\n\n기와 계단·반블록이 점수가 돼요. 6개당 1점, 최대 12점. 역사 손님은 상자 속 물건이 아니라 진짜 건물을 봐요. 홀로그램 가이드를 사면 완성된 정자를 미리 볼 수 있어요."),
    ("book.tycoonrogue.dancheong.1", "DANCHEONG\n\nDancheong is the painted decoration on Korean wooden buildings. It uses five traditional colours (obangsaek): blue, white, red, black and yellow. Green is a common stand-in for the blue family.",
     "단청\n\n단청은 한국 목조 건물에 칠하는 장식 그림이에요. 오방색인 청·백·적·흑·황 다섯 색을 쓰고, 청색 계열은 녹색으로 많이 표현해요."),
    ("book.tycoonrogue.dancheong.2", "PATTERN\n\nSimple gitgi style: paint each beam with a blue or green base, red at both ends, thin white lines between, and a yellow centre. Pillars are plain red.",
     "무늬\n\n간단한 긋기단청: 들보마다 청색이나 녹색 바탕, 양 끝은 붉은색, 사이에 가는 흰 줄, 가운데는 노란색으로 칠하세요. 기둥은 붉은색 한 가지예요."),
    ("book.tycoonrogue.dancheong.3", "IN MINECRAFT\n\nUse terracotta: blue, green, red, yellow, white, black. Pillars: stripped crimson stems. Put a beam ring just under the eaves, and brackets (gongpo) of stairs and trapdoors on top of the pillars.",
     "마인크래프트에서\n\n테라코타(파랑·초록·빨강·노랑·흰색·검정)를 쓰세요. 기둥은 껍질 벗긴 진홍 줄기, 처마 바로 밑에 들보 띠를 두르고 기둥 위에는 계단과 다락문으로 공포를 만드세요."),
    ("book.tycoonrogue.dancheong.4", "SCORING\n\nPainted-eave blocks count: 5 blocks = 1 point, up to 10 points. Lanterns and bells add up to 3 more. The pavilion hologram uses exactly this pattern.",
     "평가 방식\n\n단청 블록 5개당 1점, 최대 10점. 등불과 종은 최대 3점이 더 붙어요. 홀로그램 정자가 바로 이 무늬예요."),
    ("book.tycoonrogue.flight.1", "FLIGHT GUIDE\n\nCreate Aeronautics (with Simulated) turns a structure of blocks into a flying vehicle. This guide follows the public 'Survival Plane MK1' build on createmod.com. Details may change with mod updates.",
     "비행 가이드\n\nCreate Aeronautics(와 Simulated)는 블록 구조물을 날아다니는 탈것으로 만들어요. 이 가이드는 createmod.com의 공개 설계도 'Survival Plane MK1'을 따랐어요. 모드 업데이트로 달라질 수 있어요."),
    ("book.tycoonrogue.flight.2", "BODY AND WINGS\n\nThe body and wings are sails: create:white_sail and simulated:white_symmetric_sail. They are light. Add a few levitite blocks (aeronautics) near the middle for lift. Keep the shape symmetric.",
     "기체와 날개\n\n기체와 날개는 돛 블록(create:white_sail, simulated:white_symmetric_sail)으로 만들어요. 가벼워요. 가운데 근처에 레비타이트 블록(aeronautics)을 몇 개 넣어 양력을 주고, 좌우 대칭으로 만드세요."),
    ("book.tycoonrogue.flight.3", "POWER\n\nPut a propeller bearing (aeronautics:propeller_bearing) on the nose. Feed it with Create shafts, cogwheels and gearshifts from portable engines (simulated:red_portable_engine) behind the seat. A simulated analog transmission lets you adjust propeller speed.",
     "동력\n\n기수에 프로펠러 베어링(aeronautics:propeller_bearing)을 달고, 좌석 뒤의 휴대용 엔진(simulated:red_portable_engine)에서 Create 축·톱니바퀴·기어시프트로 연결하세요. 아날로그 트랜스미션으로 프로펠러 속도를 조절해요."),
    ("book.tycoonrogue.flight.4", "ASSEMBLE AND FLY\n\nPlace a physics assembler (simulated:physics_assembler) against the frame and use it; afterwards it can be removed. Add fuel to the engines, sit in the seat, and right-click the linked typewriter. In Survival Plane MK1: W/S pitch, A/D roll, hold Shift for throttle, Ctrl to brake.",
     "조립과 비행\n\n골조에 물리 조립기(simulated:physics_assembler)를 붙여 사용하세요. 조립 후에는 떼어내도 돼요. 엔진에 연료를 넣고, 좌석에 앉아 연결된 타자기(linked typewriter)를 우클릭하세요. Survival Plane MK1은 W/S 피치, A/D 롤, Shift 가속, Ctrl 제동이에요."),
    ("book.tycoonrogue.flight.5", "SCORING\n\nAirplane lovers count a propeller bearing, levitite, sails and engines near the Tycoon Counter, and airplane items in chests. Hold W over any item in JEI to open its Ponder for exact steps.",
     "평가 방식\n\n비행기 손님은 타이쿤 카운터 근처의 프로펠러 베어링, 레비타이트, 돛, 엔진과 상자 속 비행기 아이템을 봐요. 정확한 방법은 JEI에서 아이템 위에 W를 누르면 나오는 Ponder에서 확인하세요."),
    ("book.tycoonrogue.car.1", "CAR GUIDE\n\nCars use Create Offroad wheel mounts (offroad:wheel_mount) on a frame that Simulated's physics assembler turns into a vehicle. Details may change with mod updates.",
     "자동차 가이드\n\n자동차는 Create Offroad의 휠 마운트(offroad:wheel_mount)를 단 골조를 Simulated의 물리 조립기로 탈것으로 만들어요. 모드 업데이트로 달라질 수 있어요."),
    ("book.tycoonrogue.car.2", "CHASSIS\n\nBuild a flat frame from Create casings and a linear chassis, with a seat (create:red_seat) in the middle. Keep it wide and low so it does not tip.",
     "섀시\n\nCreate 케이싱과 선형 섀시로 납작한 골조를 만들고 가운데에 좌석(create:red_seat)을 놓으세요. 넓고 낮게 만들어야 뒤집히지 않아요."),
    ("book.tycoonrogue.car.3", "WHEELS AND POWER\n\nPlace wheel mounts at the four corners and attach the wheels. Power them with Create shafts and cogwheels from portable engines (simulated:red_portable_engine); a gearshift lets you reverse.",
     "바퀴와 동력\n\n네 모서리에 휠 마운트를 놓고 바퀴를 달아요. 휴대용 엔진(simulated:red_portable_engine)에서 Create 축과 톱니바퀴로 동력을 전달하고, 기어시프트로 후진을 만들 수 있어요."),
    ("book.tycoonrogue.car.4", "ASSEMBLE AND DRIVE\n\nUse the physics assembler on the frame, fuel the engines, sit down and right-click the linked typewriter to take control. Tune speed with an analog transmission.",
     "조립과 운전\n\n골조에 물리 조립기를 사용하고, 엔진에 연료를 넣은 뒤 좌석에 앉아 타자기를 우클릭해 조종하세요. 아날로그 트랜스미션으로 속도를 조절해요."),
    ("book.tycoonrogue.car.5", "SCORING\n\nCar lovers count wheel mounts, engines and a physics assembler near the Tycoon Counter, plus car items in chests. Hold W over an item in JEI for its Ponder.",
     "평가 방식\n\n자동차 손님은 타이쿤 카운터 근처의 휠 마운트, 엔진, 물리 조립기와 상자 속 자동차 아이템을 봐요. 아이템 위에 W를 누르면 Ponder가 나와요."),
    ("message.tycoonrogue.counter.operation", "Running machines: %1$s (spinning or lit) = %2$s/15", "실제 작동 중인 기계: %1$s개 (회전·점화) = %2$s/15"),
    ("message.tycoonrogue.counter.art", "Art: %1$s/10. Most used colours: %2$s", "예술성: %1$s/10. 많이 쓴 색: %2$s"),
    ("message.tycoonrogue.counter.vehicles", "Vehicles: aircraft %1$s/25, cars %2$s/25 (Create Aeronautics / Offroad parts)", "탈것: 비행체 %1$s/25, 자동차 %2$s/25 (Create Aeronautics / Offroad 부품)"),
    ("message.tycoonrogue.rated.flow", "Steady production %1$s/10 · Inspection %2$s/8 (saw %3$s working)", "꾸준한 생산 %1$s/10 · 현장 점검 %2$s/8 (작동하는 곳 %3$s곳 확인)"),
    ("message.tycoonrogue.counter.flow", "Steady production: %1$s/10 (goods produced in %2$s of the last 24 checks, a full score needs 16)", "꾸준한 생산: %1$s/10 (최근 24번 확인 중 %2$s번 생산, 만점은 16번)"),
    ("message.tycoonrogue.counter.parts", "Inspection spots: belts %1$s, wheels %2$s, rocket engines %3$s, propeller/windmill bearings %4$s, sails/wings %5$s, engines %6$s, balloons %7$s", "점검 장소: 벨트 %1$s개, 바퀴 %2$s개, 로켓 엔진 %3$s개, 프로펠러·풍차 베어링 %4$s개, 풍차 날개 %5$s개, 엔진 %6$s개, 열기구 %7$s개"),
    ("message.tycoonrogue.rated.detail", "Running %1$s/15 · Art %2$s/10 · Favourite colour (%3$s) %4$s/10", "작동 %1$s/15 · 예술 %2$s/10 · 좋아하는 색(%3$s) %4$s/10"),
    ("screen.tycoonrogue.phone", "Phone", "휴대폰"),
    ("screen.tycoonrogue.phone.sns", "SNS", "SNS"),
    ("screen.tycoonrogue.phone.now", "now", "지금"),
    ("screen.tycoonrogue.phone.sns_title", "Factory Talk", "공장 톡톡"),
    ("screen.tycoonrogue.phone.news_title", "Tycoon News", "타이쿤 뉴스"),
    ("screen.tycoonrogue.phone.rep", "Rep %s", "평판 %s"),
    ("screen.tycoonrogue.phone.rumor", "Rumor %s", "소문 %s"),
    ("screen.tycoonrogue.phone.news", "News", "뉴스"),
    ("screen.tycoonrogue.phone.status", "Rating %1$s  Rep %2$s  Rumor %3$s", "평점 %1$s  평판 %2$s  소문 %3$s"),
    ("screen.tycoonrogue.phone.trend", "Trending: %s", "인기 직업: %s"),
    ("screen.tycoonrogue.phone.empty", "Nothing yet.", "아직 아무것도 없어요."),
    ("feed.tycoonrogue.review.1.1", "Terrible factory. Never again.", "최악의 공장. 다신 안 가요."),
    ("feed.tycoonrogue.review.1.2", "Tiny and boring. 1 star.", "작고 지루해요. 별 1개."),
    ("feed.tycoonrogue.review.2.1", "Disappointing. Expected more.", "실망이에요. 더 기대했어요."),
    ("feed.tycoonrogue.review.2.2", "Not great, not terrible.", "별로예요. 아주 나쁘진 않지만."),
    ("feed.tycoonrogue.review.3.1", "Decent factory. Worth a visit.", "괜찮은 공장. 한번 가볼 만해요."),
    ("feed.tycoonrogue.review.3.2", "Okay place, fair prices.", "무난한 곳, 가격도 적당해요."),
    ("feed.tycoonrogue.review.4.1", "Great factory! Highly recommend.", "훌륭한 공장! 강력 추천해요."),
    ("feed.tycoonrogue.review.4.2", "Loved the machines here!", "여기 기계들 정말 좋아요!"),
    ("feed.tycoonrogue.review.5.1", "Best factory in the world! 5 stars!", "세계 최고의 공장! 별 다섯 개!"),
    ("feed.tycoonrogue.review.5.2", "Mind-blowing! Go there NOW!", "대박! 지금 당장 가보세요!"),
    ("feed.tycoonrogue.gun.1", "The owner pointed a GUN at me! Stay away!", "사장이 나한테 총을 겨눴어요! 가지 마세요!"),
    ("feed.tycoonrogue.gun.2", "Scary place. They aim guns at customers.", "무서운 곳. 손님에게 총을 겨눠요."),
    ("feed.tycoonrogue.news.gun", "Factory owner reported pointing a gun at a customer", "공장 주인이 손님에게 총을 겨눴다는 제보"),
    ("feed.tycoonrogue.news.scam", "Scammer fleeces local factory", "사기꾼이 공장을 상대로 사기 행각"),
    ("feed.tycoonrogue.news.robbery", "Robber strikes factory owner in broad daylight", "대낮에 공장 주인 상대 강도 사건"),
    ("feed.tycoonrogue.news.raid", "Armed raiders hit the factory district", "무장 강도단이 공장 지대를 습격"),
    ("feed.tycoonrogue.news.bad_factory", "Visitors call local factory a 'disappointment'", "방문객들 '실망스러운 공장'이라 혹평"),
    ("feed.tycoonrogue.news.round_success", "Factory hits its target in round %s", "공장, %s라운드 목표 달성"),
    ("feed.tycoonrogue.news.round_fail", "Factory misses its target in round %s", "공장, %s라운드 목표 미달"),
    ("feed.tycoonrogue.viral.1", "Streamed this factory live. Chat went wild!", "이 공장 생방송했는데 채팅창 난리났어요!"),
    ("feed.tycoonrogue.viral.2", "New video up: I visited the factory everyone talks about!", "새 영상 업로드: 다들 말하던 그 공장 다녀왔어요!"),
    ("feed.tycoonrogue.news.streamer", "Popular streamer's factory visit goes viral", "인기 스트리머의 공장 방문 영상이 화제"),
    ("feed.tycoonrogue.news.trend", "Demand surges for %s goods", "%s 관련 물품 수요 급증"),
]

if __name__ == "__main__":
    sys.exit(main())
