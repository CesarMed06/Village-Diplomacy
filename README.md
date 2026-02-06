# Diplomatia de Aldeas

Sistema de reputación y relaciones para Minecraft 1.20.1 que registra las interacciones del jugador con aldeanos y aldeas. Cada aldea mantiene datos de reputación independientes, y los aldeanos responden dinámicamente según tus acciones y sus personalidades individuales.

## Descripción General

Este mod implementa un sistema completo de reputación donde cada acción cerca de aldeanos tiene consecuencias. Comerciar aumenta la reputación, mientras que robar o comportamientos destructivos la disminuyen. El sistema rastra 7 rangos de reputación desde HÉROE hasta VILLANO, con cada rango afectando cómo los aldeanos y golems de hierro interactúan con el jugador.

Las aldeas se detectan automáticamente en un radio de 200 bloques. Cada aldea mantiene puntuaciones de reputación separadas, permitiendo a los jugadores ser héroes en una aldea mientras son villanos en otra.

## Características Principales

- **Seguimiento de reputación por aldea** - Cada aldea mantiene datos de reputación independientes
- **Personalidades de aldeanos** - 15 tipos de temperamento distintos (valiente, tímido, codicioso, paranoico, sabio, etc.)
- **Sistema de memoria de Golems de Hierro** - Los golems rastrean la agresión del jugador y pueden volverse permanentemente hostiles
- **Mensajería dinámica** - Más de 250 mensajes contextuales basados en nivel de reputación y personalidad del aldeano
- **Sistema de nombrado de aldeas** - Asigna nombres personalizados a las aldeas para identificarlas fácilmente
- **Eventos de interacción con bloques** - Los aldeanos reaccionan a la colocación de camas, cofres, hornos y otros bloques
- **Sistema de saludos** - Los jugadores con alta reputación (HÉROE/ALIADO) reciben saludos personalizados de los aldeanos

## Mecánicas de Reputación

**Acciones Positivas:**
- Completar intercambio con aldeano: +5
- Entrar a casas de aldea (cuando la reputación es positiva): +5

**Acciones Negativas:**
- Abrir cofres/barriles de aldea: -10
- Robar items de contenedores: -15
- Romper camas de aldeanos: -15
- Destruir cultivos: -15
- Romper estructuras de aldea: -10
- Atacar aldeanos: -10
- Matar aldeanos: -50

## Rangos de Reputación

- **HÉROE** (500+): Las aldeas te aman, los golems nunca atacan, mensajes más amigables
- **ALIADO** (200-499): Amigo de confianza, los golems perdonan crímenes, muy amigable
- **AMIGABLE** (100-199): Bienvenido en la aldea, interacciones positivas
- **NEUTRAL** (0-99): Interacciones básicas, sin trato especial
- **NO BIENVENIDO** (-1 a -100): Los aldeanos desconfían de ti, penalizaciones por acciones
- **ENEMIGO** (-101 a -200): Mensajes hostiles, penalizaciones significativas
- **VILLANO** (-500 o menos): Las aldeas te odian, hostilidad máxima

## Comandos

**Comandos de Jugador:**
```
/pardon me              - Cuesta 100 lingotes de oro, reinicia hostilidad de golems
/diplomacy name <name>  - Asigna nombre personalizado a la aldea más cercana
/diplomacy info         - Muestra aldea actual y estado de reputación
```

**Comandos de Administrador:**
- `/diplomacy reputation get/set/add` - Gestionar valores de reputación del jugador
- `/diplomacy villages list` - Ver todas las aldeas descubiertas
- Comandos de administrador adicionales disponibles para gestión y pruebas de aldeas

## Instalación

1. Instala Minecraft Forge 47.4.10 para la versión 1.20.1
2. Coloca el archivo JAR del mod en tu carpeta `.minecraft/mods`
3. Inicia Minecraft con el perfil de Forge

## Consejos de Uso

- La reputación es específica por aldea - puedes mantener diferentes posiciones con diferentes aldeas
- Los golems de hierro requieren pago (100 lingotes de oro) via `/pardon me` para reiniciar hostilidad
- Considera establecer bases fuera de los límites de aldea si mantienes baja reputación
- Comerciar es el método más confiable para aumentar reputación
- Los jugadores con estado HÉROE o ALIADO reciben saludos personalizados de los aldeanos

## Limitaciones Conocidas

- El rastreo de bloques tiene limitaciones técnicas - romper bloques colocados por el jugador puede seguir afectando la reputación
- La detección de línea de visión de aldeanos puede ocasionalmente activarse a través de paredes

## Información Técnica

**Autor:** cesoti2006  
**Versión:** 1.0.0  
**Versión de Minecraft:** 1.20.1  
**Versión de Forge:** 47.4.10+

---

*Sistema de reputación con almacenamiento de datos persistente y seguimiento específico por aldea.*
