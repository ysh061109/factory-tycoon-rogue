#!/usr/bin/env python3
"""
The card pool. This one table produces both the card JSON files (the game rules) and the
card names and descriptions in English and Korean, so the numbers shown to the player always
match the numbers the game uses.

Run:  python3 tools/gen_cards.py     (tools/gen_lang.py also loads this table)
Output: src/main/resources/data/tycoonrogue/tycoonrogue/cards/<id>.json

Effect keys (all additive, 0.25 means +25%):
  visitor_rate     more (or fewer) visitors
  reward_mult      higher payment for every delivery
  reputation_gain  extra reputation per completed order (whole number)
  quota_mult       changes the round quota (positive = harder)
  threat_mult      changes how often robbers, scammers and raids come (negative = safer)
  tip_mult         bigger tips when customers rate the factory
  demand_bonus     extra payment for the jobs listed in "jobs" (all jobs if the list is empty)
  rating_bonus     flat points (0..100 scale) added to every factory score
  taste_bonus      extra factory rating from customers whose taste is listed in "tastes"
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
CARD_DIR = os.path.join(ROOT, "src", "main", "resources", "data", "tycoonrogue", "tycoonrogue", "cards")

# id, rarity, effects, jobs, tastes, (name_en, name_ko), (desc_en, desc_ko)
CARDS = [
    ("word_of_mouth", "common", {"visitor_rate": 0.25}, [], [],
     ("Word of Mouth", "입소문"),
     ("Visitors +25%.", "방문객 +25%.")),
    ("tool_rush", "common", {"demand_bonus": 0.40}, ["blacksmith", "miner", "builder", "mechanic"], [],
     ("Tool Rush", "도구 수요 폭발"),
     ("Blacksmiths, miners, builders and mechanics pay +40%.", "대장장이, 광부, 건축가, 정비사가 +40% 더 지불.")),
    ("kitchen_rush", "common", {"demand_bonus": 0.40}, ["chef", "farmer", "barista", "soda_vendor"], [],
     ("Kitchen Rush", "식자재 대란"),
     ("Chefs, farmers, baristas and soda vendors pay +40%.", "요리사, 농부, 바리스타, 음료 판매원이 +40% 더 지불.")),
    ("polite_service", "common", {"reward_mult": 0.10, "reputation_gain": 1}, [], [],
     ("Polite Service", "친절한 응대"),
     ("All payments +10%. +1 reputation per completed order.", "모든 지불 +10%. 주문 완료 시 평판 +1 추가.")),
    ("neon_sign", "common", {"visitor_rate": 0.15, "reputation_gain": 1}, [], [],
     ("Neon Sign", "네온 간판"),
     ("Visitors +15%. +1 reputation per completed order.", "방문객 +15%. 주문 완료 시 평판 +1 추가.")),
    ("history_corner", "common", {"taste_bonus": 0.50}, [], ["history"],
     ("History Corner", "역사 전시 코너"),
     ("History lovers rate your factory +50% higher.", "역사를 좋아하는 손님의 공장 평가 +50%.")),
    ("viral_post", "rare", {"visitor_rate": 0.60, "reputation_gain": 1}, [], [],
     ("Viral Post", "SNS 대박"),
     ("Visitors +60%. +1 reputation per completed order.", "방문객 +60%. 주문 완료 시 평판 +1 추가.")),
    ("security_guard", "rare", {"threat_mult": -0.50}, [], [],
     ("Security Guard", "경비원 고용"),
     ("Robbers, scammers and raids come 50% less often.", "강도, 사기꾼, 습격이 50% 덜 찾아옴.")),
    ("vip_lounge", "rare", {"reward_mult": 0.25, "tip_mult": 0.50}, [], [],
     ("VIP Lounge", "VIP 라운지"),
     ("All payments +25%. Tips +50%.", "모든 지불 +25%. 팁 +50%.")),
    ("coffee_craze", "rare", {"demand_bonus": 0.50, "taste_bonus": 0.50}, ["barista", "soda_vendor"], ["coffee", "cola"],
     ("Coffee & Cola Craze", "커피와 콜라 열풍"),
     ("Baristas and soda vendors pay +50%. Coffee and cola lovers rate +50% higher.",
      "바리스타와 음료 판매원이 +50% 더 지불. 커피와 콜라를 좋아하는 손님의 평가 +50%.")),
    ("auto_show", "rare", {"demand_bonus": 0.50, "taste_bonus": 0.50}, ["mechanic", "pilot"], ["cars", "airplanes"],
     ("Auto & Air Show", "모터쇼와 에어쇼"),
     ("Mechanics and pilots pay +50%. Car and airplane lovers rate +50% higher.",
      "정비사와 조종사가 +50% 더 지불. 자동차와 비행기를 좋아하는 손님의 평가 +50%.")),
    ("rush_hour", "rare", {"reward_mult": 0.60, "quota_mult": 0.50}, [], [],
     ("Rush Hour (risk)", "러시 아워 (위험)"),
     ("All payments +60%, but the round quota is 50% higher.", "모든 지불 +60%, 하지만 라운드 목표가 50% 증가.")),
    ("tycoon_magnate", "legendary", {"reward_mult": 1.00, "quota_mult": 0.30}, [], [],
     ("Tycoon Magnate", "재벌의 투자"),
     ("All payments +100%, but the round quota is 30% higher.", "모든 지불 +100%, 하지만 라운드 목표가 30% 증가.")),
    ("fort_knox", "legendary", {"threat_mult": -0.80, "reputation_gain": 2}, [], [],
     ("Fort Knox", "철벽 보안"),
     ("Threats come 80% less often. +2 reputation per completed order.", "위협이 80% 덜 찾아옴. 주문 완료 시 평판 +2 추가.")),
    ("celebrity_visit", "legendary", {"visitor_rate": 1.00, "reward_mult": 0.30, "reputation_gain": 2}, [], [],
     ("Celebrity Visit", "유명인 방문"),
     ("Visitors +100%. All payments +30%. +2 reputation per completed order.",
      "방문객 +100%. 모든 지불 +30%. 주문 완료 시 평판 +2 추가.")),
    ("polished_showroom", "common", {"rating_bonus": 8}, [], [],
     ("Polished Showroom", "반짝이는 쇼룸"),
     ("Every factory rating +8 points.", "모든 공장 평가 점수 +8.")),
    ("car_museum", "common", {"taste_bonus": 0.50}, [], ["cars"],
     ("Car Museum Corner", "자동차 전시 코너"),
     ("Car lovers rate your factory +50% higher.", "자동차를 좋아하는 손님의 공장 평가 +50%.")),
    ("airplane_hangar", "common", {"taste_bonus": 0.50}, [], ["airplanes"],
     ("Airplane Hangar Corner", "비행기 전시 코너"),
     ("Airplane lovers rate your factory +50% higher.", "비행기를 좋아하는 손님의 공장 평가 +50%.")),
    ("tour_guide", "rare", {"tip_mult": 0.50, "rating_bonus": 5}, [], [],
     ("Tour Guide", "공장 투어 가이드"),
     ("Tips +50%. Every factory rating +5 points.", "팁 +50%. 모든 공장 평가 점수 +5.")),
    ("grand_expo", "legendary", {"rating_bonus": 20, "tip_mult": 1.00, "visitor_rate": 0.30}, [], [],
     ("Grand Expo", "그랜드 엑스포"),
     ("Every factory rating +20 points. Tips +100%. Visitors +30%.",
      "모든 공장 평가 점수 +20. 팁 +100%. 방문객 +30%.")),
]

RARITY_NAMES = {"common": ("Common", "일반"), "rare": ("Rare", "희귀"), "legendary": ("Legendary", "전설")}


def write_json():
    os.makedirs(CARD_DIR, exist_ok=True)
    for old in os.listdir(CARD_DIR):
        if old.endswith(".json"):
            os.remove(os.path.join(CARD_DIR, old))
    for cid, rarity, effects, jobs, tastes, _name, _desc in CARDS:
        data = {"rarity": rarity, "effects": effects}
        if jobs:
            data["jobs"] = jobs
        if tastes:
            data["tastes"] = tastes
        with open(os.path.join(CARD_DIR, cid + ".json"), "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    write_json()
    print("wrote %d cards" % len(CARDS))
