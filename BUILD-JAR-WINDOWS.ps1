# ReFlash Vulkan v0.9.0: one-click Windows Fabric Loom JAR builder.
# Only downloads signed/hashed public build tools; does not request credentials or touch your Minecraft files.
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$tools = Join-Path $root '.reflash-build-tools'
$log = Join-Path $root 'reflash-build.log'
$targetName = 'ReFlash-Vulkan-0.9.0-ALPHA.jar'
New-Item -ItemType Directory -Path $tools -Force | Out-Null
function Say([string]$msg) { Write-Host ('[ReFlash] ' + $msg) -ForegroundColor Cyan }
function Grab([string]$url, [string]$output) {
    Say ('Downloading ' + $url)
    Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $output -MaximumRedirection 8
}
function CheckSha([string]$file, [string]$expected) {
    $actual=(Get-FileHash -Algorithm SHA256 -Path $file).Hash.ToLowerInvariant()
    if ($actual -ne $expected.Trim().ToLowerInvariant()) { throw "SHA-256 mismatch for $file" }
}
try {
    Say 'Checking Java 21 JDK'
    $javaOk=$false
    try {
        # Java --version prints to stdout; java -version prints to stderr.
        # Under Windows PowerShell 5 and ErrorActionPreference=Stop, -version
        # incorrectly raises NativeCommandError even when Java works.
        $v= (& java --version | Out-String)
        $javaExit=$LASTEXITCODE
        $j= (& javac --version | Out-String)
        $javacExit=$LASTEXITCODE
        $maj=[regex]::Match($v,'(?:openjdk|java)\s+(\d+)').Groups[1].Value
        $compilerMaj=[regex]::Match($j,'javac\s+(\d+)').Groups[1].Value
        if ($javaExit -eq 0 -and $javacExit -eq 0 -and
            $maj -and $compilerMaj -and
            [int]$maj -ge 21 -and [int]$maj -eq [int]$compilerMaj) {
            $javaOk=$true
        }
    } catch { $javaOk=$false }
    if (-not $javaOk) {
        $jdkHome=Join-Path $tools 'jdk21'
        $javac=Get-ChildItem $jdkHome -Filter javac.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $javac) {
            Say 'Installing portable Temurin JDK 21 locally (not system-wide)'
            $uri='https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&heap_size=normal&jvm_impl=hotspot'
            $meta=Invoke-RestMethod -Uri $uri
            if (-not $meta -or -not $meta[0].binary.package.link) { throw 'Unable to locate Temurin JDK download' }
            $pkg=$meta[0].binary.package
            $archive=Join-Path $tools 'jdk21.zip'
            Grab $pkg.link $archive
            CheckSha $archive $pkg.checksum
            Expand-Archive -LiteralPath $archive -DestinationPath $jdkHome -Force
            $javac=Get-ChildItem $jdkHome -Filter javac.exe -Recurse | Select-Object -First 1
            if (-not $javac) { throw 'Downloaded JDK did not contain javac.exe' }
        }
        $env:JAVA_HOME=Split-Path -Parent (Split-Path -Parent $javac.FullName)
        $env:PATH=(Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH
    }
    Say ('Java: ' + ((& java --version | Select-Object -First 1) -join ' '))

    $gradleVersion='9.2.1'
    $gradle=Join-Path $tools ('gradle-' + $gradleVersion + '\bin\gradle.bat')
    if (-not (Test-Path $gradle)) {
        $archive=Join-Path $tools ('gradle-' + $gradleVersion + '-bin.zip')
        $url='https://services.gradle.org/distributions/gradle-' + $gradleVersion + '-bin.zip'
        $hashUrl=$url + '.sha256'
        Grab $url $archive
        $hashFile=$archive+'.sha256'
        Grab $hashUrl $hashFile
        $expected=([IO.File]::ReadAllText($hashFile) -split '\s+')[0]
        CheckSha $archive $expected
        Say 'Extracting Gradle'
        Expand-Archive -LiteralPath $archive -DestinationPath $tools -Force
    }
    if (-not (Test-Path $gradle)) { throw 'Gradle extraction failed' }

    Say 'Running actual Minecraft/Fabric Loom build. First build downloads dependencies.'
    Say ('Build log: ' + $log)
    # Run the Gradle .bat file through cmd.exe so ordinary stderr warnings
    # cannot be mistaken for fatal PowerShell errors. Keep the entire log.
    $command='""' + $gradle + '" --no-daemon clean build --stacktrace > "' + $log + '" 2>&1"'
    & $env:ComSpec /d /s /c $command
    $gradleExit=$LASTEXITCODE
    if (Test-Path $log) {
        Get-Content -Path $log -Tail 20 | ForEach-Object { Write-Host $_ }
    }
    if ($gradleExit -ne 0) { throw ('Gradle failed with code ' + $gradleExit + '. See reflash-build.log.') }

    $jar=Join-Path $root 'build\libs\reflash-0.9.0-vulkan-alpha.jar'
    if (-not (Test-Path $jar)) { throw ('No production JAR at ' + $jar) }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $z=[IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $required=@('fabric.mod.json', 'dev/reflash/client/ReFlashClient.class',
            'dev/reflash/client/ReFlashScreen.class', 'dev/reflash/client/MomentonBridge.class',
            'assets/reflash/textures/gui/studio_landscape.png')
        foreach($item in $required) { if (-not $z.GetEntry($item)) { throw ('Missing JAR file: ' + $item) } }
        $stream=[IO.StreamReader]::new($z.GetEntry('fabric.mod.json').Open())
        try { $meta=$stream.ReadToEnd() | ConvertFrom-Json } finally { $stream.Dispose() }
        if ($meta.id -ne 'reflash' -or $meta.version -ne '0.9.0-vulkan-alpha') { throw 'Unexpected Fabric metadata/version' }
        if (-not $meta.depends.momenton) { throw 'Momenton dependency missing' }
    } finally { $z.Dispose() }
    $target=Join-Path $root $targetName
    Copy-Item $jar $target -Force
    $sha=(Get-FileHash -Algorithm SHA256 $target).Hash
    Set-Content -Path ($target+'.sha256') -Value ($sha+'  '+$targetName)
    Say ('SUCCESS! Production remapped JAR: ' + $target)
    Say ('SHA256: ' + $sha)
    Say 'Install this JAR with Minecraft 1.21.11, Fabric, Fabric API, VulkanMod and Momenton.'
    exit 0
} catch {
    Write-Host ('[ReFlash] BUILD FAILED: ' + $_.Exception.Message) -ForegroundColor Red
    Add-Content -Path $log -Value ("`n[ReFlash BUILD FAILED] " + $_.Exception.ToString())
    Write-Host ('Please share the file reflash-build.log. No JAR has been certified.') -ForegroundColor Yellow
    exit 1
}
