import yaml, json, sys
from fontTools.ttLib import TTFont
src, ttf, out = sys.argv[1:4]
d = yaml.safe_load(open(src))
cmap = TTFont(ttf).getBestCmap()
ES = {  # Spanish search terms for common habit icons
 'droplet':'agua beber hidratar','glass-water':'agua vaso beber','bottle-water':'agua botella',
 'book':'libro leer lectura','book-open':'leer libro lectura estudiar','book-open-reader':'leer lectura',
 'person-running':'correr running ejercicio','person-walking':'caminar andar paseo','person-biking':'bici bicicleta ciclismo',
 'person-swimming':'nadar natación piscina','dumbbell':'pesas gimnasio gym ejercicio','heart-pulse':'salud cardio corazón',
 'bed':'dormir sueño cama descanso','moon':'noche dormir luna','sun':'sol mañana día','mug-hot':'café té taza',
 'apple-whole':'manzana fruta comer sano','carrot':'verdura comer sano zanahoria','utensils':'comer comida cubiertos',
 'spa':'meditar relajar spa','om':'meditar meditación yoga','brain':'mente pensar cerebro','pen':'escribir diario',
 'pen-to-square':'escribir diario notas','graduation-cap':'estudiar estudio clase','language':'idioma idiomas',
 'music':'música tocar','guitar':'guitarra música tocar','gamepad':'jugar videojuegos juegos consola',
 'mobile-screen':'móvil teléfono pantalla','ban':'prohibido dejar no','smoking':'fumar tabaco cigarro',
 'wine-glass':'alcohol vino beber','beer-mug-empty':'cerveza alcohol','cookie':'dulces galleta azúcar',
 'pills':'pastillas medicina medicamento','tooth':'dientes cepillar dental','shower':'ducha bañarse',
 'broom':'limpiar limpieza barrer','house':'casa hogar','sack-dollar':'ahorrar dinero','piggy-bank':'ahorrar ahorro hucha',
 'wallet':'dinero gastos cartera','briefcase':'trabajo oficina','laptop-code':'programar código ordenador',
 'seedling':'planta crecer jardín','leaf':'naturaleza hoja','dog':'perro pasear mascota','cat':'gato mascota',
 'phone':'llamar teléfono','users':'familia amigos gente','hands-praying':'rezar orar oración','star':'estrella favorito',
 'heart':'amor corazón','face-smile':'feliz sonrisa ánimo','fire':'fuego racha','bolt':'energía rayo','clock':'hora reloj tiempo',
 'calendar-check':'calendario cita','check':'hecho listo','camera':'foto fotografía','paintbrush':'pintar dibujar arte',
 'bicycle':'bici bicicleta','weight-scale':'peso báscula','bowl-food':'comida cuenco','fish':'pescado pez',
}
icons = []
for name, v in d.items():
    if 'solid' not in (v.get('styles') or []): continue
    code = int(v['unicode'], 16)
    if code not in cmap: continue  # Pro-only glyphs are not in the free font
    terms = [t.lower() for t in (v.get('search') or {}).get('terms') or []]
    if name in ES: terms += ES[name].split()
    icons.append([name, v['unicode'], ' '.join(sorted(set(terms)))])
json.dump(icons, open(out, 'w'), ensure_ascii=False, separators=(',', ':'))
missing = [n for n in ES if n not in {i[0] for i in icons}]
print(len(icons), 'icons; ES names missing:', missing)
