# -*- coding: utf-8 -*-
"""Iron golem personality, strike warnings, and combat barks."""


def inject_golem_lines(en, es):
    def both(key, en_t, es_t):
        en[key] = en_t
        es[key] = es_t

    traits_en = {
        "gentle": "Gentle",
        "stern": "Stern",
        "fierce": "Fierce",
        "devoted": "Devoted",
        "dutiful": "Dutiful",
        "independent": "Independent",
    }
    traits_es = {
        "gentle": "Gentil",
        "stern": "Severo",
        "fierce": "Feroz",
        "devoted": "Devoto",
        "dutiful": "Cumplidor",
        "independent": "Independiente",
    }
    for k, v in traits_en.items():
        en[f"villagediplomacy.golem.trait.{k}"] = v
    for k, v in traits_es.items():
        es[f"villagediplomacy.golem.trait.{k}"] = v

    both("villagediplomacy.golem.title", "%s §7(%s§7)", "%s §7(%s§7)")

    for mood, en_t, es_t in [
        (
            "gentle",
            "§a[%s] *nods peacefully*",
            "§a[%s] *asiente pacíficamente*",
        ),
        (
            "stern",
            "§7[%s] *stands guard in silence*",
            "§7[%s] *permanece en guardia silenciosamente*",
        ),
        (
            "fierce",
            "§c[%s] *stomps, ready for battle*",
            "§c[%s] *pisa fuerte, listo para la batalla*",
        ),
        (
            "default",
            "§7[%s] *watches silently*",
            "§7[%s] *mira silenciosamente*",
        ),
    ]:
        both(f"villagediplomacy.golem.greet.{mood}", en_t, es_t)

    for loy, en_t, es_t in [
        (
            "devoted",
            "§6[%s] Nothing will harm this village!",
            "§6[%s] ¡Nada dañará esta aldea!",
        ),
        (
            "dutiful",
            "§e[%s] *patrols the perimeter*",
            "§e[%s] *patrulla el perímetro*",
        ),
        (
            "independent",
            "§b[%s] *wanders freely*",
            "§b[%s] *vaga libremente*",
        ),
        (
            "default",
            "§7[%s] *patrols*",
            "§7[%s] *patrulla*",
        ),
    ]:
        both(f"villagediplomacy.golem.patrol.{loy}", en_t, es_t)

    for mood, en_t, es_t in [
        (
            "gentle",
            "§a[%s] Please leave in peace!",
            "§a[%s] ¡Por favor, vete en paz!",
        ),
        (
            "stern",
            "§7[%s] You are not welcome here.",
            "§7[%s] No eres bienvenido aquí.",
        ),
        (
            "fierce",
            "§c[%s] *HOSTILE ROAR*",
            "§c[%s] *RUGIDO HOSTIL*",
        ),
        (
            "default",
            "§c[%s] *alert*",
            "§c[%s] *alerta*",
        ),
    ]:
        both(f"villagediplomacy.golem.threat.{mood}", en_t, es_t)

    for loy, en_t, es_t in [
        (
            "devoted",
            "§6[%s] I will protect this village with my life!",
            "§6[%s] ¡Protegeré esta aldea con mi vida!",
        ),
        (
            "dutiful",
            "§e[%s] I only do my duty.",
            "§e[%s] Solo cumplo mi deber.",
        ),
        (
            "independent",
            "§b[%s] I protect those who deserve it.",
            "§b[%s] Protejo a quienes lo merecen.",
        ),
        (
            "default",
            "§7[%s] I serve the village.",
            "§7[%s] Sirvo a la aldea.",
        ),
    ]:
        both(f"villagediplomacy.golem.loyalty_line.{loy}", en_t, es_t)

    for mood, en_t, es_t in [
        (
            "gentle",
            "§a[%s] We fight for peace.",
            "§a[%s] La paz es por lo que luchamos.",
        ),
        (
            "stern",
            "§7[%s] Vigilance is eternal.",
            "§7[%s] La vigilancia es eterna.",
        ),
        (
            "fierce",
            "§c[%s] *clanks fists*",
            "§c[%s] *golpea sus puños*",
        ),
        (
            "default",
            "§7[%s] *silent*",
            "§7[%s] *silencioso*",
        ),
    ]:
        both(f"villagediplomacy.golem.temperament_line.{mood}", en_t, es_t)

    danger_en = [
        "§c[%s] You have made yourself an enemy of this village!",
        "§4[%s] *raises fist* LEAVE NOW!",
        "§c[%s] Your crimes will not go unpunished!",
        "§4[%s] *angry stomping*",
        "§c[%s] The village demands justice!",
    ]
    danger_es = [
        "§c[%s] ¡Te has convertido en enemigo de esta aldea!",
        "§4[%s] *levanta el puño amenazadoramente* ¡VETE AHORA!",
        "§c[%s] ¡Tus crímenes no quedarán impunes!",
        "§4[%s] *PISOTONES ENFADADOS*",
        "§c[%s] ¡La aldea exige justicia!",
    ]
    for i in range(5):
        en[f"villagediplomacy.golem.danger.{i}"] = danger_en[i]
        es[f"villagediplomacy.golem.danger.{i}"] = danger_es[i]

    both(
        "villagediplomacy.golem.busy",
        "§7[%s] *busy patrolling*",
        "§7[%s] *ocupado patrullando*",
    )

    stories_en = [
        "Created during a zombie siege to protect %s",
        "Forged by the village blacksmith in ancient times",
        "Awakened when the village was in grave danger",
        "Built by the elders to guard the village gates",
        "Formed from iron donated by grateful traders",
        "Created during a great celebration of peace",
        "Raised to honor a fallen hero of the village",
    ]
    stories_es = [
        "Creado durante un asedio zombi para proteger %s",
        "Forjado por el herrero de la aldea en tiempos antiguos",
        "Despertado cuando la aldea estaba en grave peligro",
        "Construido por los ancianos para guardar las puertas de la aldea",
        "Formado de hierro donado por comerciantes agradecidos",
        "Creado durante una gran celebración de paz",
        "Alzado para honrar a un héroe caído de la aldea",
    ]
    for i in range(7):
        en[f"villagediplomacy.golem.story.{i}"] = stories_en[i]
        es[f"villagediplomacy.golem.story.{i}"] = stories_es[i]

    both(
        "villagediplomacy.golem.player_hit.gentle",
        "§e[%s] Why...? I must defend myself!",
        "§e[%s] ¿Por qué...? ¡Debo defenderme!",
    )
    both(
        "villagediplomacy.golem.player_hit.stern",
        "§c[%s] You chose this.",
        "§c[%s] Tú elegiste esto.",
    )
    both(
        "villagediplomacy.golem.player_hit.fierce",
        "§4[%s] *ROARS* NOW YOU DIE!",
        "§4[%s] *RUGE* ¡AHORA MUERES!",
    )
    both(
        "villagediplomacy.golem.player_hit.default",
        "§c[%s] So be it!",
        "§c[%s] ¡Que así sea!",
    )
    both(
        "villagediplomacy.golem.generic_warn",
        "§c[%s] You asked for it!",
        "§c[%s] ¡Tú te lo buscaste!",
    )
    both(
        "villagediplomacy.golem.generic_name",
        "Iron Golem",
        "Gólem de hierro",
    )

    both(
        "villagediplomacy.sys.golem_hostile_timer",
        "§4[Village Diplomacy] Iron golems are HOSTILE toward you for 30 seconds!",
        "§4[Village Diplomacy] ¡Los gólems de hierro están HOSTILES contigo durante 30 segundos!",
    )

    violent_en = [
        "§c[%s] YOU DARE STRIKE MORE VILLAGERS!?",
        "§4[%s] *FURIOUS STOMPING* STOP NOW!",
        "§c[%s] You're making it worse for yourself!",
        "§4[%s] I WILL CRUSH YOU FOR THIS!",
        "§c[%s] *ROARS* ENOUGH VIOLENCE!",
        "§4[%s] EVERY BLOW SEALS YOUR FATE!",
        "§c[%s] YOU'LL PAY FOR EVERY VILLAGER YOU HURT!",
        "§4[%s] *FIST POUNDS* THIS ENDS NOW!",
    ]
    violent_es = [
        "§c[%s] ¿¡TE ATREVES A ATACAR MÁS ALDEANOS!?",
        "§4[%s] *PISOTONES FURIOSOS* ¡DETENTE AHORA!",
        "§c[%s] ¡ESTÁS EMPEORANDO TU SITUACIÓN!",
        "§4[%s] ¡TE APLASTARÉ POR ESTO!",
        "§c[%s] *RUGE* ¡BASTA DE VIOLENCIA!",
        "§4[%s] ¡CADA GOLPE SELLA TU DESTINO!",
        "§c[%s] ¡PAGARÁS POR CADA ALDEANO QUE LASTIMES!",
        "§4[%s] *GOLPES DE PUÑO* ¡ESTO TERMINA AHORA!",
    ]
    for i in range(8):
        en[f"villagediplomacy.golem.strike.violent.{i}"] = violent_en[i]
        es[f"villagediplomacy.golem.strike.violent.{i}"] = violent_es[i]

    warn1_en = [
        "§e[%s] Hey! Stop that.",
        "§e[%s] Don't touch them.",
        "§e[%s] I'm watching you...",
        "§e[%s] Leave them alone.",
        "§e[%s] That's enough.",
        "§e[%s] Stay away from them.",
        "§e[%s] Don't make me come over there.",
        "§e[%s] You don't want trouble.",
        "§e[%s] These villagers are under MY protection.",
        "§e[%s] Keep your hands to yourself.",
    ]
    warn1_es = [
        "§e[%s] ¡Oye! Detén eso.",
        "§e[%s] No los toques.",
        "§e[%s] Te estoy vigilando...",
        "§e[%s] Déjalos en paz.",
        "§e[%s] Es suficiente.",
        "§e[%s] Aléjate de ellos.",
        "§e[%s] No hagas que vaya hasta allá.",
        "§e[%s] No quieres problemas.",
        "§e[%s] Estos aldeanos están bajo MI protección.",
        "§e[%s] Mantén tus manos quietas.",
    ]
    for i in range(10):
        en[f"villagediplomacy.golem.strike.warn1.{i}"] = warn1_en[i]
        es[f"villagediplomacy.golem.strike.warn1.{i}"] = warn1_es[i]

    warn2_en = [
        "§6[%s] I said STOP!",
        "§6[%s] You're pushing your luck...",
        "§6[%s] Back off NOW!",
        "§6[%s] Final warning!",
        "§6[%s] You don't want to test me!",
        "§6[%s] This is your LAST chance!",
        "§6[%s] I'm losing patience!",
        "§6[%s] Back away or face the consequences!",
        "§6[%s] You're making a HUGE mistake!",
        "§6[%s] One more hit and you're DONE!",
    ]
    warn2_es = [
        "§6[%s] ¡Dije que PARES!",
        "§6[%s] Estás tentando tu suerte...",
        "§6[%s] ¡Retrocede AHORA!",
        "§6[%s] ¡Última advertencia!",
        "§6[%s] ¡No quieres ponerme a prueba!",
        "§6[%s] ¡Esta es tu ÚLTIMA oportunidad!",
        "§6[%s] ¡Estoy perdiendo la paciencia!",
        "§6[%s] ¡Aléjate o enfrenta las consecuencias!",
        "§6[%s] ¡Estás cometiendo un GRAN error!",
        "§6[%s] ¡Un golpe más y estarás ACABADO!",
    ]
    for i in range(10):
        en[f"villagediplomacy.golem.strike.warn2.{i}"] = warn2_en[i]
        es[f"villagediplomacy.golem.strike.warn2.{i}"] = warn2_es[i]

    final_en = [
        "§c[%s] THAT'S IT!",
        "§c[%s] You crossed the line!",
        "§c[%s] NOW YOU'VE DONE IT!",
        "§c[%s] PREPARE YOURSELF!",
        "§c[%s] I'VE HAD ENOUGH!",
        "§c[%s] TIME TO PAY!",
        "§c[%s] YOU'RE FINISHED!",
        "§c[%s] NO MORE MERCY!",
        "§c[%s] FACE MY WRATH!",
        "§c[%s] YOU'VE SEALED YOUR FATE!",
    ]
    final_es = [
        "§c[%s] ¡ESO ES TODO!",
        "§c[%s] ¡Cruzaste la línea!",
        "§c[%s] ¡AHORA SÍ LO HICISTE!",
        "§c[%s] ¡PREPÁRATE!",
        "§c[%s] ¡YA TUVE SUFICIENTE!",
        "§c[%s] ¡HORA DE PAGAR!",
        "§c[%s] ¡ESTÁS ACABADO!",
        "§c[%s] ¡NO MÁS PIEDAD!",
        "§c[%s] ¡ENFRENTA MI IRA!",
        "§c[%s] ¡HAS SELLADO TU DESTINO!",
    ]
    for i in range(10):
        en[f"villagediplomacy.golem.strike.final.{i}"] = final_en[i]
        es[f"villagediplomacy.golem.strike.final.{i}"] = final_es[i]
