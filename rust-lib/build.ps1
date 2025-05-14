# 1
# cargo build

# 2 Para todas las arquitecturas
$JniLibsPath = "D:\Android\Lancelot\app\src\main\jniLibs"
$CargoToml = ".\Cargo.toml"
$Architectures = @("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

Write-Host "Compilando para las siguientes arquitecturas: $($Architectures -join ', ')"

# Construir el comando base
$Command = "cargo ndk -o `"$JniLibsPath`" --manifest-path `"$CargoToml`""

# Añadir cada arquitectura como un argumento separado
foreach ($arch in $Architectures) {
    $Command += " -t $arch"
}

# Añadir los argumentos finales
$Command += " build --release"

# Ejecutar el comando
Write-Host "Ejecutando: $Command"
Invoke-Expression $Command

# 3 para generar los bindings a kotlin 