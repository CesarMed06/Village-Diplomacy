# -*- coding: utf-8 -*-
"""Villager reactions when the player attacks (not kills) village animals."""


def inject_animal_attack(en, es):
    def add(animal, baby_en, baby_es, adult_en, adult_es):
        for i, t in enumerate(baby_en):
            en[f"villagediplomacy.react.animalattack.{animal}.baby.{i}"] = t
        for i, t in enumerate(baby_es):
            es[f"villagediplomacy.react.animalattack.{animal}.baby.{i}"] = t
        for i, t in enumerate(adult_en):
            en[f"villagediplomacy.react.animalattack.{animal}.adult.{i}"] = t
        for i, t in enumerate(adult_es):
            es[f"villagediplomacy.react.animalattack.{animal}.adult.{i}"] = t

    add(
        "cow",
        [
            "§c[Baby Villager] Don't hurt our cows! We need milk!",
            "§c[Baby Villager] That cow gives us milk! *cries*",
            "§c[Baby Villager] Moo is my friend!",
            "§c[Baby Villager] Leave the cow alone!",
            "§c[Baby Villager] I love that cow!",
        ],
        [
            "§c[Bebé] ¡No lastimes nuestras vacas! ¡Necesitamos leche!",
            "§c[Bebé] ¡Esa vaca nos da leche! *llora*",
            "§c[Bebé] ¡Muuu es mi amiga!",
            "§c[Bebé] ¡Deja a la vaca en paz!",
            "§c[Bebé] ¡Quiero mucho a esa vaca!",
        ],
        [
            "§c[Villager] Stop! That cow provides milk for the village!",
            "§c[Villager] Our dairy supply! Leave her alone!",
            "§c[Villager] That cow feeds our children!",
            "§c[Villager] We depend on those cows for milk and leather!",
            "§c[Villager] Stay away from our livestock!",
            "§c[Villager] Those cows are essential for our survival!",
            "§c[Villager] That's weeks of milk you're threatening!",
        ],
        [
            "§c[Aldeano] ¡Para! ¡Esa vaca da leche a la aldea!",
            "§c[Aldeano] ¡Nuestro suministro de leche! ¡Déjala en paz!",
            "§c[Aldeano] ¡Esa vaca alimenta a nuestros niños!",
            "§c[Aldeano] ¡Dependemos de esas vacas para leche y cuero!",
            "§c[Aldeano] ¡Aléjate de nuestro ganado!",
            "§c[Aldeano] ¡Esas vacas son vitales para sobrevivir!",
            "§c[Aldeano] ¡Estás amenazando semanas de leche!",
        ],
    )

    add(
        "sheep",
        [
            "§c[Baby Villager] Don't hurt Fluffy! *cries*",
            "§c[Baby Villager] That sheep makes our beds!",
            "§c[Baby Villager] I like petting the sheep!",
            "§c[Baby Villager] Leave the woolly one alone!",
            "§c[Baby Villager] The sheep is so soft!",
        ],
        [
            "§c[Bebé] ¡No lastimes a Fluffy! *llora*",
            "§c[Bebé] ¡Esa oveja hace nuestras camas!",
            "§c[Bebé] ¡Me gusta acariciar a las ovejas!",
            "§c[Bebé] ¡Deja en paz a la lanuda!",
            "§c[Bebé] ¡La oveja es tan suave!",
        ],
        [
            "§c[Villager] That sheep provides our wool!",
            "§c[Villager] Stop! We need that wool for blankets!",
            "§c[Villager] Our textile supply! Leave her alone!",
            "§c[Villager] Those sheep keep us warm in winter!",
            "§c[Villager] We shear those sheep for clothes!",
            "§c[Villager] That's our source of wool, brute!",
            "§c[Villager] Without wool, we freeze!",
        ],
        [
            "§c[Aldeano] ¡Esa oveja nos da lana!",
            "§c[Aldeano] ¡Para! ¡Necesitamos esa lana para mantas!",
            "§c[Aldeano] ¡Nuestro textil! ¡Déjala en paz!",
            "§c[Aldeano] ¡Esas ovejas nos abrigan en invierno!",
            "§c[Aldeano] ¡Esquilamos esas ovejas para la ropa!",
            "§c[Aldeano] ¡Esa es nuestra lana, bruto!",
            "§c[Aldeano] ¡Sin lana nos helamos!",
        ],
    )

    add(
        "pig",
        [
            "§c[Baby Villager] Don't hurt the piggy! *sobs*",
            "§c[Baby Villager] That pig makes funny noises!",
            "§c[Baby Villager] Oink-oink is cute!",
            "§c[Baby Villager] Leave the piggy alone!",
            "§c[Baby Villager] I feed that pig every day!",
        ],
        [
            "§c[Bebé] ¡No lastimes al cerdito! *solloza*",
            "§c[Bebé] ¡Ese cerdo hace ruidos graciosos!",
            "§c[Bebé] ¡El oink-oink es mono!",
            "§c[Bebé] ¡Deja al cerdito en paz!",
            "§c[Bebé] ¡Yo alimento a ese cerdo cada día!",
        ],
        [
            "§c[Villager] That pig is valuable livestock!",
            "§c[Villager] Stop! Those pigs are for breeding!",
            "§c[Villager] We raise those pigs carefully!",
            "§c[Villager] That pig will feed families this winter!",
            "§c[Villager] Leave our meat supply alone!",
            "§c[Villager] Those pigs are our investment!",
            "§c[Villager] Back off! That pig is reserved!",
        ],
        [
            "§c[Aldeano] ¡Ese cerdo es ganado valioso!",
            "§c[Aldeano] ¡Para! ¡Esos cerdos son para criar!",
            "§c[Aldeano] ¡Criamos esos cerdos con cuidado!",
            "§c[Aldeano] ¡Ese cerdo alimentará familias este invierno!",
            "§c[Aldeano] ¡Deja nuestro suministro de carne!",
            "§c[Aldeano] ¡Esos cerdos son nuestra inversión!",
            "§c[Aldeano] ¡Atrás! ¡Ese cerdo está reservado!",
        ],
    )

    add(
        "chicken",
        [
            "§c[Baby Villager] Don't hurt the chicken! *cries*",
            "§c[Baby Villager] That chicken gives us eggs!",
            "§c[Baby Villager] I collect eggs from them!",
            "§c[Baby Villager] Chickens are my job!",
            "§c[Baby Villager] Feathers is so cute!",
        ],
        [
            "§c[Bebé] ¡No lastimes a la gallina! *llora*",
            "§c[Bebé] ¡Esa gallina nos da huevos!",
            "§c[Bebé] ¡Yo recojo sus huevos!",
            "§c[Bebé] ¡Las gallinas son mi trabajo!",
            "§c[Bebé] ¡Plumas es tan mona!",
        ],
        [
            "§c[Villager] Those chickens lay our eggs!",
            "§c[Villager] Stop! That chicken is our breakfast!",
            "§c[Villager] We need those eggs daily!",
            "§c[Villager] That chicken is part of our farm!",
            "§c[Villager] Leave our birds alone!",
            "§c[Villager] Those chickens are egg producers!",
            "§c[Villager] No chickens, no eggs!",
        ],
        [
            "§c[Aldeano] ¡Esas gallinas ponen nuestros huevos!",
            "§c[Aldeano] ¡Para! ¡Esa gallina es nuestro desayuno!",
            "§c[Aldeano] ¡Necesitamos esos huevos cada día!",
            "§c[Aldeano] ¡Esa gallina es parte de nuestra granja!",
            "§c[Aldeano] ¡Deja a nuestras aves en paz!",
            "§c[Aldeano] ¡Esas gallinas son productoras de huevos!",
            "§c[Aldeano] ¡Sin gallinas, no hay huevos!",
        ],
    )

    add(
        "rabbit",
        [
            "§c[Baby Villager] Don't hurt the bunny! *cries*",
            "§c[Baby Villager] Bunnies are so cute!",
            "§c[Baby Villager] That's my favorite rabbit!",
            "§c[Baby Villager] Leave the hopper alone!",
            "§c[Baby Villager] I want to pet them!",
        ],
        [
            "§c[Bebé] ¡No lastimes al conejito! *llora*",
            "§c[Bebé] ¡Los conejitos son tan monos!",
            "§c[Bebé] ¡Ese es mi conejo favorito!",
            "§c[Bebé] ¡Deja en paz al saltarín!",
            "§c[Bebé] ¡Quiero acariciarlos!",
        ],
        [
            "§c[Villager] Those rabbits are part of our ecosystem!",
            "§c[Villager] Leave the rabbits alone!",
            "§c[Villager] They're harmless creatures!",
            "§c[Villager] Stop attacking innocent animals!",
            "§c[Villager] Those rabbits help our gardens!",
            "§c[Villager] What did that rabbit ever do to you!?",
            "§c[Villager] They're just rabbits!",
        ],
        [
            "§c[Aldeano] ¡Esos conejos son parte de nuestro ecosistema!",
            "§c[Aldeano] ¡Deja a los conejos en paz!",
            "§c[Aldeano] ¡Son criaturas inofensivas!",
            "§c[Aldeano] ¡Deja de atacar animales inocentes!",
            "§c[Aldeano] ¡Esos conejos ayudan nuestros huertos!",
            "§c[Aldeano] ¿¡Qué te hizo ese conejo!?",
            "§c[Aldeano] ¡Solo son conejos!",
        ],
    )

    add(
        "horse",
        [
            "§c[Baby Villager] Don't hurt the horsie! *cries*",
            "§c[Baby Villager] I want to ride horses when I grow up!",
            "§c[Baby Villager] That horse is so pretty!",
            "§c[Baby Villager] Leave the horse alone!",
            "§c[Baby Villager] Horses are noble!",
        ],
        [
            "§c[Bebé] ¡No lastimes al caballito! *llora*",
            "§c[Bebé] ¡De mayor quiero montar a caballo!",
            "§c[Bebé] ¡Ese caballo es tan bonito!",
            "§c[Bebé] ¡Deja al caballo en paz!",
            "§c[Bebé] ¡Los caballos son nobles!",
        ],
        [
            "§c[Villager] That horse is our transport!",
            "§c[Villager] Stop! We need that horse for travel!",
            "§c[Villager] Those horses are expensive!",
            "§c[Villager] That's weeks of breeding work!",
            "§c[Villager] Leave our horses alone!",
            "§c[Villager] We use those horses for trade routes!",
            "§c[Villager] That horse carries our supplies!",
            "§c[Villager] You're attacking our mobility!",
        ],
        [
            "§c[Aldeano] ¡Ese caballo es nuestro transporte!",
            "§c[Aldeano] ¡Para! ¡Necesitamos ese caballo para viajar!",
            "§c[Aldeano] ¡Esos caballos son caros!",
            "§c[Aldeano] ¡Son semanas de trabajo de cría!",
            "§c[Aldeano] ¡Deja nuestros caballos en paz!",
            "§c[Aldeano] ¡Usamos esos caballos en rutas comerciales!",
            "§c[Aldeano] ¡Ese caballo lleva nuestros suministros!",
            "§c[Aldeano] ¡Estás atacando nuestra movilidad!",
        ],
    )

    add(
        "camel",
        [
            "§c[Baby Villager] Don't hurt the camel! *cries*",
            "§c[Baby Villager] Camels are for the desert!",
            "§c[Baby Villager] That camel is cool!",
            "§c[Baby Villager] Leave the camel alone!",
            "§c[Baby Villager] I like camels!",
        ],
        [
            "§c[Bebé] ¡No lastimes al camello! *llora*",
            "§c[Bebé] ¡Los camellos son para el desierto!",
            "§c[Bebé] ¡Ese camello mola!",
            "§c[Bebé] ¡Deja al camello en paz!",
            "§c[Bebé] ¡Me gustan los camellos!",
        ],
        [
            "§c[Villager] That camel is our desert transport!",
            "§c[Villager] Stop! We need that camel for travel!",
            "§c[Villager] Those camels are expensive!",
            "§c[Villager] That's weeks of breeding work!",
            "§c[Villager] Leave our camels alone!",
            "§c[Villager] We use those camels for desert routes!",
            "§c[Villager] That camel carries our supplies!",
            "§c[Villager] You're attacking our mobility!",
        ],
        [
            "§c[Aldeano] ¡Ese camello es nuestro transporte del desierto!",
            "§c[Aldeano] ¡Para! ¡Necesitamos ese camello para viajar!",
            "§c[Aldeano] ¡Esos camellos son caros!",
            "§c[Aldeano] ¡Son semanas de trabajo de cría!",
            "§c[Aldeano] ¡Deja nuestros camellos en paz!",
            "§c[Aldeano] ¡Usamos esos camellos en rutas del desierto!",
            "§c[Aldeano] ¡Ese camello lleva nuestros suministros!",
            "§c[Aldeano] ¡Estás atacando nuestra movilidad!",
        ],
    )

    add(
        "other",
        ["§c[Baby Villager] Don't hurt them!"],
        ["§c[Bebé] ¡No les hagas daño!"],
        ["§c[Villager] Stop!"],
        ["§c[Aldeano] ¡Para!"],
    )
