# -*- coding: utf-8 -*-
"""Generate en_us.json and es_es.json for Village Diplomacy update."""
import json
import sys
from pathlib import Path

_SCRIPTS_DIR = Path(__file__).resolve().parent
if str(_SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS_DIR))
from animal_attack_lines import inject_animal_attack
from animal_death_lines import inject_animal_death
from door_react_lines import inject_door_react
from golem_lines import inject_golem_lines
from i18n_misc import inject_misc
from place_react_lines import inject_place_react
from personality_misc_lines import inject_personality_misc

ROOT = Path(__file__).resolve().parents[1]
OUT_EN = ROOT / "src/main/resources/assets/villagediplomacy/lang/en_us.json"
OUT_ES = ROOT / "src/main/resources/assets/villagediplomacy/lang/es_es.json"

# --- English theft reactions (from original mod) ---
THEFT_CHEST_ADULT_EN = [
    "§c[Villager] HEY! That's MINE!",
    "§c[Villager] *gasps* A THIEF!",
    "§c[Villager] STOP! Give that back!",
    "§c[Villager] You're... you're robbing us!",
    "§c[Villager] I can't believe you're doing this!",
    "§c[Villager] GUARDS! We have a thief!",
    "§c[Villager] How DARE you open that!",
    "§c[Villager] That chest is NOT yours!",
    "§c[Villager] Stay away from my belongings!",
    "§c[Villager] What do you think you're doing!?",
    "§c[Villager] I worked YEARS for what's in there!",
    "§c[Villager] You'll regret this, thief!",
    "§c[Villager] That's private property!",
    "§c[Villager] My life savings are in there!",
    "§c[Villager] HELP! ROBBERY!",
    "§c[Villager] This is a breach of trust!",
    "§c[Villager] Get away from my chest!",
    "§c[Villager] You're no better than a pillager!",
]
THEFT_CHEST_BABY_EN = [
    "§c[Baby Villager] *cries* Mommy! They're stealing our stuff!",
    "§c[Baby Villager] Noooo! That's our family chest!",
    "§c[Baby Villager] Why are you mean? *sobs*",
    "§c[Baby Villager] I'm telling my dad!",
    "§c[Baby Villager] *runs away crying* THIEF!",
    "§c[Baby Villager] That's not yours! *cries*",
    "§c[Baby Villager] Bad person! Bad!",
    "§c[Baby Villager] My toys are in there!",
    "§c[Baby Villager] *shouts* STOOOOP!",
    "§c[Baby Villager] You're a big bully!",
    "§c[Baby Villager] I'm scared! *cries*",
    "§c[Baby Villager] Daddy said strangers are dangerous!",
    "§c[Baby Villager] *whimpers* Not the chest...",
]
THEFT_LOOT_ADULT_EN = [
    "§c[Villager] Those are OUR supplies!",
    "§c[Villager] You're taking everything we have!",
    "§c[Villager] THIEF! Someone help!",
    "§c[Villager] I worked HARD for those items!",
    "§c[Villager] You're robbing us mercilessly!",
    "§c[Villager] May you be cursed forever!",
    "§c[Villager] The Iron Golem will hear about this!",
    "§c[Villager] You're leaving us with NOTHING!",
    "§c[Villager] How will we survive now!?",
    "§c[Villager] That was for the winter!",
    "§c[Villager] You're worse than the pillagers!",
    "§c[Villager] I hope karma gets you!",
    "§c[Villager] You've doomed us all!",
    "§c[Villager] Our children will starve because of you!",
    "§c[Villager] This is unforgivable!",
 "§c[Villager] *shouting* Every emerald counted!",
]
THEFT_LOOT_BABY_EN = [
    "§c[Baby Villager] *sobs* That was my favorite toy!",
    "§c[Baby Villager] No no no! Not our food!",
    "§c[Baby Villager] You're so mean!",
    "§c[Baby Villager] I hate you! *cries loudly*",
    "§c[Baby Villager] Give it baaaack! *shouts*",
    "§c[Baby Villager] That's not fair!",
    "§c[Baby Villager] You're evil! *sobs*",
    "§c[Baby Villager] I'll never forget this!",
    "§c[Baby Villager] Why would you do that!? *cries*",
    "§c[Baby Villager] Mommy is going to be so mad!",
    "§c[Baby Villager] *wails* Our things!",
]

THEFT_CHEST_ADULT_ES = [
    "§c[Aldeano] ¡Oye! ¡Eso es MÍO!",
    "§c[Aldeano] *jadea* ¡Un LADRÓN!",
    "§c[Aldeano] ¡ALTO! ¡Devuelve eso!",
    "§c[Aldeano] Nos estás... nos estás robando.",
    "§c[Aldeano] No puedo creer que hagas esto.",
    "§c[Aldeano] ¡GUARDIAS! ¡Hay un ladrón!",
    "§c[Aldeano] ¿¡Cómo te ATREVES a abrir eso!?",
    "§c[Aldeano] ¡Ese cofre NO es tuyo!",
    "§c[Aldeano] ¡Apártate de mis cosas!",
    "§c[Aldeano] ¿¡Qué crees que estás haciendo!?",
    "§c[Aldeano] ¡Trabajé Años por lo que hay ahí!",
    "§c[Aldeano] ¡Te arrepentirás, ladrón!",
    "§c[Aldeano] ¡Es propiedad privada!",
    "§c[Aldeano] ¡Ahí están mis ahorros de toda la vida!",
    "§c[Aldeano] ¡SOCORRO! ¡ROBO!",
    "§c[Aldeano] ¡Esto rompe nuestra confianza!",
    "§c[Aldeano] ¡Fuera de mi cofre!",
    "§c[Aldeano] ¡No eres mejor que un saqueador!",
]
THEFT_CHEST_BABY_ES = [
    "§c[Bebé] *llora* ¡Mamá! ¡Nos roban!",
    "§c[Bebé] ¡Nooo! ¡Ese es el cofre de la familia!",
    "§c[Bebé] ¿Por qué eres malo? *solloza*",
    "§c[Bebé] ¡Se lo diré a papá!",
    "§c[Bebé] *corre llorando* ¡LADRÓN!",
    "§c[Bebé] ¡Eso no es tuyo! *llora*",
    "§c[Bebé] ¡Persona mala! ¡Mala!",
    "§c[Bebé] ¡Mis juguetes están ahí!",
    "§c[Bebé] *grita* ¡PARAAAA!",
    "§c[Bebé] ¡Eres un abusón!",
    "§c[Bebé] ¡Tengo miedo! *llora*",
    "§c[Bebé] Papá dijo que los forasteros son peligrosos.",
    "§c[Bebé] *gemidos* No el cofre...",
]
THEFT_LOOT_ADULT_ES = [
    "§c[Aldeano] ¡Esas son NUESTRAS provisiones!",
    "§c[Aldeano] ¡Te llevas todo lo que tenemos!",
    "§c[Aldeano] ¡LADRÓN! ¡Que alguien ayude!",
    "§c[Aldeano] ¡Trabajé DURO por esos objetos!",
    "§c[Aldeano] ¡Nos estás desvalijando!",
    "§c[Aldeano] ¡Ojalá te maldigan!",
    "§c[Aldeano] ¡El golem de hierro se enterará!",
    "§c[Aldeano] ¡Nos dejas sin NADA!",
    "§c[Aldeano] ¿¡Cómo sobreviviremos ahora!?",
    "§c[Aldeano] ¡Eso era para el invierno!",
    "§c[Aldeano] ¡Eres peor que los saqueadores!",
    "§c[Aldeano] ¡Ojalá el karma te alcance!",
    "§c[Aldeano] ¡Nos has condenado a todos!",
    "§c[Aldeano] ¡Nuestros niños pasarán hambre por ti!",
    "§c[Aldeano] ¡Esto es imperdonable!",
    "§c[Aldeano] *grita* ¡Cada esmeralda contaba!",
]
THEFT_LOOT_BABY_ES = [
    "§c[Bebé] *solloza* ¡Era mi juguete favorito!",
    "§c[Bebé] ¡No no no! ¡Nuestra comida!",
    "§c[Bebé] ¡Eres muy malo!",
    "§c[Bebé] ¡Te odio! *llora fuerte*",
    "§c[Bebé] ¡Devuélvelo! *grita*",
    "§c[Bebé] ¡Eso no es justo!",
    "§c[Bebé] ¡Eres malvado! *solloza*",
    "§c[Bebé] ¡Nunca lo olvidaré!",
    "§c[Bebé] ¿¡Por qué harías eso!? *llora*",
    "§c[Bebé] ¡Mamá se va a enfadar mucho!",
    "§c[Bebé] *llanto* ¡Nuestras cosas!",
]

def add_indexed(d_en, d_es, prefix, en_list, es_list):
    for i, (a, b) in enumerate(zip(en_list, es_list)):
        d_en[f"{prefix}.{i}"] = a
        d_es[f"{prefix}.{i}"] = b

def break_lines_en_es(category, adult_en, adult_es, baby_en, baby_es, extra_adult_en=None, extra_adult_es=None):
    out_en, out_es = {}, {}
    ae, aa = list(adult_en), list(adult_es)
    if extra_adult_en:
        ae.extend(extra_adult_en)
        aa.extend(extra_adult_es)
    add_indexed(out_en, out_es, f"villagediplomacy.react.break.{category}.adult", ae, aa)
    if baby_en:
        add_indexed(out_en, out_es, f"villagediplomacy.react.break.{category}.baby", baby_en, baby_es)
    return out_en, out_es

def main():
    en = {}
    es = {}

    # --- Core HUD / village ---
    en.update({
        "villagediplomacy.village.name_prefix": "Village of",
        "villagediplomacy.village.compound": "%1$s%2$s",
        "villagediplomacy.hud.line_village": "%1$s %2$s",
        "villagediplomacy.hud.line_reputation": "Reputation: %s",
        "villagediplomacy.hud.line_relations": "Standing: %s",
        "villagediplomacy.hud.reputation": "Reputation",
        "villagediplomacy.hud.relations": "Relations",
        "villagediplomacy.hud.neutral": "Neutral",
        "villagediplomacy.hud.ally": "Ally",
        "villagediplomacy.hud.enemy": "Enemy",
        "villagediplomacy.hud.friendly": "Friendly",
        "villagediplomacy.hud.hostile": "Hostile",
    })
    es.update({
        "villagediplomacy.village.name_prefix": "Aldea de",
        "villagediplomacy.village.compound": "%1$s%2$s",
        "villagediplomacy.hud.line_village": "%1$s %2$s",
        "villagediplomacy.hud.line_reputation": "Reputación: %s",
        "villagediplomacy.hud.line_relations": "Situación: %s",
        "villagediplomacy.hud.reputation": "Reputación",
        "villagediplomacy.hud.relations": "Relaciones",
        "villagediplomacy.hud.neutral": "Neutral",
        "villagediplomacy.hud.ally": "Aliado",
        "villagediplomacy.hud.enemy": "Enemigo",
        "villagediplomacy.hud.friendly": "Amistoso",
        "villagediplomacy.hud.hostile": "Hostil",
    })

    # Reputation tiers
    tiers = [
        ("legendary_hero", "Legendary Hero", "Héroe legendario"),
        ("hero", "Hero", "Héroe"),
        ("champion", "Champion", "Campeón"),
        ("trusted_friend", "Trusted Friend", "Amigo de confianza"),
        ("friendly", "Friendly", "Amistoso"),
        ("neutral", "Neutral", "Neutral"),
        ("suspicious", "Suspicious", "Sospechoso"),
        ("disliked", "Disliked", "Mal visto"),
        ("unwelcome", "Unwelcome", "No bienvenido"),
        ("unfriendly", "Ill-favored", "Poco amistoso"),
        ("hostile", "Hostile", "Hostil"),
        ("enemy", "Enemy", "Enemigo"),
        ("wanted_criminal", "Wanted Criminal", "Criminal buscado"),
    ]
    for k, a, b in tiers:
        en[f"villagediplomacy.rep.{k}"] = a
        es[f"villagediplomacy.rep.{k}"] = b

    en["villagediplomacy.rel.allied"] = "Allied"
    en["villagediplomacy.rel.neutral_village"] = "Neutral"
    en["villagediplomacy.rel.hostile_village"] = "Hostile"
    es["villagediplomacy.rel.allied"] = "Aliado"
    es["villagediplomacy.rel.neutral_village"] = "Neutral"
    es["villagediplomacy.rel.hostile_village"] = "Hostil"

    en["villagediplomacy.sys.chest_open"] = "§c[Village Diplomacy] You opened a village chest! Reputation %s (Total: %s - %s)"
    en["villagediplomacy.sys.loot_village"] = "§c[Village Diplomacy] You stole from the village! Reputation %s (Total: %s - %s)"
    en["villagediplomacy.sys.villager_attacked"] = "§c[Village Diplomacy] You attacked a villager! Reputation %s (Total: %s - %s)"
    es["villagediplomacy.sys.chest_open"] = "§c[Diplomacia de aldea] ¡Abriste un cofre de la aldea! Reputación %s (Total: %s - %s)"
    es["villagediplomacy.sys.loot_village"] = "§c[Diplomacia de aldea] ¡Robaste a la aldea! Reputación %s (Total: %s - %s)"
    es["villagediplomacy.sys.villager_attacked"] = "§c[Diplomacia de aldea] ¡Atacaste a un aldeano! Reputación %s (Total: %s - %s)"
    en["villagediplomacy.sys.animal_killed"] = "§c[Village Diplomacy] You killed %s! Reputation -25 (Total: %s - %s)"
    es["villagediplomacy.sys.animal_killed"] = "§c[Diplomacia de aldea] ¡Mataste a %s! Reputación -25 (Total: %s - %s)"

    bar = "§8=================================================="
    en["villagediplomacy.enter.bar"] = bar
    en["villagediplomacy.enter.leaving"] = "  §7<- Leaving %s"
    en["villagediplomacy.enter.line1"] = "  %s §6Entering %s"
    en["villagediplomacy.enter.line2"] = "  §7Reputation: §e%s §8[§f%s§8]"
    es["villagediplomacy.enter.bar"] = bar
    es["villagediplomacy.enter.leaving"] = "  §7<- Saliendo de %s"
    es["villagediplomacy.enter.line1"] = "  %s §6Entrando a %s"
    es["villagediplomacy.enter.line2"] = "  §7Reputación: §e%s §8[§f%s§8]"

    sys_break = [
        ("break_bell", "§c[Village Diplomacy] You broke the village bell! Reputation %s (Total: %s - %s)",
         "§c[Diplomacia de aldea] ¡Rompiste la campana! Reputación %s (Total: %s - %s)"),
        ("break_bed", "§c[Village Diplomacy] You broke a villager's bed! Reputation %s (Total: %s - %s)",
         "§c[Diplomacia de aldea] ¡Rompiste una cama! Reputación %s (Total: %s - %s)"),
        ("break_crop", "§c[Village Diplomacy] You destroyed village crops! Reputation %s (Total: %s - %s)",
         "§c[Diplomacia de aldea] ¡Destruiste cultivos! Reputación %s (Total: %s - %s)"),
        ("break_workstation", "§c[Village Diplomacy] You broke a workstation! Reputation %s (Total: %s - %s)",
         "§c[Diplomacia de aldea] ¡Rompiste un puesto de trabajo! Reputación %s (Total: %s - %s)"),
        ("break_decoration", "§c[Village Diplomacy] You broke village decoration! Reputation %s (Total: %s - %s)",
         "§c[Diplomacia de aldea] ¡Rompiste una decoración! Reputación %s (Total: %s - %s)"),
        ("break_well", "§c[Village Diplomacy] You damaged the village well! Reputation %s (Total: %s - %s)",
         "§c[Diplomacia de aldea] ¡Dañaste el pozo! Reputación %s (Total: %s - %s)"),
        ("break_house", "§c[Village Diplomacy] You damaged a house! Reputation %s (Total: %s - %s)",
         "§c[Diplomacia de aldea] ¡Dañaste una casa! Reputación %s (Total: %s - %s)"),
    ]
    for key, a, b in sys_break:
        en[f"villagediplomacy.sys.{key}"] = a
        es[f"villagediplomacy.sys.{key}"] = b

    add_indexed(en, es, "villagediplomacy.react.theft.chest.adult", THEFT_CHEST_ADULT_EN, THEFT_CHEST_ADULT_ES)
    add_indexed(en, es, "villagediplomacy.react.theft.chest.baby", THEFT_CHEST_BABY_EN, THEFT_CHEST_BABY_ES)
    add_indexed(en, es, "villagediplomacy.react.theft.loot.adult", THEFT_LOOT_ADULT_EN, THEFT_LOOT_ADULT_ES)
    add_indexed(en, es, "villagediplomacy.react.theft.loot.baby", THEFT_LOOT_BABY_EN, THEFT_LOOT_BABY_ES)

    # Bell break reactions (10+2 extra EN / ES)
    bell_adult_en = [
        "§4[Villager] THE BELL! Our emergency system!",
        "§4[Villager] NO! That was our warning bell!",
        "§4[Villager] You destroyed our bell!",
        "§4[Villager] *horrified* The bell is our lifeline!",
        "§4[Villager] How will we call for help now!",
        "§4[Villager] That bell has saved lives!",
        "§4[Villager] Guards! The bell is destroyed!",
        "§4[Villager] This is unforgivable!",
        "§c[Villager] *panicking* Our alert system!",
        "§c[Villager] Raiders could attack and we wouldn't know!",
        "§c[Villager] That sound carried hope—now silence.",
        "§4[Villager] Every home listened for that ring!",
    ]
    bell_adult_es = [
        "§4[Aldeano] ¡LA CAMPANA! ¡Nuestro sistema de emergencia!",
        "§4[Aldeano] ¡NO! ¡Era nuestra campana de alerta!",
        "§4[Aldeano] ¡Destruiste nuestra campana!",
        "§4[Aldeano] *horrorizado* ¡La campana es nuestra línea de vida!",
        "§4[Aldeano] ¿¡Cómo pediremos ayuda ahora!",
        "§4[Aldeano] ¡Esa campana ha salvado vidas!",
        "§4[Aldeano] ¡Guardias! ¡La campana está destruida!",
        "§4[Aldeano] ¡Esto es imperdonable!",
        "§c[Aldeano] *en pánico* ¡Nuestro sistema de alerta!",
        "§c[Aldeano] ¡Los saqueadores podrían atacar y no nos enteraríamos!",
        "§c[Aldeano] Ese sonido traía esperanza... ahora silencio.",
        "§4[Aldeano] ¡Cada casa escuchaba ese tañido!",
    ]
    bell_baby_en = [
        "§c[Baby Villager] *wailing* The bell! It's broken!",
        "§c[Baby Villager] Why did you break it!",
        "§c[Baby Villager] Mom! The bell!",
        "§c[Baby Villager] *sobbing* That was important!",
        "§c[Baby Villager] *sniffles* No more ringing...",
        "§c[Baby Villager] I'm scared without the bell!",
    ]
    bell_baby_es = [
        "§c[Bebé] *llora* ¡La campana! ¡Está rota!",
        "§c[Bebé] ¿¡Por qué la rompiste!",
        "§c[Bebé] ¡Mamá! ¡La campana!",
        "§c[Bebé] *solloza* ¡Era importante!",
        "§c[Bebé] *snif* Ya no sonará...",
        "§c[Bebé] ¡Sin campana me da miedo!",
    ]
    be, bs = break_lines_en_es("bell", bell_adult_en, bell_adult_es, bell_baby_en, bell_baby_es)
    en.update(be); es.update(bs)

    # Bed
    bed_ae = [
        "§c[Villager] That's MY bed!", "§c[Villager] Where am I supposed to sleep now!",
        "§c[Villager] Monster!", "§c[Villager] *furious* That took me weeks to make!",
        "§c[Villager] I JUST finished that bed!", "§c[Villager] Have you no respect!",
        "§c[Villager] That's my only bed!", "§c[Villager] I work all day and you break my bed!",
        "§c[Villager] *indignant* I'll sleep on the floor because of you!",
        "§c[Villager] Breaking someone's bed—seriously!?",
        "§c[Villager] The straw still smelled like home...", "§c[Villager] I carved those posts myself!",
    ]
    bed_aa = ["§c[Aldeano] ¡Esa es MI cama!", "§c[Aldeano] ¿¡Dónde duermo ahora!", "§c[Aldeano] ¡Monstruo!",
              "§c[Aldeano] *furioso* ¡Me tomó semanas hacerla!", "§c[Aldeano] ¡ACABO de hacer esa cama!",
              "§c[Aldeano] ¿¡No tienes respeto!?", "§c[Aldeano] ¡Esa es mi única cama!",
              "§c[Aldeano] ¡Trabajo todo el día y rompes mi cama!",
              "§c[Aldeano] *indignado* ¡Dormiré en el suelo por tu culpa!",
              "§c[Aldeano] ¿¡Romper la cama de alguien, en serio!?",
              "§c[Aldeano] La paja aún olía a hogar...", "§c[Aldeano] ¡Yo tallé esos postes!"]
    bed_be = ["§c[Baby Villager] My bed! *uncontrollable sobbing*", "§c[Baby Villager] I need that to sleep!",
              "§c[Baby Villager] *crying* Where will I sleep!", "§c[Baby Villager] Dad! My bed is broken!",
              "§c[Baby Villager] *whimpering* Why!", "§c[Baby Villager] The blanket was still warm...",
              "§c[Baby Villager] Monsters come when beds break!"]
    bed_ba = ["§c[Bebé] ¡Mi cama! *solloza*", "§c[Bebé] ¡Necesito eso para dormir!",
              "§c[Bebé] *llorando* ¿¡Dónde dormiré!", "§c[Bebé] ¡Papá! ¡Mi cama está rota!",
              "§c[Bebé] *gemidos* ¿¡Por qué!", "§c[Bebé] La manta aún estaba tibia...",
              "§c[Bebé] ¡Si rompes la cama vienen monstruos!"]
    be, bs = break_lines_en_es("bed", bed_ae, bed_aa, bed_be, bed_ba)
    en.update(be); es.update(bs)

    # Crop (14 adult)
    crop_ae = [
        "§c[Villager] Our food! You're destroying our crops!", "§c[Villager] We need those to survive!",
        "§c[Villager] STOP trampling our farm!", "§c[Villager] That took MONTHS to grow!",
        "§c[Villager] We'll go hungry because of you!", "§c[Villager] *desperate* That's our winter reserve!",
        "§c[Villager] Do you know how hard farming is!", "§c[Villager] Those crops feed the whole village!",
        "§c[Villager] *angry* Get off our fields!", "§c[Villager] You're destroying our livelihood!",
        "§c[Villager] The whole village depends on these crops!", "§c[Villager] Have you no shame!",
        "§c[Villager] I was going to harvest tomorrow!", "§c[Villager] The bees loved those flowers!",
    ]
    crop_aa = [
        "§c[Aldeano] ¡Nuestra comida! ¡Destruyes los cultivos!", "§c[Aldeano] ¡Los necesitamos para vivir!",
        "§c[Aldeano] ¡DEJA de pisotear la granja!", "§c[Aldeano] ¡Eso tardó MESES en crecer!",
        "§c[Aldeano] ¡Pasaremos hambre por ti!", "§c[Aldeano] *desesperado* ¡Reserva de invierno!",
        "§c[Aldeano] ¿¡Sabes lo duro que es cultivar!", "§c[Aldeano] ¡Esos campos alimentan a la aldea!",
        "§c[Aldeano] *enojado* ¡Fuera de nuestros campos!", "§c[Aldeano] ¡Destruyes nuestro sustento!",
        "§c[Aldeano] ¡La aldea entera depende de esto!", "§c[Aldeano] ¿¡No tienes vergüenza!",
        "§c[Aldeano] ¡Mañana iba a cosechar!", "§c[Aldeano] ¡A las abejas les encantaban esas flores!",
    ]
    crop_be = ["§c[Baby Villager] The food! *cries*", "§c[Baby Villager] Mom said don't touch the crops!",
               "§c[Baby Villager] *points* Bad! Bad!", "§c[Baby Villager] Those were going to be bread!",
               "§c[Baby Villager] The carrots had faces in my head...", "§c[Baby Villager] *wails* Hungry winter!"]
    crop_ba = ["§c[Bebé] ¡La comida! *llora*", "§c[Bebé] ¡Mamá dijo que no tocara los cultivos!",
               "§c[Bebé] *señala* ¡Malo! ¡Malo!", "§c[Bebé] ¡De eso iba a salir pan!",
               "§c[Bebé] A las zanahorias les puse nombre...", "§c[Bebé] *llanto* ¡Invierno hambriento!"]
    be, bs = break_lines_en_es("crop", crop_ae, crop_aa, crop_be, crop_ba)
    en.update(be); es.update(bs)

    # Workstation 14 / 4
    ws_ae = [
        "§4[Villager] That's my livelihood!", "§4[Villager] I need that to work!",
        "§4[Villager] How DARE you!", "§4[Villager] *shocked* My workbench!",
        "§4[Villager] I've had it for YEARS!", "§4[Villager] That's how I earn my living!",
        "§4[Villager] You just destroyed my job!", "§4[Villager] *furious* How am I supposed to work now!",
        "§4[Villager] That station was essential to the village!", "§c[Villager] Without it I can't earn emeralds!",
        "§c[Villager] Do you know how expensive those are!", "§c[Villager] My whole profession depends on that!",
        "§c[Villager] Years of stains on that wood—gone.", "§4[Villager] The village economy needed that table!",
    ]
    ws_aa = [
        "§4[Aldeano] ¡Ese es mi sustento!", "§4[Aldeano] ¡Lo necesito para trabajar!",
        "§4[Aldeano] ¿¡Cómo te atreves!", "§4[Aldeano] *conmocionado* ¡Mi mesa!",
        "§4[Aldeano] ¡La tengo desde hace Años!", "§4[Aldeano] ¡Así me gano la vida!",
        "§4[Aldeano] ¡Acabas de destruir mi trabajo!", "§4[Aldeano] *furioso* ¿¡Y ahora cómo trabajo!",
        "§4[Aldeano] ¡Esa estación era vital para la aldea!", "§c[Aldeano] ¡Sin eso no gano esmeraldas!",
        "§c[Aldeano] ¿¡Sabes lo que cuesta!", "§c[Aldeano] ¡Mi oficio entero dependía de eso!",
        "§c[Aldeano] Años de manchas en esa madera... perdidos.", "§4[Aldeano] ¡La economía del pueblo necesitaba esa mesa!",
    ]
    ws_be = ["§c[Baby Villager] Dad's workstation!", "§c[Baby Villager] *gasps* You broke it!",
             "§c[Baby Villager] He carved his name underneath...", "§c[Baby Villager] No more shiny tools!"]
    ws_ba = ["§c[Bebé] ¡El taller de papá!", "§c[Bebé] *jadea* ¡La rompiste!",
             "§c[Bebé] Talló su nombre debajo...", "§c[Bebé] ¡Ya no habrá herramientas brillantes!"]
    be, bs = break_lines_en_es("workstation", ws_ae, ws_aa, ws_be, ws_ba)
    en.update(be); es.update(bs)

    # Decoration 12 adult, no baby
    dec_ae = [
        "§c[Villager] Hey! That made the village pretty!", "§c[Villager] Why would you do that!",
        "§c[Villager] We worked hard to decorate!", "§6[Villager] *sighs* That was nice...",
        "§6[Villager] Can't we have nice things!", "§c[Villager] Show some respect for our village!",
        "§6[Villager] I just placed that yesterday...", "§c[Villager] Now you're smashing decorations!",
        "§7[Villager] Small joys matter in hard times.", "§c[Villager] The festival lights were stored there!",
        "§6[Villager] *mutters* Philistine.", "§c[Villager] That pot had a story!",
    ]
    dec_aa = [
        "§c[Aldeano] ¡Oye! ¡Eso embellecía la aldea!", "§c[Aldeano] ¿¡Por qué harías eso!",
        "§c[Aldeano] ¡Nos costó decorar!", "§6[Aldeano] *suspira* Era bonito...",
        "§6[Aldeano] ¿¡No podemos tener cosas lindas!", "§c[Aldeano] ¡Respeta nuestro pueblo!",
        "§6[Aldeano] Lo coloqué ayer...", "§c[Aldeano] ¿¡Ahora rompes decoración!",
        "§7[Aldeano] Las pequeñas alegrías importan.", "§c[Aldeano] ¡Ahí guardábamos luces de fiesta!",
        "§6[Aldeano] *murmura* Bárbaro.", "§c[Aldeano] ¡Esa maceta tenía historia!",
    ]
    be, bs = break_lines_en_es("decoration", dec_ae, dec_aa, [], [])
    en.update(be); es.update(bs)

    # Well
    well_ae = [
        "§4[Villager] THE WELL! Our water source!", "§4[Villager] That's our only water supply!",
        "§4[Villager] *horrified* The well is destroyed!", "§4[Villager] We'll die of thirst!",
        "§c[Villager] How will we get water now!", "§c[Villager] That well served us for generations!",
        "§c[Villager] You've doomed us all!", "§c[Villager] *panicking* Our water! Our precious water!",
        "§4[Villager] This is a catastrophe!", "§c[Villager] The bucket chain starts at dawn—ruined!",
        "§4[Villager] Even the travelers drank here!",
    ]
    well_aa = [
        "§4[Aldeano] ¡EL POZO! ¡Nuestra fuente!", "§4[Aldeano] ¡Es nuestro único suministro!",
        "§4[Aldeano] *horrorizado* ¡El pozo está destruido!", "§4[Aldeano] ¡Moriremos de sed!",
        "§c[Aldeano] ¿¡Cómo obtendremos agua!", "§c[Aldeano] ¡Ese pozo nos sirvió generaciones!",
        "§c[Aldeano] ¡Nos has condenado!", "§c[Aldeano] *pánico* ¡Nuestra agua!",
        "§4[Aldeano] ¡Catástrofe!", "§c[Aldeano] ¡La cadena de cubos al alba... arruinada!",
        "§4[Aldeano] ¡Hasta los viajeros bebían aquí!",
    ]
    well_be = ["§c[Baby Villager] *crying* Where do we get water!", "§c[Baby Villager] I'm thirsty! The well!",
               "§c[Baby Villager] Mom! The water place is broken!", "§c[Baby Villager] The bucket was so heavy...",
               "§c[Baby Villager] *sniff* Splash days are over."]
    well_ba = ["§c[Bebé] *llora* ¿¡Dónde hay agua!", "§c[Bebé] ¡Sed! ¡El pozo!",
               "§c[Bebé] ¡Mamá! ¡El lugar del agua está roto!", "§c[Bebé] El cubo pesaba tanto...",
               "§c[Bebé] *snif* Se acabaron los chapoteos."]
    be, bs = break_lines_en_es("well", well_ae, well_aa, well_be, well_ba)
    en.update(be); es.update(bs)

    # House
    house_ae = [
        "§c[Villager] You're destroying my home!", "§c[Villager] STOP! I live here!",
        "§c[Villager] My house! You're smashing it!", "§c[Villager] *desperate* I have nowhere else!",
        "§c[Villager] That's MY HOUSE you're breaking!", "§c[Villager] I built this with my own hands!",
        "§c[Villager] *furious* Get away from my home!", "§c[Villager] This house protects me from monsters!",
        "§c[Villager] You're leaving me homeless!", "§c[Villager] Have you no decency!",
        "§c[Villager] The door still had my height marks!", "§c[Villager] Rain is coming—where do I hide!?",
    ]
    house_aa = [
        "§c[Aldeano] ¡Destruyes mi hogar!", "§c[Aldeano] ¡ALTO! ¡Aquí vivo!",
        "§c[Aldeano] ¡Mi casa! ¡La destrozas!", "§c[Aldeano] *desesperado* ¡No tengo otro sitio!",
        "§c[Aldeano] ¡Es MI CASA la que rompes!", "§c[Aldeano] ¡Lo construí con mis manos!",
        "§c[Aldeano] *furioso* ¡Fuera de mi hogar!", "§c[Aldeano] ¡Esta casa me protege de monstruos!",
        "§c[Aldeano] ¡Me dejas sin techo!", "§c[Aldeano] ¿¡No tienes decencia!",
        "§c[Aldeano] ¡En la puerta marcaba mi altura!", "§c[Aldeano] ¡Viene lluvia... ¿dónde me escondo!?",
    ]
    house_be = ["§c[Baby Villager] Our house! *cries*", "§c[Baby Villager] *sobbing* Where will we live!",
                "§c[Baby Villager] Don't break our home!", "§c[Baby Villager] Dad! Our house!",
                "§c[Baby Villager] My toys were in the corner...", "§c[Baby Villager] The roof leaked stories!"]
    house_ba = ["§c[Bebé] ¡Nuestra casa! *llora*", "§c[Bebé] *solloza* ¿¡Dónde viviremos!",
                  "§c[Bebé] ¡No rompas nuestro hogar!", "§c[Bebé] ¡Papá! ¡Nuestra casa!",
                  "§c[Bebé] Mis juguetes estaban en la esquina...", "§c[Bebé] ¡El techo contaba cuentos!"]
    be, bs = break_lines_en_es("house", house_ae, house_aa, house_be, house_ba)
    en.update(be); es.update(bs)

    inject_animal_death(en, es)
    inject_animal_attack(en, es)
    inject_door_react(en, es)
    inject_golem_lines(en, es)
    inject_place_react(en, es)
    inject_personality_misc(en, es)
    inject_misc(en, es)

    # Village name fragments (English)
    vn_en = {
        "oak": "Oak", "stone": "Stone", "river": "River", "green": "Green", "silver": "Silver", "iron": "Iron",
        "gold": "Gold", "willow": "Willow", "maple": "Maple", "pine": "Pine", "cedar": "Cedar", "ash": "Ash",
        "elder": "Elder", "white": "White", "black": "Black", "red": "Red", "spring": "Spring", "summer": "Summer",
        "winter": "Winter", "autumn": "Autumn", "dawn": "Dawn", "dusk": "Dusk", "moon": "Moon", "sun": "Sun",
        "north": "North", "south": "South", "east": "East", "west": "West", "high": "High", "low": "Low",
        "deep": "Deep", "bright": "Bright", "crystal": "Crystal", "shadow": "Shadow", "dragon": "Dragon",
        "wolf": "Wolf", "fox": "Fox", "star": "Star", "mist": "Mist", "frost": "Frost", "thunder": "Thunder",
        "blossom": "Blossom", "royal": "Royal", "emerald": "Emerald", "copper": "Copper", "azure": "Azure",
        "obsidian": "Obsidian", "amber": "Amber",
        "shire": "shire", "wood": "wood", "vale": "vale", "field": "field", "brook": "brook", "ford": "ford",
        "haven": "haven", "ton": "ton", "burg": "burg", "mill": "mill", "ridge": "ridge", "cliff": "cliff",
        "crest": "crest", "dale": "dale", "glen": "glen", "hollow": "hollow", "port": "port", "watch": "watch",
        "guard": "guard", "mount": "mount", "hill": "hill", "peak": "peak", "shore": "shore", "bay": "bay",
        "rest": "rest", "moor": "moor", "marsh": "marsh", "fen": "fen", "grove": "grove", "glade": "glade",
        "point": "point", "end": "end", "fort": "fort", "market": "market", "cross": "cross", "falls": "falls",
        "meadow": "meadow", "spire": "spire", "gate": "gate", "cove": "cove",
    }
    vn_es = {
        "oak": "Roble", "stone": "Piedra", "river": "Río", "green": "Verde", "silver": "Argent", "iron": "Hierro",
        "gold": "Oro", "willow": "Sauce", "maple": "Arce", "pine": "Pino", "cedar": "Cedro", "ash": "Fresno",
        "elder": "Saúco", "white": "Blanco", "black": "Negro", "red": "Rojo", "spring": "Primavera",
        "summer": "Verano", "winter": "Invierno", "autumn": "Otoño", "dawn": "Alba", "dusk": "Ocaso",
        "moon": "Luna", "sun": "Sol", "north": "Norte", "south": "Sur", "east": "Este", "west": "Oeste",
        "high": "Alto", "low": "Bajo", "deep": "Hondo", "bright": "Brillante", "crystal": "Cristal",
        "shadow": "Sombra", "dragon": "Dragón", "wolf": "Lobo", "fox": "Zorro", "star": "Estrella",
        "mist": "Bruma", "frost": "Escarcha", "thunder": "Trueno", "blossom": "Flor", "royal": "Real",
        "emerald": "Esmeralda", "copper": "Cobre", "azure": "Azur", "obsidian": "Obsidiana", "amber": "Ámbar",
        "shire": "valle", "wood": "bosque", "vale": "valle", "field": "prado", "brook": "arroyo", "ford": "vado",
        "haven": "refugio", "ton": "ton", "burg": "burgo", "mill": "molino", "ridge": "cuchilla", "cliff": "risco",
        "crest": "cumbre", "dale": "vega", "glen": "hondonada", "hollow": "hondon", "port": "puerto",
        "watch": "atalaya", "guard": "guardia", "mount": "monte", "hill": "colina", "peak": "pico",
        "shore": "ribera", "bay": "bahía", "rest": "descanso", "moor": "páramo", "marsh": "marisma", "fen": "turbera",
        "grove": "arboleda", "glade": "claro", "point": "punta", "end": "final", "fort": "fuerte",
        "market": "mercado", "cross": "cruz", "falls": "saltos", "meadow": "pradera", "spire": "aguja",
        "gate": "puerta", "cove": "calita",
    }
    for k, v in vn_en.items():
        en[f"villagediplomacy.vn.{k}"] = v
        es[f"villagediplomacy.vn.{k}"] = vn_es[k]

    # Expanded dialogs (extra variety)
    base_dialog_en = {
        "villagediplomacy.dialog.greeting.4": "The elders still speak of travelers who helped us long ago.",
        "villagediplomacy.dialog.greeting.5": "Mind the path—wolves get bold at dusk.",
        "villagediplomacy.dialog.greeting.6": "If you're hungry, the baker might spare a loaf.",
        "villagediplomacy.dialog.friendly.5": "Trade fair, friend, and our doors stay open for you.",
        "villagediplomacy.dialog.friendly.6": "The golems nod when you pass. That means something.",
        "villagediplomacy.dialog.neutral.4": "We've seen heroes and villains wear the same cloak.",
        "villagediplomacy.dialog.neutral.5": "Keep your sword sheathed unless the raid horn sounds.",
        "villagediplomacy.dialog.hostile.4": "Iron remembers every blow you've dealt here.",
        "villagediplomacy.dialog.hostile.5": "One more misstep and the meeting bell rings for you alone.",
        "villagediplomacy.dialog.trade.5": "Emeralds don't lie—neither do my prices.",
        "villagediplomacy.dialog.warning.4": "Light a torch before the cellar—something skitters down there.",
        "villagediplomacy.dialog.warning.5": "The cartographer marks a ruin to the north. Tread carefully.",
        "villagediplomacy.dialog.quest.3": "Bring back three strings from spiders—we'll weave new nets.",
        "villagediplomacy.dialog.quest.4": "The river ran cloudy this morning. Could you look upstream?",
    }
    base_dialog_es = {
        "villagediplomacy.dialog.greeting.4": "Los mayores aún hablan de viajeros que nos ayudaron hace tiempo.",
        "villagediplomacy.dialog.greeting.5": "Cuidado con el camino: los lobos se atreven al ocaso.",
        "villagediplomacy.dialog.greeting.6": "Si tienes hambre, el panadero quizá te de un pan.",
        "villagediplomacy.dialog.friendly.5": "Comercia con justicia, amigo, y nuestras puertas seguirán abiertas.",
        "villagediplomacy.dialog.friendly.6": "Los golems asienten al verte pasar. Eso significa algo.",
        "villagediplomacy.dialog.neutral.4": "Hemos visto héroes y villanos con la misma capa.",
        "villagediplomacy.dialog.neutral.5": "Mantén la espada envainada hasta que suene la alarma.",
        "villagediplomacy.dialog.hostile.4": "El hierro recuerda cada golpe que diste aquí.",
        "villagediplomacy.dialog.hostile.5": "Un error más y la campana tañe solo para ti.",
        "villagediplomacy.dialog.trade.5": "Las esmeraldas no mienten—mis precios tampoco.",
        "villagediplomacy.dialog.warning.4": "Enciende una antorcha antes del sótano: algo se arrastra ahí.",
        "villagediplomacy.dialog.warning.5": "El cartógrafo marcó una ruina al norte. Con cuidado.",
        "villagediplomacy.dialog.quest.3": "Trae tres cuerdas de araña—tejemos redes nuevas.",
        "villagediplomacy.dialog.quest.4": "El río salió turbio esta mañana. ¿Podrías mirar río arriba?",
    }

    # Merge original dialog keys from existing file content
    orig_en = {
        "villagediplomacy.dialog.greeting.0": "Hello, traveler. Welcome to our village.",
        "villagediplomacy.dialog.greeting.1": "Good to see you around here.",
        "villagediplomacy.dialog.greeting.2": "What brings you to our lands?",
        "villagediplomacy.dialog.greeting.3": "Ah, a visitor! Always welcome.",
        "villagediplomacy.dialog.friendly.0": "Our village is glad to have your support!",
        "villagediplomacy.dialog.friendly.1": "We trust you, friend. Come back anytime.",
        "villagediplomacy.dialog.friendly.2": "Thanks to you, our village thrives.",
        "villagediplomacy.dialog.friendly.3": "You are always welcome at our hearths.",
        "villagediplomacy.dialog.friendly.4": "The children speak of your deeds with admiration.",
        "villagediplomacy.dialog.neutral.0": "We neither trust nor distrust you... yet.",
        "villagediplomacy.dialog.neutral.1": "Show us your intentions, stranger.",
        "villagediplomacy.dialog.neutral.2": "Our village remains cautious around outsiders.",
        "villagediplomacy.dialog.neutral.3": "Trade fairly and we shall get along.",
        "villagediplomacy.dialog.hostile.0": "Leave our village at once!",
        "villagediplomacy.dialog.hostile.1": "You are not welcome here.",
        "villagediplomacy.dialog.hostile.2": "Guards! An enemy approaches!",
        "villagediplomacy.dialog.hostile.3": "We remember what you did. Be gone!",
        "villagediplomacy.dialog.trade.0": "Looking to trade? I have fine goods.",
        "villagediplomacy.dialog.trade.1": "A fair deal benefits us both.",
        "villagediplomacy.dialog.trade.2": "I might have something useful for you.",
        "villagediplomacy.dialog.trade.3": "These prices are the best in the region!",
        "villagediplomacy.dialog.trade.4": "Come, let us see what we can arrange.",
        "villagediplomacy.dialog.warning.0": "Be careful, dark creatures roam nearby.",
        "villagediplomacy.dialog.warning.1": "We heard strange noises last night...",
        "villagediplomacy.dialog.warning.2": "Adventurer, beware the nearby caves.",
        "villagediplomacy.dialog.warning.3": "Our village has been under attack recently.",
        "villagediplomacy.dialog.quest.0": "Could you help us recover something stolen?",
        "villagediplomacy.dialog.quest.1": "We need a brave soul for a dangerous task.",
        "villagediplomacy.dialog.quest.2": "If you prove your worth, we will reward you well.",
        "villagediplomacy.dialog.reputation_up": "Your reputation with our village has improved!",
        "villagediplomacy.dialog.reputation_down": "Your reputation with our village has decreased.",
        "villagediplomacy.dialog.become_ally": "You are now considered an ally of our village!",
        "villagediplomacy.dialog.become_enemy": "You are now considered an enemy of our village!",
        "villagediplomacy.command.reputation": "Reputation with %s: %d",
        "villagediplomacy.command.set_reputation": "Reputation set to %d with village %s",
        "villagediplomacy.itemgroup.villagediplomacy": "Village Diplomacy",
    }
    orig_es = {
        "villagediplomacy.dialog.greeting.0": "Hola, viajero. Bienvenido a nuestra aldea.",
        "villagediplomacy.dialog.greeting.1": "Qué bien verte por aquí.",
        "villagediplomacy.dialog.greeting.2": "¿Qué te trae por estas tierras?",
        "villagediplomacy.dialog.greeting.3": "¡Ah, un visitante! Siempre bienvenido.",
        "villagediplomacy.dialog.friendly.0": "¡Nuestra aldea agradece tu apoyo!",
        "villagediplomacy.dialog.friendly.1": "Confiamos en ti, amigo. Vuelve cuando quieras.",
        "villagediplomacy.dialog.friendly.2": "Gracias a ti, nuestra aldea prospera.",
        "villagediplomacy.dialog.friendly.3": "Siempre eres bienvenido junto a nuestras hogueras.",
        "villagediplomacy.dialog.friendly.4": "Los niños hablan de tus hazañas con admiración.",
        "villagediplomacy.dialog.neutral.0": "No te confiamos ni desconfiamos... todavía.",
        "villagediplomacy.dialog.neutral.1": "Muéstranos tus intenciones, forastero.",
        "villagediplomacy.dialog.neutral.2": "Nuestra aldea es cautelosa con los extraños.",
        "villagediplomacy.dialog.neutral.3": "Comercia con justicia y nos llevaremos bien.",
        "villagediplomacy.dialog.hostile.0": "¡Abandona nuestra aldea de inmediato!",
        "villagediplomacy.dialog.hostile.1": "No eres bienvenido aquí.",
        "villagediplomacy.dialog.hostile.2": "¡Guardias! ¡Se acerca un enemigo!",
        "villagediplomacy.dialog.hostile.3": "Recordamos lo que hiciste. ¡Fuera de aquí!",
        "villagediplomacy.dialog.trade.0": "¿Buscas comerciar? Tengo buenas mercancías.",
        "villagediplomacy.dialog.trade.1": "Un trato justo nos beneficia a ambos.",
        "villagediplomacy.dialog.trade.2": "Puede que tenga algo útil para ti.",
        "villagediplomacy.dialog.trade.3": "¡Estos precios son los mejores de la región!",
        "villagediplomacy.dialog.trade.4": "Ven, veamos qué podemos acordar.",
        "villagediplomacy.dialog.warning.0": "Ten cuidado, criaturas oscuras merodean cerca.",
        "villagediplomacy.dialog.warning.1": "Escuchamos ruidos extraños anoche...",
        "villagediplomacy.dialog.warning.2": "Aventurero, ten cuidado con las cuevas cercanas.",
        "villagediplomacy.dialog.warning.3": "Nuestra aldea ha sido atacada recientemente.",
        "villagediplomacy.dialog.quest.0": "¿Podrías ayudarnos a recuperar algo robado?",
        "villagediplomacy.dialog.quest.1": "Necesitamos un alma valiente para una tarea peligrosa.",
        "villagediplomacy.dialog.quest.2": "Si demuestras tu valía, te recompensaremos bien.",
        "villagediplomacy.dialog.reputation_up": "¡Tu reputación con nuestra aldea ha mejorado!",
        "villagediplomacy.dialog.reputation_down": "Tu reputación con nuestra aldea ha disminuido.",
        "villagediplomacy.dialog.become_ally": "¡Ahora eres considerado aliado de nuestra aldea!",
        "villagediplomacy.dialog.become_enemy": "¡Ahora eres considerado enemigo de nuestra aldea!",
        "villagediplomacy.command.reputation": "Reputación con %s: %d",
        "villagediplomacy.command.set_reputation": "Reputación establecida a %d con la aldea %s",
        "villagediplomacy.itemgroup.villagediplomacy": "Diplomacia de aldea",
    }

    en.update(orig_en)
    en.update(base_dialog_en)
    es.update(orig_es)
    es.update(base_dialog_es)

    for path in (OUT_EN, OUT_ES):
        path.parent.mkdir(parents=True, exist_ok=True)

    with OUT_EN.open("w", encoding="utf-8") as f:
        json.dump(dict(sorted(en.items())), f, ensure_ascii=False, indent=2)
        f.write("\n")
    with OUT_ES.open("w", encoding="utf-8") as f:
        json.dump(dict(sorted(es.items())), f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("Wrote", OUT_EN, "and", OUT_ES, "keys:", len(en))

if __name__ == "__main__":
    main()
