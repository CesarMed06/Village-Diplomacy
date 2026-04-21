# -*- coding: utf-8 -*-
"""Animal-kill reaction lines; injected into lang JSON by generate_lang.py."""


def inject_animal_death(en, es):
    def add(animal, baby_en, baby_es, adult_en, adult_es):
        for i, t in enumerate(baby_en):
            en[f"villagediplomacy.react.animaldeath.{animal}.baby.{i}"] = t
        for i, t in enumerate(baby_es):
            es[f"villagediplomacy.react.animaldeath.{animal}.baby.{i}"] = t
        for i, t in enumerate(adult_en):
            en[f"villagediplomacy.react.animaldeath.{animal}.adult.{i}"] = t
        for i, t in enumerate(adult_es):
            es[f"villagediplomacy.react.animaldeath.{animal}.adult.{i}"] = t

    add(
        "cow",
        [
            "§c[Baby Villager] NOOO! You killed Bessie! *screams*",
            "§c[Baby Villager] No more milk now! *sobs*",
            "§c[Baby Villager] That cow had a calf! *cries*",
            "§c[Baby Villager] Why!? She gave us milk! *heartbroken*",
            "§c[Baby Villager] Moo is gone... *sobs*",
        ],
        [
            "§c[Bebé] ¡NOOO! ¡Mataste a Bessie! *gritos*",
            "§c[Bebé] ¡Ya no hay leche! *solloza*",
            "§c[Bebé] ¡Esa vaca tenía un ternero! *llora*",
            "§c[Bebé] ¿¡Por qué!? ¡Nos daba leche! *desolado*",
            "§c[Bebé] Muuu se ha ido... *solloza*",
        ],
        [
            "§c[Villager] YOU KILLED OUR COW!",
            "§c[Villager] MONTHS of milk production LOST!",
            "§c[Villager] How will we feed our children without milk!?",
            "§c[Villager] That cow was worth 10 emeralds!",
            "§c[Villager] You've destroyed our dairy farm!",
            "§c[Villager] ANIMAL KILLER! That cow had calves!",
            "§c[Villager] We raised that cow from birth!",
            "§c[Villager] No milk, no cheese, no leather! Thanks to YOU!",
        ],
        [
            "§c[Aldeano] ¡MATASTE A NUESTRA VACA!",
            "§c[Aldeano] ¡MESES de producción de leche PERDIDOS!",
            "§c[Aldeano] ¿¡Cómo alimentaremos a los niños sin leche!?",
            "§c[Aldeano] ¡Esa vaca valía 10 esmeraldas!",
            "§c[Aldeano] ¡Has arruinado nuestra granja lechera!",
            "§c[Aldeano] ¡ASESINO DE ANIMALES! ¡Esa vaca tenía terneros!",
            "§c[Aldeano] ¡Criamos esa vaca desde que nació!",
            "§c[Aldeano] ¡Sin leche, sin queso, sin cuero! ¡Por TU culpa!",
        ],
    )
    add(
        "sheep",
        [
            "§c[Baby Villager] You killed Fluffy! NOOO! *cries*",
            "§c[Baby Villager] No more wool now! *sobs*",
            "§c[Baby Villager] That sheep was so soft! *screams*",
            "§c[Baby Villager] Why kill the woolly one!? *heartbroken*",
            "§c[Baby Villager] I was going to shear her tomorrow! *devastated*",
        ],
        [
            "§c[Bebé] ¡Mataste a Fluffy! ¡NOOO! *llora*",
            "§c[Bebé] ¡Ya no hay lana! *solloza*",
            "§c[Bebé] ¡Esa oveja era tan suave! *gritos*",
            "§c[Bebé] ¿¡Por qué a la lanuda!? *desolado*",
            "§c[Bebé] ¡Mañana la iba a esquilar! *devastado*",
        ],
        [
            "§c[Villager] YOU KILLED OUR SHEEP!",
            "§c[Villager] That sheep produced wool for YEARS!",
            "§c[Villager] How will we make blankets now!?",
            "§c[Villager] We'll FREEZE without that wool!",
            "§c[Villager] That sheep was our textile source!",
            "§c[Villager] MURDERER! We raised that sheep with care!",
            "§c[Villager] No wool means no warm clothes!",
            "§c[Villager] That sheep was white wool - RARE!",
        ],
        [
            "§c[Aldeano] ¡MATASTE A NUESTRA OVEJA!",
            "§c[Aldeano] ¡Esa oveja dio lana durante muchos años!",
            "§c[Aldeano] ¿¡Cómo haremos mantas ahora!?",
            "§c[Aldeano] ¡Nos HELAREMOS sin esa lana!",
            "§c[Aldeano] ¡Era nuestra fuente de textiles!",
            "§c[Aldeano] ¡ASESINO! ¡Criamos esa oveja con mimo!",
            "§c[Aldeano] ¡Sin lana no hay ropa de abrigo!",
            "§c[Aldeano] ¡Era lana blanca—RARA!",
        ],
    )
    add(
        "pig",
        [
            "§c[Baby Villager] You killed the piggy! MONSTER! *cries*",
            "§c[Baby Villager] That pig was going to have babies! *sobs*",
            "§c[Baby Villager] Oink-oink is gone! *screams*",
            "§c[Baby Villager] Why!? He was so funny! *devastated*",
            "§c[Baby Villager] I fed that pig carrots! *heartbroken*",
        ],
        [
            "§c[Bebé] ¡Mataste al cerdito! ¡MONSTRUO! *llora*",
            "§c[Bebé] ¡Ese cerdo iba a tener crías! *solloza*",
            "§c[Bebé] ¡Oink-oink se ha ido! *gritos*",
            "§c[Bebé] ¿¡Por qué!? ¡Era tan gracioso! *devastado*",
            "§c[Bebé] ¡Yo le daba zanahorias! *desolado*",
        ],
        [
            "§c[Villager] YOU SLAUGHTERED OUR PIG!",
            "§c[Villager] That pig was breeding stock!",
            "§c[Villager] You just killed our WINTER MEAT SUPPLY!",
            "§c[Villager] We raised that pig for MONTHS!",
            "§c[Villager] THIEF! That pig was our investment!",
            "§c[Villager] How DARE you kill our livestock!?",
            "§c[Villager] That pig was going to feed families!",
            "§c[Villager] You've ruined our breeding program!",
        ],
        [
            "§c[Aldeano] ¡MASACRASTE A NUESTRO CERDO!",
            "§c[Aldeano] ¡Era cerdo reproductor!",
            "§c[Aldeano] ¡Acabas de matar nuestro SUMINISTRO de carne de invierno!",
            "§c[Aldeano] ¡Criamos ese cerdo durante MESES!",
            "§c[Aldeano] ¡LADRÓN! ¡Ese cerdo era nuestra inversión!",
            "§c[Aldeano] ¿¡Cómo te ATREVES a matar nuestro ganado!?",
            "§c[Aldeano] ¡Ese cerdo iba a alimentar familias!",
            "§c[Aldeano] ¡Has arruinado nuestro programa de cría!",
        ],
    )
    add(
        "rabbit",
        [
            "§c[Baby Villager] You killed the bunny! *cries*",
            "§c[Baby Villager] That bunny was so cute! *sobs*",
            "§c[Baby Villager] Why did you do that!? *devastated*",
            "§c[Baby Villager] I wanted to pet the bunny! *heartbroken*",
        ],
        [
            "§c[Bebé] ¡Mataste al conejito! *llora*",
            "§c[Bebé] ¡Era tan mono! *solloza*",
            "§c[Bebé] ¿¡Por qué hiciste eso!? *devastado*",
            "§c[Bebé] ¡Quería acariciarlo! *desolado*",
        ],
        [
            "§c[Villager] YOU KILLED THE RABBIT!",
            "§c[Villager] That rabbit helped control garden pests!",
            "§c[Villager] What kind of MONSTER kills rabbits!?",
            "§c[Villager] They're harmless creatures!",
            "§c[Villager] The children loved that rabbit!",
            "§c[Villager] You're a BRUTE and a BULLY!",
            "§c[Villager] Killing innocent animals! Shame!",
            "§c[Villager] That rabbit never hurt anyone!",
        ],
        [
            "§c[Aldeano] ¡MATASTE AL CONEJO!",
            "§c[Aldeano] ¡Ese conejo ayudaba con las plagas del huerto!",
            "§c[Aldeano] ¿¡Qué clase de MONSTRUO mata conejos!?",
            "§c[Aldeano] ¡Son criaturas inofensivas!",
            "§c[Aldeano] ¡A los niños les encantaba ese conejo!",
            "§c[Aldeano] ¡Eres un BRUTO y un ABUSÓN!",
            "§c[Aldeano] ¡Matar animales inocentes! ¡Qué vergüenza!",
            "§c[Aldeano] ¡Ese conejo no hizo daño a nadie!",
        ],
    )
    add(
        "horse",
        [
            "§c[Baby Villager] You killed the horsie! NOOO! *screams*",
            "§c[Baby Villager] That horse was so strong! *cries*",
            "§c[Baby Villager] I wanted to ride him! *devastated*",
            "§c[Baby Villager] Horses are noble! Why!? *sobs*",
            "§c[Baby Villager] That's the worst! *heartbroken*",
        ],
        [
            "§c[Bebé] ¡Mataste al caballito! ¡NOOO! *gritos*",
            "§c[Bebé] ¡Ese caballo era tan fuerte! *llora*",
            "§c[Bebé] ¡Quería montarlo! *devastado*",
            "§c[Bebé] ¡Los caballos son nobles! ¿¡Por qué!? *solloza*",
            "§c[Bebé] ¡Esto es lo peor! *desolado*",
        ],
        [
            "§c[Villager] YOU KILLED OUR HORSE!",
            "§c[Villager] That horse was EXPENSIVE! 20 emeralds!",
            "§c[Villager] We needed that horse for TRAVEL!",
            "§c[Villager] That took WEEKS to tame and breed!",
            "§c[Villager] How will we transport goods now!?",
            "§c[Villager] HORSE KILLER! That was our LIVELIHOOD!",
            "§c[Villager] We used that horse for trade routes!",
            "§c[Villager] You've paralyzed our trade!",
            "§c[Villager] That horse was part of the family!",
            "§c[Villager] Killing a horse!? You have NO HEART!",
        ],
        [
            "§c[Aldeano] ¡MATASTE A NUESTRO CABALLO!",
            "§c[Aldeano] ¡Ese caballo era CARO! ¡20 esmeraldas!",
            "§c[Aldeano] ¡Lo necesitábamos para VIAJAR!",
            "§c[Aldeano] ¡Tardamos SEMANAS en domesticarlo y criarlo!",
            "§c[Aldeano] ¿¡Cómo transportaremos mercancías ahora!?",
            "§c[Aldeano] ¡ASESINO DE CABALLOS! ¡Era nuestro SUSTENTO!",
            "§c[Aldeano] ¡Usábamos ese caballo en las rutas comerciales!",
            "§c[Aldeano] ¡Has paralizado nuestro comercio!",
            "§c[Aldeano] ¡Ese caballo era de la familia!",
            "§c[Aldeano] ¿¡Matar un caballo!? ¡No tienes corazón!",
        ],
    )
    add(
        "camel",
        [
            "§c[Baby Villager] You killed the camel! NOOO! *screams*",
            "§c[Baby Villager] That camel was so tall! *cries*",
            "§c[Baby Villager] I wanted to ride him! *devastated*",
            "§c[Baby Villager] Camels are amazing! Why!? *sobs*",
            "§c[Baby Villager] That's so cruel! *heartbroken*",
        ],
        [
            "§c[Bebé] ¡Mataste al camello! ¡NOOO! *gritos*",
            "§c[Bebé] ¡Ese camello era tan alto! *llora*",
            "§c[Bebé] ¡Quería montarlo! *devastado*",
            "§c[Bebé] ¡Los camellos son increíbles! ¿¡Por qué!? *solloza*",
            "§c[Bebé] ¡Qué crueldad! *desolado*",
        ],
        [
            "§c[Villager] YOU KILLED OUR CAMEL!",
            "§c[Villager] That camel was EXPENSIVE! 30 emeralds!",
            "§c[Villager] We needed that camel for DESERT TRAVEL!",
            "§c[Villager] That took WEEKS to tame and breed!",
            "§c[Villager] How will we cross the desert now!?",
        ],
        [
            "§c[Aldeano] ¡MATASTE A NUESTRO CAMELLO!",
            "§c[Aldeano] ¡Ese camello era CARO! ¡30 esmeraldas!",
            "§c[Aldeano] ¡Lo necesitábamos para cruzar el DESIERTO!",
            "§c[Aldeano] ¡Tardamos SEMANAS en domesticarlo y criarlo!",
            "§c[Aldeano] ¿¡Cómo cruzaremos el desierto ahora!?",
        ],
    )
    add(
        "chicken",
        [
            "§c[Baby Villager] The chicken! *cries* No more eggs!",
            "§c[Baby Villager] That clucker was my friend! *sobs*",
            "§c[Baby Villager] Why hurt the fluffy chicken!?",
        ],
        [
            "§c[Bebé] ¡La gallina! *llora* ¡Ya no hay huevos!",
            "§c[Bebé] ¡Esa clueca era mi amiga! *solloza*",
            "§c[Bebé] ¿¡Por qué a la gallina esponjosa!?",
        ],
        [
            "§c[Villager] YOU KILLED OUR CHICKEN!",
            "§c[Villager] That bird gave us eggs every morning!",
            "§c[Villager] Small loss, big insult—why our livestock!?",
            "§c[Villager] The coop feels empty now.",
            "§c[Villager] Feathers and eggs—both gone thanks to you.",
        ],
        [
            "§c[Aldeano] ¡MATASTE A NUESTRA GALLINA!",
            "§c[Aldeano] ¡Ese ave nos daba huevos cada mañana!",
            "§c[Aldeano] Parece poca cosa, pero es un insulto—¿por qué nuestro ganado?",
            "§c[Aldeano] El gallinero se siente vacío ahora.",
            "§c[Aldeano] Plumas y huevos—se acabaron por tu culpa.",
        ],
    )
    add(
        "other",
        ["§c[Baby Villager] You killed it! *cries*"],
        ["§c[Bebé] ¡Lo mataste! *llora*"],
        ["§c[Villager] MURDERER!"],
        ["§c[Aldeano] ¡ASESINO!"],
    )
