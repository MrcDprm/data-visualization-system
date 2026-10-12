; Data Visualizer - Inno Setup kurulum betiği
; Derleme: önce installer\build.ps1 ile dist\DataVisualizer klasörünü oluştur, sonra bu dosyayı ISCC ile derle.
; Kullanıcı verisi (%APPDATA%\MrcDprm\DataVisualizer) program klasöründe değildir; kaldırınca silinmez.

#define AppName "Data Visualizer"
#define AppVersion "1.0.0"
#define AppPublisher "Miraç Deprem"
#define AppURL "https://github.com/MrcDprm/data-visualization-system"
#define AppExeName "DataVisualizer.exe"

[Setup]
AppId={{7A30421B-0943-424F-A450-82ECE5DD100D}
AppName={#AppName}
AppVersion={#AppVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL={#AppURL}
AppSupportURL={#AppURL}/issues
DefaultDirName={autopf}\DataVisualizer
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
OutputDir=Output
OutputBaseFilename=DataVisualizer-{#AppVersion}-Setup
SetupIconFile=icon.ico
LicenseFile=..\LICENSE
UninstallDisplayIcon={app}\{#AppExeName}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
; .dvz proje dosyaları çift tıklayınca uygulamayla açılsın
ChangesAssociations=yes

[Languages]
Name: "turkish"; MessagesFile: "compiler:Languages\Turkish.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\dist\DataVisualizer\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{app}\{#AppExeName}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; Tasks: desktopicon

[Registry]
; Kullanıcı düzeyinde (HKA: yönetici izni yoksa HKCU) .dvz dosya ilişkilendirmesi; kaldırınca silinir
Root: HKA; Subkey: "Software\Classes\.dvz"; ValueType: string; ValueName: ""; ValueData: "DataVisualizer.Project"; Flags: uninsdeletevalue uninsdeletekeyifempty
Root: HKA; Subkey: "Software\Classes\DataVisualizer.Project"; ValueType: string; ValueName: ""; ValueData: "{#AppName} project"; Flags: uninsdeletekey
Root: HKA; Subkey: "Software\Classes\DataVisualizer.Project\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\{#AppExeName},0"
Root: HKA; Subkey: "Software\Classes\DataVisualizer.Project\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#AppExeName}"" ""%1"""

[Run]
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(AppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
