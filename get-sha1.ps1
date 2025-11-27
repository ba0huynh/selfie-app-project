# PowerShell script to get SHA-1 fingerprint for Android OAuth setup
# Run this script from the project root directory

Write-Host "Getting SHA-1 fingerprint for Android OAuth setup..." -ForegroundColor Green
Write-Host ""

# Default debug keystore path
$debugKeystorePath = "$env:USERPROFILE\.android\debug.keystore"

if (Test-Path $debugKeystorePath) {
    Write-Host "Found debug keystore at: $debugKeystorePath" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "SHA-1 Fingerprint:" -ForegroundColor Cyan
    Write-Host "==================" -ForegroundColor Cyan
    
    # Get SHA-1
    $result = keytool -list -v -keystore $debugKeystorePath -alias androiddebugkey -storepass android -keypass android 2>&1
    
    # Extract SHA-1 line
    $sha1Line = $result | Select-String "SHA1:"
    if ($sha1Line) {
        $sha1 = ($sha1Line -split "SHA1:")[1].Trim()
        Write-Host $sha1 -ForegroundColor White
        Write-Host ""
        Write-Host "Copy this SHA-1 value to Google Cloud Console when creating OAuth credentials." -ForegroundColor Green
        Write-Host ""
        
        # Copy to clipboard
        $sha1 | Set-Clipboard
        Write-Host "SHA-1 has been copied to clipboard!" -ForegroundColor Green
    } else {
        Write-Host "Could not extract SHA-1. Please run manually:" -ForegroundColor Red
        Write-Host "keytool -list -v -keystore `"$debugKeystorePath`" -alias androiddebugkey -storepass android -keypass android" -ForegroundColor Yellow
    }
} else {
    Write-Host "Debug keystore not found at: $debugKeystorePath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please run this command manually:" -ForegroundColor Yellow
    Write-Host "keytool -list -v -keystore `"$debugKeystorePath`" -alias androiddebugkey -storepass android -keypass android" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")


