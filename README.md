# PlantIA

<img width="1200" height="655" alt="PlantIA" src="https://github.com/user-attachments/assets/cc8c9c52-1a06-4704-818b-60c9afd33cec" />

Aplicación web para identificar plantas a partir de fotografías usando **Google Gemini**. Guarda cada identificación en una base de datos local SQLite para consultarla después.

PlantIA crea un pequeño cuaderno digital de tus plantas. Identifica cada ejemplar mediante IA, almacena sus fotografías y reúne en un solo lugar la información necesaria para cuidarlo.

PlantIA está pensada para identificar y cuidar plantas de la forma más sencilla posible. La aplicación prioriza la facilidad de uso frente a la complejidad.

## Funcionalidades

- Subir fotos de plantas (JPEG, PNG, WebP)
- Identificación automática con Gemini Vision
- Guía de cultivo orientada a principiantes y usuarios avanzados
- Historial consultable con búsqueda por nombre o familia
- Cuaderno de observaciones por planta (anotaciones personales tras cada identificación)
- Calendario lunar en la ficha de cada planta
- API REST para integraciones futuras

## Qué información obtiene de cada imagen

Al subir una fotografía, PlantIA envía la imagen a **Gemini Vision** y guarda una ficha completa en la base de datos local. La información se muestra en la pantalla de resultado y en la ficha detallada de cada planta.

Cada planta queda registrada en tu colección personal junto con su fotografía, cuidados, notas y recomendaciones. Con el tiempo PlantIA se convierte en un cuaderno digital de todas tus plantas.

### Identificación básica

| Campo | Descripción |
|-------|-------------|
| Nombre común | Nombre popular de la planta en español |
| Nombre científico | Nomenclatura binomial (género y especie) |
| Familia | Familia botánica |
| Tipo | Categoría general (suculenta, árbol, hierba, arbusto, trepadora, etc.) |
| Descripción | Breve texto descriptivo de la planta |
| Confianza | Nivel estimado de certeza: `alto`, `medio` o `bajo` |

### Taxonomía y ayuda a la identificación

Cuando la identificación no es totalmente segura, PlantIA aporta contexto adicional:

| Campo | Descripción |
|-------|-------------|
| Taxonomía | Reino, orden, familia, género y especie (cuando es posible) |
| Rasgos observados | Lista de características visibles que justifican la identificación |
| Preguntas para mejorar | Preguntas al usuario para afinar el resultado (si la confianza es baja) |
| Candidatos | Hasta 5 plantas alternativas con nombre, razón y nivel de confianza |
| Imágenes de candidatos | Miniaturas automáticas obtenidas por nombre científico (Wikipedia) |

Los candidatos se muestran como **tarjetas con imagen** en la ficha completa de la planta.

### Guía para empezar (principiantes)

| Campo | Descripción |
|-------|-------------|
| Pasos iniciales | 5–8 acciones sencillas para empezar a cuidar la planta hoy |
| Errores comunes | 3–6 errores frecuentes y cómo evitarlos |

### Cuidados

| Campo | Descripción |
|-------|-------------|
| Riego | Frecuencia y cantidad recomendada |
| Luz | Necesidades de exposición (pleno sol, sombra parcial, etc.) |
| Temperatura | Rango de temperatura ideal |
| Humedad | Necesidades de humedad ambiental |
| Suelo | Tipo de sustrato recomendado |
| Fertilización | Cuándo y cómo fertilizar |
| Problemas comunes | Plagas, enfermedades o problemas frecuentes de cultivo |
| Plagas habituales | Insectos y plagas que suelen afectar a esta planta |

### Características y mantenimiento

| Campo | Descripción |
|-------|-------------|
| Dificultad | Nivel de cuidado: fácil, media o difícil |
| Tamaño adulto | Altura y anchura aproximadas en condiciones normales |
| Crecimiento | Ritmo y tipo de crecimiento |
| Floración | Si florece, color y época aproximada |
| Época de poda | Cuándo y cómo podar |
| Época de trasplante | Mejor momento y frecuencia orientativa |
| Señales de trasplante | Indicadores de que necesita cambiar de maceta |
| Maceta y sustrato | Recomendación de maceta (tamaño, material, drenaje) y sustrato |

### Seguridad para mascotas

| Campo | Descripción |
|-------|-------------|
| Toxicidad perros | Nivel de toxicidad y síntomas posibles |
| Toxicidad gatos | Nivel de toxicidad y síntomas posibles |

### Datos adicionales en la interfaz

| Elemento | Descripción |
|----------|-------------|
| Calendario lunar | Fase lunar actual, fecha e iluminación estimada (en la ficha) |
| Cuaderno de observaciones | Anotaciones personales (riego, trasplante, floración, etc.) — una línea por evento |
| Fotografía | Imagen original guardada en `~/.plantia/uploads/` |

### Limitaciones

- La identificación es **orientativa**: Gemini puede equivocarse, especialmente con plantas muy similares o fotos de baja calidad.
- Las plantas identificadas **antes de una actualización** pueden no tener todos los campos nuevos (aparecerán vacíos).
- Las miniaturas de candidatos dependen de la disponibilidad de imágenes en Wikipedia.

## Personalizar el prompt de la IA

El texto que se envía a Gemini para analizar cada imagen está definido en:

```
plantia/services/gemini_service.py
```

Busca la constante `PROMPT` al inicio del archivo. Ahí puedes modificar:

- Las instrucciones al modelo (rol, idioma, reglas de comportamiento)
- La estructura JSON que debe devolver
- Los campos que se solicitan (cuidados, características, candidatos, etc.)

Si añades o cambias campos en el prompt, también debes actualizar:

| Archivo | Para qué |
|---------|----------|
| `plantia/schemas/planta.py` | Esquemas Pydantic que validan la respuesta de Gemini |
| `plantia/models/planta.py` | Columnas de la base de datos SQLite |
| `plantia/database.py` | Migración automática de columnas nuevas (`_PLANTAS_COLUMN_MIGRATIONS`) |
| `plantia/services/planta_service.py` | Mapeo de la respuesta de Gemini al modelo de datos |
| `plantia/templates/detalle.html` | Visualización en la ficha de la planta |

El modelo de Gemini se configura en **Configuración** (interfaz web) o con la variable `GEMINI_MODEL` en `.env`. El valor por defecto es `gemini-2.5-flash`.

## Requisitos

- Python 3.10 o superior
- Conexión a internet (solo al identificar plantas)
- Clave de API de [Google AI Studio](https://aistudio.google.com/apikey)

## Instalación rápida

```bash
cd PlantIA
python3 run_app.py
```

Abre [http://localhost:8000](http://localhost:8000) en el navegador.

Después, entra en **Configuración** y pega tu API key (y elige el modelo).

> Alternativa: si prefieres usar `.env`, copia `.env.example` a `.env` y define `GEMINI_API_KEY`.

## Configuración

Variables en `.env`:

| Variable | Descripción | Por defecto |
|----------|-------------|-------------|
| `GEMINI_API_KEY` | Clave de Google AI Studio | *(obligatoria)* |
| `GEMINI_MODEL` | Modelo de Gemini | `gemini-2.5-flash` |
| `PLANTIA_PORT` | Puerto del servidor | `8000` |

### Configurar desde la interfaz

También puedes configurar Gemini desde la propia aplicación:

- Abre PlantIA y entra en **Configuración**
- Pega tu API key y elige un modelo (recomendado: **Gemini 2.5 Flash**)
- Pulsa **Guardar configuración**

La configuración se guarda localmente en `~/.plantia/config.json` (con permisos restringidos) y tiene prioridad sobre el `.env`.

> **Nota:** `gemini-2.0-flash` fue retirado por Google en junio de 2026. Si tenías ese modelo configurado, PlantIA usará `gemini-2.5-flash` automáticamente.

## Datos locales

Todo se guarda en `~/.plantia/`:

```
~/.plantia/
├── config.json     # API key y modelo de Gemini
├── plantia.db      # Base de datos SQLite
└── uploads/        # Fotografías subidas
```

### Copia de seguridad

Para hacer backup, copia la carpeta completa:

```bash
cp -r ~/.plantia ~/backup-plantia-$(date +%Y%m%d)
```

## API REST

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/plantas/identificar` | Sube imagen, identifica y guarda |
| `GET` | `/api/plantas?q=` | Listado con búsqueda |
| `GET` | `/api/plantas/{id}` | Detalle de una planta |
| `PATCH` | `/api/plantas/{id}` | Actualizar notas (`{"notas_usuario": "..."}`) |
| `DELETE` | `/api/plantas/{id}` | Eliminar planta |

Ejemplo con `curl`:

```bash
curl -X POST http://localhost:8000/api/plantas/identificar \
  -F "file=@mi-planta.jpg"
```

## Arranque manual (con venv activo)

```bash
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env
uvicorn main:app --reload --port 8000
```

## Aviso

La identificación por IA es **orientativa**. Gemini puede equivocarse, especialmente con plantas muy similares o fotos de baja calidad. El campo de confianza (`alto`, `medio`, `bajo`) indica la certeza estimada.

## Licencia

GNU GPL v3.0
