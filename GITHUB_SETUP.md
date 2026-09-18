# Subir a GitHub

1. Crea un repositorio llamado `ZenvoraEvents`.
2. Sube todo el contenido de esta carpeta.
3. En GitHub Actions se ejecutará el workflow de build.
4. Para compilar localmente necesitas Gradle 9.x o generar el wrapper con:
   `gradle wrapper --gradle-version 9.1.0`
5. Después ejecuta:
   `./gradlew build`

El JAR queda en `build/libs/`.
