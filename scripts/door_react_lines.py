# -*- coding: utf-8 -*-
"""Villager door open/close reactions by reputation tier."""


def _add_list(en, es, prefix, lines_en, lines_es):
    for i, t in enumerate(lines_en):
        en[f"{prefix}.{i}"] = t
    for i, t in enumerate(lines_es):
        es[f"{prefix}.{i}"] = t


def inject_door_react(en, es):
    p = "villagediplomacy.react.door"

    _add_list(
        en,
        es,
        f"{p}.high.open.baby",
        [
            "§a[Baby Villager] Hi hero! *waves*",
            "§a[Baby Villager] Come in, you're the best!",
            "§a[Baby Villager] Welcome! *giggles*",
            "§a[Baby Villager] Our hero is here!",
            "§a[Baby Villager] Mom says you're really good!",
        ],
        [
            "§a[Aldeano Bebé] ¡Hola héroe! *saluda*",
            "§a[Aldeano Bebé] ¡Entra, eres el mejor!",
            "§a[Aldeano Bebé] ¡Bienvenido! *se ríe*",
            "§a[Aldeano Bebé] ¡Nuestro héroe está aquí!",
            "§a[Aldeano Bebé] ¡Mamá dice que eres muy bueno!",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.high.open.night",
        [
            "§a[Villager] Welcome, friend. Travel safe in the dark!",
            "§a[Villager] Step in from the night, champion!",
            "§a[Villager] Our home is yours, even at night!",
            "§a[Villager] Please stay safe inside!",
        ],
        [
            "§a[Aldeano] Bienvenido, amigo. ¡Viaja seguro en la noche!",
            "§a[Aldeano] Entra de la oscuridad, ¡campeón!",
            "§a[Aldeano] ¡Nuestra casa es tu casa, incluso de noche!",
            "§a[Aldeano] ¡Por favor, quédate seguro adentro!",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.high.open.morning",
        [
            "§a[Villager] Good morning, hero! Come in!",
            "§a[Villager] Early bird! Please, come inside!",
            "§a[Villager] Fresh morning—welcome, friend!",
            "§a[Villager] Good morning! Our doors are always open to you!",
        ],
        [
            "§a[Aldeano] ¡Buenos días, héroe! ¡Entra!",
            "§a[Aldeano] ¡Madrugador! ¡Por favor, entra!",
            "§a[Aldeano] ¡Día fresco, bienvenido amigo!",
            "§a[Aldeano] ¡Buenos días! ¡Nuestras puertas siempre están abiertas para ti!",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.high.open.day",
        [
            "§a[Villager] Welcome, champion!",
            "§a[Villager] Please, come in!",
            "§a[Villager] Our doors are open to you!",
            "§a[Villager] Feel free to enter, friend!",
            "§a[Villager] Come in, make yourself at home!",
            "§a[Villager] You're always welcome here!",
            "§a[Villager] Ah, our protector arrives!",
            "§a[Villager] Enter freely, brave one!",
        ],
        [
            "§a[Aldeano] ¡Bienvenido, campeón!",
            "§a[Aldeano] ¡Por favor, pasa!",
            "§a[Aldeano] ¡Nuestras puertas están abiertas para ti!",
            "§a[Aldeano] ¡Siéntete libre de entrar, amigo!",
            "§a[Aldeano] ¡Entra, ponte cómodo!",
            "§a[Aldeano] ¡Siempre eres bienvenido aquí!",
            "§a[Aldeano] ¡Ah, nuestro protector llega!",
            "§a[Aldeano] ¡Entra libremente, valiente!",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.high.close.baby",
        [
            "§a[Baby Villager] Thanks! *smiles*",
            "§a[Baby Villager] Good manners!",
            "§a[Baby Villager] You're so kind!",
            "§a[Baby Villager] Mom taught me that too!",
        ],
        [
            "§a[Aldeano Bebé] ¡Gracias! *sonríe*",
            "§a[Aldeano Bebé] ¡Buenos modales!",
            "§a[Aldeano Bebé] ¡Eres tan amable!",
            "§a[Aldeano Bebé] ¡Mamá también me enseñó eso!",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.high.close.adult",
        [
            "§a[Villager] Thanks for closing it!",
            "§a[Villager] I appreciate the courtesy, friend!",
            "§a[Villager] Such good manners!",
            "§a[Villager] You're so considerate!",
            "§a[Villager] Thanks—it keeps the cold out!",
            "§a[Villager] Much appreciated, hero!",
        ],
        [
            "§a[Aldeano] ¡Gracias por cerrarla!",
            "§a[Aldeano] ¡Aprecio la cortesía, amigo!",
            "§a[Aldeano] ¡Qué buenos modales!",
            "§a[Aldeano] ¡Eres tan considerado!",
            "§a[Aldeano] ¡Gracias, mantiene el frío afuera!",
            "§a[Aldeano] ¡Muy apreciado, héroe!",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.neutral.open.night",
        [
            "§e[Villager] Come in. Careful—it's dark out.",
            "§e[Villager] Go ahead. Watch for mobs.",
            "§e[Villager] Fine. Don't stay outside long.",
        ],
        [
            "§e[Aldeano] Entra. Cuidado, está oscuro afuera.",
            "§e[Aldeano] Adelante. Cuidado con los mobs.",
            "§e[Aldeano] Seguro. No te quedes afuera mucho tiempo.",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.neutral.open.day",
        [
            "§e[Villager] Go ahead.",
            "§e[Villager] Sure, come in.",
            "§e[Villager] Alright.",
            "§e[Villager] Feel free.",
            "§e[Villager] Yes, that's fine.",
            "§e[Villager] Enter if you need to.",
        ],
        [
            "§e[Aldeano] Adelante.",
            "§e[Aldeano] Seguro, entra.",
            "§e[Aldeano] De acuerdo.",
            "§e[Aldeano] Siéntete libre.",
            "§e[Aldeano] Sí, está bien.",
            "§e[Aldeano] Entra si lo necesitas.",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.neutral.close",
        [
            "§e[Villager] Thanks.",
            "§e[Villager] Alright.",
            "§e[Villager] Appreciated.",
            "§e[Villager] Fine.",
        ],
        [
            "§e[Aldeano] Gracias.",
            "§e[Aldeano] De acuerdo.",
            "§e[Aldeano] Apreciado.",
            "§e[Aldeano] Bien.",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.low.baby",
        [
            "§6[Baby Villager] Umm... I'm watching you...",
            "§6[Baby Villager] Mommy doesn't trust you...",
            "§6[Baby Villager] *hides behind the door*",
        ],
        [
            "§6[Aldeano Bebé] Umm... Te estoy observando...",
            "§6[Aldeano Bebé] Mami no confía en ti...",
            "§6[Aldeano Bebé] *se esconde detrás de la puerta*",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.low.adult",
        [
            "§6[Villager] *watches suspiciously*",
            "§6[Villager] I've got my eye on you...",
            "§6[Villager] Don't try anything funny.",
            "§6[Villager] Make it quick.",
            "§6[Villager] I'm not happy about this.",
            "§6[Villager] You'd better not steal anything...",
        ],
        [
            "§6[Aldeano] *observa sospechosamente*",
            "§6[Aldeano] Te tengo vigilado...",
            "§6[Aldeano] No intentes nada gracioso.",
            "§6[Aldeano] Hazlo rápido.",
            "§6[Aldeano] No estoy feliz con esto.",
            "§6[Aldeano] Mejor que no robes nada...",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.neg.baby",
        [
            "§c[Baby Villager] Stop touching our doors!",
            "§c[Baby Villager] That's not yours!",
            "§c[Baby Villager] Mommy! A bad person! *cries*",
            "§c[Baby Villager] Go away! *scared*",
            "§c[Baby Villager] You're scary!",
            "§c[Baby Villager] HELP! *runs*",
        ],
        [
            "§c[Aldeano Bebé] ¡Deja de tocar nuestras puertas!",
            "§c[Aldeano Bebé] ¡Eso no es tuyo!",
            "§c[Aldeano Bebé] ¡Mami! ¡Hay una mala persona! *llora*",
            "§c[Aldeano Bebé] ¡Vete! *asustado*",
            "§c[Aldeano Bebé] ¡Das miedo!",
            "§c[Aldeano Bebé] ¡AYUDA! *corre*",
        ],
    )

    _add_list(
        en,
        es,
        f"{p}.neg.adult",
        [
            "§c[Villager] Get your hands off our doors!",
            "§c[Villager] That's private property!",
            "§c[Villager] You're not welcome here!",
            "§c[Villager] Stop entering our homes!",
            "§c[Villager] GET OUT!",
            "§c[Villager] How DARE you!?",
            "§c[Villager] GUARDS! Intruder!",
            "§c[Villager] This is OUR home, thief!",
            "§c[Villager] You have NO right to be here!",
            "§c[Villager] I should call the Iron Golems!",
        ],
        [
            "§c[Aldeano] ¡Quita tus manos de nuestras puertas!",
            "§c[Aldeano] ¡Eso es propiedad privada!",
            "§c[Aldeano] ¡No eres bienvenido aquí!",
            "§c[Aldeano] ¡Deja de entrar a nuestros hogares!",
            "§c[Aldeano] ¡SAL de aquí!",
            "§c[Aldeano] ¿¡Cómo TE ATREVES!?",
            "§c[Aldeano] ¡GUARDIAS! ¡Intruso!",
            "§c[Aldeano] ¡Esta es NUESTRA casa, ladrón!",
            "§c[Aldeano] ¡NO tienes derecho a estar aquí!",
            "§c[Aldeano] ¡Debería llamar a los Gólems de Hierro!",
        ],
    )
