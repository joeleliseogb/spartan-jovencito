package com.example.jarvis.cdpe

import java.text.Normalizer
import java.util.Locale

data class CdpeFranchise(
    val id: String,
    val name: String,
    val category: String,
    val investment: String,
    val margin: String,
    val roi: String,
    val description: String
)

data class CdpeManual(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val type: String
)

object CdpeKnowledgeManager {

    const val PORTAL_URL = "https://cdpe.accion.app/"
    const val DIRECTOR_GENERAL = "Joel"
    const val INSTITUCION = "CDPE — Centro de Desarrollo de Pequeñas Empresas & Acción Cooperativa"

    val franchises: List<CdpeFranchise> = listOf(
        CdpeFranchise(
            id = "kung-fu-pollo",
            name = "Kung Fu Chicken (KFC) — Comida China Express",
            category = "Gastronomía & Alimentos",
            investment = "Q380,000",
            margin = "62%",
            roi = "26%",
            description = "Cadena QSR de comida china rápida y salteados al wok con arquitectura de impacto visual inspirada en templos dojo orientales (aleros curvos estilo pagoda, faroles rojos, ventanales de cristal templado, carril Auto-Wok y marquesina LED). Modelo empresarial comp"
        ),
        CdpeFranchise(
            id = "don-pollo",
            name = "Don Pollo",
            category = "Gastronomía & Alimentos",
            investment = "Q250,000",
            margin = "50%",
            roi = "25%",
            description = "Franquicia especializada en pollo frito y broaster en canasta. Opera como negocio completo con obrador in-house de macerado artesanal en 12 especias, corte técnico de presas y elaboración de complementos tradicionales (ensalada coleslaw y panes calientes), cos"
        ),
        CdpeFranchise(
            id = "casa-empenos",
            name = "Prenda Fácil · Casa de Empeños & Joyería",
            category = "Gastronomía & Alimentos",
            investment = "Q700,000",
            margin = "N/A",
            roi = "22%",
            description = "Prenda Fácil · Casa de Empeños & Joyería. Modelo financiero completo con taller in-house de orfebrería para fundición, afinación y contrastado de oro y plata de segunda oportunidad, diseño y manufactura de piezas de joyería fina y laboratorio gemológico y de r"
        ),
        CdpeFranchise(
            id = "colegio-particular-mixto",
            name = "Colegio Particular Mixto",
            category = "Servicios Empresariales",
            investment = "Q800,000",
            margin = "45%",
            roi = "18%",
            description = "Colegio Particular Mixto — Institución educativa privada pre-primaria, primaria y secundaria. Estructurada como empresa completa con centro de diseño curricular y editorial escolar in-house para la diagramación e impresión de guías pedagógicas interactivas, cu"
        ),
        CdpeFranchise(
            id = "casa-software",
            name = "SimplyGest — Casa de Software",
            category = "Tecnología, Seguridad & Medios",
            investment = "Q500,000",
            margin = "70%",
            roi = "35%",
            description = "SimplyGest — Casa de Software y fábrica de soluciones SaaS empresariales. Opera como una Software Factory completa con células ágiles de ingeniería in-house para desarrollo de código fuente, diseño de interfaces UX/UI, pasarelas de pago y controladores de peri"
        ),
        CdpeFranchise(
            id = "transporte-logistica",
            name = "Empresa de Transporte y Logística",
            category = "Automotriz & Logística",
            investment = "Q1,000,000",
            margin = "30%",
            roi = "22%",
            description = "Empresa de Transporte y Logística de carga pesada y distribución regional. Negocio completo con taller in-house de mantenimiento de cabezales y semirremolques, fabricación y soldadura de plataformas y barandas de carga reforzadas, finanzas de costos por flete "
        ),
        CdpeFranchise(
            id = "farmacia",
            name = "Farmacia Comunitaria",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q350,000",
            margin = "35%",
            roi = "18%",
            description = "Farmacia comunitaria de alta confianza vecinal. Modelo de negocio completo que incorpora su propio recetario galénico in-house para la preparación de fórmulas magistrales, preparados oficinales y línea dermocosmética propia bajo Buenas Prácticas Farmacéuticas,"
        ),
        CdpeFranchise(
            id = "ferreteria",
            name = "MaxTools · Ferretería de Proximidad",
            category = "Servicios Empresariales",
            investment = "Q400,000",
            margin = "30%",
            roi = "15%",
            description = "MaxTools · Ferretería de Proximidad y suministros industriales. Modelo completo equipado con taller in-house de dimensionado y corte a medida de perfiles metálicos, tuberías y varillas, estación computarizada de formulación y mezclado de pinturas (colorimetría"
        ),
        CdpeFranchise(
            id = "franquicia-comida",
            name = "Franquicia de Comida Rápida Gourmet",
            category = "Gastronomía & Alimentos",
            investment = "Q450,000",
            margin = "55%",
            roi = "23%",
            description = "Franquicia de comida rápida gourmet y hamburguesas de autor. Modelo completo con taller in-house de molienda y formulación de blends de carne vacuna seleccionada, horneado propio de pan brioche y emulsión de aderezos de autor, finanzas de unit economics, ensam"
        ),
        CdpeFranchise(
            id = "gasolineras",
            name = "Gasolineras",
            category = "Gastronomía & Alimentos",
            investment = "Q2,500,000",
            margin = "15%",
            roi = "14%",
            description = "Estación de servicio de combustibles y tienda de conveniencia de alta rentabilidad. Modelo empresarial completo que integra centro propio de lubricación express y afinación de fluidos, junto a una panadería y cafetería artesanal horneada diariamente in-house e"
        ),
        CdpeFranchise(
            id = "libreria",
            name = "Librería",
            category = "Servicios Empresariales",
            investment = "Q350,000",
            margin = "40%",
            roi = "20%",
            description = "Librería & Papelería Técnica. Modelo integral con taller in-house de imprenta digital express, encuadernación térmica y de espiral, corte de guillotina y personalización de sellos y papelería corporativa, márgenes de servicios gráficos vs reventa de libros, de"
        ),
        CdpeFranchise(
            id = "hotel-boutique",
            name = "Hotel Boutique",
            category = "Automotriz & Logística",
            investment = "Q1,500,000",
            margin = "50%",
            roi = "12%",
            description = "Hotel Boutique & Hospedaje con encanto y alta hospitalidad. Modelo completo que cuenta con cocina de autor y panadería artesanal in-house para desayunos gourmet, obrador de lavandería industrial y taller in-house de formulación de amenidades botánicas biodegra"
        ),
        CdpeFranchise(
            id = "joyeria",
            name = "Joyería Fina",
            category = "Tecnología, Seguridad & Medios",
            investment = "Q750,000",
            margin = "50%",
            roi = "18%",
            description = "Joyería fina, diamantes y alta orfebrería. Modelo empresarial completo con taller in-house de microfundición a la cera perdida, engaste de piedras preciosas, modelado en cera 3D y grabado láser personalizado de joyas de oro de 10k, 14k, 18k y plata 925, costeo"
        ),
        CdpeFranchise(
            id = "tienda-electrodomesticos",
            name = "Tienda de Electrodomésticos",
            category = "Comercio & Retail",
            investment = "Q450,000",
            margin = "30%",
            roi = "19%",
            description = "Tienda de Electrodomésticos, línea blanca y confort para el hogar. Negocio completo con taller técnico in-house de revisión, pre-instalación y armado de electrodomésticos, servicio técnico postventa de garantía oficial y banco de pruebas eléctricas, finanzas d"
        ),
        CdpeFranchise(
            id = "pizzeria",
            name = "Pizzería Artesanal",
            category = "Gastronomía & Alimentos",
            investment = "Q200,000",
            margin = "65%",
            roi = "25%",
            description = "Pizzería artesanal y trattoria de horno de piedra. Modelo empresarial completo con obrador in-house para fermentación lenta y maduración de masa madre por 48 horas, elaboración diaria de salsa pomodoro con tomate natural y formulación de mezclas de quesos fund"
        ),
        CdpeFranchise(
            id = "salon-belleza",
            name = "Salón de Belleza",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q280,000",
            margin = "60%",
            roi = "25%",
            description = "Salón de Belleza & Centro de Estilismo profesional. Negocio completo con laboratorio técnico in-house de colorimetría para la formulación personalizada de mascarillas capilares reconstructivas, cócteles de keratina pura y aceites orgánicos selladores de brillo"
        ),
        CdpeFranchise(
            id = "taller-chapa",
            name = "Taller de Chapa y Pintura",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q380,000",
            margin = "45%",
            roi = "20%",
            description = "Taller de Chapa y Pintura automotriz. Centro de colisión integral equipado con cabina presurizada de pintura al horno, banco de enderezado de chasis (l-car) y laboratorio in-house de colorimetría computarizada para formulación exacta de pinturas bicapa y trica"
        ),
        CdpeFranchise(
            id = "taller-mecanica",
            name = "Taller de Mecánica",
            category = "Automotriz & Logística",
            investment = "Q450,000",
            margin = "40%",
            roi = "18%",
            description = "Taller de Mecánica General y afinación automotriz mayor. Modelo completo con área in-house de prensa hidráulica para cambio de cojinetes y bujes, estación de diagnóstico computarizado y banco de pruebas de compresión de motores, control financiero de tiempos d"
        ),
        CdpeFranchise(
            id = "tienda-consumo",
            name = "Tienda de Consumo Diario",
            category = "Comercio & Retail",
            investment = "Q250,000",
            margin = "22%",
            roi = "20%",
            description = "Tienda de Consumo Diario y abarrotes de proximidad. Modelo completo que integra panadería in-house de mostrador con horneado caliente diario, área de empaque y fraccionamiento de granos básicos y especias bajo marca propia de la cooperativa, gestión financiera"
        ),
        CdpeFranchise(
            id = "tienda-electronicos",
            name = "Tienda de Electrónicos",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q400,000",
            margin = "20%",
            roi = "17%",
            description = "Tienda de Electrónicos, audio profesional y accesorios de consumo. Modelo completo con laboratorio in-house de diagnóstico y banco de ensamblaje de sistemas de audio residencial y comercial, cableados especiales a medida y calibración de equipos multimedia, ro"
        ),
        CdpeFranchise(
            id = "tienda-informatica",
            name = "Tienda de Informática",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q300,000",
            margin = "25%",
            roi = "19%",
            description = "Tienda de Informática, cómputo y suministros tecnológicos. Negocio completo con laboratorio in-house de microelectrónica, reballing, reparación de placas base y banco de ensamblaje de computadoras personalizadas a medida para gaming, arquitectura y render 3D, "
        ),
        CdpeFranchise(
            id = "tienda-moda",
            name = "Tienda de Moda y Accesorios",
            category = "Automotriz & Logística",
            investment = "Q250,000",
            margin = "55%",
            roi = "22%",
            description = "Casa de moda y manufactura integral de marca propia. A diferencia de tiendas revendedoras convencionales, este modelo produce in-house todo su catálogo insignia y productos estrella: confección textil de colecciones cápsula (patronaje industrial y corte técnic"
        ),
        CdpeFranchise(
            id = "tienda-deportiva",
            name = "Tienda Deportiva",
            category = "Comercio & Retail",
            investment = "Q400,000",
            margin = "45%",
            roi = "22%",
            description = "Tienda Deportiva, calzado técnico y ropa atlética. Modelo integral con taller in-house de personalización deportiva equipado con prensas de sublimación textil, estampado térmico de nombres y números en camisetas de fútbol y encordado de raquetas, rentabilidad "
        ),
        CdpeFranchise(
            id = "tienda-ropa-tipica",
            name = "Tienda de Ropa Típica",
            category = "Comercio & Retail",
            investment = "Q280,000",
            margin = "55%",
            roi = "24%",
            description = "Tienda de Ropa Típica estilizada y artesanías textiles finas. Negocio completo con taller in-house de tejido en telar de cintura y pedal, corte y adaptación de textiles mayas a prendas modernas y confección de accesorios de cuero y telar, comercio justo y cost"
        ),
        CdpeFranchise(
            id = "tienda-ropa",
            name = "Tienda de Ropa Casual",
            category = "Comercio & Retail",
            investment = "Q220,000",
            margin = "50%",
            roi = "25%",
            description = "Tienda de Ropa Casual y básicos urbanos contemporáneos. Modelo de negocio completo con taller in-house de corte, patronaje industrial y confección textil de prendas de marca privada, control de márgenes retail y rotación de stock, asesoras de imagen en piso de"
        ),
        CdpeFranchise(
            id = "tienda-mayorista",
            name = "Tienda Mayorista",
            category = "Gastronomía & Alimentos",
            investment = "Q1,200,000",
            margin = "15%",
            roi = "18%",
            description = "Tienda Mayorista y bodega de distribución comercial. Negocio completo con planta in-house de empaque, sellado al vacío y fraccionamiento de alimentos a granel y granos básicos bajo marcas privadas de alta rentabilidad, finanzas de volumen y rotación rápida de "
        ),
        CdpeFranchise(
            id = "transporte-ultima-milla",
            name = "Transporte de Última Milla",
            category = "Automotriz & Logística",
            investment = "Q500,000",
            margin = "25%",
            roi = "20%",
            description = "Transporte de Última Milla & Delivery corporativo para comercio electrónico. Modelo autosuficiente con hub logístico urbano y taller in-house de ensamblaje, puesta a punto y mantenimiento preventivo de motocicletas y furgonetas eléctricas de reparto, métricas "
        ),
        CdpeFranchise(
            id = "materiales-electricos",
            name = "Venta de Materiales Eléctricos",
            category = "Servicios Empresariales",
            investment = "Q600,000",
            margin = "28%",
            roi = "16%",
            description = "Venta de Materiales Eléctricos e Iluminación técnica. Modelo de negocio completo con taller in-house de ensamblaje de tableros eléctricos de distribución, armado de acometidas normalizadas y corte/bobinado de conductores a medida, finanzas de crédito para inst"
        ),
        CdpeFranchise(
            id = "muebles",
            name = "Venta de Muebles",
            category = "Comercio & Retail",
            investment = "Q550,000",
            margin = "40%",
            roi = "14%",
            description = "Venta y Fabricación de Muebles de diseño residencial y corporativo. Modelo empresarial completo con taller in-house de ebanistería y carpintería fina en maderas tratadas (cedro, caoba y tableros melamínicos de alta densidad) y obrador de tapicería en cueros y "
        ),
        CdpeFranchise(
            id = "telefonos",
            name = "Franquicia de Telefonía Celular",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q150,000",
            margin = "45%",
            roi = "30%",
            description = "Franquicia de Telefonía Celular, smartphones y accesorios. Modelo completo con laboratorio in-house de micro-soldadura, laminado y cambio de pantallas táctiles al vacío y taller de grabado láser para personalización inmediata de fundas y cristales templados, m"
        ),
        CdpeFranchise(
            id = "tienda-seguridad-informatica",
            name = "Tienda de Seguridad Informática",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q350,000",
            margin = "60%",
            roi = "28%",
            description = "Tienda de Seguridad Informática, redes empresariales y conectividad. Modelo completo con laboratorio in-house de pre-configuración de firewalls perimetrales (UTM), confección de pach-cords de fibra óptica/cobre certificados y pruebas de penetración en red simu"
        ),
        CdpeFranchise(
            id = "fabricas",
            name = "Fábricas",
            category = "Automotriz & Logística",
            investment = "Q2,000,000",
            margin = "25%",
            roi = "20%",
            description = "Fábricas Modulares CDPE — Complejo de manufactura ligera, inyección plástica, termoformado y empaques. Planta industrial modular con taller in-house de maquinado de moldes, matricería y líneas automatizadas de extrusión y ensamblaje en serie, análisis de costo"
        ),
        CdpeFranchise(
            id = "securytec-cctv",
            name = "SecuryTec CCTV",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q 0.00",
            margin = "%",
            roi = "%",
            description = "SecuryTec CCTV — Videovigilancia profesional y seguridad electrónica inteligente. Negocio completo con laboratorio in-house de armado y configuración de servidores NVR, bancos de prueba de cámaras térmicas e IP y armado de racks de telecomunicaciones para proy"
        ),
        CdpeFranchise(
            id = "guardian-central",
            name = "Guardian Central",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q 0.00",
            margin = "%",
            roi = "%",
            description = "Guardian Central — Central receptora de alarmas, monitoreo de pánico y respuesta táctica 24/7. Modelo autosuficiente con laboratorio in-house de programación de paneles de alarma inalámbricos, pruebas de radioenlace y ensamble de kits de intrusión y cercos elé"
        ),
        CdpeFranchise(
            id = "titan-del-mar",
            name = "El Titán del Mar",
            category = "Gastronomía & Alimentos",
            investment = "Q1,200,000",
            margin = "45%",
            roi = "22%",
            description = "El Titán del Mar — Marisquería y restaurante de mariscos para familias y grupos. Modelo completo con área de producción in-house de caldos concentrados de mariscos, empanizados marinos y conservas de mariscos en escabeche, costeo de platos de alta porción, ser"
        ),
        CdpeFranchise(
            id = "pollo-ya",
            name = "Pollo Ya",
            category = "Gastronomía & Alimentos",
            investment = "Q250,000",
            margin = "45%",
            roi = "30%",
            description = "Concepto QSR de pollo crujiente express para zonas de alto tráfico peatonal. Modelo completo con estación in-house de dosificación de marinados y rebozados crocantes diarios, costeo financiero de combos populares, entrenamiento en despacho ultrarrápido, promoc"
        ),
        CdpeFranchise(
            id = "pinpollo",
            name = "Pinpollo",
            category = "Gastronomía & Alimentos",
            investment = "Q250,000",
            margin = "50%",
            roi = "25%",
            description = "Restaurante familiar de pollo rostizado y dorado a la leña, combos campestres y cocina criolla tradicional con concepto arquitectónico rústico-industrial. Modelo completo con cocina-taller in-house para marinado en frío de 18 especias, panificación y tortillas"
        ),
        CdpeFranchise(
            id = "restaurante-supremo",
            name = "Don Rodrigo — Salón Familiar y Buffet (Especializado en Asados)",
            category = "Gastronomía & Alimentos",
            investment = "Q600,000",
            margin = "50%",
            roi = "20%",
            description = "Don Rodrigo — Salón familiar, asados y buffet de cortes a la parrilla. Modelo completo con cocina central in-house para la panificación caliente diaria, estación de ahumado y preparación de ensaladas y postres de autor, control de mermas y yield de buffet, bri"
        ),
        CdpeFranchise(
            id = "asados-gourmet",
            name = "Brasa & Carbón — Asados Gourmet Premium (Corporativo, Catering & Eventos)",
            category = "Gastronomía & Alimentos",
            investment = "Q500,000",
            margin = "60%",
            roi = "24%",
            description = "Brasa & Carbón — Asados Gourmet Premium para eventos, catering y servicio en salón. Negocio integral equipado con obrador propio de charcutería artesanal in-house (chorizos parrilleros, salchichas de autor, sales saborizadas y rubs secos) y cámara de maduració"
        ),
        CdpeFranchise(
            id = "pupusas-salvadorenas",
            name = "El Pulgarcito - Pupusas & Antojitos",
            category = "Gastronomía & Alimentos",
            investment = "Q180,000",
            margin = "68%",
            roi = "28%",
            description = "El Pulgarcito — Pupusería y antojitos típicos centroamericanos. Modelo integral con obrador in-house de molienda y preparación diaria de masa de arroz y maíz, cocción artesanal de chicharrón prensado, curtido fermentado y salsa de tomate casera, costeo por uni"
        ),
        CdpeFranchise(
            id = "eco-carwash",
            name = "Eco CarWash",
            category = "Automotriz & Logística",
            investment = "Q220,000",
            margin = "62%",
            roi = "26%",
            description = "Eco CarWash — Centro de detallado automotriz (detailing) y lavado sustentable. Modelo completo con área in-house de formulación y dosificación de shampoos con pH neutro, ceras de carnauba y polímeros nanotecnológicos biodegradables de consumo hídrico menor a 5"
        ),
        CdpeFranchise(
            id = "clinica-dental",
            name = "Clínica Dental CDPE",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q450,000",
            margin = "50%",
            roi = "20%",
            description = "Clínica Dental CDPE — Centro odontológico integral y estética oral. Opera como unidad autosuficiente con su propio laboratorio dental digital in-house (sistemas CAD/CAM para escaneo intraoral, fresado de coronas de zirconio y termoformado de alineadores invisi"
        ),
        CdpeFranchise(
            id = "heladeria-cremosa",
            name = "Heladería Cremosa",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q250,000",
            margin = "65%",
            roi = "25%",
            description = "Heladería Cremosa — Gelatería artesanal italiana y paletería natural. Modelo completo con laboratorio in-house de pasteurización y mantecado, balance de bases lácteas y elaboración de sorbetes de fruta natural 100% pura y paletas rellenas, costeo por gramo de "
        ),
        CdpeFranchise(
            id = "diagnostica-cdpe",
            name = "Diagnóstica CDPE - Laboratorio & Diagnóstico",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q750,000",
            margin = "48%",
            roi = "22%",
            description = "Diagnóstica CDPE — Laboratorio clínico, análisis bioquímicos y diagnóstico por imágenes. Empresa integral con área técnica in-house de preparación de reactivos de control interno, diluciones calibradas y medios de cultivo microbiológicos, finanzas de costos po"
        ),
        CdpeFranchise(
            id = "salud-integral",
            name = "Salud Integral - Especialidades Médicas",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q900,000",
            margin = "40%",
            roi = "20%",
            description = "Salud Integral — Policlínica de especialidades médicas ambulatorias y medicina preventiva. Modelo autosuficiente con central in-house de esterilización y armado de paquetes quirúrgicos estériles, laboratorio de soluciones de rehidratación y curaciones para peq"
        ),
        CdpeFranchise(
            id = "powerfit-gym",
            name = "PowerFit Gym & Wellness",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q650,000",
            margin = "55%",
            roi = "25%",
            description = "PowerFit Gym & Wellness — Centro de entrenamiento funcional, fuerza y acondicionamiento integral. Modelo completo que integra su propia barra funcional in-house de nutrición deportiva (elaboración diaria de batidos proteicos naturales, bebidas isotónicas formu"
        ),
        CdpeFranchise(
            id = "aura-spa",
            name = "Aura Spa & Relajación",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q350,000",
            margin = "60%",
            roi = "24%",
            description = "Aura Spa & Relajación — Centro holístico de bienestar y terapias de hidro-relajación. Modelo empresarial completo con obrador in-house de botánica cosmética para la formulación de aceites esenciales de aromaterapia, exfoliantes de sales volcánicas orgánicas y "
        ),
        CdpeFranchise(
            id = "petlife-veterinaria",
            name = "PetLife Veterinaria & Grooming",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q280,000",
            margin = "50%",
            roi = "22%",
            description = "PetLife Veterinaria & Grooming — Hospital veterinario, farmacia zootécnica y centro estético canino. Modelo integral que incorpora cocina y laboratorio in-house de formulación de dietas naturales balanceadas (BARF horneado y premios nutricionales saludables de"
        ),
        CdpeFranchise(
            id = "momentos-al-horno",
            name = "Momentos al Horno",
            category = "Servicios Empresariales",
            investment = "Q250,000",
            margin = "65%",
            roi = "26%",
            description = "Pizzería artesanal de alta rotación enfocada en delivery y consumo en sala. Negocio completo con taller in-house de pre-fermentos, estirado artesanal y producción propia de pastas frescas y panes de ajo, control de costos unitarios de ingredientes, personal de"
        ),
        CdpeFranchise(
            id = "que-tacones",
            name = "¡Qué Tacones! — Taquería & Comida Mexicana",
            category = "Gastronomía & Alimentos",
            investment = "Q220,000",
            margin = "64%",
            roi = "27%",
            description = "¡Qué Tacones! — Taquería mexicana de trompo al pastor y comal. Modelo autosuficiente con tortillería in-house con comal de maíz fresco nixtamalizado, preparación de adobos al pastor con achiote natural y salsas tatemadas en molcajete, finanzas de costo por tac"
        ),
        CdpeFranchise(
            id = "manhattan-hotel-restaurante",
            name = "Manhattan Hotel y Restaurante",
            category = "Gastronomía & Alimentos",
            investment = "Q1,500,000",
            margin = "52%",
            roi = "22%",
            description = "Manhattan Hotel y Restaurante — Hospedaje ejecutivo y restaurante gourmet. Empresa completa con cocina central in-house para servicio de restaurante internacional y banquetes, taller propio de lavandería y planchado industrial y elaboración in-house de panader"
        ),
        CdpeFranchise(
            id = "tecnologico-victor-jowell",
            name = "Tecnológico Victor Jowell — Instituto de I+D",
            category = "Automotriz & Logística",
            investment = "Q450,000",
            margin = "65%",
            roi = "28%",
            description = "Tecnológico Victor Jowell — Instituto de I+D, automatización y prototipado tecnológico. Centro integral con taller in-house de robótica, corte y grabado láser CNC, banco de impresión 3D industrial e instrumentación electrónica para desarrollo de prototipos ind"
        ),
        CdpeFranchise(
            id = "gdata-lab",
            name = "GData Lab — Laboratorio de Seguridad Informática",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q380,000",
            margin = "68%",
            roi = "30%",
            description = "GData Lab — Laboratorio de Ciberseguridad, respuesta a incidentes y cómputo forense. Empresa integral con laboratorio in-house de Ethical Hacking, auditorías de código fuente, simulaciones de ataques (Red Teaming) y banco técnico de recuperación de datos en di"
        ),
        CdpeFranchise(
            id = "jowell-tech-hidro-power",
            name = "Jowell Tech (Hidro Power) — Franquicia de Energía",
            category = "Tecnología, Seguridad & Medios",
            investment = "Q650,000",
            margin = "48%",
            roi = "24%",
            description = "Jowell Tech (Hidro Power) — Sistemas de energía solar, bombeo solar y eficiencia hídrica. Modelo integral con taller in-house de ensamblaje de módulos de control fotovoltaico, estructuras de anclaje para paneles solares y bancos de baterías con inversores híbr"
        ),
        CdpeFranchise(
            id = "publimundo",
            name = "PubliMundo — Telecomunicaciones FTTH, FTTx, Play TV y Radio",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q600,000",
            margin = "58%",
            roi = "26%",
            description = "PubliMundo — Operador de telecomunicaciones de fibra óptica FTTH, televisión digital y conectividad. Empresa integral con laboratorio in-house de conectorización y empalme de fibra óptica por fusión, configuración de routers OLT/ONT y producción propia de cana"
        ),
        CdpeFranchise(
            id = "romantica-radio",
            name = "Romántica Radio — Medios y Radiodifusión",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q350,000",
            margin = "62%",
            roi = "25%",
            description = "Romántica Radio — Emisora multimedia y cadena de radiodifusión en streaming y FM. Empresa integral con estudio in-house de transmisión en vivo, laboratorio de producción y masterización de spots publicitarios, jingles y programas temáticos de autor, comerciali"
        ),
        CdpeFranchise(
            id = "jowell-agro",
            name = "Jowell Agro — Invernaderos y Agricultura de Precisión",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q550,000",
            margin = "52%",
            roi = "24%",
            description = "Jowell Agro — Invernaderos tecnificados y agricultura de precisión. Explotación agroindustrial autosuficiente con semillero in-house de propagación de plántulas de alta genética, laboratorio de formulación de soluciones nutritivas hidropónicas y bio-fertilizan"
        ),
        CdpeFranchise(
            id = "biocampo",
            name = "BioCampo — Producción Agrícola Estandarizada",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q420,000",
            margin = "50%",
            roi = "23%",
            description = "BioCampo — Producción agrícola estandarizada bajo invernadero y bio-insumos. Modelo completo con laboratorio in-house de cultivo de bio-fertilizantes orgánicos y microorganismos benéficos del suelo, módulo de deshidratado solar de hierbas aromáticas y empaque "
        ),
        CdpeFranchise(
            id = "agrovida",
            name = "AgroVida — Agricultura Tecnificada e Invernaderos",
            category = "Automotriz & Logística",
            investment = "Q480,000",
            margin = "54%",
            roi = "25%",
            description = "AgroVida — Agricultura protegida, invernaderos modulares y macro-túneles. Modelo agrícola completo con semillero in-house automatizado de plántulas hortícolas, banco de preparación de nutrientes solubles y estación de selección, lavado y empaque en atmósfera m"
        ),
        CdpeFranchise(
            id = "innovapack-planta-maestra",
            name = "InnovaPack / Planta Maestra — Fábrica de Alimentos",
            category = "Gastronomía & Alimentos",
            investment = "Q950,000",
            margin = "46%",
            roi = "24%",
            description = "InnovaPack / Planta Maestra — Complejo agroalimentario de envasado, conservas y empaque industrial. Planta procesadora con líneas in-house de termoformado de bandejas, dosificación al vacío, deshidratado de vegetales y envasado de conservas bajo normas de inoc"
        ),
        CdpeFranchise(
            id = "tienda-decoraciones",
            name = "DecoHogar & Eventos Studio — Tienda de Decoraciones",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q320,000",
            margin = "58%",
            roi = "26%",
            description = "DecoHogar & Eventos Studio — Concept store de interiorismo, decoración y ambientación. Modelo completo con taller in-house de confección textil de cortinas a medida, cojinería de diseño, elaboración de arreglos florales preservados y corte CNC de rotulación de"
        ),
        CdpeFranchise(
            id = "parque-diversiones",
            name = "Mundo Aventura Park — Parque de Diversiones y Entretenimiento",
            category = "Comercio & Retail",
            investment = "Q1,400,000",
            margin = "62%",
            roi = "25%",
            description = "Mundo Aventura Park — Parque de diversiones, atracciones mecánicas y entretenimiento familiar. Negocio completo con taller electromecánico in-house para inspección, soldadura y certificación diaria de juegos mecánicos y simuladores, cocina central para dulcerí"
        ),
        CdpeFranchise(
            id = "centro-recreativo",
            name = "Oasis Club & Resort — Centro Recreativo y Turístico Familiar",
            category = "Gastronomía & Alimentos",
            investment = "Q1,150,000",
            margin = "56%",
            roi = "24%",
            description = "Oasis Club & Resort — Centro recreativo, balneario y parque turístico familiar. Modelo autosuficiente con taller in-house de mantenimiento electromecánico y tratamiento físico-químico de piscinas, cocina central para kioscos y restaurante de snacks campestres "
        ),
        CdpeFranchise(
            id = "senor-pollo",
            name = "Señor Pollo — Restaurante Familiar de Pollo Tradicional y a la Parrilla",
            category = "Gastronomía & Alimentos",
            investment = "Q850,000",
            margin = "58%",
            roi = "26%",
            description = "Restaurante de pollo asado al carbón, leña y parrilla campestre. Unidad de negocio completa con taller in-house de marinado campestre en salmueras naturales, ahumado lento y producción propia de chimichurris y salsas rústicas, costeo de materias primas por kil"
        ),
        CdpeFranchise(
            id = "del-riomonta",
            name = "Auto Hotel Del Río (Rimonta) — Franquicia de Auto Hotel & Suites de Privacidad",
            category = "Automotriz & Logística",
            investment = "Q1,650,000",
            margin = "68%",
            roi = "28%",
            description = "Auto Hotel Del Río (Rimonta) — Suites de privacidad, descanso y hospedaje express. Modelo de alta rentabilidad con taller in-house de lavandería industrial de alto ciclo térmico (desinfección hospitalaria de sábanas y toallas), cocina express de snacks y servi"
        ),
        CdpeFranchise(
            id = "autofix",
            name = "AutoFix · Centro de Servicio Automotriz",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q450,000",
            margin = "40%",
            roi = "18%",
            description = "AutoFix · Centro de Servicio Automotriz y electromecánica preventiva. Taller completo con área técnica in-house de rectificación de discos y tambores de frenos, banco de ultrasonido para limpieza y calibración de inyectores y reacondicionamiento de alternadore"
        ),
        CdpeFranchise(
            id = "cevicheria",
            name = "Cevichería el Puerto",
            category = "Gastronomía & Alimentos",
            investment = "Q180,000",
            margin = "50%",
            roi = "28%",
            description = "Cevichería el Puerto — Marisquería tradicional y cocina de costa. Estructurada como empresa completa con taller frío in-house para la preparación diaria de leches de tigre base, concentrados marinos, pastas de ají y tostadas de maíz horneadas en casa, cadena d"
        ),
        CdpeFranchise(
            id = "cevicheria-nombre-definicion",
            name = "Marea Brava · Cevichería",
            category = "Gastronomía & Alimentos",
            investment = "Q180,000",
            margin = "50%",
            roi = "28%",
            description = "Marea Brava · Cevichería — Cocina marina de autor y coctelería de mariscos. Modelo completo con laboratorio in-house de salsas negras fermentadas, clamatos artesanales y marinados cítricos para ceviches y aguachiles, control de compras en puertos y trazabilida"
        ),
        CdpeFranchise(
            id = "felinas-express",
            name = "Felinas Express · Transporte Extraurbano",
            category = "Automotriz & Logística",
            investment = "Q1,000,000",
            margin = "30%",
            roi = "22%",
            description = "Felinas Express · Transporte Extraurbano y de pasajeros interdepartamental. Empresa completa con taller propio in-house de mantenimiento mecánico preventivo de flota, calibración de frenos de aire de seguridad y taller de tapicería ergonómica de asientos, gest"
        ),
        CdpeFranchise(
            id = "hidro-power",
            name = "Hidro Power · Bombas & Sistemas Hidráulicos",
            category = "Servicios Empresariales",
            investment = "Q600,000",
            margin = "28%",
            roi = "16%",
            description = "Hidro Power · Bombas & Sistemas Hidráulicos. Empresa de ingeniería hidráulica y bombeo con taller in-house de maquinado en torno, banco de pruebas de caudal/presión y ensamblaje a medida de equipos hidroneumáticos y bombas sumergibles, presupuestación técnica "
        ),
        CdpeFranchise(
            id = "laboratorio-inox",
            name = "Laboratorio Inox de Proyectos",
            category = "Salud, Bienestar & Clínicas",
            investment = "Q550,000",
            margin = "40%",
            roi = "14%",
            description = "Laboratorio Inox de Proyectos — Planta de manufactura y transformación en acero inoxidable sanitario (grados 304 y 316). Cuenta con taller in-house de corte láser CNC, plegado de chapa, soldadura TIG/MIG y pulido sanitario para cocinas industriales, barandales"
        ),
        CdpeFranchise(
            id = "montana-azul",
            name = "Montaña Azul · Café & Bistro",
            category = "Gastronomía & Alimentos",
            investment = "Q450,000",
            margin = "55%",
            roi = "23%",
            description = "Montaña Azul · Café & Bistro — Cafetería de especialidad y cocina ligera. Modelo completo con tostaduría in-house de micro-lotes de café de altura con curvas de tueste personalizadas y obrador de panadería y repostería fina horneada al día, costeo por taza y r"
        ),
        CdpeFranchise(
            id = "pub-licores",
            name = "Pub & Boutique de Licores",
            category = "Gastronomía & Alimentos",
            investment = "Q450,000",
            margin = "70%",
            roi = "24%",
            description = "Pub & Boutique de Licores — Taberna artesanal y tienda de destilados. Modelo completo con micro-planta in-house de maceración botánica de licores, jarabes artesanales para coctelería y envasado de cócteles de autor listos para servir, márgenes de barra y venta"
        ),
        CdpeFranchise(
            id = "publi-mundo",
            name = "Publi Mundo · Agencia & Medios Digitales",
            category = "Servicios Empresariales",
            investment = "Q500,000",
            margin = "70%",
            roi = "35%",
            description = "Publi Mundo · Agencia & Medios Digitales — Agencia de publicidad, marketing digital y producción de medios. Empresa completa con estudio in-house de grabación audiovisual 4K y podcast, cabina insonorizada de locución y taller de artes gráficas de gran formato "
        ),
        CdpeFranchise(
            id = "sabor-del-campo",
            name = "Sabor del Campo",
            category = "Gastronomía & Alimentos",
            investment = "Q250,000",
            margin = "45%",
            roi = "30%",
            description = "Restaurante de cocina criolla tradicional, asados de granja y pollo campestre. Modelo completo con cocina de producción in-house para la elaboración artesanal de tortillas de comal recién palmeadas, guisados típicos y adobos autóctonos molcajeteados, análisis "
        ),
        CdpeFranchise(
            id = "sapore-mio",
            name = "Sapore Mio · Trattoria & Pizza",
            category = "Gastronomía & Alimentos",
            investment = "Q200,000",
            margin = "65%",
            roi = "25%",
            description = "Trattoria italiana de pastas tradicionales, focaccias y pizzas a la piedra. Modelo autosuficiente con laboratorio culinario in-house para la elaboración de pasta fresca al huevo (tagliatelle, ravioli), salsas base reducidas a fuego lento y masa fermentada de p"
        ),
        CdpeFranchise(
            id = "que-tacones",
            name = "Qué Tacones — Franquicia de Calzado & Moda Femenina CDPE",
            category = "Comercio & Retail",
            investment = "Q180,000",
            margin = "55%",
            roi = "26%",
            description = "Boutique especializada en calzado femenino formal, casual y de temporada con exhibición modular estandarizada, rotación rápida de stock y fidelización de clientela."
        )
    )

    val manuals: List<CdpeManual> = listOf(
        CdpeManual(
            id = "dueno-director",
            title = "De Dueño a Director de Empresa",
            subtitle = "Transición del Hombre Orquesta a Director de Orquesta",
            category = "Alta Dirección & Dueñez",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-1",
            title = "Estrategias Low Cost para Emprendedores",
            subtitle = "Estrategias _Low Cost_ para Emprendedores. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-2",
            title = "Cómo gestionar el crecimiento de tu empresa",
            subtitle = "Cómo gestionar el crecimiento de tu empresa. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-3",
            title = "Cooperación empresarial",
            subtitle = "Cooperación empresarial. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-4",
            title = "Técnicas de Negociación para Emprendedores",
            subtitle = "Técnicas de Negociación para Emprendedores. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-5",
            title = "La Innovación como Factor Clave de Competitividad",
            subtitle = "La Innovación como Factor Clave de Competitividad. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-6",
            title = "Gestión Administrativa y Económica de Proyectos de I+D+i",
            subtitle = "Gestión Administrativa y Económica de Proyectos de I+D+i. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-7",
            title = "Metodología Lean Startup",
            subtitle = "Metodología Lean Startup. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-8",
            title = "Metodologia de la innovacion",
            subtitle = "Metodologia de la innovacion. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-9",
            title = "Cómo diseñar tu modelo de negocio. Modelo CANVAS",
            subtitle = "Cómo diseñar tu modelo de negocio. Modelo CANVAS. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-10",
            title = "Elaboración Propuestas de Proyectos de I+D+i",
            subtitle = "Elaboración Propuestas de Proyectos de I+D+i. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-11",
            title = "Planificación, Control y Seguimiento de Proyectos de I+D+i",
            subtitle = "Planificación, Control y Seguimiento de Proyectos de I+D+i. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-12",
            title = "La Sostenibilidad en la Empresa. Aplicación Práctica a las PYMES",
            subtitle = "La Sostenibilidad en la Empresa. Aplicación Práctica a las PYMES. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-13",
            title = "Análisis de posición competitiva",
            subtitle = "Análisis de posición competitiva. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-14",
            title = "Instrumentos de Planificación Estratégica",
            subtitle = "Instrumentos de Planificación Estratégica. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-15",
            title = "Estrategias competitivas básicas",
            subtitle = "Estrategias competitivas básicas. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-16",
            title = "Gestión de procesos de Innovación en la empresa",
            subtitle = "Gestión de procesos de Innovación en la empresa. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-17",
            title = "Producto Mínimo Viable",
            subtitle = "Producto Mínimo Viable. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-18",
            title = "OKR. Objetivos y Resultados Claves",
            subtitle = "OKR. Objetivos y Resultados Claves. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-19",
            title = "Validar la hoja de ruta",
            subtitle = "Validar la hoja de ruta - Customer Validation. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-20",
            title = "Proceso de elaboración de un plan estratégico",
            subtitle = "Proceso de elaboración de un plan estratégico. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-21",
            title = "Escalabilidad Startups",
            subtitle = "Escalabilidad Startups. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-22",
            title = "Metodologia de desarrollo de clientes en la validacion de plan de negocio",
            subtitle = "Metodologia de desarrollo de clientes en la validacion de plan de negocio - Business Design & Customer Discovery. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-23",
            title = "Escalado ágil, Cómo descubrir y usar palancas exponenciales de un modelo de negocio",
            subtitle = "Escalado ágil, Cómo descubrir y usar palancas exponenciales de un modelo de negocio. Área: Estrategias de Negocio.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-24",
            title = "Modelos de contratos entre empresas",
            subtitle = "Modelos de contratos entre empresas. Área: Administración y Aspectos Jurídicos.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-25",
            title = "Fiscalidad y formas jurídicas",
            subtitle = "Fiscalidad y formas jurídicas. Área: Administración y Aspectos Jurídicos.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-direccion-26",
            title = "Propiedad industrial e intelectual",
            subtitle = "Propiedad industrial e intelectual. Área: Administración y Aspectos Jurídicos.",
            category = "🏛️ Dirección & Estrategia",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-finanzas-1",
            title = "Valoración de Empresas",
            subtitle = "M&A, Flujos de Caja y Valoración de Negocios en Marcha",
            category = "💰 Finanzas, Costos & Tesorería",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-finanzas-2",
            title = "Presentación de proyectos empresariales a posibles inversores",
            subtitle = "Presentación de proyectos empresariales a posibles inversores. Área: Finanzas y Financiación.",
            category = "💰 Finanzas, Costos & Tesorería",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-finanzas-3",
            title = "Cultura Economica para Emprendedores",
            subtitle = "Cultura Economica para Emprendedores. Área: Finanzas y Financiación.",
            category = "💰 Finanzas, Costos & Tesorería",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-finanzas-4",
            title = "Análisis Financiero Básico",
            subtitle = "Análisis Financiero Básico. Área: Finanzas y Financiación.",
            category = "💰 Finanzas, Costos & Tesorería",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-finanzas-5",
            title = "El presupuesto de tesorería en la empresa",
            subtitle = "El presupuesto de tesorería en la empresa. Área: Finanzas y Financiación.",
            category = "💰 Finanzas, Costos & Tesorería",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-finanzas-6",
            title = "Cómo solicita financiación un Joven Emprendedor a la Banca",
            subtitle = "Cómo solicita financiación un Joven Emprendedor a la Banca. Área: Finanzas y Financiación.",
            category = "💰 Finanzas, Costos & Tesorería",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-finanzas-7",
            title = "Alternativas de Financiación y sus costes",
            subtitle = "Alternativas de Financiación y sus costes. Área: Finanzas y Financiación.",
            category = "💰 Finanzas, Costos & Tesorería",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-1",
            title = "Herramientas de creatividad aplicada a la empresa",
            subtitle = "Herramientas de creatividad aplicada a la empresa. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-2",
            title = "Marca Personal",
            subtitle = "Marca Personal. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-3",
            title = "Técnicas de Venta para Emprendedores",
            subtitle = "Técnicas de Venta para Emprendedores. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-4",
            title = "Diseñar red comercial y fijar objetivos de ventas",
            subtitle = "Diseñar red comercial y fijar objetivos de ventas. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-5",
            title = "Marca y Posicionamiento",
            subtitle = "Marca y Posicionamiento. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-6",
            title = "Estudios de Mercado",
            subtitle = "Estudios de Mercado. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-7",
            title = "Lanzamiento y promoción de productos innovadores",
            subtitle = "Lanzamiento y promoción de productos innovadores. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-8",
            title = "Fijación de Precios",
            subtitle = "Fijación de Precios. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-9",
            title = "Plan y Presupuesto de Comunicación",
            subtitle = "Plan y Presupuesto de Comunicación. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-10",
            title = "Utilización de las Redes Sociales para Posicionar la Empresa y Vender",
            subtitle = "Utilización de las Redes Sociales para Posicionar la Empresa y Vender. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-11",
            title = "Análisis de Satisfacción del Cliente",
            subtitle = "Análisis de Satisfacción del Cliente. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-12",
            title = "Cómo crear una tienda online",
            subtitle = "Cómo crear una tienda online. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-13",
            title = "Marketing Online y Posicionamiento en Internet",
            subtitle = "Marketing Online y Posicionamiento en Internet. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-14",
            title = "Marketing Sensorial",
            subtitle = "Marketing Sensorial. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-15",
            title = "Métricas para Startups",
            subtitle = "Métricas para Startups. Área: Ventas y Marketing.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-16",
            title = "Plan de Internacionalización de Empresas",
            subtitle = "Plan de Internacionalización de Empresas. Área: Internacionalización.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-17",
            title = "Internacionalizarse desde el origen",
            subtitle = "Internacionalizarse desde el origen. Área: Internacionalización.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-comercial-18",
            title = "Cómo Vender en el Exterior",
            subtitle = "Cómo Vender en el Exterior. Área: Internacionalización.",
            category = "📈 Potencia Comercial & Ventas",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-personas-1",
            title = "Claves para Elaborar un Plan de Igualdad",
            subtitle = "Claves para Elaborar un Plan de Igualdad. Área: Recursos Humanos.",
            category = "👥 Personas, Estructura & Talento",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-personas-2",
            title = "Dirigir eficazmente",
            subtitle = "Dirigir eficazmente. Área: Recursos Humanos.",
            category = "👥 Personas, Estructura & Talento",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-personas-3",
            title = "Definir funciones y estructuras organizativas",
            subtitle = "Definir funciones y estructuras organizativas. Área: Recursos Humanos.",
            category = "👥 Personas, Estructura & Talento",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-personas-4",
            title = "Fidelización de los recursos humanos",
            subtitle = "Fidelización de los recursos humanos. Área: Recursos Humanos.",
            category = "👥 Personas, Estructura & Talento",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-personas-5",
            title = "Reclutar y seleccionar personas",
            subtitle = "Reclutar y seleccionar personas. Área: Recursos Humanos.",
            category = "👥 Personas, Estructura & Talento",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-personas-6",
            title = "Teletrabajo",
            subtitle = "Teletrabajo. Área: Recursos Humanos.",
            category = "👥 Personas, Estructura & Talento",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-tecnologia-1",
            title = "Desarrollo Agil",
            subtitle = "Desarrollo Agil - Scrum. Área: Estrategias de Negocio.",
            category = "💻 Tecnología, Operaciones & Procesos",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-tecnologia-2",
            title = "Metodología SCRUM",
            subtitle = "Metodología SCRUM. Área: Estrategias de Negocio.",
            category = "💻 Tecnología, Operaciones & Procesos",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-tecnologia-3",
            title = "Distribucion en Planta",
            subtitle = "Distribucion en Planta. Área: Producción y Productividad.",
            category = "💻 Tecnología, Operaciones & Procesos",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-tecnologia-4",
            title = "Reingeniería de procesos",
            subtitle = "Reingeniería de procesos. Área: Producción y Productividad.",
            category = "💻 Tecnología, Operaciones & Procesos",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-tecnologia-5",
            title = "Logística y Distribución",
            subtitle = "Logística y Distribución. Área: Producción y Productividad.",
            category = "💻 Tecnología, Operaciones & Procesos",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-tecnologia-6",
            title = "Organización de la Producción",
            subtitle = "Organización de la Producción. Área: Producción y Productividad.",
            category = "💻 Tecnología, Operaciones & Procesos",
            type = "gestion"
        ),
        CdpeManual(
            id = "man-tecnologia-7",
            title = "Guía de Digitalización para Pymes",
            subtitle = "Guía de Digitalización para Pymes. Área: Ventas y Marketing.",
            category = "💻 Tecnología, Operaciones & Procesos",
            type = "gestion"
        ),
        CdpeManual(
            id = "franq-pollo-1",
            title = "Manual Operativo Pollo Ya",
            subtitle = "Fritura a Presión, Empanizado Crocante y Despacho en 90s",
            category = "🍗 Pollo Frito",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-pollo-2",
            title = "Manual Operativo Pinpollo",
            subtitle = "Receta Tradicional, Marinados Criollos y Control de Mermas",
            category = "🍗 Pollo Frito",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-pollo-3",
            title = "Manual Operativo Kung Fu Pollo",
            subtitle = "Fritura Rápida Oriental, Salsas Agridulces y Empaque",
            category = "🍗 Pollo Frito",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-pollo-4",
            title = "Manual Operativo Pollo Supremo",
            subtitle = "Punto de Venta Exprés y Combos de Alto Volumen",
            category = "🍗 Pollo Frito",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-pizza-5",
            title = "Manual Operativo Pizzería Sapore Mío",
            subtitle = "Fermentación Artesanal 48h, Hornos de Piedra y Pomodoro",
            category = "🍕 Pizzerías",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-pizza-6",
            title = "Manual Operativo Roberto's Pizza",
            subtitle = "Pizza Estilo New York, Hornos de Cinta Continua",
            category = "🍕 Pizzerías",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-pizza-7",
            title = "Manual Operativo Robleros Pizza",
            subtitle = "Pizza Familiar Económica y Combos Vecinales",
            category = "🍕 Pizzerías",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-parrilla-8",
            title = "Manual Operativo Sabor del Campo",
            subtitle = "Cortes de Carne a la Parrilla, Carbón y Guarniciones",
            category = "🥩 Parrilladas",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-parrilla-9",
            title = "Manual Operativo Restaurante Supremo",
            subtitle = "Servicio a Mesa, Menú Ejecutivo y Costeo de Platos",
            category = "🥩 Parrilladas",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-mariscos-10",
            title = "Manual Operativo Titán del Mar",
            subtitle = "Cadena de Frío, Fileteado y Coctelería de Mariscos",
            category = "🦐 Marisquería",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-mariscos-11",
            title = "Manual Operativo Cevichería El Puerto",
            subtitle = "Ceviches Criollos, Curiles, Camarones y Salsas Especiales",
            category = "🦐 Marisquería",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-automotriz-12",
            title = "Manual Operativo Autofix Centro Técnico",
            subtitle = "Baremos de Tiempo, Mecánica Rápida y Diagnóstico OBD2",
            category = "🔧 Taller Automotriz",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-ferreteria-13",
            title = "Manual Operativo Ferretería MaxTools",
            subtitle = "Codificación SKU, Surtido Técnico y Venta Consultiva",
            category = "🔨 Ferretería",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-calzado-14",
            title = "Manual Operativo Tienda Qué Tacones",
            subtitle = "Curvas de Tallas, Exhibición y Rotación Estacional",
            category = "👠 Calzado & Zapatería",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-cafeteria-15",
            title = "Manual Operativo Cafetería Montaña Azul",
            subtitle = "Barismo, Calibración de Molinos, Repostería y Despacho",
            category = "☕ Cafetería & Panadería",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-seguridad-16",
            title = "Manual Operativo Guardián Central",
            subtitle = "Protocolos de Custodia, Consignas de Garita y Rondas",
            category = "🛡️ Seguridad Privada",
            type = "franquicia"
        ),
        CdpeManual(
            id = "franq-seguridad-17",
            title = "Manual Operativo Securytec CCTV",
            subtitle = "Cableado Estructurado, Cámaras IP y Monitoreo Remoto",
            category = "🛡️ Seguridad Electrónica",
            type = "franquicia"
        ),
        CdpeManual(
            id = "guia-gastronomia-1",
            title = "Pollo Frito Don Pollo: Cocina y Empaque",
            subtitle = "Fórmula Secreta de Marinado y Fritura a 175°C",
            category = "Guía Secreta (Gastronomia)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-gastronomia-1",
            title = "Producción de Pizza: Masa, Horno y Empaque",
            subtitle = "Fermentación Rústica Sapore Mío y Horno Rápido",
            category = "Guía Secreta (Gastronomia)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-gastronomia-2",
            title = "Cocina Combinada: Pizza + Don Pollo",
            subtitle = "Modelo de Doble Facturación y Renta Compartida",
            category = "Guía Secreta (Gastronomia)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-gastronomia-3",
            title = "Apertura Integral de Franquicia Gastronómica",
            subtitle = "Auditoría Previa, Marcha Blanca y Lanzamiento",
            category = "Guía Secreta (Gastronomia)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-gastronomia-4",
            title = "Empaquetado de Franquicia Gastronómica",
            subtitle = "Licenciamiento, Manuales Maestros y Expansión",
            category = "Guía Secreta (Gastronomia)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-gastronomia-5",
            title = "Comisariato Central & Plantas de Producción",
            subtitle = "Abastecimiento Masivo para Cadenas de 20+ Tiendas",
            category = "Guía Secreta (Gastronomia)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-automotriz-1",
            title = "Operación de Taller Mecánico Exprés",
            subtitle = "Afinación, Cambio de Aceite y Protocolo en Fosa",
            category = "Guía Secreta (Automotriz)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-automotriz-3",
            title = "Taller de Chapa y Pintura Profesional",
            subtitle = "Baremos de Horas, Cabinas Presurizadas y Aseguradoras",
            category = "Guía Secreta (Automotriz)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-automotriz-5",
            title = "Estaciones de Servicio y Gasolineras",
            subtitle = "Inversión en Infraestructura de Alto Capital",
            category = "Guía Secreta (Automotriz)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-retail-2",
            title = "Gestión Comercial de Ferretería de Proximidad",
            subtitle = "Cuentas a Maestros de Obra y Rotación de Mayoreo",
            category = "Guía Secreta (Retail)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-retail-3",
            title = "Buenas Prácticas en Farmacia Comunitaria",
            subtitle = "Control Sanitario, Dispensación y Cuadre de Turno",
            category = "Guía Secreta (Retail)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-retail-5",
            title = "Comercializadora Mayorista e Importaciones",
            subtitle = "Contenedores, Almacenes Centrales y Distribución",
            category = "Guía Secreta (Retail)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-servicios-4",
            title = "Estándares Operativos de Hotel Boutique",
            subtitle = "Protocolos de Recepción, Ama de Llaves y Hospitalidad",
            category = "Guía Secreta (Servicios)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-servicios-4",
            title = "Gestión de Empresa de Desarrollo y Software",
            subtitle = "Estructura Ágil, Scrum y Propiedad Intelectual",
            category = "Guía Secreta (Servicios)",
            type = "guia"
        ),
        CdpeManual(
            id = "guia-servicios-2",
            title = "Logística, Rutas y Flotillas de Transporte",
            subtitle = "Ruteo Inteligente, Rendimiento y Mantenimiento",
            category = "Guía Secreta (Servicios)",
            type = "guia"
        ),
    )

    private fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale("es"))
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .trim()
            .replace("\\s+".toRegex(), " ")
    }

    fun searchFranchises(query: String): List<CdpeFranchise> {
        val norm = normalize(query)
        val tokens = norm.split(" ").filter { it.length > 2 }
        return franchises.filter { f ->
            val fNorm = normalize(f.name + " " + f.category + " " + f.description)
            fNorm.contains(norm) || (tokens.isNotEmpty() && tokens.all { fNorm.contains(it) })
        }
    }

    fun searchManuals(query: String): List<CdpeManual> {
        val norm = normalize(query)
        val tokens = norm.split(" ").filter { it.length > 2 }
        return manuals.filter { m ->
            val mNorm = normalize(m.title + " " + m.subtitle + " " + m.category)
            mNorm.contains(norm) || (tokens.isNotEmpty() && tokens.any { mNorm.contains(it) })
        }
    }

    fun getFranchiseSpokenInfo(query: String): String? {
        val matches = searchFranchises(query)
        if (matches.isEmpty()) return null
        val f = matches.first()
        return "La franquicia de ${f.name} en el CDPE requiere una inversión de ${f.investment}, con un margen operativo del ${f.margin} y un retorno de inversión estimado de ${f.roi}. ${f.description}"
    }

    fun getTopCategoriesSummary(): String {
        val total = franchises.size
        val totalManuals = manuals.size
        return "El CDPE cuenta en su portal oficial (https://cdpe.accion.app/) con $total franquicias estandarizadas en sectores como Gastronomía, Salud, Automotriz, Comercio y Tecnología, y un Campus Virtual con $totalManuals libros, manuales y guías de gestión y operación."
    }

    fun getKnowledgeContextForGemini(): String {
        val sampleFranchises = franchises.take(30).joinToString("\n") {
            "- ${it.name} [Inversión: ${it.investment}, Margen: ${it.margin}, ROI: ${it.roi}]: ${it.description.take(120)}..."
        }
        val sampleManuals = manuals.take(25).joinToString("\n") {
            "- [${it.category}] ${it.title}: ${it.subtitle}"
        }

        return """
CONOCIMIENTO INTEGRAL DEL CDPE Y PORTAL WEB (https://cdpe.accion.app/):
1. MISIÓN Y ESTRUCTURA:
   - Centro de Desarrollo de Pequeñas Empresas (CDPE) y Acción Cooperativa en Guatemala y la región.
   - Director General y Comandante Táctico: Joel.
   - Portal web y campus: https://cdpe.accion.app/. Ofrece estandarización de franquicias, financiamiento cooperativo, asesoría técnica, software POS/KDS e incubación empresarial.
   - Si un empresario ya tiene un negocio exitoso pero es dependiente de su presencia, el CDPE lo audita y lo convierte en Franquiciante oficial para expandirse de 1 a +15 sucursales.

2. CATÁLOGO DE FRANQUICIAS CDPE (${franchises.size} Modelos Estandarizados):
- REGLA ESTRICTA DE PROYECTOS: Únicamente puedes mencionar proyectos y franquicias que estén formalmente dados de alta en el portal oficial del CDPE (https://cdpe.accion.app/). Ningún proyecto externo o no oficial puede estar en la lista.
$sampleFranchises

3. CAMPUS VIRTUAL CDPE (${manuals.size} Libros, Manuales y Guías SG Consulting):
   - 5 Escalones: 0: Diagnóstico, 1: Manuales de Gestión, 2: Guías SG, 3: Laboratorios, 4: Certificación.
   - Bestseller: "De Dueño a Director de Empresa - Transición del Hombre Orquesta a Director de Orquesta".
   - 64 Manuales en 5 Pilares: Dirección, Finanzas, Operaciones, Comercial, Talento Humano.
   - 17 Manuales de Operación de Franquicia (Pollo Ya, Pinpollo, Kung Fu Pollo, Pollo Supremo, Pizzerías Sapore Mío y Roberto's, Asados Sabor del Campo, Cevichería El Puerto, Titán del Mar, MaxTools, Autofix, Qué Tacones, etc.).
$sampleManuals
""".trimIndent()
    }
}
