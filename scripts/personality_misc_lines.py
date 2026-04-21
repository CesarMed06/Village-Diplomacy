# -*- coding: utf-8 -*-
"""Coward flee lines, villager activity barks, generous gift, testament item, brave bell."""


def inject_personality_misc(en, es):
    def both(k, en_t, es_t):
        en[k] = en_t
        es[k] = es_t

    coward_en = [
        "§e[%s] AAAH! Stay away from me!",
        "§e[%s] *screams* I'm out of here!",
        "§e[%s] No no no! *runs away*",
        "§e[%s] Help! Someone help!",
        "§e[%s] *sobs* I don't want to die!",
        "§e[%s] *flees in terror*",
        "§e[%s] Leave me alone! *panicking*",
    ]
    coward_es = [
        "§e[%s] ¡AAAH! ¡Aléjate de mí!",
        "§e[%s] *grita* ¡Me largo de aquí!",
        "§e[%s] ¡No no no! *huye corriendo*",
        "§e[%s] ¡Ayuda! ¡Que alguien ayude!",
        "§e[%s] *llora* ¡No quiero morir!",
        "§e[%s] *huye aterrorizado*",
        "§e[%s] ¡Déjame en paz! *entrando en pánico*",
    ]
    for i in range(7):
        en[f"villagediplomacy.personality.flee.coward.{i}"] = coward_en[i]
        es[f"villagediplomacy.personality.flee.coward.{i}"] = coward_es[i]

    cautious_en = [
        "§e[%s] *backs away carefully*",
        "§e[%s] I should get out of here...",
        "§e[%s] This doesn't feel safe.",
        "§e[%s] *retreats nervously*",
        "§e[%s] Better safe than sorry...",
        "§e[%s] I'll find somewhere safer.",
    ]
    cautious_es = [
        "§e[%s] *retrocede cautelosamente*",
        "§e[%s] Debería salir de aquí...",
        "§e[%s] Esto no parece seguro.",
        "§e[%s] *se retira nerviosamente*",
        "§e[%s] Mejor prevenir que lamentar...",
        "§e[%s] Encontraré un lugar más seguro.",
    ]
    for i in range(6):
        en[f"villagediplomacy.personality.flee.cautious.{i}"] = cautious_en[i]
        es[f"villagediplomacy.personality.flee.cautious.{i}"] = cautious_es[i]

    both("villagediplomacy.villager.activity.work.farmer", "§e[%s] *starts tending the fields*", "§e[%s] *comienza a cultivar*")
    both(
        "villagediplomacy.villager.activity.work.smith",
        "§e[%s] *begins smithing work*",
        "§e[%s] *comienza trabajo de herrería*",
    )
    both("villagediplomacy.villager.activity.work.butcher", "§e[%s] *prepares meat for the day*", "§e[%s] *prepara carne para el día*")
    both("villagediplomacy.villager.activity.work.librarian", "§e[%s] *opens a book to study*", "§e[%s] *abre un libro para estudiar*")
    both("villagediplomacy.villager.activity.work.cleric", "§e[%s] *prepares morning brews*", "§e[%s] *prepara pociones matutinas*")
    both("villagediplomacy.villager.activity.work.fisherman", "§e[%s] *heads out to fish*", "§e[%s] *va a pescar*")
    both("villagediplomacy.villager.activity.work.default", "§e[%s] *starts the day's work*", "§e[%s] *comienza a trabajar*")

    both(
        "villagediplomacy.villager.activity.eating",
        "§e[%s] *takes a lunch break*",
        "§e[%s] *toma descanso para almorzar*",
    )
    both(
        "villagediplomacy.villager.activity.lighting",
        "§e[%s] *lights a torch as evening falls*",
        "§e[%s] *enciende antorcha al caer la tarde*",
    )

    both(
        "villagediplomacy.personality.gift_bread",
        "§a[%s] Here—take this bread. You look hurt.",
        "§a[%s] ¡Toma, toma este pan! Te ves herido.",
    )

    both("villagediplomacy.bell.brave_ring", "§6[%s] *rings the bell furiously* DANGER! EVERYONE TO THE SQUARE!", "§6[%s] *toca la campana con furia* ¡PELIGRO! ¡TODOS A LA PLAZA!")

    both(
        "villagediplomacy.debug.trade_bond",
        "§7[Debug] %s bond: %s/30 toward will",
        "§7[Debug] %s vínculo: %s/30 para testamento",
    )

    both("villagediplomacy.testament.item_name", "§6§lLast Will of %s", "§6§lTestamento de %s")
    both("villagediplomacy.testament.lore.bar", "§7━━━━━━━━━━━━━━━━━━━", "§7━━━━━━━━━━━━━━━━━━━")
    both("villagediplomacy.testament.lore.title", "§7Last Will and Testament", "§7Última Voluntad y Testamento")
    both("villagediplomacy.testament.lore.of", "§7of %s", "§7de %s")
    both("villagediplomacy.testament.lore.quote1", "§e'To my dear friend,'", "§e'A mi querido amigo,'")
    both("villagediplomacy.testament.lore.quote2", "§e'thank you for your'", "§e'Gracias por tu'")
    both("villagediplomacy.testament.lore.quote3", "§e'kindness and trust.'", "§e'bondad y confianza.'")
    both("villagediplomacy.testament.lore.died", "§8Died at: %s", "§8Murió en: %s")
    both("villagediplomacy.testament.lore.job", "§8Profession: %s", "§8Profesión: %s")
